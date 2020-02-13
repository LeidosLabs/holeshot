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
import java.io.File;
import java.io.FileOutputStream;

import org.image.common.geojson.GeoJsonModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.util.IOUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.leidoslabs.holeshot.tileserver.mrf.MRFIndexFile;
import com.leidoslabs.holeshot.imaging.ImageKey;
import com.leidoslabs.holeshot.imaging.metadata.TilePyramidDescriptor;

/**
 * This implementation of the TilePyramidListener interface writes the tiles and metadata to the
 * local filesystem.
 *
 * The individual tiles are stored as PNGs in a directories under the output path as follows:
 *  outputPath/collectionID/timestamp/r-level/column/row/band.png
 *
 * The metadata is encoded to JSON and stored at: outputPath/collectionID/timestamp/metadata.json
 */
public class FileSystemTileListener extends TilePyramidListener {

   private final static Logger LOGGER = LoggerFactory.getLogger(FileSystemTileListener.class);

   private final ObjectMapper mapper;
   private final String outputPath;

   public FileSystemTileListener(String outputPath) {
      super();
      this.outputPath = outputPath;
      this.mapper = new ObjectMapper();
      this.mapper.registerModule(new GeoJsonModule());
      this.mapper.enable(SerializationFeature.INDENT_OUTPUT);
   }


   /**
    * @{inheritDoc}
    */
   @Override
   protected long handleTileInternal(ImageKey imageKey, int band, int rlevel, int c, int r, BufferedImage tile) {
      long tileSize = -1;
      try {
         final String filePath = new FileTilePyramidAccessor(outputPath, imageKey).getTilePath(rlevel, c, r, band);
         File tileFile = new File(filePath);
         tileFile.getParentFile().mkdirs();
         try (FileOutputStream fos = new FileOutputStream(tileFile)) {
            tileSize = this.writeImage(fos, tile);
         }
      } catch (Exception e) {
         LOGGER.error("Unable to write tile ({},{},{})",c,r,band,e);
      }
      return tileSize;
   }

   /**
    * @{inheritDoc}
    */
   @Override
   protected void handleMetadataInternal(ImageKey imageKey, TilePyramidDescriptor metadata) throws Exception {
      final String filePath = new FileTilePyramidAccessor(outputPath, imageKey).getMetadataPath();
      File metadataFile = new File(filePath);
      metadataFile.getParentFile().mkdirs();
      mapper.writeValue(metadataFile, metadata);
   }

   @Override
   protected void handleMRFInternal(ImageKey imageKey, MRFIndexFile mrfIndexFile) throws Exception {
      final String filePath = new FileTilePyramidAccessor(outputPath, imageKey).getMRFIndexPath();
      File outputFile = new File(filePath);
      outputFile.getParentFile().mkdirs();
      try (FileOutputStream fos = new FileOutputStream(outputFile)) {
         mrfIndexFile.writeToOutputStream(fos);
      }
   }
}
