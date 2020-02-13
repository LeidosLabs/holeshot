/*
 * Licensed to The Leidos Corporation under one or more contributor license agreements.
 * See the NOTICE file distributed with this work for additional information regarding copyright ownership.
 * The Leidos Corporation licenses this file to You under the Apache License, Version 2.0
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
package com.leidoslabs.holeshot.ingest;

import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.awt.image.SampleModel;
import java.awt.image.renderable.ParameterBlock;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.media.jai.BorderExtender;
import javax.media.jai.ImageLayout;
import javax.media.jai.Interpolation;
import javax.media.jai.JAI;
import javax.media.jai.ParameterBlockJAI;
import javax.media.jai.PlanarImage;
import javax.media.jai.RenderedImageAdapter;
import javax.media.jai.RenderedOp;
import javax.media.jai.TileCache;
import javax.media.jai.util.ImagingListener;

import org.codice.imaging.nitf.core.image.ImageSegment;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.leidoslabs.holeshot.imaging.DefaultImageSourceFactory;
import com.leidoslabs.holeshot.imaging.ImageKey;
import com.leidoslabs.holeshot.imaging.ImageSource;
import com.leidoslabs.holeshot.imaging.ImageSourceSegment;
import com.leidoslabs.holeshot.imaging.geotiff.GeoTIFFImageSourceSegment;
import com.leidoslabs.holeshot.imaging.geotiff.GeoTIFFMetadataSerializer;
import com.leidoslabs.holeshot.imaging.geotiff.RPCBTreSerializer;
import com.leidoslabs.holeshot.imaging.io.CheckedSupplier;
import com.leidoslabs.holeshot.imaging.metadata.TilePyramidDescriptor;
import com.leidoslabs.holeshot.imaging.nitf.ImageSegmentMetadataSerializer;
import com.leidoslabs.holeshot.imaging.photogrammetry.CameraModel;
import com.leidoslabs.holeshot.imaging.photogrammetry.rpc.RPCBTre;
import com.sun.media.jai.util.JDKWorkarounds;

import it.geosolutions.jaiext.JAIExt;

/**
 * Create a standard tile pyramid builder that can build a tile pyramids from files with one image (not mosaics)
 */
public class TilePyramidBuilder {
   static {
      JAIExt.initJAIEXT();
   }

   private static final Logger LOGGER = LoggerFactory.getLogger(TilePyramidBuilder.class);

   private static final int TILE_SIZE = 512;
   private ExecutorService executor = Executors.newFixedThreadPool(Math.max(1, Runtime.getRuntime().availableProcessors() - 1));
   private ObjectMapper mapper;
   private boolean metadataOnly;
   private RenderedImage image;

   private final DefaultImageSourceFactory imageSourceFactory;

   public TilePyramidBuilder() {
      this(false);
   }

    /**
     * Initialize a builder for single image NITF and GeoTIFF files
     * @param metadataOnly flag if only metadata should be generated with no tiling performed
     */
   public TilePyramidBuilder(boolean metadataOnly) {
      this.metadataOnly = metadataOnly;
      this.imageSourceFactory = new DefaultImageSourceFactory();
      mapper = new ObjectMapper();
      //TODO: Define these modules elsewhere...
      // ADD SERIALIZERS TO ObjectMapper
      SimpleModule nitfMetadataModule =
            new SimpleModule("NITFMetadataModule", new Version(1, 0, 0, null));
      nitfMetadataModule.addSerializer(ImageSegment.class, new ImageSegmentMetadataSerializer());
      mapper.registerModule(nitfMetadataModule);

      SimpleModule geoTIFFMetadataModule =
            new SimpleModule("GeoTIFFMetadataModule", new Version(1, 0, 0, null));
      geoTIFFMetadataModule.addSerializer(GeoTIFFImageSourceSegment.class, new GeoTIFFMetadataSerializer());
      geoTIFFMetadataModule.addSerializer(RPCBTre.class, new RPCBTreSerializer());
      mapper.registerModule(geoTIFFMetadataModule);
   }

