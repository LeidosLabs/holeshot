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

package com.leidoslabs.holeshot.analytics.common.model.aoi;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.leidoslabs.holeshot.analytics.common.model.Indexable;

@JsonIgnoreProperties(ignoreUnknown = true)
public class UserAOI implements Indexable {
	@JsonProperty("elements")
	private List<AOIElement> elements;
	@JsonProperty("startTime")
	private long startTime;
	@JsonProperty("endTime")
	private long endTime;
	@JsonProperty("userID")
	private String userID;
	
	public UserAOI(List<AOIElement> elements, long startTime, long endTime, String userID) {
		this.elements = elements;
		this.startTime = startTime;
		this.endTime = endTime;
		this.userID = userID;
	}
	
	public UserAOI() {}

	public List<AOIElement> getElements() {
		return elements;
	}

	public void setElements(List<AOIElement> elements) {
		this.elements = elements;
	}

	public long getStartTime() {
		return startTime;
	}

	public void setStartTime(long startTime) {
		this.startTime = startTime;
	}

	public long getEndTime() {
		return endTime;
	}

	public void setEndTime(long endTime) {
		this.endTime = endTime;
	}

	public String getUserID() {
		return userID;
	}

	public void setUserID(String userID) {
		this.userID = userID;
	}

	@Override
	public String getId() {
		return userID + ":" + startTime + ":" + endTime;
	}
	
	
}
