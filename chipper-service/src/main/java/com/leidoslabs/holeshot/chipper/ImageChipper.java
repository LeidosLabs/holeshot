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
import java.io.Closeable;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URL;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import org.apache.commons.collections4.map.LRUMap;
import org.image.common.util.CloseableUtils;
import org.joml.Vector2d;
import org.joml.Vector2dc;
import org.joml.Vector2i;
import org.joml.Vector2ic;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.opengis.util.FactoryException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.leidoslabs.holeshot.elt.ELTDisplayContext;
import com.leidoslabs.holeshot.elt.ELTDisplayExecutor;
import com.leidoslabs.holeshot.elt.ELTDisplayExecutor.ExecMode;
import com.leidoslabs.holeshot.elt.ELTFrame;
import com.leidoslabs.holeshot.elt.Interpolation;
import com.leidoslabs.holeshot.elt.LayerManager;
import com.leidoslabs.holeshot.elt.coord.GeodeticELTCoordinate;
import com.leidoslabs.holeshot.elt.coord.ImageELTCoordinate;
import com.leidoslabs.holeshot.elt.coord.MapScale;
import com.leidoslabs.holeshot.elt.coord.Radians;
import com.leidoslabs.holeshot.elt.imageop.ogl.OglImageOpFactory;
import com.leidoslabs.holeshot.elt.layers.BaseLayer;
import com.leidoslabs.holeshot.elt.tileserver.TileserverImage;
import com.leidoslabs.holeshot.elt.utils.GeometryUtils;
import com.leidoslabs.holeshot.elt.viewport.ImageWorld;
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

	private static final int KNOWN_IMAGES_SIZE = 40;

	private static final Vector2ic HISTOGRAM_VIEWPORT = new Vector2i(512, 512);
	private static final Map<String, TileserverImage> KNOWN_IMAGES =
			new LRUMap<String, TileserverImage>(KNOWN_IMAGES_SIZE);

	private final ImageWorld imageWorld;
	private final LayerManager layerManager;
	private double defaultHeightOffset;
	private final GraphicsContext gc;
	private static volatile ChipperPool pool;

	/**
	 * Initialize the chipper pool
	 * @throws IllegalArgumentException
	 * @throws Exception
	 */
	public static synchronized void initializePool() throws IllegalArgumentException, Exception {
		pool = new ChipperPool();
	}


	/**
	 * If our pool gets corrupted (e.g we fail to invalidate a corrupted object),
	 * we attempt to recreate a chipper pool.
	 * @throws IllegalArgumentException
	 * @throws Exception
	 */
	public static synchronized void recreatePool() throws IllegalArgumentException, Exception {
		if (!pool.isClosed()) {
			pool.close();
		}
		initializePool();
	}

	/**
	 * Invalidate an ImageChipper in the pool, and create a new chipper in its place.
	 * Calling methods should make sure chipper is not subsequently returned to pool. 
	 * @param chipper
	 * @throws IllegalStateException chipper not in pool
	 * @throws Exception Something went wrong when invalidating/recreating chipper
	 */
	public static synchronized void recreate(ImageChipper chipper) throws IllegalStateException, Exception {
		pool.invalidateObject(chipper);
		pool.addObject();
	}


	/**
	 * Borrow a ImageChipper instance from ChipperPool, and point it to the desired
	 * TileServerImage.
	 * @param imageMetadataURL
	 * @return ImageChipper instance
	 * @throws Exception
	 */
	public static ImageChipper borrow(TileserverImage img) throws Exception {
		ImageChipper chipper = null;

		try {
			// borrow should be mutually exclusive with recreatePool() and recreate()
			synchronized (ImageChipper.class) {
				chipper = pool.borrowObject(); 
			}
			chipper.setImage(img);
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
	public static synchronized TileserverImage getTileserverImage(URL imageMetadataURL) throws IOException {
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
		ELTDisplayExecutor.getInstance().submit(SYNCHRONOUS, gc.getELTDisplayContext(), ()->gc.init());

		imageWorld = new ImageWorld(new Vector2i(ELTFrame.DEFAULT_WIDTH, ELTFrame.DEFAULT_HEIGHT));
		layerManager = new LayerManager();

		final BaseLayer baseLayer = new BaseLayer(imageWorld, gc.getELTDisplayContext(), new OglImageOpFactory(), false);
		baseLayer.setInterpolation(Interpolation.CATMULL);
		layerManager.setBaseLayer(baseLayer);

		defaultHeightOffset = 0.0;
	}

	private void setImage(TileserverImage tileserverImage) throws InterruptedException, ExecutionException, IOException, FactoryException {
		ELTDisplayExecutor.getInstance().submit(SYNCHRONOUS, gc.getELTDisplayContext(), ()-> {
			try {
				TileserverImage image = getImage();
				if (tileserverImage == null) {
					layerManager.getBaseLayer().clearAllImages();
				} else if (!tileserverImage.equals(image)) {
					layerManager.getBaseLayer().clearAllImages();
					layerManager.getBaseLayer().addImage(tileserverImage);
					imageWorld.setProjection(tileserverImage);
					final CameraModel cameraModel = tileserverImage.getCameraModel();
					if (cameraModel instanceof RPCCameraModel) {
						final RPCCameraModel rpcCameraModel = (RPCCameraModel)cameraModel;
						defaultHeightOffset = rpcCameraModel.getNormalization().getHtOff();
					} else {
						defaultHeightOffset = 0.0;
					}
					setLockHistogram(false);
				}
			} catch (InterruptedException | ExecutionException | FactoryException | IOException e) {
				LOGGER.error("Error setting image", e);
			}
		});
	}

	@Override
	/**
	 * Close resources used by this chipper (i.e, LayerManager and it's dependencies)
	 * by executing an asynchronous task in display context
	 */
	public void close() throws IOException {
		try {
			ELTDisplayExecutor.getInstance().submit(ExecMode.ASYNCHRONOUS, gc.getELTDisplayContext(), ()-> {
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

	private TileserverImage getImage() {
		TileserverImage[] images = layerManager.getBaseLayer().getImages();
		return (images != null && images.length > 0) ? images[0] : null;
	}

	private synchronized void waitForFullyRendered() throws InterruptedException, ExecutionException {
		layerManager.resetFullyRendered();
		do {
			ELTDisplayExecutor.getInstance().submit(ExecMode.SYNCHRONOUS, gc.getELTDisplayContext(), ()-> {
				try {
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
	 * @throws FactoryException 
	 */
	public void chipByGeo(Coordinate chipGeoCenter, double radiusInMeters, Dimension chipOutputDimension, OutputStream outputStream, boolean lockHistogramToOverview) throws InterruptedException, ExecutionException, NoninvertibleTransformException, CloneNotSupportedException, IllegalArgumentException, FactoryException {
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
		chipByGeo(bboxEnvelope, chipOutputDimension, outputStream, lockHistogramToOverview);
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
	 * @throws FactoryException 
	 */
	public void chipByGeo(Envelope bboxEnvelope, Dimension chipOutputDimension, OutputStream outputStream, boolean lockHistogramToOverview) throws InterruptedException, ExecutionException, NoninvertibleTransformException, CloneNotSupportedException, IllegalArgumentException, FactoryException {
		if (!imageIntersectsGeo(bboxEnvelope)) {
			throw new IllegalArgumentException("bboxEnvelope doesn't intersect image footprint");
		}

		handleHistogramLock(lockHistogramToOverview);

		imageWorld.setScreenSize(new Vector2i(chipOutputDimension.width, chipOutputDimension.height));

		final Coordinate[] bboxGeoPoints = new Coordinate[] {
				new Coordinate(bboxEnvelope.getMinX(), bboxEnvelope.getMinY(), 0.0),
				new Coordinate(bboxEnvelope.getMaxX(), bboxEnvelope.getMinY(), 0.0),
				new Coordinate(bboxEnvelope.getMaxX(), bboxEnvelope.getMaxY(), 0.0),
				new Coordinate(bboxEnvelope.getMinX(), bboxEnvelope.getMaxY(), 0.0)
		};

		imageWorld.setGeodeticViewport(getImage(), bboxGeoPoints[0], bboxGeoPoints[1], bboxGeoPoints[2], bboxGeoPoints[3]);

		waitForFullyRendered();

		ELTDisplayExecutor.getInstance().submit(SYNCHRONOUS, gc.getELTDisplayContext(), ()->layerManager.dumpImage(outputStream, chipOutputDimension));
	}

	/**
	 * Chip image by image space rectangle at a specified resolution
	 * @param chipRegion imagespace bounds to chip image
	 * @param chipOutputDimension chips's output resolution
	 * @param outputStream output stream to write image chip
	 * @param lockHistogramToOverview
	 * @throws InterruptedException
	 * @throws ExecutionException
	 * @throws FactoryException 
	 */
	public void chip(Rectangle chipRegion, Dimension chipOutputDimension, OutputStream outputStream, boolean lockHistogramToOverview) throws InterruptedException, ExecutionException, FactoryException {
		if (!imageIntersectsImage(chipRegion)) {
			throw new IllegalArgumentException("chipRegion doesn't intersect image footprint");
		}

		// Must call setScreenSize AFTER handleHistogramLock
		handleHistogramLock(lockHistogramToOverview);

		final Vector2ic newScreenSize = new Vector2i(chipOutputDimension.width, chipOutputDimension.height);
		imageWorld.clearGeodeticViewport();
		imageWorld.rotateTo(new Radians(0.0));
		imageWorld.setScreenSize(new Vector2i(newScreenSize));


		final double scaleX = newScreenSize.x() / chipRegion.getWidth() ;
		final double scaleY = newScreenSize.y() / chipRegion.getHeight();
		ImageScale imageScale = new ImageScale(scaleX, scaleY);
		final double rset = Math.max(imageScale.getRsetForScaleX(), imageScale.getRsetForScaleY());

		Vector2dc chipUL = new Vector2d(chipRegion.getMinX(), chipRegion.getMinY());
		Vector2dc chipCenter = new Vector2d(chipRegion.getCenterX(), chipRegion.getCenterY());
		final TileserverImage image = getImage();
		Coordinate geoCenter = image.getCameraModel().imageToWorld(GeometryUtils.toPoint2D(chipCenter));
		MapScale mapScale = imageWorld.getMapScale(image, geoCenter, rset);
		ImageELTCoordinate ULImage = new ImageELTCoordinate(image, imageWorld, GeometryUtils.toPoint2D(chipUL), ImageScale.forRset(0.0));

		imageWorld.zoomToUL(ULImage, mapScale);

		waitForFullyRendered();
		ELTDisplayExecutor.getInstance().submit(SYNCHRONOUS, gc.getELTDisplayContext(), ()->layerManager.dumpImage(outputStream, chipOutputDimension));
	}

	private boolean imageIntersectsGeo(Envelope geoRegion) {
		return geoRegion != null && getImage().getGeodeticBounds().getEnvelopeInternal().intersects(geoRegion);
	}
	private boolean imageIntersectsImage(Rectangle imageRegion) {
		return imageRegion != null && getImage().getR0ImageRectangle().intersects(imageRegion);
	}
	private void setLockHistogram(boolean lock) throws InterruptedException, ExecutionException {
		layerManager.getBaseLayer().enableDRA(!lock);
	}
	private boolean isHistogramLocked() {
		return !layerManager.getBaseLayer().isDRAEnabled();
	}
	private void handleHistogramLock(boolean lockHistogramToOverview) throws InterruptedException, ExecutionException {
		final boolean histogramLocked = isHistogramLocked();
		if (lockHistogramToOverview != histogramLocked) {
			if (!histogramLocked) {
				// We need to reset to a common viewport size so that the histogram will
				// be consistent across chip requests of different sizes.
				imageWorld.clearGeodeticViewport();
				imageWorld.setScreenSize(HISTOGRAM_VIEWPORT);

				TileserverImage image = getImage();

				final Coordinate tileCenter = image.getGeodeticBounds().getCentroid().getCoordinate();
				tileCenter.z = defaultHeightOffset;
				imageWorld.rotateTo(new Radians(0.0));
				final GeodeticELTCoordinate eltCenter = new GeodeticELTCoordinate(imageWorld, tileCenter);
				imageWorld.zoomToImage(image, eltCenter, image.getMaxRLevel());

				waitForFullyRendered();
			}
			setLockHistogram(lockHistogramToOverview);
		}
	}
}

