/*
 * Licensed to Leidos, Inc. under one or more contributor license agreements.  
 * See the NOTICE file distributed with this work for additional information regarding copyright ownership.
 * Leidos, Inc. licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.leidoslabs.holeshot.chipper;

import static com.leidoslabs.holeshot.elt.ELTDisplayExecutor.ExecMode.ASYNCHRONOUS;
import static com.leidoslabs.holeshot.elt.ELTDisplayExecutor.ExecMode.SYNCHRONOUS;
import static org.lwjgl.opengl.GL20.glUseProgram;
import static org.lwjgl.opengl.GL30.GL_DRAW_FRAMEBUFFER;
import static org.lwjgl.opengl.GL30.GL_READ_FRAMEBUFFER;
import static org.lwjgl.opengl.GL30.glBindFramebuffer;

import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Point2D;
import java.io.Closeable;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URL;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import org.apache.commons.collections4.map.LRUMap;
import org.image.common.util.CloseableUtils;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableSet;
import com.leidoslabs.holeshot.elt.ELTDisplayContext;
import com.leidoslabs.holeshot.elt.ELTDisplayExecutor;
import com.leidoslabs.holeshot.elt.LayerManager;
import com.leidoslabs.holeshot.elt.coord.GeodeticELTCoordinate;
import com.leidoslabs.holeshot.elt.coord.ImageELTCoordinate;
import com.leidoslabs.holeshot.elt.coord.ImageWorld;
import com.leidoslabs.holeshot.elt.gpuimage.HistogramType;
import com.leidoslabs.holeshot.elt.imagechain.ImageChain;
import com.leidoslabs.holeshot.elt.imagechain.ImageChain.ImageChainBuilder;
import com.leidoslabs.holeshot.elt.imageop.CumulativeHistogram;
import com.leidoslabs.holeshot.elt.imageop.DRAParameters;
import com.leidoslabs.holeshot.elt.imageop.Histogram;
import com.leidoslabs.holeshot.elt.imageop.ImageOpFactory;
import com.leidoslabs.holeshot.elt.imageop.SummedArea;
import com.leidoslabs.holeshot.elt.imageop.ogl.OglImageOpFactory;
import com.leidoslabs.holeshot.elt.layers.BaseLayer;
import com.leidoslabs.holeshot.elt.tileserver.TileRef;
import com.leidoslabs.holeshot.elt.tileserver.TileserverImage;
import com.leidoslabs.holeshot.elt.utils.GeometryUtils;
import com.leidoslabs.holeshot.imaging.coord.GeodeticCoordinate;
import com.leidoslabs.holeshot.imaging.coord.ImageScale;
import com.leidoslabs.holeshot.imaging.photogrammetry.CameraModel;
import com.leidoslabs.holeshot.imaging.photogrammetry.LocalCartesianCoordinateSystem;
import com.leidoslabs.holeshot.imaging.photogrammetry.rpc.RPCCameraModel;

/**
 * Provides functions to chip an image at some region, and apply SIPs DRA 
 * and retrieve image data at a specified resolution
 */
public class ImageChipper implements Closeable {
   private static final Logger LOGGER = LoggerFactory.getLogger(ImageChipper.class);

   private static final Set<Class> ACTIVE_DRA_IMAGEOPS = ImmutableSet.of(Histogram.class, CumulativeHistogram.class, SummedArea.class, DRAParameters.class);
   private static final int KNOWN_IMAGES_SIZE = 40;

   private static final Dimension HISTOGRAM_VIEWPORT = new Dimension(512, 512);
   private static final Map<String, TileserverImage> KNOWN_IMAGES =
        new LRUMap<String, TileserverImage>(KNOWN_IMAGES_SIZE);

   private final ImageChain imageChain;
   private final ImageWorld imageWorld;
   private final LayerManager layerManager;
   private final ELTDisplayContext eltDisplayContext;
   private final ELTDisplayExecutor eltDisplayExecutor;
   private double defaultHeightOffset;
   private final GraphicsContext gc;
   private static ChipperPool pool;

   /**
    * Initialize the chipper pool
    * @throws IllegalArgumentException
    * @throws Exception
    */
   public static synchronized void initializePool() throws IllegalArgumentException, Exception {
      GraphicsContext gc = GraphicsContext.createGraphicsContext();
      pool = new ChipperPool(gc.getDisplayExecutor());
   }

   
   /**
    * Borrow a ImageChipper instance from ChipperPool, and point it to the desired
    * TileServerImage.
    * @param imageMetadataURL
    * @return ImageChipper instance
    * @throws Exception
    */
   public static ImageChipper borrow(URL imageMetadataURL) throws Exception {
      ImageChipper chipper = null;
      try {
         chipper = pool.borrowObject();
         chipper.setImage(getTileserverImage(imageMetadataURL));
      } catch (Exception e) {
         if (chipper != null) {
            chipper.returnObject();
         }
         throw e;
      }
      return chipper;
   }
   public void returnObject() {
      pool.returnObject(this);
   }

