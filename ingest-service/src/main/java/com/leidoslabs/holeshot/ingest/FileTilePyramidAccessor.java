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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.leidoslabs.holeshot.imaging.ImageKey;

/**
 * TilePyramidAccessor utilizing a local filesystem as the backing store
 */
public class FileTilePyramidAccessor extends TilePyramidAccessor {
   private final String basePath;

    /**
     * Use the local filesystem at basePath to store or retrieve data related to imageKey
     * @param basePath The base file path to use for storage and retrieval
     * @param imageKey The image key associated with this tile ingest operation
     */
   public FileTilePyramidAccessor(String basePath, ImageKey imageKey) {
      super(imageKey);
      this.basePath = basePath;
   }

    /**
     * Walk the file tree rooted at basePath and return all paths (relative to basePath) of all regular files
     * @return A Set of relative path strings to files within basePath directory
     * @throws IOException On failure to enumerate files in the file tree
     */
   public Set<String> listKeys() throws IOException {
      Set<String> keys = ConcurrentHashMap.newKeySet();
      Path root = Paths.get(basePath);
      Files.walk(root).filter(Files::isRegularFile).forEach(s->keys.add(root.relativize(s).toString()));
      return keys;
   }

    /**
     * Get the standard OS path separated
     * @return java.io.File.separator
     */
   protected String getPathSeparator() {
      return File.separator;
   }

    /**
     * The base path associated with this file system accessor
     * @return basePath
     */
   protected String getBasePath() {
      return this.basePath;
   }

    /**
     * Open a FileInputStream for the file at Path
     * @param path The file to open, including the basePath, which must match this accessors base path
     * @return FileInputStream connection to the given file
     * @throws IOException On failure to connect to a FileInputStream from given path
     */
   protected InputStream open(String path) throws IOException {
      if (!path.startsWith(basePath)) {
         throw new IllegalArgumentException(String.format("this accessor can only read from path %s, not %s", this.basePath, path));
      }
      return new FileInputStream(path);
   }
}
