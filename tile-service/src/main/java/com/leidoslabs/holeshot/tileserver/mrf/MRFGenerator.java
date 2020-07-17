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

package com.leidoslabs.holeshot.tileserver.mrf;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.regex.Pattern;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.leidoslabs.holeshot.elt.tileserver.TileserverImage;
import com.leidoslabs.holeshot.tileserver.service.S3Handler;
import com.leidoslabs.holeshot.tileserver.service.ITileStore;
import sun.misc.Regexp;

/**
 * Driver class for generating .idx files for images on s3
 *
 */
public class MRFGenerator {
   private final AmazonS3 s3Client;
   private final String tileserverURL;
   private final ITileStore storeHandler;
   
   /**
    * Initializes s3 client, and writes all objects with metadata.json to a MRFIndexFile
    * @param storeHandler
    * @param region
    * @param bucket
    * @param tileserverURL
    */
   public MRFGenerator(ITileStore storeHandler, String region, String bucket, String tileserverURL) {
      this.s3Client = AmazonS3ClientBuilder.defaultClient();
      this.tileserverURL = tileserverURL;
      this.storeHandler = storeHandler;

      crawlBucket(region, bucket);
   }
   
   /**
    * Entry point for writing MRFIndexFiles for images
    * @param args
    */
   public static void main(String[] args) {
      final String bucket = "advanced-analytics-geo-tile-images";
      final String bucketRegion = "us-east-1";
      final String tileserverURL = "https://tileserver.leidoslabs.com";
      S3Handler s3Handler = new S3Handler(bucket, bucketRegion, 10);
      new MRFGenerator(s3Handler, bucketRegion, bucket, tileserverURL);

   }
   /**
    * Get TileserverImage given image collection and timestep prefix
    * @param prefix
    * @return
    * @throws MalformedURLException
    * @throws IOException
    */
   private TileserverImage getImage(String prefix) throws MalformedURLException, IOException {
      return new TileserverImage(new URL(getMetadataURL(prefix)));
   }
   
   /**
    * Get URL from prefix
    * @param prefix
    * @return
    */
   private String getMetadataURL(String prefix) {
      return String.join("/", tileserverURL, "tileserver", prefix, "metadata.json");
   }
   
   /**
    * Retrieve S3 objects that have a metadata.json, and calls writeMRFIndexFile on their prefix
    * @param region
    * @param bucket
    */
   private void crawlBucket(String region, String bucket) {

      storeHandler.listObjects("", Pattern.compile("[A-Z,a-z,0-9/]*metadata.json\\b"))
      .forEach(p->writeMRFIndexFile(p.getKey( )));
   }

   /**
    * Write a MRFIndexFile from a IndexFile's output stream to s3 given bucket, image collection id, and timestamp (prefix)
    * @param prefix
    */
   private void writeMRFIndexFile(String prefix) {
      try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
         final TileserverImage image = getImage(prefix);
         final MRFIndexFile mrfIndex = new MRFIndexFile(image, storeHandler, prefix);
         mrfIndex.writeToOutputStream(bos);
         
         try (ByteArrayInputStream bis = new ByteArrayInputStream(bos.toByteArray())) {

            final String mrfIndexObjectname = String.format("%s/image.idx", prefix);
            System.out.println(String.format("Writing %s", mrfIndexObjectname));

            storeHandler.putObject(mrfIndexObjectname, bis, "application/octet-stream");

         }
      } catch (Exception ioe) {
         ioe.printStackTrace();
      }
   }
}