   /**
    * Get a TileserverImage instance from a metadata url. Gets from a Uses a map to
    * cache requests
    * @param imageMetadataURL 
    * @return requested tileserverimage
    * @throws IOException
    */
   private static synchronized TileserverImage getTileserverImage(URL imageMetadataURL) throws IOException {
      final String key = imageMetadataURL.toString();
      TileserverImage result = KNOWN_IMAGES.get(key);
      if (result == null) {
         result = new TileserverImage(imageMetadataURL);
         KNOWN_IMAGES.put(key,  result);
      }
      return result;
   }

   /**
    * Constructor
    * Initializes elt display context/executor, and sets up image chain
    * @throws Exception
    */
   public ImageChipper() throws Exception {
      gc = GraphicsContext.createGraphicsContext();
      eltDisplayContext = new ImageChipperDisplayContext();
      eltDisplayExecutor = gc.getDisplayExecutor();
      eltDisplayExecutor.submit(SYNCHRONOUS, ()->gc.init());

      imageWorld = new ImageWorld();
      layerManager = new LayerManager();

      ImageOpFactory imageOpFactory = new OglImageOpFactory();
      imageChain = new ImageChainBuilder(imageOpFactory, imageWorld, eltDisplayContext)
            .rawImage(false)
            .histogram(HistogramType.RGB)
            .summedArea()
            .cumulativeHistogram()
            .draParameters(false)
            .dynamicRangeAdjust()
            .toneTransferCurve()
            .build();

      final BaseLayer baseLayer = new BaseLayer(imageChain);
      layerManager.setBaseLayer(baseLayer);

      defaultHeightOffset = 0.0;
   }

   private void setImage(TileserverImage tileserverImage) throws InterruptedException, ExecutionException {
      final TileRef topTile = tileserverImage.getTopTile();
      layerManager.getBaseLayer().setImage(tileserverImage);
      imageWorld.setTopTile(topTile);
      final CameraModel cameraModel = tileserverImage.getCameraModel();
      if (cameraModel instanceof RPCCameraModel) {
         final RPCCameraModel rpcCameraModel = (RPCCameraModel)cameraModel;
         defaultHeightOffset = rpcCameraModel.getNormalization().getHtOff();
      } else {
         defaultHeightOffset = 0.0;
      }
      setLockHistogram(false);
      waitForFullyRendered();
   }

   @Override
   /**
    * Close resources used by this chipper (i.e, LayerManager and it's dependencies)
    * by executing an asynchronous task in display context
    */
   public void close() throws IOException {
      try {
         eltDisplayContext.asyncExec(()-> {
            try {
               // Closing the layerManager will close the image chain as well.
               CloseableUtils.close(layerManager, gc);
            } catch (IOException e) {
               LOGGER.error(e.getMessage(), e);
            }
         });
      } catch (InterruptedException | ExecutionException e) {
         LOGGER.error(e.getMessage(), e);
      }
   }

   /**
    * Display context for ImageChipper, sends tasks to chipper's ETLDisplayExecutor
    */
   class ImageChipperDisplayContext extends ELTDisplayContext {
      public ImageChipperDisplayContext() {
         super();
      }
      @Override
      public void asyncExec(Runnable runnable) throws InterruptedException, ExecutionException {
         eltDisplayExecutor.submit(ASYNCHRONOUS, runnable);
      }

      @Override
      public void syncExec(Runnable runnable) throws InterruptedException, ExecutionException {
         eltDisplayExecutor.submit(SYNCHRONOUS, runnable);
      }

      @Override
      protected void setOpenGLContextCurrent() {
      }
   }

   private TileserverImage getImage() {
      return getTopTile().getImage();
   }
   private TileRef getTopTile() {
      return imageWorld.getTopTile();
   }

   private synchronized void waitForFullyRendered() throws InterruptedException, ExecutionException {
      do {
         eltDisplayExecutor.submit(SYNCHRONOUS, () -> {
            try {
               eltDisplayContext.setContextThread();
               layerManager.drawAll(imageWorld);
               gc.swapBuffers();
            } finally {
               glUseProgram(0);
               glBindFramebuffer(GL_READ_FRAMEBUFFER, 0);
               glBindFramebuffer(GL_DRAW_FRAMEBUFFER, 0);

            }
         });
      } while (!layerManager.isFullyRendered());
   }

