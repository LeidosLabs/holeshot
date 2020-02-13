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
import java.io.IOException;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.leidoslabs.holeshot.imaging.ImageKey;

/**
 * Run the ingest process on local images. Arguments should be paths to local images,
 * output files will be written to the local filesystem at the level of the inputs parent directory
 */
public class TilePyramidApp {

   private static final Logger LOGGER = LoggerFactory.getLogger(TilePyramidApp.class);

   private TilePyramidListener tilePyramidListener;
   private TilePyramidBuilder tilePyramidBuilder;
   private File imageFile;

   public TilePyramidApp(File imageFile, TilePyramidBuilder tilePyramidBuilder, String outputPath) {
      this.tilePyramidListener = new FileSystemTileListener(outputPath);
      this.tilePyramidBuilder = tilePyramidBuilder;
      this.imageFile = imageFile;
   }

   public void invoke() throws TilePyramidException, IOException {
      final ImageKey fallbackImageKey = new ImageKey(FilenameUtils.removeExtension(imageFile.getName()), ZonedDateTime.ofInstant(Instant.ofEpochMilli(imageFile.lastModified()), ZoneOffset.UTC), ZonedDateTime.now(ZoneOffset.UTC) );

      tilePyramidBuilder.buildTilePyramid(()->new BufferedInputStream(new FileInputStream(imageFile)), tilePyramidListener, fallbackImageKey);
   }

   public static void main(String[] args) {

      try {

         TilePyramidBuilder builder = new TilePyramidBuilder();

         for (String imageFileName:args) {
            File imageFile = new File(imageFileName);
            LOGGER.info("Begin Processing: {}",imageFileName);
            long startTimestamp = System.currentTimeMillis();
            TilePyramidApp app = new TilePyramidApp(imageFile, builder, imageFile.getParent());
            app.invoke();
            LOGGER.info("Done Processing: {}; {}s elapsed",((System.currentTimeMillis() - startTimestamp)/1000),"s elapsed.");
         }

      } catch (Exception e) {
         e.printStackTrace();
         System.exit(1);
      }
      System.exit(0);
   }
}
