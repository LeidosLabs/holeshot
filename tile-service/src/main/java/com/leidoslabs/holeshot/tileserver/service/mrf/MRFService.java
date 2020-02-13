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
package com.leidoslabs.holeshot.tileserver.service.mrf;


import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.NoSuchElementException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.apache.commons.lang3.NotImplementedException;
import org.ehcache.Cache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Range;
import com.leidoslabs.holeshot.elt.tileserver.TileserverImage;
import com.leidoslabs.holeshot.tileserver.cache.EHCache;
import com.leidoslabs.holeshot.tileserver.mrf.MRFIndexFile;
import com.leidoslabs.holeshot.tileserver.mrf.MRFMetadataFile;
import com.leidoslabs.holeshot.tileserver.mrf.MRFTiledInputStream;
import com.leidoslabs.holeshot.tileserver.mrf.jaxb.MRFMETA;
import com.leidoslabs.holeshot.tileserver.service.S3Handler;
import com.leidoslabs.holeshot.tileserver.utils.HttpRangeHeader;
import com.leidoslabs.holeshot.tileserver.v1.TileServerClient;

@Path("/")
public class MRFService {

   private static final Logger LOGGER = LoggerFactory.getLogger(MRFService.class);
   private static final String MRF_INDEX_CACHE_NAME = "MRF_INDEX_CACHE";
   private final String tileServerUrl;
   private final String region;
   private final String bucket;
   private final S3Handler s3Handler;
   private Cache<String, MRFIndexFile> mrfIndexCache;
   private TileServerClient tileserverClient;

   @Context
   private UriInfo info;

   @Context private HttpServletRequest servletRequest;

   /**
    * Setup MRFService, initialize EHCache
    * @param tileserverClient
    * @param s3Handler
    * @param tileServerUrl
    * @param region
    * @param bucket
    */
   public MRFService(TileServerClient tileserverClient, S3Handler s3Handler, String tileServerUrl, String region, String bucket) {
      this.tileserverClient = tileserverClient;
      this.tileServerUrl = tileServerUrl;
      this.region = region;
      this.bucket = bucket;
      this.s3Handler = s3Handler;
      
      this.mrfIndexCache = EHCache.getInstance().getCacheManager().getCache(MRF_INDEX_CACHE_NAME,
            String.class, MRFIndexFile.class);


   }

   /**
    * Produces MRFMetadata as an XML
    * @param imageId
    * @param timestamp
    * @return
    */
   @GET
   @Produces({"application/xml"})
   @Path("{imageId}/{timestamp}/image.mrf")
   public Response getImageMetadata(@PathParam("imageId") String imageId,
                                    @PathParam("timestamp") String timestamp) {
      Response response = null;
      try {
         MRFMETA meta = new MRFMetadataFile(getImage(imageId, timestamp)).getMRFMETA();

         response = Response.ok(meta).type("application/xml").build();
      } catch (Exception e) {
         LOGGER.error(e.getMessage(), e);
         response = Response.serverError().status(HttpServletResponse.SC_INTERNAL_SERVER_ERROR).entity(e.getMessage()).build();
      }

      return response;
   }

   private String getImageKey(String imageId, String timestamp) {
      return String.join("/", imageId, timestamp);
   }
   
   /**
    * Get MRFIndexFile from imageId and timestamp
    * @param imageId
    * @param timestamp
    * @return
    * @throws NoSuchElementException
    * @throws IllegalStateException
    * @throws Exception
    */
   private synchronized MRFIndexFile getIndexFile(String imageId, String timestamp) throws NoSuchElementException, IllegalStateException, Exception {
      final String mrfIndexPrefix = getMRFIndexPrefix(imageId, timestamp);
      MRFIndexFile indexFile = mrfIndexCache.get(mrfIndexPrefix);
      if (indexFile == null) {
         final TileserverImage image = getImage(imageId, timestamp);
         indexFile = new MRFIndexFile(image, s3Handler, region, bucket, mrfIndexPrefix);
         mrfIndexCache.put(mrfIndexPrefix, indexFile);
      }
      return indexFile;
   }
   
   private String getMRFIndexPrefix(String imageId, String timestamp) {
      return String.join("/", imageId, timestamp, "image.idx");
   }
   
   private TileserverImage getImage(String imageId, String timestamp) throws MalformedURLException, IOException {
      return new TileserverImage(new URL(getMetadataURL(imageId, timestamp)));
   }
   private String getMetadataURL(String imageId, String timestamp) {
      return String.join("/", tileServerUrl, "tileserver", imageId, timestamp, "metadata.json");
   }

   /**
    * Produces Image Index file as a binary file
    * @param imageId
    * @param timestamp
    * @return
    */
   @GET
   @Produces({"application/octet-stream"})
   @Path("{imageId}/{timestamp}/image.idx")
   public Response getImageIndex(@PathParam("imageId") String imageId,
         @PathParam("timestamp") String timestamp /*,
                                       @HeaderParam("Range") HttpRangeHeader rangeHeader*/) {
      Response response = null;
      response = s3Handler.getResponse(servletRequest, getMRFIndexPrefix(imageId, timestamp));
      return response;
   }

   /**
    * Returns a MRFTiledInputStream as a Response for ppg images
    * @param imageId
    * @param timestamp
    * @param rangeHeader
    * @return
    */
    @GET
    @Produces({"application/octet-stream"})
    @Path("{imageId}/{timestamp}/image.ppg")
    public Response getImageDataAsPNGs(@PathParam("imageId") String imageId,
                                       @PathParam("timestamp") String timestamp,
                                       @HeaderParam("Range") HttpRangeHeader rangeHeader) {
       Response response = null;
       try {
          MRFIndexFile index = getIndexFile(imageId, timestamp);
          long offset=0;
          long size;
          if (rangeHeader == null) {
             size = index.getDataSize();
          } else {
             if (rangeHeader.getRanges().size() != 1) {
                throw new NotImplementedException("getImageData currently only supports a single range");
             }
             final Range<Long> range = rangeHeader.getRanges().get(0);
             if (range.hasLowerBound()) {
                offset = range.lowerEndpoint();
             }
             long upperBound;
             if (range.hasUpperBound()) {
                upperBound = range.upperEndpoint();
             } else {
                upperBound = index.getDataSize();
             }
             size = upperBound - offset;
          }
          final MRFTiledInputStream inputStream = new MRFTiledInputStream(tileserverClient, index, offset, size); 
          response = Response.ok(inputStream).type("application/octet-stream").build();
             
       } catch (Exception e) {
          LOGGER.error(e.getMessage(), e);
          response = Response.serverError().status(HttpServletResponse.SC_INTERNAL_SERVER_ERROR).entity(e.getMessage()).build();
       }

       return response;
    }
   /*
    @GET
    @Produces({"image/mrf"})
    @Path("{imageId}/{timestamp}/image.pjg")
    public Response getImageDataAsJPEGs(@PathParam("imageId") String imageId,
                                        @PathParam("timestamp") String timestamp,
                                        @HeaderParam("Range") HttpRangeHeader rangeHeader) {
       throw new UnsupportedOperationException("MRF JPEG Implementation not implemented yet.");
    }

    private String getBaseURL() {
       return String.format("%s//%s:port, args)
    }
    }*/
}
