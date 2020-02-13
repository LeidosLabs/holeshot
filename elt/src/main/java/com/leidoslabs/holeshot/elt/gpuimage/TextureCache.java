/*
 * Licensed to Leidos, Inc. under one or more contributor license agreements.  
 * See the NOTICE file distributed with this work for additional information regarding copyright ownership.
 * Leidos, Inc. licenses this file to You under the Apache License, Version 2.0
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

package com.leidoslabs.holeshot.elt.gpuimage;

import static org.ehcache.event.EventType.EVICTED;
import static org.ehcache.event.EventType.EXPIRED;
import static org.ehcache.event.EventType.REMOVED;
import static org.ehcache.event.EventType.UPDATED;

import java.io.IOException;
import java.util.EnumSet;

import org.ehcache.Cache;
import org.ehcache.event.EventFiring;
import org.ehcache.event.EventOrdering;
import org.ehcache.impl.events.CacheEventAdapter;
import org.image.common.util.CloseableUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.leidoslabs.holeshot.elt.ELTImageTexture;
import com.leidoslabs.holeshot.elt.utils.EHCache;

/**
 * EHCache implementation for Texture data 
 */
public class TextureCache {
   private static final Logger LOGGER = LoggerFactory.getLogger(TextureCache.class);
   private static final String TEXTURE_CACHE_NAME="TEXTURE_CACHE";

   private static final TextureCache instance = new TextureCache();
   private Cache<String, ELTImageTexture> textureCache;

   public static TextureCache getInstance() {
      return instance;
   }

   private TextureCache() {
      textureCache = EHCache.getInstance().getCacheManager().getCache(TEXTURE_CACHE_NAME, String.class, ELTImageTexture.class);
      textureCache.getRuntimeConfiguration().registerCacheEventListener(new TextureCacheListener(), EventOrdering.UNORDERED, EventFiring.ASYNCHRONOUS, EnumSet.of(EVICTED, EXPIRED, REMOVED, UPDATED));
   }


   private <T> T getFromCache(Cache<String, T> cache, String key) {
      T value;
      synchronized (cache) {
         value = cache.get(key);
      }
      return value;
   }

   private <T> void setCache(Cache<String, T> cache, String key, T value) {
      synchronized(cache) {
         if (value == null) {
            removeFromCache(cache, key);
         } else {
            cache.put(key, value);
         }
      }
   }
   private <T> void removeFromCache(Cache<String, T> cache, String key) {
      synchronized(cache) {
         cache.remove(key);
      }
   }

   public ELTImageTexture getTexture(String textureKey) {
      return getFromCache(textureCache, textureKey);
   }
   public void setTexture(String textureKey, ELTImageTexture texture) {
      if (getTexture(textureKey) != null) {
         System.out.println("overwriting value for key " + textureKey);
         new Throwable().printStackTrace(System.out);
      }
      setCache(textureCache, textureKey, texture);
   }

   private class TextureCacheListener extends CacheEventAdapter<String, ELTImageTexture> {
      public TextureCacheListener() {
         super();
      }
      @Override
      protected void onEviction(String key, ELTImageTexture evictedValue) {
         cleanupTexture(evictedValue);
      }
      @Override
      protected void onExpiry(String key, ELTImageTexture expiredValue) {
         cleanupTexture(expiredValue);
      }
      @Override
      protected void onRemoval(String key, ELTImageTexture removedValue) {
         cleanupTexture(removedValue);
      }
      @Override
      protected void onUpdate(String key, ELTImageTexture oldValue, ELTImageTexture newValue) {
         cleanupTexture(oldValue);
      }
      private void cleanupTexture(ELTImageTexture texture) {
         try {
            CloseableUtils.close(texture);
         } catch (IOException e) {
            LOGGER.error(e.getMessage(), e);
         }
      }
   }
}
