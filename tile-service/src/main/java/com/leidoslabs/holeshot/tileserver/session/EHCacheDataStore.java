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
package com.leidoslabs.holeshot.tileserver.session;

import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.commons.lang3.tuple.Pair;
import org.eclipse.jetty.server.session.AbstractSessionDataStore;
import org.eclipse.jetty.server.session.SessionData;
import org.ehcache.Cache;
import org.ehcache.CacheManager;
import org.ehcache.config.builders.CacheConfigurationBuilder;
import org.ehcache.config.builders.ResourcePoolsBuilder;
import org.slf4j.ext.XLogger;
import org.slf4j.ext.XLoggerFactory;
import com.leidoslabs.holeshot.tileserver.cache.EHCache;


/**
 * Datastore for our EHCache
 *
 */
public class EHCacheDataStore extends AbstractSessionDataStore {
  private static final int MAX_SESSIONS = 512;
  private static final XLogger logger = XLoggerFactory.getXLogger(EHCacheDataStore.class);
  private static final String CACHE_NAME_BASE="EHCacheDataStore";
  private static final Random random = new Random(System.currentTimeMillis());
  
  private static Cache<String, SessionData> cache;
  
  public EHCacheDataStore() {
    String cacheName = String.format("%s-%d", CACHE_NAME_BASE, random.nextInt());
    CacheManager cacheManager = EHCache.getInstance().getCacheManager();
    cache = cacheManager.createCache(cacheName, 
        CacheConfigurationBuilder.newCacheConfigurationBuilder(String.class, SessionData.class, ResourcePoolsBuilder.heap(MAX_SESSIONS)));
  }

  @Override
  public boolean isPassivating() {
    return true;
  }

  @Override
  public boolean exists(String id) throws Exception {
    return cache.containsKey(id);
  }

  @Override
  public SessionData doLoad(String id) throws Exception {
    return cache.get(id);
  }

  @Override
  public boolean delete(String id) throws Exception {
    boolean result = exists(id);
    cache.remove(id);
    return result;
  }

  @Override
  public void doStore(String id, SessionData data, long lastSaveTime) throws Exception {
    cache.put(id, data);
  }

  
  /**
   * Determines and returns expired elements
   */
  @Override
  public Set<String> doGetExpired(Set<String> candidates) {
    final long now = System.currentTimeMillis();
    Set<String> expired = candidates.stream()
        .map(c-> {
          Pair<String, SessionData> result = null;
          try {
            result = Pair.of(c, this.load(c));
          } catch (Exception e) {
            logger.error(e.getMessage(), e);
          }
          return result;
        })
        .filter(c->c.getRight()==null || c.getRight().getExpiry() < now)
        .map(c->c.getLeft())
        .collect(Collectors.toSet());
    return expired;
  }
}
