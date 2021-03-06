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

import org.locationtech.jts.geom.Geometry;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class AOIElement {
	@JsonProperty("geometry")
	private Geometry geometry;
	@JsonProperty("importance")
	private float importance;
	@JsonProperty("startTime")
	private long startTime;
	@JsonProperty("endTime")
	private long endTime;
	
	public AOIElement(Geometry geometry, float importance, long startTime, long endTime) {
		this.geometry = geometry;
		this.importance = importance;
		this.startTime = startTime;
		this.endTime = endTime;
	}
	
	public AOIElement(Geometry geometry, float importance, long startTime) 	{
		this(geometry, importance, startTime, startTime);
	}
	
	public AOIElement (){}

	
	public Geometry getGeometry() {
		return geometry;
	}

	public void setGeometry(Geometry geometry) {
		this.geometry = geometry;
	}

	public float getImportance() {
		return importance;
	}

	public void setImportance(float importance) {
		this.importance = importance;
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

}