   /**
    * Get image chip from a geographic center and radius at a specified resolution
    * @param chipGeoCenter Geographic coordinates of desired chip's center
    * @param radiusInMeters radius of chip in meters
    * @param chipOutputDimension output resolution of chip
    * @param outputStream output stream to write image data
    * @param warpToFit warp world space to fit bounding box in image space?
    * @param lockHistogramToOverview lock histogram to a common viewport size
    * @throws InterruptedException
    * @throws ExecutionException
    * @throws NoninvertibleTransformException
    * @throws CloneNotSupportedException
    * @throws IllegalArgumentException
    */
   public void chipByGeo(Coordinate chipGeoCenter, double radiusInMeters, Dimension chipOutputDimension, OutputStream outputStream, boolean warpToFit, boolean lockHistogramToOverview) throws InterruptedException, ExecutionException, NoninvertibleTransformException, CloneNotSupportedException, IllegalArgumentException {
      chipGeoCenter.z = defaultHeightOffset;
      final LocalCartesianCoordinateSystem crs = new LocalCartesianCoordinateSystem(chipGeoCenter);

      final double chipWidth = chipOutputDimension.getWidth();
      final double chipHeight = chipOutputDimension.getHeight();
      final double minDimension = Math.min(chipWidth, chipHeight);
      final double maxDimension = Math.max(chipWidth, chipHeight);
      final double chipShortSide = radiusInMeters;
      final double chipLongSide = radiusInMeters * (maxDimension / minDimension);
      final double xRadiusInMeters = (chipWidth == minDimension) ? chipShortSide : chipLongSide;
      final double yRadiusInMeters = (chipHeight == minDimension) ? chipShortSide : chipLongSide;

      final Coordinate northPointLocal = new Coordinate(0.0, yRadiusInMeters, 0.0);
      final Coordinate southPointLocal = new Coordinate(0.0, -yRadiusInMeters, 0.0);
      final Coordinate eastPointLocal = new Coordinate(xRadiusInMeters, 0.0, 0.0);
      final Coordinate westPointLocal = new Coordinate(-xRadiusInMeters, 0.0, 0.0);

      final Coordinate northPointGlobal = crs.localToGlobal(northPointLocal);
      final Coordinate southPointGlobal = crs.localToGlobal(southPointLocal);
      final Coordinate eastPointGlobal = crs.localToGlobal(eastPointLocal);
      final Coordinate westPointGlobal = crs.localToGlobal(westPointLocal);
      Envelope bboxEnvelope = new Envelope(westPointGlobal.x, eastPointGlobal.x, southPointGlobal.y, northPointGlobal.y);
      chipByGeo(bboxEnvelope, chipOutputDimension, outputStream, warpToFit, lockHistogramToOverview);
   }

   
   /**
    * Get image chip from a geographic Envelope at a specified resolution
    * @param bboxEnvelope Geographic envelope (bounding box)
    * @param chipOutputDimension chips's output resolution
    * @param outputStream output stream to write image chip
    * @param warpToFit warp world space to fit bounding box in image space?
    * @param lockHistogramToOverview lock histograms to a common viewport size
    * @throws InterruptedException
    * @throws ExecutionException
    * @throws NoninvertibleTransformException
    * @throws CloneNotSupportedException
    * @throws IllegalArgumentException
    */
   public void chipByGeo(Envelope bboxEnvelope, Dimension chipOutputDimension, OutputStream outputStream, boolean warpToFit, boolean lockHistogramToOverview) throws InterruptedException, ExecutionException, NoninvertibleTransformException, CloneNotSupportedException, IllegalArgumentException {
      if (!imageIntersectsGeo(bboxEnvelope)) {
         throw new IllegalArgumentException("bboxEnvelope doesn't intersect image footprint");
      }
      handleHistogramLock(lockHistogramToOverview);

      final Coordinate[] bboxGeoPoints = new Coordinate[] {
            new Coordinate(bboxEnvelope.getMinX(), bboxEnvelope.getMinY(), 0.0),
            new Coordinate(bboxEnvelope.getMaxX(), bboxEnvelope.getMinY(), 0.0),
            new Coordinate(bboxEnvelope.getMaxX(), bboxEnvelope.getMaxY(), 0.0),
            new Coordinate(bboxEnvelope.getMinX(), bboxEnvelope.getMaxY(), 0.0)
      };
      final Coordinate[] bboxImagePoints =
            Arrays.stream(bboxGeoPoints)
            .map(c->new GeodeticELTCoordinate(imageWorld, c, ImageScale.forRset(0)))
            .map(g->g.getR0ImageCoordinate())
            .map(p -> GeometryUtils.toCoordinate(p))
            .toArray(Coordinate[]::new);

      double tileWidth = bboxImagePoints[0].distance(bboxImagePoints[1]);
      double tileHeight = bboxImagePoints[0].distance(bboxImagePoints[3]);

      final double scaleX = chipOutputDimension.getWidth() / tileWidth;
      final double scaleY = chipOutputDimension.getHeight() / tileHeight;
      ImageScale imageScale = new ImageScale(scaleX, scaleY);

      imageWorld.setImageScale(imageScale);
      imageWorld.reshapeViewport(0, 0, chipOutputDimension.width, chipOutputDimension.height);

      imageWorld.setGeodeticViewport(bboxGeoPoints[0], bboxGeoPoints[1], bboxGeoPoints[2], bboxGeoPoints[3]);

      waitForFullyRendered();

      eltDisplayExecutor.submit(SYNCHRONOUS, ()->layerManager.dumpImage(outputStream, chipOutputDimension, warpToFit));
   }