    /**
     * Create the tile pyramid from a single image and publish the tiles
     * @param inputStreamSupplier A CheckedSupplier containing an InputStream to the image data that will by tiled
     * @param callback The class to perform publication of the generated tiles and metadata to a specific backing store
     * @param fallbackImageKey ImageKey to use if one can't be recovered from the stream
     * @throws TilePyramidException on any failure that occurs during building
     */
   public void buildTilePyramid(CheckedSupplier<InputStream, IOException> inputStreamSupplier, TilePyramidListener callback, ImageKey fallbackImageKey) throws TilePyramidException {
      try (ImageSource imageSource = imageSourceFactory.getImageSource(inputStreamSupplier, fallbackImageKey)) {
         List<? extends ImageSourceSegment> imageSourceSegments = imageSource.getSegments();

         for (ImageSourceSegment imageSegment: imageSourceSegments) {
            // TODO: Add TileCache here
            RenderedImage renderedImage = imageSegment.getRenderedImage();
            int w = renderedImage.getWidth();
            int h = renderedImage.getHeight();

            LOGGER.info("Original Image Segment Dimensions: {}x{}", w, h);

            // Set Metadata
            final CameraModel cameraModel = imageSegment.getCameraModel();

            Coordinate[] boundaryCoords = new Coordinate[5];
            boundaryCoords[0] = cameraModel.imageToWorld(new Point2D.Double(0, 0));
            boundaryCoords[1] = cameraModel.imageToWorld(new Point2D.Double(w, 0));
            boundaryCoords[2] = cameraModel.imageToWorld(new Point2D.Double(w, h));
            boundaryCoords[3] = cameraModel.imageToWorld(new Point2D.Double(0, h));
            boundaryCoords[4] = boundaryCoords[0];
            GeometryFactory gf = new GeometryFactory();

            TilePyramidDescriptor metadata = new TilePyramidDescriptor();
            metadata.setTileWidth(TILE_SIZE);
            metadata.setTileHeight(TILE_SIZE);
            metadata.setWidth(w);
            metadata.setHeight(h);
            metadata.setBounds(gf.createPolygon(boundaryCoords));
            metadata.setName(imageSegment.getImageKey().getName());

            Object segmentMetadataObject = imageSegment.getSegmentMetadataObject();
            if (segmentMetadataObject != null) {
               JsonNode segmentMetadata = mapper.valueToTree(segmentMetadataObject);
               metadata.setMetadata(segmentMetadata);
            }

            // TODO: Allow for a producer code on this GUIDE
            // TODO: Fix this, input to UUID should really be a hashed version of the namespace and string
            metadata.setIdentifier("guide://000000/" + UUID.nameUUIDFromBytes(("IMAGE:" + metadata.getName()).getBytes()));

            final double maxDim = Math.max(w,h);
            final int maxRLevel = Math.max((int)Math.ceil(Math.log(maxDim/((double)TILE_SIZE)) / Math.log(2.0)), 0);

            metadata.setMinRLevel(0);
            metadata.setMaxRLevel(maxRLevel);

            // Write Metadata
            final ImageKey imageKey = imageSegment.getImageKey();
            callback.handleMetadata(imageKey, metadata);

            if (!metadataOnly) {
               TileCache tileCache = JAI.getDefaultInstance().getTileCache();
               long tileCacheSize = (long) (0.6 * Runtime.getRuntime().maxMemory());
               LOGGER.info("Limiting tile cache to 60 percent of maximum memory: {}", tileCacheSize);
               tileCache.setMemoryCapacity(tileCacheSize);

               // Get RenderedImage
//               final SampleModel sampleModel = renderedImage.getSampleModel();
//               final ColorModel colorModel = renderedImage.getColorModel();
//               final boolean compatible =
//                     JDKWorkarounds.areCompatibleDataModels(sampleModel, colorModel);

               // We retile images to protect against exceptionally large image tiles blowing out our memory
               final RenderedOp retiledImage = retile(renderedImage, TILE_SIZE, TILE_SIZE);
               PlanarImage scaled = retiledImage;

               for (int i=0;i<=maxRLevel;++i) {
                  outputTiles(callback, imageKey, i, scaled);

                  if (i<maxRLevel) {
                     scaled = scale(scaled);
                  }
               }
               tileCache.flush();
               
               callback.handleMRF(imageKey);
            }
         }
      } catch (Exception e) {
         throw new TilePyramidException(e);
      }
   }

