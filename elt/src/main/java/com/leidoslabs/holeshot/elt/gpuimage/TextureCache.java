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
import java.time.Duration;
import java.util.EnumSet;

import org.ehcache.Cache;
import org.ehcache.config.ResourceType;
import org.ehcache.config.builders.CacheConfigurationBuilder;
import org.ehcache.config.builders.ExpiryPolicyBuilder;
import org.ehcache.config.builders.ResourcePoolsBuilder;
import org.ehcache.event.EventFiring;
import org.ehcache.event.EventOrdering;
import org.ehcache.expiry.ExpiryPolicy;
import org.ehcache.impl.events.CacheEventAdapter;
import org.image.common.util.CloseableUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.leidoslabs.holeshot.elt.utils.EHCache;

/**
 * EHCache implementation for Texture data 
 */
public class TextureCache {
	private static final Logger LOGGER = LoggerFactory.getLogger(TextureCache.class);
	private static final String TEXTURE_CACHE_NAME="TEXTURE_CACHE";

	private static final TextureCache instance = new TextureCache();
	private Cache<String, Texture> textureCache;

	public static TextureCache getInstance() {
		return instance;
	}

	private TextureCache() {
		textureCache = EHCache.getInstance().getCacheManager().getCache(TEXTURE_CACHE_NAME, String.class, Texture.class);
		textureCache.getRuntimeConfiguration().registerCacheEventListener(new TextureCacheListener(), EventOrdering.UNORDERED, EventFiring.ASYNCHRONOUS, EnumSet.of(EVICTED, EXPIRED, REMOVED, UPDATED));
		textureCache.getRuntimeConfiguration().getResourcePools().getPoolForResource(ResourceType.Core.HEAP);
	}

	
	public long getMaxTextures() {
		return textureCache.getRuntimeConfiguration().getResourcePools().getPoolForResource(ResourceType.Core.HEAP).getSize();
	}

	private <T> T getFromCache(Cache<String, T> cache, String key) {
		T value;
		value = cache.get(key);
		return value;
	}

	private <T> void setCache(Cache<String, T> cache, String key, T value) {
		if (value == null) {
			removeFromCache(cache, key);
		} else {
			cache.put(key, value);
		}
	}
	private <T> void removeFromCache(Cache<String, T> cache, String key) {
		cache.remove(key);
	}

	public boolean containsKey(String key) {
		return textureCache.containsKey(key);
	}

	public Texture getTexture(String textureKey) { 
		return getFromCache(textureCache, textureKey);
	}
	public void setTexture(String textureKey, Texture texture) {
		if (getTexture(textureKey) != null) {
			LOGGER.error(String.format("TextureCache:: Attempted to overwrite value for key %s.  Shouldn't happen, ignoring.", textureKey), new Throwable());
		} else {
			setCache(textureCache, textureKey, texture);
		}
	}

	public void clearCache() {
		textureCache.clear();
	}

	private class TextureCacheListener extends CacheEventAdapter<String, Texture> {
		public TextureCacheListener() {
			super();
		}
		@Override
		protected void onEviction(String key, Texture evictedValue) {
			cleanupTexture(evictedValue);
		}
		@Override
		protected void onExpiry(String key, Texture expiredValue) {
			cleanupTexture(expiredValue);
		}
		@Override
		protected void onRemoval(String key, Texture removedValue) {
			cleanupTexture(removedValue);
		}
		@Override
		protected void onUpdate(String key, Texture oldValue, Texture newValue) {
			cleanupTexture(oldValue);
		}
		private void cleanupTexture(Texture texture) {
			try {
				CloseableUtils.close(texture);
			} catch (IOException e) {
				LOGGER.error(e.getMessage(), e);
			}
		}
	}
}
