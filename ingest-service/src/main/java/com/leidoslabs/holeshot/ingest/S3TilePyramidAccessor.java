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

import java.io.IOException;
import java.io.InputStream;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.ListObjectsRequest;
import com.amazonaws.services.s3.model.ObjectListing;
import com.leidoslabs.holeshot.imaging.ImageKey;

/**
 * TilePyramidAccessor utilizing AWS S3 As the backing store
 */
public class S3TilePyramidAccessor extends TilePyramidAccessor {
   private static final Pattern PATH_PATTERN = Pattern.compile("^s3://([^/]*)/(.*)$");
   private final String bucketName;
   private final AmazonS3 s3Client;

    /**
     * Construct a new accessor to retrieve and store objects in S3
     * @param bucketName The bucket for ingesting tiled imagery
     * @param imageKey The imageKey for the image we are ingesting
     */
   public S3TilePyramidAccessor(String bucketName, ImageKey imageKey) {
      super(imageKey);
      this.bucketName = bucketName;
      this.s3Client = AmazonS3ClientBuilder.defaultClient();
   }

    /**
     * Return the keys of all objects under this image in the image bucket
     * @return A Set containing all object keys below this image key
     * @throws IOException
     */
   public Set<String> listKeys() throws IOException {
      Set<String> keys = ConcurrentHashMap.newKeySet();
      ListObjectsRequest req = new ListObjectsRequest().withBucketName(bucketName).withPrefix(getImageKey().getName("/"));

      ObjectListing objectListing = null;
      do {
         objectListing = (objectListing == null) ? s3Client.listObjects(req) : s3Client.listNextBatchOfObjects(objectListing);
         objectListing.getObjectSummaries().stream().map(s->s.getKey()).forEach(s->keys.add(s));
      } while (objectListing.isTruncated());
      return keys;
   }
   
   private String parsePath(String path, int group) {
      String result = null;
      Matcher m = PATH_PATTERN.matcher(path);
      if (m.find()) {
         result = m.group(group);
      }
      return result;
   }

   private String getBucket(String path) {
      return parsePath(path, 1);
   }

   private String getKey(String path) {
      return parsePath(path, 2);
   }

    /**
     * The standard delimeter for s3 paths
     * @return "/"
     */
   protected String getPathSeparator() {
      return "/";
   }

    /**
     * Get the base s3 path associated with this instance
     * @return "s3://{bucketName}"
     */
   protected String getBasePath() {
      return String.format("s3://%s", bucketName);
   }

    /**
     * Get an input stream of the object content from the remote connection at the s3 URL path
     * @param path S3 Path to the object we want to access
     * @return InputStream, containing byte data from the specified object
     * @throws IOException
     */
   protected InputStream open(String path) throws IOException {
      InputStream result = null;
      final String bucket = getBucket(path);
      final String key = getKey(path);
      if (!this.bucketName.equals(bucket)) {
         throw new IllegalArgumentException(String.format("this accessor can only read from bucket %s, not %s", this.bucketName, bucket));
      }
      if (s3Client.doesObjectExist(bucket, key)) {
         result = s3Client.getObject(bucket, key).getObjectContent();
      }
      return result;
   }
}
