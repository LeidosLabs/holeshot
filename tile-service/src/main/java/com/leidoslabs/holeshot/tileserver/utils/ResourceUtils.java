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
package com.leidoslabs.holeshot.tileserver.utils;

import java.io.Closeable;
import java.io.IOException;
import org.apache.commons.pool2.ObjectPool;
import org.slf4j.ext.XLogger;
import org.slf4j.ext.XLoggerFactory;

/**
 * Collection of static methods for handling common operations that throw exceptions
 */
public class ResourceUtils {
  private static final XLogger logger = XLoggerFactory.getXLogger(ResourceUtils.class);

  public static <T> void returnToPoolQuietly(ObjectPool<T> pool, T item) {
    if (item != null) {
      try {
        pool.returnObject(item);
      } catch (Exception e) {
        logger.error(e.getMessage(), e);
      }
    }
  }
  
  public static void closeQuietly(Closeable closeable) {
    if (closeable != null) {
      try {
        closeable.close();
      } catch (IOException ioe) {
        logger.error(ioe.getMessage(), ioe);
      }
    }
  }
}
