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

package com.leidoslabs.holeshot.analytics.caching.user.data;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.leidoslabs.holeshot.analytics.common.model.aoi.AOIConsumable;
import com.leidoslabs.holeshot.analytics.common.model.aoi.AOIElement;
import com.leidoslabs.holeshot.analytics.common.model.aoi.UserAOI;

public abstract class AOIDataSource<T extends AOIConsumable> implements DataAOIAdapter<T> {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(AOIDataSource.class);

	protected List<AOIElement> elements;
	private boolean populated;
	protected String userID;
	
	public AOIDataSource(String userID) {
		this.userID = userID;
		this.populated = false;
	}
	
	public final void populate() throws IOException {
		List<T> data = fetch();
		// We could potentially add some validation here 
		elements = data.stream().map(d -> toElement(d)).collect(Collectors.toList());
		populated = true;
	}
	
	abstract List<T> fetch() throws IOException;
	
	public List<AOIElement> getElements() {
		return elements;
	}

	public String getUserID() {
		return userID;
	}
	
	@SafeVarargs
	public static UserAOI aggregate(String userID, AOIDataSource<? extends AOIConsumable> ... adapters ) {
		List<AOIElement> elements = new ArrayList<AOIElement>();
		long startTime = Long.MAX_VALUE;
		long endTime = Long.MIN_VALUE;
		
		for (AOIDataSource<?> adapter : adapters) {
			if (adapter.populated) {
				for (AOIElement elem : adapter.getElements()) {
					startTime = Math.min(elem.getStartTime(), startTime);
					endTime = Math.max(elem.getEndTime(), endTime);
					elements.add(elem);
				}
			} else {
				LOGGER.warn("Warning: adapter of type " + adapter.getClass() + "is not populated. Skipping");
			}
		}
		if (elements.size() > 0) {
			return new UserAOI(elements, startTime, endTime, userID);
		}
		return null;
		
	}
	


} 
