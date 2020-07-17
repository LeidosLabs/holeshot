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
package com.leidoslabs.holeshot.tileserver.service;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectSummary;

import com.leidoslabs.holeshot.tileserver.mrf.S3ObjectsIterator;
import com.leidoslabs.holeshot.tileserver.service.io.ExposedByteArrayOutputStream;
import com.leidoslabs.holeshot.tileserver.service.pool.CacheBufferPool;
import com.leidoslabs.holeshot.tileserver.service.pool.TransferBufferPool;
import com.leidoslabs.holeshot.tileserver.utils.ResourceUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.TeeOutputStream;
import org.apache.commons.lang3.time.StopWatch;
import org.slf4j.ext.XLogger;
import org.slf4j.ext.XLoggerFactory;
import xyz.cloudkeeper.s3.io.S3Connection;
import xyz.cloudkeeper.s3.io.S3ConnectionBuilder;

import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.Response;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLConnection;
import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Manages s3 client to handle gets, writes, copies from/to either S3 or Cache
 */
public class S3Handler extends AbstractTileStoreBase implements ITileStore {
   private static final XLogger logger = XLoggerFactory.getXLogger(S3Handler.class);

   private final static int S3_READ_CONCURRENT_CONNECTIONS = 4;
   private final static String SKIP_CACHE_PARAMETER = "skipcache";
   private final static String PARALLEL_S3_PARAMETER = "parallelS3";

   protected AmazonS3 s3Client;
   private String bucketName;
   private TransferBufferPool transferBufferPool;
   private CacheBufferPool cacheBufferPool;


   /**
    * Builds S3 client and initializes buffer pools
    *
    * @param bucket
    * @param bucketRegion
    * @param maxNumberOfAcceptors Max number of concurrent requests
    */
   public S3Handler(String bucket, String bucketRegion, int maxNumberOfAcceptors) {
      logger.entry();
      s3Client = AmazonS3ClientBuilder.standard().withRegion(bucketRegion).withPathStyleAccessEnabled(true).build();
      bucketName = bucket;
      transferBufferPool = new TransferBufferPool(maxNumberOfAcceptors, 1, TRANSFER_POOL_BUFFER_SIZE_IN_BYTES);
      cacheTransferBufferPool = new TransferBufferPool(maxNumberOfAcceptors, 1, MAX_CACHED_TILE_SIZE_IN_BYTES);
      cacheBufferPool = new CacheBufferPool(maxNumberOfAcceptors, 1);

      logger.exit();
   }

