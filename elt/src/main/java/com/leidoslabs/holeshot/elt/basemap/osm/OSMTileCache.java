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
package com.leidoslabs.holeshot.elt.basemap.osm;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.apache.commons.io.IOUtils;
import org.ehcache.Cache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.leidoslabs.holeshot.elt.tileserver.CoreImage;
import com.leidoslabs.holeshot.elt.utils.EHCache;
import com.leidoslabs.holeshot.elt.utils.KeyedLIFOExecutorService;

/**
 * @author robertsrg
 *
 */
public class OSMTileCache {
	private static final Logger LOGGER = LoggerFactory.getLogger(OSMTileCache.class);
	private KeyedLIFOExecutorService executorService;

	private static OSMTileCache instance = new OSMTileCache();
	private static final String TILE_CACHE_NAME = "OSM_TILE_CACHE";
	private static final int RETRIES = 3;


	private Cache<String, byte[]> imageCache;

	public static OSMTileCache getInstance() {
		return instance;
	}

	private OSMTileCache() {
		executorService = KeyedLIFOExecutorService.newFixedThreadPool(10);
		imageCache = EHCache.getInstance().getCacheManager().getCache(TILE_CACHE_NAME, String.class, byte[].class);
	}

	private <T> T getFromCache(Cache<String, T> cache, String key) {
		T value;
		//		synchronized (cache) {
		value = cache.get(key);
		//		}
		return value;
	}

	private <T> void setCache(Cache<String, T> cache, String key, T value) {
		//		synchronized(cache) {
		if (value == null) {
			removeFromCache(cache, key);
		} else {
			cache.put(key, value);
		}
		//		}
	}
	private <T> void removeFromCache(Cache<String, T> cache, String key) {
		//		synchronized(cache) {
		cache.remove(key);
		//		}
	}

	private InputStream tileInputStream(OSMTile tile, byte[] givenImageBytes) throws IOException {
		final String key = tile.getKey();

		byte[] imageBytes;
		imageBytes = (givenImageBytes == null) ? getFromCache(imageCache, key) : givenImageBytes;
		if (imageBytes == null) {
			URL url = tile.getURL();
		    HttpURLConnection httpcon = (HttpURLConnection) url.openConnection();
		    httpcon.addRequestProperty("User-Agent", "Holeshot v0.2");
			
			try ( InputStream tileStream = httpcon.getInputStream()) {
				imageBytes = IOUtils.toByteArray(tileStream);
				if ((imageBytes != null) && imageBytes.length > 0) {
					setCache(imageCache, key, imageBytes);
				}
			}
		}
		return imageBytes == null ? null : new ByteArrayInputStream(imageBytes);
	}

	private CoreImage readImage(OSMTile tile, byte[] imageBytes) throws IOException {
		CoreImage result = null;
		final String key = tile.getKey();

		for (int i=0;i<RETRIES && result == null;++i) {
			try (InputStream resultStream = tileInputStream(tile, imageBytes)) {
				CoreImage decodedImage = OSMTile.getTileImage(resultStream);
				result = decodedImage;
			} catch (IOException e) {
				LOGGER.error(String.format("Couldn't decode image (%s).  Removing from cache and retrying", key));
				e.printStackTrace();
				removeFromCache(imageCache, key);
			}
		}

		return result;
	}

	public CoreImage getTile(OSMTile tile, boolean waitForResult, boolean minPriority) throws IOException {
		CoreImage result = null;
		Future<?> future = null;
		final String tileKey = tile.getKey();

		synchronized (tileKey.intern()) {
			byte[] imageBytes = getFromCache(imageCache, tileKey);
			if (imageBytes != null) {
				result = readImage(tile, imageBytes);
			} else {
				if (!executorService.isInProcess(tileKey)) {
					Runnable tileTask = () -> {
						synchronized (tileKey.intern()) {
							if (!imageCache.containsKey(tileKey)) {
								try {
									readImage(tile, null);
								} catch (IOException e) {
									LOGGER.error("error reading " + tileKey);
									e.printStackTrace();
								}
							}
						}
					};
					if (minPriority) {
						future = executorService.submit(tileKey, tileTask, Long.MIN_VALUE);
					}
					else {
						future = executorService.submit(tileKey, tileTask);
					}
				}
			}
		}
		if (waitForResult && future != null) {
			try {
				future.get();

				// Get the result from the cache.
				result = getTile(tile, false, false);

			} catch (InterruptedException | ExecutionException e) {
				e.printStackTrace();
			}
		}
		return result;
	}


}