   /**
    * Chip image by image space rectangle at a specified resolution
    * @param chipRegion imagespace bounds to chip image
    * @param chipOutputDimension chips's output resolution
    * @param outputStream output stream to write image chip
    * @param warpToFit warp world space to fit bounding box in image space?
    * @param lockHistogramToOverview
    * @throws InterruptedException
    * @throws ExecutionException
    */
   public void chip(Rectangle chipRegion, Dimension chipOutputDimension, OutputStream outputStream, boolean warpToFit, boolean lockHistogramToOverview) throws InterruptedException, ExecutionException {
      if (!imageIntersectsImage(chipRegion)) {
         throw new IllegalArgumentException("chipRegion doesn't intersect image footprint");
      }
      handleHistogramLock(lockHistogramToOverview);

      imageWorld.setOrthoViewport();
      imageWorld.reshapeViewport(0, 0, chipOutputDimension.width, chipOutputDimension.height);
      imageWorld.setRotate(0.0);

      final Envelope viewport = imageWorld.getCurrentViewport();

      final double scale = Math.min(chipRegion.getWidth() / viewport.getWidth(),
            chipRegion.getHeight() / viewport.getHeight());

      final ImageScale imageScale = new ImageScale(scale, scale);
      imageWorld.setImageScale(imageScale);

      Point2D chipCenter = new Point2D.Double(chipRegion.getMinX(),
            chipRegion.getMinY());
      chipCenter = chipRegion.getLocation();
      ImageELTCoordinate ULImage = new ImageELTCoordinate(imageWorld, chipCenter, ImageScale.forRset(0));
      imageWorld.zoomToUL(ULImage, imageScale);

      waitForFullyRendered();
      eltDisplayExecutor.submit(SYNCHRONOUS, ()->layerManager.dumpImage(outputStream, chipOutputDimension, warpToFit));
   }

   private boolean imageIntersectsGeo(Envelope geoRegion) {
      return geoRegion != null && this.getTopTile().getGeodeticBounds().getEnvelopeInternal().intersects(geoRegion);
   }
   private boolean imageIntersectsImage(Rectangle imageRegion) {
      return imageRegion != null && getImage().getR0ImageRectangle().intersects(imageRegion);
   }
   private void setLockHistogram(boolean lock) throws InterruptedException, ExecutionException {
      imageChain.getImageOps().stream()
      .filter(i->ACTIVE_DRA_IMAGEOPS.stream().anyMatch(ai->ai.isInstance(i)))
      .forEach(i->i.setRenderingEnabled(!lock));
   }
   private boolean isHistogramLocked() {
      return imageChain.getImageOps().stream()
            .filter(i->ACTIVE_DRA_IMAGEOPS.stream().anyMatch(ai->ai.isInstance(i)))
            .anyMatch(i->!i.isRenderingEnabled());
   }
   private void handleHistogramLock(boolean lockHistogramToOverview) throws InterruptedException, ExecutionException {
      final boolean histogramLocked = isHistogramLocked();
      if (lockHistogramToOverview != histogramLocked) {
         if (!histogramLocked) {
            // We need to reset to a common viewport size so that the histogram will
            // be consistent across chip requests of different sizes.
            imageWorld.setOrthoViewport();
            imageWorld.reshapeViewport(0, 0, HISTOGRAM_VIEWPORT.width, HISTOGRAM_VIEWPORT.height);

            final TileRef topTile = imageWorld.getTopTile();
            final Coordinate tileCenter = topTile.getGeodeticBounds().getCentroid().getCoordinate();
            tileCenter.z = defaultHeightOffset;
            final GeodeticELTCoordinate tileCenterGeo = new GeodeticELTCoordinate(imageWorld, tileCenter, ImageScale.forRset(topTile.getRset()));
            imageWorld.setRotate(0.0);
            imageWorld.zoomTo(tileCenterGeo, ImageScale.forRset(topTile.getRset()));
            waitForFullyRendered();
         }
         setLockHistogram(lockHistogramToOverview);
      }
   }
}

