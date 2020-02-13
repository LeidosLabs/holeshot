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
package com.leidoslabs.holeshot.tileserver.v1;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.util.regex.Matcher;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * OfflineCache for tileserver client's fetches
 */
public class OfflineCache {
   private static final Logger LOGGER = LoggerFactory.getLogger(OfflineCache.class);
   private File rootDirectory;

   public OfflineCache(File rootDirectory) {
      this.rootDirectory = rootDirectory;
   }

   /**
    * @param uri
    * @return An InputStream for file located at this.rootDirectory + uriPath, if it exists
    */
   public InputStream getInputStream(URI uri) {
      InputStream result = null;
      File localFile = uriToFile(uri);

      if (localFile.exists()) {
         try {
            result = new FileInputStream(localFile);
         } catch (FileNotFoundException e) {
            LOGGER.error(e.getMessage(), e);
         }
      }
      return result;
   }

   private File uriToFile(URI uri) {
      final String server = uri.getHost();
      final String path = uri.getPath().replaceFirst("/", "").replaceAll("/", Matcher.quoteReplacement(File.separator));

      final String filename = String.join(File.separator, rootDirectory.getAbsolutePath(), server, path);
      return new File(filename);
   }

}
