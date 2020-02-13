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
import java.awt.image.BufferedImage;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.awt.image.SampleModel;
import java.awt.image.renderable.ParameterBlock;
import java.io.InputStream;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import javax.imageio.ImageIO;
import javax.media.jai.BorderExtender;
import javax.media.jai.Interpolation;
import javax.media.jai.JAI;
import javax.media.jai.PlanarImage;
import javax.media.jai.RenderedImageAdapter;
import javax.media.jai.RenderedOp;
import javax.media.jai.TileCache;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.leidoslabs.holeshot.imaging.ImageKey;

/**
 * This is an abstract foundation for classes designed to convert images split into multiple
 * files/objects into a single tiled image pyramid. It provides reusable methods to turn a single
 * image into a tiled pyramid and to output those tiles through the listener on a multi-threaded
 * executor.
 */
public abstract class MultiPartTilePyramidBuilder {

   private static final Logger LOGGER = LoggerFactory.getLogger(MultiPartTilePyramidBuilder.class);

   private Executor executor = Executors.newFixedThreadPool(Math.max(1, Runtime.getRuntime().availableProcessors() - 1));
   protected static final int TILE_SIZE = 512;
   protected final MultiPartImageAccessor accessor;
   protected final TilePyramidListener listener;
   private int lastRsetProcessed;
   private ImageKey imageKey;
   private boolean metadataOnly;

    /**
     * Retrieve an accessor for accessing image parts data
     * @return An accessor that can provide a InputStream of data for a part given the name of that part
     */
   public MultiPartImageAccessor getAccessor() {
      return accessor;
   }

    /**
     * Retrieve the listener that will publish metadata and tiles as they finish
     * @return the TilePyramidListener being used for publication
     */
   public TilePyramidListener getListener() {
      return listener;
   }

    /**
     * Construction, defines how the pyramid build should access image data and publish processed tiles/metadata
     * @param accessor MultiPartImageAccessor implementation which can retrieve the image parts for processing
     * @param listener Component implementation that can handle finished tiles and metadata
     * @param metadataOnly flag=true if only metadata should be processed, no tiles generated
     */
   protected MultiPartTilePyramidBuilder(MultiPartImageAccessor accessor,TilePyramidListener listener, boolean metadataOnly) {
      this.accessor = accessor;
      this.listener = listener;
      this.lastRsetProcessed = -1;
      this.metadataOnly = metadataOnly;
   }

    /**
     * Retrieve the level of the most recently processed r set
     * @return the most recently processed r set, -1 if none
     */
   public int getLastRsetProcessed() {
      return lastRsetProcessed;
   }

    /**
     * Get the image key for the image we are building, generally retrieved from a metadata file while processing
     * metadata, and may not be populated until buildMetadata runs
     * @return Image Key for the image being processed
     */
   public ImageKey getImageKey() {
      return imageKey;
   }

    /**
     * Set the image key to the image being processed
     * @param imageKey image key of image being processed
     */
   public void setImageKey(ImageKey imageKey) {
      this.imageKey = imageKey;
   }

    /**
     * Given the name of the image, build the tile pyramid from all constituent parts
     * @param name image name
     * @throws TilePyramidException
     */
   public abstract void buildTilePyramid(String name)
         throws TilePyramidException;

    /**
     * Reads in an image part, and renders its sub tiles at each rSet. Tiles generated are given rows and columns
     * relative to the complete mosaic, not the image part they are in, based on the rOffset and cOffset.
     * Note image parts in a mosaic may be referred to as "tiles" as well in the metadata.
     * @param imageFile The image part to process
     * @param imageKey The full key of the image
     * @param rOffset The row location of this part in the mosaic
     * @param cOffset The column location of this part in the mosaic
     * @throws TilePyramidException On any failure during processing
     */
   protected void processImage(String imageFile, ImageKey imageKey, int rOffset, int cOffset) throws TilePyramidException {
      LOGGER.info("Processing Image Part: {}", imageFile);

      try {
         InputStream imageStream = accessor.getPart(imageFile);
         RenderedImage renderedImage = ImageIO.read(imageStream);

         final double maxDim = Math.max(renderedImage.getWidth(),  renderedImage.getHeight());
         lastRsetProcessed = (int)Math.ceil(Math.log(maxDim/((double)TILE_SIZE)) / Math.log(2.0));
         
         if (!isMetadataOnly()) {
            TileCache tileCache = JAI.getDefaultInstance().getTileCache();
            long tileCacheSize = (long) (0.9 * Runtime.getRuntime().maxMemory());
            LOGGER.info("Limiting tile cache to 90 percent of maximum memory: {}", tileCacheSize);
            tileCache.setMemoryCapacity(tileCacheSize);

            int scaledRowOffset = rOffset;
            int scaledColumnOffset = cOffset;
            PlanarImage scaled = new RenderedImageAdapter(renderedImage);

            for (int rset=0; rset<= lastRsetProcessed; ++rset) {
               outputTiles(imageKey, rset, scaledRowOffset, scaledColumnOffset, scaled);   
               scaledRowOffset /= 2;
               scaledColumnOffset /= 2;
               scaled = scale(scaled);
            }
            tileCache.flush();
         }
      } catch (Exception e) {
         throw new TilePyramidException("Exception thrown processing image " + imageFile,e);
      }
   }

