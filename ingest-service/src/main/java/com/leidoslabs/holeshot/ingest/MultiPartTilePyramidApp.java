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

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;


/**
 * This is a sample desktop application demonstrating how to use the MultiPartTilePyramidBuilder.
 *
 * The arguments are a S3 bucket name followed by one or more full paths to a Digital Globe image
 * directory. e.g.
 *
 * output-tiles-bucket-name /data/SpaceNet/AOI_5_Khartoum/srcData/rasterData/RGB-PanSharpen/15APR13081815-S2AS-056155973020_01_P001.XML
 *
 *   or
 *
 * output-tiles-bucket-name s3://spacenet-dataset/AOI_5_Khartoum/srcData/rasterData/RGB-PanSharpen/15APR13081815-S2AS-056155973020_01_P001.XML
 */
public class MultiPartTilePyramidApp {

  private static final Logger LOGGER = LoggerFactory.getLogger(MultiPartTilePyramidApp.class);

  public static void main(String[] args) {

    try {
      CommandLineParser parser = new DefaultParser();
      Options options = new Options();
      options.addOption(
            Option.builder("m")
            .longOpt("metadataonly")
            .hasArg(false)
            .desc("Generate metadata.json files only (no image tiles)")
            .build()
            );

      CommandLine line = parser.parse(options,  args);
      final boolean metadataOnly = line.hasOption('m');

      final String[] leftoverArgs = line.getArgs();

      MultiPartImageAccessor accessor = null;
      if (leftoverArgs[1].startsWith("s3://")) {
        accessor = new S3ImageAccessor();
      } else {
        accessor = new FileSystemImageAccessor();
      }
      S3TileListener listener = new S3TileListener(leftoverArgs[0],null);
      MultiPartTilePyramidBuilder builder = new DGTiffTilePyramidBuilder(accessor,listener, metadataOnly);

      LOGGER.info("Writing tiles to bucket {}",leftoverArgs[0]);
      int lowestRsetProcessed = Integer.MAX_VALUE;
      for (int i=1;i<leftoverArgs.length;i++) {
        String imageFileName = leftoverArgs[i];

        LOGGER.info("Begin Processing: {}",imageFileName);
        long startTimestamp = System.currentTimeMillis();
        builder.buildTilePyramid(imageFileName);
        lowestRsetProcessed = Math.min(lowestRsetProcessed, builder.getLastRsetProcessed());
        LOGGER.info("Done Processing: {}; {}s elapsed", ((System.currentTimeMillis() - startTimestamp)/1000),"s elapsed.");
      }

      if (!metadataOnly) {
         final TilePyramidAccessor tileAccessor = new S3TilePyramidAccessor(leftoverArgs[0], builder.getImageKey());
         final MultiPartPyramidReducer reducer =
               new MultiPartPyramidReducer(tileAccessor, listener,
                     lowestRsetProcessed);
         reducer.reduce();
      }
    } catch (Exception e) {
      e.printStackTrace();
      System.exit(1);
    }
    System.exit(0);
  }
}
