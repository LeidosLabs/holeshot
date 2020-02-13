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

import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.services.s3.event.S3EventNotification;
import com.amazonaws.services.sqs.AmazonSQSAsync;
import com.amazonaws.services.sqs.AmazonSQSAsyncClient;
import com.amazonaws.services.sqs.buffered.AmazonSQSBufferedAsyncClient;
import com.amazonaws.services.sqs.buffered.QueueBufferConfig;
import com.amazonaws.services.sqs.model.DeleteMessageRequest;
import com.amazonaws.services.sqs.model.DeleteMessageResult;
import com.amazonaws.services.sqs.model.Message;
import com.amazonaws.services.sqs.model.ReceiveMessageRequest;
import com.amazonaws.services.sqs.model.ReceiveMessageResult;
import com.amazonaws.util.StringUtils;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.apache.commons.daemon.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Monitor an SQS Queue for S3 Events (like image uploads) and submit them to
 * A tile listener for processing. Intended to be installed as a service daemon.
 */
public class SQSEventMonitorDaemon implements Daemon {

  private static final Logger LOGGER = LoggerFactory.getLogger(SQSEventMonitorDaemon.class);

  private String sqsQueueURL;
  private String bucketName;
  private String metadataTopicName;
  private SQSMonitorRunnable monitor;
  private Thread monitorThread;

  /**
   * Start a queue using the daemonContext to provide the SQS Queue URL to listen to
   * for s3 events, the bucket to publish processed tiles to, and the SNS topic
   * to publish metadata to, as the 1st, 2nd, and 3rd arguments respectively
   * @param daemonContext The service context as defined in the service tpl file
   * @throws DaemonInitException
   * @throws Exception
   */
  @Override
  public void init(DaemonContext daemonContext) throws DaemonInitException, Exception {
    LOGGER.info("SQS Event Monitor Initializing");
    this.sqsQueueURL = daemonContext.getArguments()[0];
    this.bucketName = daemonContext.getArguments()[1];
    this.metadataTopicName = daemonContext.getArguments()[2];
    LOGGER.info("  SQS Queue URL: {}",this.sqsQueueURL);
    LOGGER.info("  Output Tile Bucket: {}",this.bucketName);
    LOGGER.info("  Output Metadata Topic: {}",this.metadataTopicName);
  }

  /**
   * Begin continuous monitoring process
   * @throws Exception
   */
  @Override
  public void start() throws Exception {
    LOGGER.info("Starting SQS Event Monitor");
    this.monitor = new SQSMonitorRunnable();
    this.monitorThread = new Thread(monitor);
    monitorThread.start();
  }

  /**
   * Attempt to stop SQS monitor thread
   * @throws Exception
   */
  @Override
  public void stop() throws Exception {
    LOGGER.info("Stopping SQS Event Monitor");
    try {
      monitor.setRunning(false);
      monitorThread.join(300000);
    } catch (InterruptedException ie) {
      LOGGER.warn("Unable to gracefully shut down SQS polling thread. Exiting anyway.",ie);
    }
  }

  /**
   * Destroy SQS monitor thread
   */
  @Override
  public void destroy() {
    LOGGER.info("Destroying SQS Event Monitor");
    this.monitor = null;
    this.monitorThread = null;
  }

  /**
   * Thread implementing continuous SQS monitoring on top of the processing, and publishing behavior
   * of the TilePyramidListener
   */
  private class SQSMonitorRunnable extends AWSTilePyramidListener implements Runnable {

    private final QueueBufferConfig sqsQueueConfig = new QueueBufferConfig();
    private final AmazonSQSAsync sqsClient = new AmazonSQSBufferedAsyncClient(
        new AmazonSQSAsyncClient(new DefaultAWSCredentialsProviderChain()),sqsQueueConfig);

    /**
     * True if service is started and thread hasn't received stop() signal
     * @return running flag
     */
    public boolean isRunning() {
      return running;
    }

    /**
     * Set running flag to false to stop running monitor loop
     * @param running running flag
     */
    public void setRunning(boolean running) {
      this.running = running;
    }

    private boolean running = false;

    /**
     * Create a new runnable monitoring thread.
     * Defines the Tile Pyramid Listener parameters, including the bucket
     * to write tiles to and the SNS queue to publish metadata to based
     * on the daemon context.
     * @throws Exception
     */
    public SQSMonitorRunnable() throws Exception {
      super(SQSEventMonitorDaemon.this.bucketName,SQSEventMonitorDaemon.this.metadataTopicName);
    }

    /**
     * Begin continuously monitoring the SQS Queue and attempt to process s3 Events as they show up
     */
    @Override
    public void run() {
      running = true;
      LOGGER.info("SQS Polling Loop Running");
      while (running) {

        try {
          // Retrieve message using the max wait for slow polling (20s) and a 10m timeout on this
          // message. The expectation is that a single message will take awhile to process so there
          // isn't much benefit in retrieving several messages at once.
          // TODO: Monitor the image ingest times. If they're more than 10 minutes adjust SQS visibility timeout.
          ReceiveMessageRequest receiveMessageRequest = new ReceiveMessageRequest()
              .withMaxNumberOfMessages(1)
              .withWaitTimeSeconds(20)
              .withVisibilityTimeout(1200)
              .withQueueUrl(sqsQueueURL);

          LOGGER.info("Polling SQS: {}", sqsQueueURL);
          ReceiveMessageResult receiveMessageResult = sqsClient.receiveMessage(receiveMessageRequest);

          for (Message m : receiveMessageResult.getMessages()) {

            ObjectMapper mapper = new ObjectMapper();
            JsonNode node = mapper.readTree(m.getBody());
            String messageString = node.get("Message").asText();
            LOGGER.info("Message String: {}",messageString);

            if (! StringUtils.isNullOrEmpty(messageString)) {
              S3EventNotification s3Event = S3EventNotification.parseJson(messageString);
              LOGGER.info("Parsed s3Event {}",s3Event);
              if (s3Event != null && s3Event.getRecords() != null) {
                LOGGER.info("s3Event.getRecords().size() = {}",s3Event.getRecords().size());
                s3Event.getRecords().forEach(this::handleS3EventRecord);
              } else {
                LOGGER.warn("Unexpected Message in SQS Queue: {}", m.getBody());
              }

              DeleteMessageRequest deleteMessageRequest = new DeleteMessageRequest()
                  .withQueueUrl(sqsQueueURL)
                  .withReceiptHandle(m.getReceiptHandle());
              DeleteMessageResult deleteMessageResult = sqsClient.deleteMessage(deleteMessageRequest);
              if (deleteMessageResult.getSdkHttpMetadata().getHttpStatusCode() != 200) {
                LOGGER.error("Unable to remove message from queue");
                LOGGER.error("Result: {}", deleteMessageResult);
              }

            } else {
              LOGGER.warn("Unable to parse message from notification. {}",m.getBody());
            }

          }

        } catch (Exception e) {
          LOGGER.error("Exception in SQS Message Worker", e);
        }
      }
      LOGGER.info("SQS Polling Loop Stopped");
    }
  }

}
