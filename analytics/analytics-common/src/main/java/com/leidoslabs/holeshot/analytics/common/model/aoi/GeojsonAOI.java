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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import org.image.common.geojson.GeoJsonModule;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Polygon;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Represents a potential source of UserAOI data that would come from geojson data
 * (e.g manually created AOIs in Tier1). Currently we don't have a real source for this 
 * in our analytics pipeline
 */
public class GeojsonAOI implements AOIConsumable {
	
	private Polygon polygon;
	private long startDate;
	private long endDate;
	
	public GeojsonAOI() {}
	
	public GeojsonAOI( InputStream geojsonStream, long startDate, long endDate) throws IOException {
		this.polygon = readPoly(geojsonStream);
		this.startDate = startDate;
		this.endDate = endDate;
	}
	
	public GeojsonAOI(InputStream geojsonStream, long startDate) throws IOException {
		this(geojsonStream, startDate, startDate);
	}


	public Polygon getPolygon() {
		return polygon;
	}

	public void setPolygon(Polygon polygon) {
		this.polygon = polygon;
	}
	
	public long getStartDate() {
		return startDate;
	}

	public void setStartDate(long startDate) {
		this.startDate = startDate;
	}

	public long getEndDate() {
		return endDate;
	}

	public void setEndDate(long endDate) {
		this.endDate = endDate;
	}
	
	public Polygon readPoly(InputStream stream) throws IOException {
		ObjectMapper mapper = new ObjectMapper();
		GeoJsonModule gjsonMod = new GeoJsonModule();
		mapper.registerModules(gjsonMod);
		return mapper.readValue(stream, Polygon.class);
	}
}
