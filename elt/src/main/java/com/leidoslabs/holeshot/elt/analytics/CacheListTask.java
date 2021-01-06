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

package com.leidoslabs.holeshot.elt.analytics;

import java.io.IOException;
import java.net.URL;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.management.RuntimeErrorException;

import com.leidoslabs.holeshot.elt.UserConfiguration;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.leidoslabs.holeshot.analytics.common.client.AnalyticsClient;
import com.leidoslabs.holeshot.analytics.common.model.CacheList;
import com.leidoslabs.holeshot.analytics.common.model.ScoredTile;
import com.leidoslabs.holeshot.elt.ELT;
import com.leidoslabs.holeshot.elt.tileserver.TileRef;
import com.leidoslabs.holeshot.elt.tileserver.TileserverImage;
import com.leidoslabs.holeshot.tileserver.v1.TileServerClient;

public class CacheListTask implements Runnable {
	private static final Logger LOGGER = LoggerFactory.getLogger(CacheListTask.class);

	private final AnalyticsClient analyticsClient;
	private final long intervalSec;
	private long lastRun;
	
	public CacheListTask(long intervalSec) {
		this.analyticsClient = new AnalyticsClient(UserConfiguration.getAnalyticsEndpoint(), UserConfiguration.getAnalyticsApiKey());
		this.intervalSec = intervalSec;
		lastRun = 0;
	}

	@Override
	public void run() {
		if (UserConfiguration.getUsername().equalsIgnoreCase("anonymous")) {
			LOGGER.warn("Cache suggestions disabled in anonymous mode");
			return;
		}
		try {
			LOGGER.debug("Fetching Tile Cache refs");
			analyticsClient.updateList(UserConfiguration.getUsername());
			try {
				Thread.sleep(intervalSec);
			} catch (InterruptedException e) {
				throw new RuntimeException(e);
			}
			List<CacheList> lists = analyticsClient.getCacheLists(UserConfiguration.getUsername(), lastRun);
			if (lists.size() > 0) {
				Set<TileRef> tiles = getCacheSuggestions(lists);
				LOGGER.debug("Queing " + tiles.size() + " tile suggestions from analytics");
				
				for(TileRef tile: tiles) {
					LOGGER.debug("Tile suggestion " + tile.getKey());
					tile.getTileImageMinPriority();
				}
			} else {
				LOGGER.debug("No new cache lists");
			}
			
		} catch(IOException | RuntimeException e) {
			LOGGER.error(e.getMessage());
			e.printStackTrace();
			throw new RuntimeException(e);
		}
		lastRun = System.currentTimeMillis();
			
	}
	
	public void cacheTiles(List<TileRef> suggested) {
		for (TileRef t : suggested) {
			
			try {
				t.getTileImageMinPriority();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	public Set<TileRef> getCacheSuggestions(List<CacheList> lists) {
		Set<TileRef> toCache = new HashSet<>();
		try {
			for(CacheList list: lists) {
				String modifiedLink = list.getImage().replaceFirst("https://tileserver*\\.leidoslabs.com/tileserver", UserConfiguration.getTileserviceEndpoint());
				URL metaURL = new URL(modifiedLink);
				TileserverImage tsi = new TileserverImage(metaURL);
				for (ScoredTile t : list.getTiles()) {
					for (int i = 0; i < tsi.getNumBands(); i++) {
						toCache.add(new TileRef(tsi, t.getrSet(), t.getX(), t.getY(), i));
					}
				}
			} 

		} catch (IOException e) {
			e.printStackTrace();
		}
		return toCache;
	}

}
