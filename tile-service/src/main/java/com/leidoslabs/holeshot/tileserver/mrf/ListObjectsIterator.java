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

import java.util.Iterator;
import java.util.List;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.Function;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.ListObjectsV2Request;
import com.amazonaws.services.s3.model.ListObjectsV2Result;
import com.amazonaws.services.s3.model.S3ObjectSummary;

/**
 * Utility class for listing objects from s3 buckets as an iterator.
 * @param <T>
 */
public class ListObjectsIterator <T> implements Iterator<T> {
   private static final int MAX_KEYS_PER_READ=1000;
   private ListObjectsV2Result result;
   private final ListObjectsV2Request request;
   private final Function<ListObjectsV2Result, List<T>> collectionMethod;
   private Iterator<T> iterator;
   private final AmazonS3 s3Client;
   
   /**
    * Initializes s3 client
    * @param request S3 API List objects request
    * @param collectionMethod 
    */
   public ListObjectsIterator(ListObjectsV2Request request, Function<ListObjectsV2Result, List<T>> collectionMethod ) {
      s3Client = AmazonS3ClientBuilder.defaultClient();
      this.collectionMethod = collectionMethod;
      this.request = request;
      this.result = null;
      this.iterator = null;
   }
   
   /**
    * Return as a stream
    * @return
    */
   public Stream<T> stream() {
      return StreamSupport.stream(Spliterators.spliteratorUnknownSize(this, Spliterator.ORDERED), false);
   }
   
   /**
    * List S3 object prefixes as given a bucket and prefix
    * @param bucket
    * @param prefix
    * @return
    */
   public static ListObjectsIterator<String> listPrefixes(String bucket, String prefix) {
      return new ListObjectsIterator<String>(new ListObjectsV2Request()
            .withBucketName(bucket)
            .withDelimiter("/")
            .withPrefix(prefix)
            .withMaxKeys(MAX_KEYS_PER_READ), ListObjectsV2Result::getCommonPrefixes);
   }
   
   /**
    * List S3 object summaries given bucket and prefix
    * @param bucket
    * @param prefix
    * @return
    */
   public static ListObjectsIterator<S3ObjectSummary> listObjects(String bucket, String prefix) {
      return new ListObjectsIterator<S3ObjectSummary>(new ListObjectsV2Request()
            .withBucketName(bucket)
            .withPrefix(prefix)
            .withMaxKeys(MAX_KEYS_PER_READ), ListObjectsV2Result::getObjectSummaries);
   }

   /**
    * Continue the request if necessary before inspecting the next element 
    */
   private void nextIterator() {
      if ((iterator == null) || (result == null) || (!iterator.hasNext() && result.isTruncated())) {
         if (result != null) {
            String token = result.getNextContinuationToken();
            request.setContinuationToken(token);
         }

         result = s3Client.listObjectsV2(request);
         iterator = collectionMethod.apply(result).iterator();
      }
   }

   @Override
   public boolean hasNext() {
      nextIterator();
      return (iterator != null && iterator.hasNext());
   }

   @Override
   public T next() {
      nextIterator();
      return iterator.next();
   }

}
