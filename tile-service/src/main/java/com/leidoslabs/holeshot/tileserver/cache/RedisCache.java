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
package com.leidoslabs.holeshot.tileserver.cache;

import java.io.File;
import org.slf4j.ext.XLogger;
import org.slf4j.ext.XLoggerFactory;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.ReadContext;
import io.lettuce.core.RedisClient;
/**
 * Setup Redis client for endpoint specified in CACHE_CONFIG. All methods are logged
 */
public class RedisCache {
  private static final XLogger logger = XLoggerFactory.getXLogger(RedisCache.class);

  private final static String CACHE_CONFIG = "/tmp/cacheclusterconfig";
  private RedisClient redisClient;
  private static RedisCache ourInstance = null;

  public static synchronized RedisCache getInstance() {
    logger.entry();
    if (ourInstance == null) {
      ourInstance = new RedisCache();
    }
    logger.exit();
    return ourInstance;
  }

  private RedisCache() {
    logger.entry();
    initialize();
    logger.exit();
  }

  /**
   * @return True if a call to getInstance has been invoked and has completed, otherwise false.
   */
  public boolean isAvailable() {
    logger.entry();
    boolean isAvailable = (redisClient != null);
    logger.exit(isAvailable);
    return isAvailable;
  }

  public RedisClient getRedis() {
    logger.entry();
    logger.exit();
    return redisClient;
  }

  /**
   * Setup Redis client at endpoint specified in CACHE_CONFIG.
   */
  public void initialize() {
    logger.entry();
    try {
      if (redisClient != null) {
        redisClient = null;
      }

      File cacheConfigFile = new File(CACHE_CONFIG);
      if (cacheConfigFile.canRead()) {
        ReadContext ctx = JsonPath.parse(new File(CACHE_CONFIG));

        String address = ctx.read("$.CacheClusters[0].CacheNodes[0].Endpoint.Address");
        Integer port = ctx.read("$.CacheClusters[0].CacheNodes[0].Endpoint.Port");

        redisClient = RedisClient.create(String.format("redis://%s:%d", address, port.intValue()));
      }
    } catch (Exception ex) {
      ex.printStackTrace();
    }
    logger.exit();
  }
}
