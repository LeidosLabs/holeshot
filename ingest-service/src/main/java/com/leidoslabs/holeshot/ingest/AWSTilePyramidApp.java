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
import java.io.File;
import java.io.FileInputStream;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Arrays;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.leidoslabs.holeshot.imaging.ImageKey;

/**
 * A command line app for running the AWS Tile Pyramid listener on a set of local image files. Form is
 * args: [-m/-metadataonly] [-f] bucketName metadataTopicName imageFileName(s)...
 * Good for local testing. For cloud operations, and running off an SQS Queue, use the SQSEventMonitorDaemon
 */
public class AWSTilePyramidApp extends AWSTilePyramidListener {
   private static final Logger LOGGER = LoggerFactory.getLogger(AWSTilePyramidApp.class);

    /**
     * Create a tile pyramid app that outputs tiles to s3 bucket bucketName and
     * publish metadata to SNS topic metadataTopicName
     * @param bucketName The s3 bucket to write tiles to
     * @param metadataTopicName The SNS topic to send metadata to when an image finishes processing
     * @throws Exception
     */
   public AWSTilePyramidApp(String bucketName,String metadataTopicName) throws Exception {
      super(bucketName,metadataTopicName);
   }


   public static void main(String[] args) {

      try {
         CommandLineParser parser = new DefaultParser();
         Options options = new Options();
         options.addOption(
               Option.builder("f")
               .longOpt("force")
               .hasArg(false)
               .desc("Bypass any confirmation prompts")
               .build()
               );

         options.addOption(
               Option.builder("m")
               .longOpt("metadataonly")
               .hasArg(false)
               .desc("Generate metadata.json files only (no image tiles)")
               .build()
               );

         CommandLine line = parser.parse(options,  args);

         final boolean force = line.hasOption('f');
         final boolean metadataOnly = line.hasOption('m');

         String[] leftoverArgs = line.getArgs();

         String bucketName = leftoverArgs[0];
         String metadataTopicName = leftoverArgs[1];
         LOGGER.info("Writing Tiles to S3 Bucket: " + bucketName);
         LOGGER.info("Writing Metadata to SNS Topic: " + metadataTopicName);

         TilePyramidBuilder builder = new TilePyramidBuilder(metadataOnly);

         final ZonedDateTime nowUTC = ZonedDateTime.now(ZoneOffset.UTC);

         for (String imageFileName: Arrays.copyOfRange(leftoverArgs,2,leftoverArgs.length)) {
            File imageFile = new File(imageFileName);
            AWSTilePyramidApp app = new AWSTilePyramidApp(bucketName,metadataTopicName);

            final ImageKey fallbackImageKey = new ImageKey(FilenameUtils.removeExtension(imageFile.getName()),
                                                           ZonedDateTime.ofInstant(Instant.ofEpochMilli(imageFile.lastModified()), ZoneOffset.UTC), nowUTC);

            if (!force) {
               System.out.println("Press ENTER to start processing: " + imageFileName + " ... ");
               System.in.read();
            }
            long startTimestamp = System.currentTimeMillis();
            LOGGER.info("  Starting at: " + startTimestamp);
            builder.buildTilePyramid(()->new BufferedInputStream(new FileInputStream(imageFile)), app, fallbackImageKey );
            long endTimestamp = System.currentTimeMillis();
            LOGGER.info("  Finished at: " + endTimestamp + " " + ((endTimestamp - startTimestamp)/1000) + "s elapsed.");
         }

      } catch (Exception e) {
         e.printStackTrace();
         System.exit(1);
      }
      System.exit(0);
   }
}
