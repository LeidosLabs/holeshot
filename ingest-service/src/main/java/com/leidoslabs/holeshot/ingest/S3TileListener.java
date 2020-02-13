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

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

import org.image.common.geojson.GeoJsonModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.sns.AmazonSNS;
import com.amazonaws.services.sns.AmazonSNSClientBuilder;
import com.amazonaws.services.sns.model.CreateTopicResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.leidoslabs.holeshot.tileserver.mrf.MRFIndexFile;
import com.leidoslabs.holeshot.imaging.ImageKey;
import com.leidoslabs.holeshot.imaging.metadata.TilePyramidDescriptor;

/**
 * This implementation of the TilePyramidListener interface writes the tiles and metadata to a
 * S3 bucket.
 *
 * The individual tiles are stored as PNGs under keys following this convention:
 *  collectionID/timestamp/r-level/column/row/band.png
 *
 * The metadata is encoded to JSON and stored under they key: collectionID/timestamp/metadata.json
 *
 * If a SNS topic name is provided on the constructor the metadata.json file is broadcast to that
 * topic as well.
 */
public class S3TileListener extends TilePyramidListener {

   private static final Logger LOGGER = LoggerFactory.getLogger(S3TileListener.class);

   protected final AmazonS3 s3Client = AmazonS3ClientBuilder.defaultClient();
   protected final AmazonSNS snsClient = AmazonSNSClientBuilder.defaultClient();
   protected final ObjectMapper mapper;
   protected final String bucketName;
   protected final String metadataTopicName;

   public S3TileListener(String bucketName, String metadataTopicName) throws Exception {
      super();
      this.bucketName = bucketName;
      this.metadataTopicName = metadataTopicName;

      mapper = new ObjectMapper();
      mapper.registerModule(new GeoJsonModule());
      mapper.enable(SerializationFeature.INDENT_OUTPUT);
   }

   public String getBucketName() {
      return bucketName;
   }

   /**
    * @{inheritDoc}
    */
   @Override
   protected void handleMetadataInternal(ImageKey imageKey, TilePyramidDescriptor metadata) throws Exception {
      LOGGER.info("Writing Image Metadata for {}", imageKey.getName());

      String key = new S3TilePyramidAccessor(bucketName, imageKey).getMetadataKey();
      String metadataJson = mapper.writeValueAsString(metadata);
      byte[] buffer = metadataJson.getBytes();
      InputStream is = new ByteArrayInputStream(buffer);
      ObjectMetadata meta = new ObjectMetadata();
      meta.setContentLength(buffer.length);
      meta.setContentType("application/json");
      s3Client.putObject(new PutObjectRequest(bucketName, key, is, meta));
      // TODO: TileHandler.handleTile: Do something with S3 put object result?

      if (metadataTopicName != null) {
         try {
            CreateTopicResult createTopicResult = snsClient.createTopic(metadataTopicName);
            snsClient.publish(createTopicResult.getTopicArn(), metadataJson);
         } catch (Exception e) {
            LOGGER.error("Unable to publish metadata for {} to {}", imageKey.getName(), metadataTopicName, e);
         }
      }
   }

   /**
    * @{inheritDoc}
    */
   @Override
   protected long handleTileInternal(ImageKey imageKey, int band, int rlevel, int c, int r, BufferedImage tile) throws Exception {
      long tileSize = -1;
      String key = new S3TilePyramidAccessor(bucketName, imageKey).getTileKey(rlevel, c, r, band);
      LOGGER.info("Writing Image Tile for {}", key);
      try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
         tileSize = this.writeImage(os, tile);
         byte[] buffer = os.toByteArray();

         try (InputStream is = new ByteArrayInputStream(buffer)) {
            ObjectMetadata meta = new ObjectMetadata();
            meta.setContentLength(buffer.length);
            meta.setContentType("image/png");
            s3Client.putObject(new PutObjectRequest(bucketName, key, is, meta));
            // TODO: TileHandler.handleTile: Do something with S3 put object result?
         }
      }
      return tileSize;
   }

   /**
    * @{inheritDoc}
    */
   protected void handleMRFInternal(ImageKey imageKey, MRFIndexFile mrfIndexFile) throws Exception {
      String key = new S3TilePyramidAccessor(bucketName, imageKey).getMRFIndexKey();
      try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
         mrfIndexFile.writeToOutputStream(bos);
         
         final byte[] bytes = bos.toByteArray();
         
         try (ByteArrayInputStream bis = new ByteArrayInputStream(bytes)) {
            ObjectMetadata meta = new ObjectMetadata();
            meta.setContentLength(bytes.length);
            meta.setContentType("application/octet-stream");
            s3Client.putObject(new PutObjectRequest(bucketName, key, bis, meta));
         }
      }
   }
}
