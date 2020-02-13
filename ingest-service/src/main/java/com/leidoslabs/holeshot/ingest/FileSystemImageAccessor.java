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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

/**
 * An implementation of a multi-part image accessor reading files from the local filesystem.
 */
public class FileSystemImageAccessor implements MultiPartImageAccessor {

  private static final Logger LOGGER = LoggerFactory.getLogger(FileSystemImageAccessor.class);

  /**
   * @{inheritDoc}
   */
  @Override
  public InputStream getPart(String name) {
    File part = new File(name);
    try {
      if (part.exists() && part.canRead()) {
        return new FileInputStream(part);
      }
    } catch (FileNotFoundException e) {
    }
    LOGGER.warn("Unable to find file: {}",part.getAbsolutePath());
    return null;
  }


}
