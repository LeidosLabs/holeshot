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

import com.leidoslabs.holeshot.imaging.ImageKey;

public abstract class TilePyramidAccessor {
   private final ImageKey imageKey;
   
   protected TilePyramidAccessor(ImageKey imageKey) {
      this.imageKey = imageKey;
      
   }
   public ImageKey getImageKey() {
      return imageKey;
   }
   
   protected abstract String getPathSeparator();
   protected abstract String getBasePath();
   protected abstract InputStream open(String path) throws IOException;

    /**
     * Get the correctly formatted key for the MRF index file
     * @return key of image.idx using the correct path separator for this implementation
     */
   public String getMRFIndexKey() {
      return String.join(getPathSeparator(), imageKey.getName(getPathSeparator()), "image.idx");
   }

    /**
     * Get the correctly formatted key for the metadata.json file
     * @return key of metadata.json using the correct path separator for this implementation
     */
   public String getMetadataKey() {
      return String.join(getPathSeparator(), imageKey.getName(getPathSeparator()), "metadata.json");
   }

    /**
     * Get the correctly formatted tile key for the given tile coordinates
     * @param rlevel level of the tile in the reduced resolution pyramid
     * @param col column of the tile
     * @param row row of the tile
     * @param band band of the tile
     * @return a tile key using the correct path separator for this implementation
     */
   public String getTileKey(int rlevel, int col, int row, int band) {
      return String.format(String.join(getPathSeparator(), "%s", "%d", "%d", "%d", "%d.png"), imageKey.getName(getPathSeparator()), rlevel, col, row, band);
   }

    /**
     * Get the path to the metadata file for this image
     * @return a path to metadata.json
     */
   public String getMetadataPath() {
      return String.join(getPathSeparator(), getBasePath(), getMetadataKey());
   }

    /**
     * Get the path to the index (.idx) file for this image
     * @return a path to image.idx
     */
   public String getMRFIndexPath() {
      return String.join(getPathSeparator(), getBasePath(), getMRFIndexKey());
   }

    /**
     * Get the URI or file path for the tile, by joining the BasePath with the TileKey using the
     * PathSeparator as defined by their respective get functions
     * @param rlevel level of the tile in the reduced resolution pyramid
     * @param col column of the tile
     * @param row row of the tile
     * @param band band of the tile
     * @return resource path to the location of the tile
     */
   public String getTilePath(int rlevel, int col, int row, int band) {
      return String.join(getPathSeparator(), getBasePath(), getTileKey(rlevel, col, row, band));
   }

    /**
     * Open an InputStream to the file loacted at getMetdataPath()
     * @return InputStream of the metadata file
     * @throws IOException on failure to open the file
     */
   public InputStream getMetadata() throws IOException {
      return open(getMetadataPath());
   }

    /**
     * Open an InputStream to the tile at getTilePath for the referenced tile coordinates
     * @param rlevel level of the tile in the reduced resolution pyramid
     * @param col column of the tile
     * @param row row of the tile
     * @param band band of the tile
     * @return An InputStream to the tile data
     * @throws IOException on failure to open the tile data
     */
   public InputStream getTile(int rlevel, int col, int row, int band) throws IOException {
      return open(getTilePath(rlevel, col, row, band));
   }

    /**
     * List the subkeys associated with this image, primarily for Multi Part images where iteration over each
     * image subkey may be necessary to build the full image
     * @return A Set of keys referencing objects in the implementation-specific container for image parts
     * @throws IOException when the container holding the keys can not be enumerated
     */
   public abstract Set<String> listKeys() throws IOException;
   
}