    /**
     * Create and apply the scaling operation to reduce the resolution of the image to the next higher reduced
     * resolution level
     * @param image The image to scale
     * @return A scaled rendered image
     */
   protected static RenderedOp scale(PlanarImage image) {
      ParameterBlock scaleParams = new ParameterBlock();
      scaleParams.addSource(image);
      scaleParams.add(0.5f).add(0.5f).add(0.0f).add(0.0f);
      scaleParams.add(Interpolation.getInstance(Interpolation.INTERP_BICUBIC_2));

      // Quality related hints when scaling the image
      RenderingHints scalingHints = new RenderingHints(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
      //scalingHints.put(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
      //scalingHints.put(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
      //scalingHints.put(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_QUALITY);
      //scalingHints.put(RenderingHints.KEY_DITHERING, RenderingHints.VALUE_DITHER_ENABLE);
      scalingHints.put(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
      scalingHints.put(JAI.KEY_BORDER_EXTENDER, BorderExtender.createInstance(BorderExtender.BORDER_COPY));
      scalingHints.put(JAI.KEY_TILE_CACHE, JAI.getDefaultInstance().getTileCache());
      return JAI.create("scale", scaleParams, scalingHints);
   }

    /**
     * Compute the sub tiles for one rLevel from a scaled image part. Each tile will be processed as its own thread,
     * and the function returns after the number of threads completed is equal to the number of tiles to be generated,
     * even if an exception occurs during the processing of any individual tile.
     * @param imageKey The key of the image being processed
     * @param rlevel The reduced resolution level to be calculated
     * @param rOffset The absolute row offset of this image parts tiles
     * @param cOffset The absolute column offset of this image parts tiles
     * @param image The image part, scaled to the resolution required for this rLevel
     * @throws Exception
     */
   protected void outputTiles(ImageKey imageKey, int rlevel, int rOffset, int cOffset, RenderedImage image) throws Exception {
      if (!isMetadataOnly()) {
         long startTime = System.currentTimeMillis();
         int srcWidth = image.getWidth();
         int srcHeight = image.getHeight();

         int nTilesAcross = (int) Math.ceil(((double) srcWidth) / TILE_SIZE);
         int nTilesDown = (int) Math.ceil(((double) srcHeight) / TILE_SIZE);
         int nTiles = nTilesAcross * nTilesDown;
         CountDownLatch latch = new CountDownLatch(nTiles);
         LOGGER.info("Creating {} tiles {}x{} for level {}", nTiles, nTilesAcross, nTilesDown, rlevel);

         for (int r = 0; r < nTilesDown; r++) {
            for (int c = 0; c < nTilesAcross; c++) {

               Rectangle tileBounds = new Rectangle(c * TILE_SIZE, r * TILE_SIZE,
                     Math.min(TILE_SIZE,srcWidth-c*TILE_SIZE), Math.min(TILE_SIZE,srcHeight-r*TILE_SIZE));
               Raster raster = image.getData(tileBounds);
               executor.execute(new TileRunnable(latch, listener, imageKey, rlevel, raster, r + rOffset, c + cOffset));
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
   }

   private class TileRunnable implements Runnable {

      private final CountDownLatch latch;
      private final Raster raster;
      private final int r;
      private final int c;
      private final TilePyramidListener callback;
      private final ImageKey imageKey;
      private final int rlevel;

      private TileRunnable(CountDownLatch latch, TilePyramidListener callback, ImageKey imageKey, int rlevel, Raster raster, int r, int c) {
         this.latch = latch;
         this.raster = raster;
         this.r = r;
         this.c = c;
         this.callback = callback;
         this.rlevel = rlevel;
         this.imageKey = imageKey;
      }

       /**
        * Creates a BufferedImage sub-tile and publishes it using the implementation of handleTile in the callback
        */
      @Override
      public void run() {
         try {
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

   protected boolean isMetadataOnly() {
      return metadataOnly;
   }

}