   private static RenderedOp scale(PlanarImage image) {
      ParameterBlock scaleParams = new ParameterBlock();
      scaleParams.addSource(image);
      scaleParams.add(0.5f).add(0.5f).add(0.0f).add(0.0f);
      scaleParams.add(Interpolation.getInstance(Interpolation.INTERP_BICUBIC_2));

      // Quality related hints when scaling the image
      RenderingHints scalingHints = new RenderingHints(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
      scalingHints.put(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
      scalingHints.put(JAI.KEY_BORDER_EXTENDER, BorderExtender.createInstance(BorderExtender.BORDER_COPY));
      scalingHints.put(JAI.KEY_TILE_CACHE, JAI.getDefaultInstance().getTileCache());
      final RenderedOp result = JAI.create("Scale", scaleParams, scalingHints);

      return result;
   }

   private static RenderedOp retile(RenderedImage image, int tileWidth, int tileHeight) {
      ParameterBlockJAI pb = new ParameterBlockJAI("format");


      pb.addSource(image);
      final SampleModel sampleModel = image.getSampleModel();
      pb.setParameter("dataType", sampleModel.getDataType());

      ImageLayout layout = new ImageLayout( image );
      layout.setTileWidth( tileWidth );
      layout.setTileHeight( tileHeight );

      RenderingHints hint = new RenderingHints(JAI.KEY_IMAGE_LAYOUT, layout);
      hint.put(JAI.KEY_TILE_CACHE, JAI.getDefaultInstance().getTileCache());

      RenderedOp formattedImage = JAI.create("format", pb, hint);
      return formattedImage;
   }


   private void outputTiles(TilePyramidListener callback, ImageKey imageKey, int rlevel, RenderedImage image) throws Exception {

      long startTime = System.currentTimeMillis();
      int srcWidth = image.getWidth();
      int srcHeight = image.getHeight();

      this.image = image;

      int nTilesAcross = (int) Math.ceil(((double) srcWidth) / TILE_SIZE);
      int nTilesDown = (int) Math.ceil(((double) srcHeight) / TILE_SIZE);
      int nTiles = nTilesAcross * nTilesDown;
      CountDownLatch latch = new CountDownLatch(nTiles);
      LOGGER.info("Creating {} tiles {}x{} for level {}", nTiles, nTilesAcross, nTilesDown, rlevel);

      for (int r = 0; r < nTilesDown; r++) {
         for (int c = 0; c < nTilesAcross; c++) {
            executor.execute(new TileRunnable(latch, callback, imageKey, rlevel, r, c));
         }
      }
      try {
         latch.await();
      } catch (InterruptedException ie) {
         ie.printStackTrace();
      }
      long endTime = System.currentTimeMillis();
      LOGGER.info("Processed level {} in {}s", rlevel, ((endTime - startTime) / 1000));
   }

   private class TileRunnable implements Runnable {

      private final CountDownLatch latch;
      private final int r;
      private final int c;
      private final TilePyramidListener callback;
      private final int rlevel;
      private final ImageKey imageKey;

      private TileRunnable(CountDownLatch latch, TilePyramidListener callback, ImageKey imageKey, int rlevel, int r, int c) {
         this.latch = latch;
         this.r = r;
         this.c = c;
         this.callback = callback;
         this.imageKey = imageKey;
         this.rlevel = rlevel;
      }

      @Override
      public void run() {
         try {

            Rectangle tileBounds = new Rectangle(c * TILE_SIZE, r * TILE_SIZE, TILE_SIZE, TILE_SIZE);
            final Raster raster = image.getData(tileBounds);

            int bufferedImageType = BufferedImage.TYPE_BYTE_GRAY;
            if (raster.getSampleModel().getSampleSize(0) > 8) {
               bufferedImageType = BufferedImage.TYPE_USHORT_GRAY;
            }
            SampleModel sampleModel = raster.getSampleModel();

            for (int b = 0; b < sampleModel.getNumBands(); b++) {
               int[] iArray = raster.getSamples(raster.getMinX(), raster.getMinY(), raster.getWidth(), raster.getHeight(), b, (int[]) null);
               BufferedImage tile = new BufferedImage(TILE_SIZE, TILE_SIZE, bufferedImageType);
               tile.getRaster().setPixels(0, 0, raster.getWidth(), raster.getHeight(), iArray);
               callback.handleTile(imageKey, b, rlevel, c, r, tile);
            }
         } catch (Exception e) {
            e.printStackTrace();
         }
         latch.countDown();
      }
   }
}
