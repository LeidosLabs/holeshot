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

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.leidoslabs.holeshot.elt.tileserver.TileserverImage;
import com.leidoslabs.holeshot.tileserver.service.S3Handler;

/**
 * Driver class for generating .idx files for images on s3
 *
 */
public class MRFGenerator {
   private final AmazonS3 s3Client;
   private final String tileserverURL;
   private final S3Handler s3Handler;
   
   /**
    * Initializes s3 client, and writes all objects with metadata.json to a MRFIndexFile
    * @param s3Handler
    * @param region
    * @param bucket
    * @param tileserverURL
    */
   public MRFGenerator(S3Handler s3Handler, String region, String bucket, String tileserverURL) {
      this.s3Client = AmazonS3ClientBuilder.defaultClient();
      this.tileserverURL = tileserverURL;
      this.s3Handler = s3Handler;

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
      ListObjectsIterator.listPrefixes(bucket, "").stream()
      .flatMap(t->ListObjectsIterator.listPrefixes(bucket, t).stream())
      .flatMap(s->ListObjectsIterator.listObjects(bucket, String.format("%smetadata.json", s)).stream())
      .map(o->o.getKey().replaceFirst("/metadata.json", ""))
      //.filter(p-> !ListObjectsIterator.listObjects(bucket, String.format("%s/image.idx", p)).stream().findAny().isPresent())
      .forEach(p->writeMRFIndexFile(region, bucket, p));
   }

   /**
    * Write a MRFIndexFile from a IndexFile's output stream to s3 given bucket, image collection id, and timestamp (prefix)
    * @param region
    * @param bucket
    * @param prefix
    */
   private void writeMRFIndexFile(String region, String bucket, String prefix) {
      try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
         final TileserverImage image = getImage(prefix);
         final MRFIndexFile mrfIndex = new MRFIndexFile(image, s3Handler, region, bucket, prefix);
         mrfIndex.writeToOutputStream(bos);
         
         try (ByteArrayInputStream bis = new ByteArrayInputStream(bos.toByteArray())) {
            final ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentType("application/octet-stream");
            final String mrfIndexObjectname = String.format("%s/image.idx", prefix);
            System.out.println(String.format("Writing %s", mrfIndexObjectname));
            final PutObjectRequest put = new PutObjectRequest(bucket, mrfIndexObjectname, bis, metadata);
            s3Client.putObject(put);
         }
      } catch (Exception ioe) {
         ioe.printStackTrace();
      }
   }
}