   /**
    * Given object key, respond with the object as a ByteArray. Gets data either from cache or S3 depending on request
    * parameters
    *
    * @param request
    * @param wholeKey
    * @return
    */
   public Response getResponse(ServletRequest request, String wholeKey) {

      Response response = null;

      logger.entry();
      StopWatch stopWatch = StopWatch.createStarted();
      try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
         writeResponse(request, wholeKey, out);
         response = Response.ok(out.toByteArray()).type(URLConnection.guessContentTypeFromName(wholeKey)).build();
         stopWatch.stop();
      } catch (AmazonS3Exception s3Exception) {
         logger.warn(s3Exception.getMessage(), s3Exception);
         response = Response.serverError().status(s3Exception.getStatusCode()).entity(s3Exception.getErrorMessage()).build();
      } catch (Exception e) {
         logger.error(e.getMessage(), e);
         response = Response.serverError().status(HttpServletResponse.SC_INTERNAL_SERVER_ERROR).entity(e.getMessage()).build();
      }
      logger.exit();
      return response;
   }


   /**
    * Given object key, write data from either S3 (parallel or serial) or cache into out
    *
    * @param request
    * @param wholeKey
    * @param out
    * @return Representation of whether written from cache or S3
    * @throws NoSuchElementException
    * @throws IllegalStateException
    * @throws Exception
    */
   public WriteFrom writeResponse(ServletRequest request, String wholeKey, OutputStream out) throws NoSuchElementException, IllegalStateException, Exception {
      logger.entry();

      final boolean useCache = (request.getParameter(SKIP_CACHE_PARAMETER) == null);
      final boolean parallelS3 = (request.getParameter(PARALLEL_S3_PARAMETER) != null);

      WriteFrom writeFrom = WriteFrom.CACHE;
      if (!(useCache && writeFromCache(wholeKey, out))) {
         if (parallelS3) {
            writeFromS3InParallel(wholeKey, out, useCache);
         } else {
            writeFromS3(wholeKey, out, useCache);
         }
         writeFrom = WriteFrom.S3;
      }
      logger.exit();

      return writeFrom;
   }

   public void writeFromStore(String wholeKey, OutputStream out, boolean useCache) throws Exception {
      writeFromS3(wholeKey, out, useCache);
   }

   /**
    * Get S3 object and copy to out, and add to cache if useCache. Ensures retrieved objects are less than
    * MAX_CACHED_TILE_SIZE_IN_BYTES
    *
    * @param wholeKey
    * @param out
    * @param useCache
    * @throws NoSuchElementException
    * @throws IllegalStateException
    * @throws Exception
    */
   private void writeFromS3(String wholeKey, OutputStream out, boolean useCache) throws NoSuchElementException, IllegalStateException, Exception {
      logger.entry();
      ExposedByteArrayOutputStream byteStream = null;
      try (S3Object singleTileObject = s3Client.getObject(new GetObjectRequest(bucketName, wholeKey));
           InputStream singleStream = singleTileObject.getObjectContent();) {
         long fileSize = singleTileObject.getObjectMetadata().getContentLength();

         if (useCache && (fileSize < MAX_CACHED_TILE_SIZE_IN_BYTES)) {
            byteStream = cacheBufferPool.borrowObject();
         }
         copyStream(singleStream, out, byteStream);

         if (useCache && byteStream.size() > 0) {
            cacheAdd(wholeKey, ByteBuffer.wrap(byteStream.buf(), 0, byteStream.size()));
         }
      } finally {
         ResourceUtils.returnToPoolQuietly(cacheBufferPool, byteStream);
      }
      logger.exit();
   }

   /**
    * Setup S3_READ_CONCURRENT_CONNECTIONS in parallel to S3 to write object data to out
    *
    * @param wholeKey
    * @param out
    * @param useCache
    * @throws NoSuchElementException
    * @throws IllegalStateException
    * @throws Exception
    */
   private void writeFromS3InParallel(String wholeKey, OutputStream out, boolean useCache) throws NoSuchElementException, IllegalStateException, Exception {
      logger.entry();
      ScheduledExecutorService executorService = Executors.newScheduledThreadPool(S3_READ_CONCURRENT_CONNECTIONS);
      S3Connection s3Connection = new S3ConnectionBuilder(s3Client, executorService).setParallelConnectionsPerRequest(S3_READ_CONCURRENT_CONNECTIONS).build();
      ExposedByteArrayOutputStream byteStream = null;
      try (InputStream singleStream = s3Connection.newBufferedInputStream(bucketName, wholeKey, 0L)) {
         long fileSize = s3Client.getObjectMetadata(bucketName, wholeKey).getInstanceLength();
         if (useCache && (fileSize < MAX_CACHED_TILE_SIZE_IN_BYTES)) {
            byteStream = cacheBufferPool.borrowObject();
         }
         copyStream(singleStream, out, byteStream);

         if (useCache && byteStream.size() > 0) {
            cacheAdd(wholeKey, ByteBuffer.wrap(byteStream.buf(), 0, byteStream.size()));
         }
      } finally {
         ResourceUtils.returnToPoolQuietly(cacheBufferPool, byteStream);
      }
      logger.exit();
   }

   /**
    * Copy data from inputstream to output using a transfer buffer
    *
    * @param inputStream
    * @param outputStream
    * @param byteStream
    * @throws NoSuchElementException
    * @throws IllegalStateException
    * @throws Exception
    */
   private void copyStream(InputStream inputStream, OutputStream outputStream, ByteArrayOutputStream byteStream) throws NoSuchElementException, IllegalStateException, Exception {
      logger.entry();
      byte[] transferBuffer = null;
      TeeOutputStream teeStream = null;
      try {
         transferBuffer = transferBufferPool.borrowObject();

         OutputStream writeStream;
         if (byteStream == null) {
            writeStream = outputStream;
         } else {
            writeStream = teeStream = new TeeOutputStream(outputStream, byteStream);
         }
         IOUtils.copyLarge(inputStream, writeStream, transferBuffer);
      } finally {
         ResourceUtils.returnToPoolQuietly(transferBufferPool, transferBuffer);
         ResourceUtils.closeQuietly(teeStream);
      }
      logger.exit();
   }

   public void putObject(String objectName, ByteArrayInputStream bis, String mimeType) {
      final ObjectMetadata metadata = new ObjectMetadata();
      metadata.setContentType(mimeType);

      final PutObjectRequest put = new PutObjectRequest(this.bucketName, objectName, bis, metadata);
      this.s3Client.putObject(put);

   }

   @Override
   public Iterator<String> getSubFolders(String parentPath) {
      return S3ObjectsIterator.listPrefixes(this.bucketName, parentPath, this.s3Client);
   }

   @Override
   public Iterator<String> getSubFoldersByName(String parentPath, String nameContains) {
      Predicate<String> byName = string -> string.contains(nameContains);

      return S3ObjectsIterator.listPrefixes(this.bucketName, parentPath, this.s3Client).stream()
              .filter(byName)
              .collect(Collectors.toList()).iterator();
   }

   public Stream<String> getSubFoldersByNameAsStream(String parentPath, String nameContains) {
      Predicate<String> byName = string -> string.contains(nameContains);

      return S3ObjectsIterator.listPrefixes(this.bucketName, parentPath, this.s3Client).stream()
              .filter(byName)
              .collect(Collectors.toList()).stream();
   }

   @Override
   public Iterator<String> getSubFoldersMatching(String parentPath, Pattern regExp) {

      Pattern full = Pattern.compile( Pattern.quote(parentPath) + regExp);
      Predicate<String> byName = full.asPredicate();

      return S3ObjectsIterator.listPrefixes(this.bucketName, parentPath, this.s3Client).stream()
              .filter(byName)
              .collect(Collectors.toList()).iterator();
   }

   public Stream<String> getSubFoldersMatchingAsStream(String parentPath, Pattern regExp) {
      Pattern full = Pattern.compile( Pattern.quote(parentPath) + regExp);
      Predicate<String> byName = full.asPredicate();

      return S3ObjectsIterator.listPrefixes(this.bucketName, parentPath, this.s3Client).stream()
              .filter(byName)
              .collect(Collectors.toList()).stream();
   }

   public class S3TileObject implements ITile {

      private S3ObjectSummary s3ObjectSummary;

      public S3TileObject(S3ObjectSummary summary) {
         this.s3ObjectSummary = summary;
      }

      @Override
      public String getKey() {
         return s3ObjectSummary.getKey();
      }

      @Override
      public long getSize() {
         return s3ObjectSummary.getSize();
      }
   }

   public ITile S3ObjToTileObject(S3ObjectSummary summary) {
      return new S3TileObject(summary);
   }

   @Override
   public Stream<ITile> listObjects(String folder) {

      return S3ObjectsIterator.listObjects(this.bucketName, folder, this.s3Client).stream()
              .map(this::S3ObjToTileObject)
              .collect(Collectors.toList()).stream();
   }

   public Stream<ITile> listObjects(String folder, Pattern objectNameRegex) {

      Predicate<S3ObjectSummary> byName = o -> o.getKey().matches(objectNameRegex.pattern());

      return S3ObjectsIterator.listObjects(this.bucketName, folder, this.s3Client).stream()
              .filter(byName)
              .map(this::S3ObjToTileObject)
              .collect(Collectors.toList()).stream();
   }

}
