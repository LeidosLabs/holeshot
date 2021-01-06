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
package com.leidoslabs.holeshot.elt.tileserver;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.ehcache.Cache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.leidoslabs.holeshot.elt.utils.EHCache;
import com.leidoslabs.holeshot.elt.utils.KeyedLIFOExecutorService;
import com.leidoslabs.holeshot.tileserver.v1.TilePyramidDescriptor;
import com.leidoslabs.holeshot.tileserver.v1.TileServerClient;



/**
 * Manages EHCache for for images and tilePyramidDescription. Handles calls to TileServerClient
 * if requested recourse is not in cache
 */
public class TileserverTileCache {
   private static final Logger LOGGER = LoggerFactory.getLogger(TileserverTileCache.class);

   private KeyedLIFOExecutorService executorService;
   private static final String IMAGE_CACHE_NAME="IMAGE_CACHE";
   private static final String TILESERVER_META_CACHE_NAME="TILESERVER_META_CACHE";
   private static final int RETRIES = 3;
   private static final int EXPECTED_CHANNELS = 1;

   private static final TileserverTileCache instance = new TileserverTileCache();
   private Cache<String, byte[]> imageCache;
   private Cache<String, TilePyramidDescriptor> tilePyramidDescriptorCache;
   
   public static TileserverTileCache getInstance() {
      return instance;
   }

   /**
    * Initialize executor service with 12 threads, and initialize tilePyramidDescriptor and image Cache
    */
   private TileserverTileCache() {
      // executorService = Executors.newFixedThreadPool(12);
      executorService = KeyedLIFOExecutorService.newFixedThreadPool(12);

      tilePyramidDescriptorCache = EHCache.getInstance().getCacheManager().getCache(TILESERVER_META_CACHE_NAME,
            String.class, TilePyramidDescriptor.class);
      imageCache = EHCache.getInstance().getCacheManager().getCache(IMAGE_CACHE_NAME, String.class, byte[].class);
   }


   private <T> T getFromCache(Cache<String, T> cache, String key) {
      T value;
         value = cache.get(key);
      return value;
   }

   private <T> void setCache(Cache<String, T> cache, String key, T value) {
         if (value == null) {
            removeFromCache(cache, key);
         } else {
            cache.put(key, value);
         }
   }
   private <T> void removeFromCache(Cache<String, T> cache, String key) {
         cache.remove(key);
   }
   private InputStream tileInputStream(ImageTileFetcher tileFetcher, TileRef tileRef, byte[] givenImageBytes) throws IOException {
      final String key = tileRef.getKey();

      byte[] imageBytes;
      imageBytes = (givenImageBytes == null) ? getFromCache(imageCache, key) : givenImageBytes;
      if (imageBytes == null) {
         try ( InputStream tileStream = tileFetcher.getTileServerClient()
               .getTile(tileRef.getCollectionID(),
                     tileRef.getTimestamp(), tileRef.getRset(), tileRef.getX(), tileRef.getY(), tileRef.getBand())) {
            if (tileStream != null) {
               imageBytes = IOUtils.toByteArray(tileStream);
               if ((imageBytes != null) && imageBytes.length > 0) {
                  setCache(imageCache, key, imageBytes);
               }
            }
         }
      }
      return imageBytes == null ? null : new ByteArrayInputStream(imageBytes);
   }
   private CoreImage readImage(ImageTileFetcher tileFetcher, TileRef tileRef, byte[] imageBytes) throws IOException {
      CoreImage result = null;
      final String key = tileRef.getKey();

      String errorMessage = "Couldn't read image";
      for (int i=0;i<RETRIES && result == null;++i) {
         try (InputStream resultStream = tileInputStream(tileFetcher, tileRef, imageBytes)) {
            if (resultStream != null) {
               CoreImage decodedImage = tileFetcher.imageDecode(resultStream);
               BufferedImage image = decodedImage.getBufferedImage();
               if ((image != null) && (image.getWidth() == tileRef.getWidth()) && (image.getHeight() == tileRef.getHeight()) && (image.getSampleModel().getNumBands() == EXPECTED_CHANNELS)) {
                  result = decodedImage;
               } else {
                  errorMessage = (image == null) ? "Couldn't decode image" :
                        String.format("Unexpected image decoded (width: %d, height: %d, channels:%d)", image.getWidth(), image.getHeight(), image.getSampleModel().getNumBands());
               }
            } else {
               errorMessage = String.format("Null InputStream received for tileRef %s", tileRef.getKey());
            }
         } catch (IOException e) {
            LOGGER.error(String.format("Couldn't decode image (%s).  Removing from cache and retrying", key));
            e.printStackTrace();
            removeFromCache(imageCache, key);
         }
      }

      if (result == null) {
         LOGGER.error(errorMessage);
         result = blankTile(tileRef);
         try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {

            ImageWriter writer =  ImageIO.getImageWritersByFormatName("png").next();
            try {
               try (ImageOutputStream ios = ImageIO.createImageOutputStream(baos)) {
                  writer.setOutput(ios);

                  ImageWriteParam param = writer.getDefaultWriteParam();
                  if (param.canWriteCompressed()){
                      param.setCompressionMode(ImageWriteParam.MODE_DISABLED);
                  }
                  writer.write(null, new IIOImage(result.getBufferedImage(), null, null), param);

                  baos.flush();
                  final byte[] baosBytes = baos.toByteArray();
                  setCache(imageCache, key, baosBytes);
               }
            } finally {
               writer.dispose();
            }
         }
      }

      return result;
   }

