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
package com.leidoslabs.holeshot.elt.utils;

import java.net.URL;

import org.ehcache.CacheManager;
import org.ehcache.config.Configuration;
import org.ehcache.config.builders.CacheManagerBuilder;
import org.ehcache.xml.XmlConfiguration;

/**
 * Offline EHCache
 */
public class EHCache {
   private static EHCache instance = new EHCache();

   private boolean shutdown = false;
   private CacheManager cacheManager;

   public static EHCache getInstance() {
      return instance;
   }
   
   /**
    * Configure EHCache from ehcache-config.xml
    */
   private EHCache() {
      try {
         URL configURL = getClass().getResource("/ehcache-config.xml");
         Thread.currentThread().setContextClassLoader(EHCache.class.getClassLoader());
         Configuration xmlConfig = new XmlConfiguration(configURL);
         cacheManager = CacheManagerBuilder.newCacheManager(xmlConfig);
         cacheManager.init();
      } catch (Throwable e) {
         e.printStackTrace();
      }

      Runtime.getRuntime().addShutdownHook(new Thread() {
         @Override
         public void run() {
            shutdown();
         }
      });
   }

   public CacheManager getCacheManager() {
      return cacheManager;
   }

   public synchronized void shutdown() {
      if (!shutdown) {
         shutdown = true;

         cacheManager.close();
      }

   }
}
