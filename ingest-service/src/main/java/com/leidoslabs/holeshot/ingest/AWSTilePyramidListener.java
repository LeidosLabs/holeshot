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

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.services.cloudwatch.AmazonCloudWatch;
import com.amazonaws.services.cloudwatch.AmazonCloudWatchClientBuilder;
import com.amazonaws.services.cloudwatch.model.Dimension;
import com.amazonaws.services.cloudwatch.model.MetricDatum;
import com.amazonaws.services.cloudwatch.model.PutMetricDataRequest;
import com.amazonaws.services.cloudwatch.model.PutMetricDataResult;
import com.amazonaws.services.cloudwatch.model.StandardUnit;
import com.amazonaws.services.s3.event.S3EventNotification;
import com.amazonaws.services.s3.model.S3Object;
import com.leidoslabs.holeshot.imaging.ImageKey;
import com.leidoslabs.holeshot.imaging.io.CheckedSupplier;

/**
 * An s3 tile listener with additional capability to download and begin image tiling
 * based on s3 events (i.e. an image being placed in a bucket).
 * Another service like SQSEventMonitorDaemon must monitor/feed these events to it
 */
public abstract class AWSTilePyramidListener extends S3TileListener {
  private static final Logger LOGGER = LoggerFactory.getLogger(AWSTilePyramidListener.class);

  private final AmazonCloudWatch cwClient = AmazonCloudWatchClientBuilder.defaultClient();
  private final Set<String> SUPPORTED_FORMATS;

    /**
     * Constructor: Define bucket name for writing tiles and topic name for publishing metadata.
     * Also defines acceptable image formats, currently only NITF/NTF
     * @param bucketName The s3 bucket to write tiles to
     * @param metadataTopicName The SNS topic to send metadata to when an image finishes processing
     * @throws Exception
     */
  public AWSTilePyramidListener(String bucketName, String metadataTopicName) throws Exception {
     super(bucketName, metadataTopicName);
    SUPPORTED_FORMATS = new HashSet<>();
    SUPPORTED_FORMATS.add("");
    SUPPORTED_FORMATS.add("nitf");
    SUPPORTED_FORMATS.add("ntf");
  }

    /**
     * Handle an S3 Event, which may be an image file being placed in a bucket. Failures, especially events
     * outside this scope, will be logged, but not thrown.
     * @param record The S3 Event to process
     * @return A log string describing the successes/failures of processing the event
     */
  public String handleS3EventRecord(S3EventNotification.S3EventNotificationRecord record) {
    StringBuilder result = new StringBuilder();
    try {
      S3EventNotification.S3ObjectEntity objectEntity = record.getS3().getObject();
      S3EventNotification.S3BucketEntity bucketEntity = record.getS3().getBucket();

      LOGGER.info("Processing S3 event for: {} ::: {}", bucketEntity.getName(), objectEntity.getKey());

      if (!SUPPORTED_FORMATS.contains(getObjectExtension(objectEntity.getKey()))) {
        LOGGER.warn("Skipping Object! Unexpected Extension: {}", objectEntity.getKey());
        result.append("ERROR: Only supports NITF format!");
      } else {
        LOGGER.info("Starting Tile Pyramid Generation.");
        long startIngest = System.currentTimeMillis();
        TilePyramidBuilder builder = new TilePyramidBuilder();
        final ImageKey fallbackImageKey = new ImageKey(FilenameUtils.removeExtension(objectEntity.getKey()), ZonedDateTime.ofInstant(Instant.ofEpochMilli(record.getEventTime().getMillis()), ZoneOffset.UTC), ZonedDateTime.now(ZoneOffset.UTC));
        builder.buildTilePyramid(getBucketObjectSupplier(bucketEntity.getName(), objectEntity.getKey()),this,fallbackImageKey);
        long endIngest = System.currentTimeMillis();
        publishIngestTimeMetric(endIngest - startIngest, "NITF");
        LOGGER.info("Completed Tile Pyramid Generation.");
        result.append("SUCCESS: ").append(bucketEntity.getName()).append("/").append(objectEntity.getKey());
      }
    } catch (Exception e) {
      LOGGER.error("Exception Caught: ", e);
      result.append("ERROR: ").append(e.getMessage()).append("\n");
    }
    return result.toString();
  }

  private CheckedSupplier<InputStream, IOException> getBucketObjectSupplier(String bucketName, String objectKey) {
     return () -> {
        S3Object object = s3Client.getObject(bucketName, objectKey);
        return new BufferedInputStream(object.getObjectContent());
     };
  }

  private void publishIngestTimeMetric(long duration, String format) {
    Dimension dimension = new Dimension()
        .withName("FORMAT")
        .withValue(format);

    MetricDatum datum = new MetricDatum()
        .withMetricName("IMAGE_INGEST_TIME")
        .withUnit(StandardUnit.Seconds)
        .withValue(duration / 1000.0)
        .withDimensions(dimension);

    PutMetricDataRequest request = new PutMetricDataRequest()
        .withNamespace("ODS/INGEST")
        .withMetricData(datum);

    PutMetricDataResult response = cwClient.putMetricData(request);
  }

  private String getObjectExtension(String objectKey) {
    int dotIndex = objectKey.lastIndexOf('.');
    if (dotIndex < 0) {
      return "";
    }
    return objectKey.substring(dotIndex + 1).toLowerCase();
  }
}