   /**
    * If we've already fetched the tile return it, otherwise put in a request to fetch it and return null
    * @param tileFetcher
    * @param tileRef
    * @return
    * @throws IOException
    */
   public CoreImage getTileserverTileForBand(ImageTileFetcher tileFetcher, TileRef tileRef) throws IOException {
      return getTileserverTileForBand(tileFetcher, tileRef, false);
   }
   
   public CoreImage getTileserverTileForBand(ImageTileFetcher tileFetcher, TileRef tileRef, boolean waitForResult) throws IOException {
	   return getTileserverTileForBand(tileFetcher, tileRef, waitForResult, false);
   }
   
   /**
    * If we've already fetched the tile return it, otherwise put in a request to fetch it and return null
    * @param tileFetcher
    * @param tileRef
    * @param waitForResult if tile is not in cache, wait for executor task to finish
    * @param minPriority When true, if tile image is not in cache, then fetch task is put on the bottom of executor's stack
    * @return
    * @throws IOException
    */
   public CoreImage getTileserverTileForBand(ImageTileFetcher tileFetcher, TileRef tileRef, boolean waitForResult, boolean minPriority) throws IOException {
      CoreImage result = null;
      Future<?> future = null;
      final String tileKey = tileRef.getKey();

      synchronized (tileKey.intern()) {
         byte[] imageBytes = getFromCache(imageCache, tileKey);
         if (imageBytes != null) {
            result = readImage(tileFetcher, tileRef, imageBytes);
         } else {
        	 if (!executorService.isInProcess(tileKey)) {
        		 Runnable tileTask = () -> {
        		      synchronized (tileKey.intern()) {
        		    	  if (!imageCache.containsKey(tileKey)) {
        		    		  try {
        		    			  readImage(tileFetcher, tileRef, null);
        		    		  } catch (IOException e) {
        		    			  LOGGER.error("error reading " + tileKey);
        		    			  e.printStackTrace();
        		    		  }
        		    	  }
        		      }
        		 };
        		 if (minPriority) {
        			 future = executorService.submit(tileKey, tileTask, Long.MIN_VALUE);
        		 }
        		 else {
        			 future = executorService.submit(tileKey, tileTask);
        		 }
        	 }
         }
      }
      if (waitForResult && future != null) {
         try {
            future.get();

            // Get the result from the cache.
            result = getTileserverTileForBand(tileFetcher, tileRef, false);

         } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
         }
      }
      return result;
   }

   /**
    * Check Cache for metadata, otherwise use client to retrieve
    * @param tileServerClient
    * @param collectionID
    * @param timestamp
    * @return
    * @throws IOException
    */
   @SuppressWarnings("unchecked")
   public TilePyramidDescriptor getTilePyramidDescriptor(TileServerClient tileServerClient, String collectionID, String timestamp) throws IOException {
      final String key = getTileDescriptorKey(collectionID, timestamp);
      TilePyramidDescriptor result = null;
      synchronized (key.intern()) {
         result = getFromCache(tilePyramidDescriptorCache, key);

         if (result == null) {
            result = tileServerClient.getMetadata(collectionID, timestamp);
            setCache(tilePyramidDescriptorCache, key, result);
         }
      }
      return result;
   }

   private String getTileDescriptorKey(String collectionID, String timestamp) {
      return String.join("/", collectionID, timestamp);
   }

   /**
    * @param tileRef
    * @return black image of the same dimensions as the tile, with same color depth
    */
   private static CoreImage blankTile(TileRef tileRef) {
      final int w = tileRef.getWidth();
      final int h = tileRef.getHeight();
      final int bpp = tileRef.getImage().getBitsPerPixel();

      final int bufferedImageType = (bpp > 8) ? BufferedImage.TYPE_USHORT_GRAY : BufferedImage.TYPE_BYTE_GRAY;
      BufferedImage image = new BufferedImage(w, h, bufferedImageType);
      Graphics2D g2d = image.createGraphics();

//      final Color color = (tileRef.getBand() == 0) ? Color.WHITE : Color.BLACK;
      final Color color = Color.BLACK;
      g2d.setPaint(color);
      g2d.fillRect(0, 0,  w, h);
      return new CoreImage(image);
   }
}
