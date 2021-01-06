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

package com.leidoslabs.holeshot.analytics.caching.user;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.image.common.geojson.GeoJsonModule;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.operation.union.CascadedPolygonUnion;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
//import org.locationtech.spatial4j;
import org.locationtech.spatial4j.io.GeohashUtils;
import org.locationtech.spatial4j.shape.Rectangle;
import org.locationtech.spatial4j.context.jts.JtsSpatialContext;
import org.locationtech.spatial4j.shape.jts.JtsGeometry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.leidoslabs.holeshot.analytics.caching.user.data.AOIDataSource;
import com.leidoslabs.holeshot.analytics.caching.user.data.GeojsonDataSource;
import com.leidoslabs.holeshot.analytics.caching.user.data.UserHeatmapDataSource;
import com.leidoslabs.holeshot.analytics.common.client.AnalyticsClient;
import com.leidoslabs.holeshot.analytics.common.model.RequestSummary;
import com.leidoslabs.holeshot.analytics.common.model.aoi.AOIElement;
import com.leidoslabs.holeshot.analytics.common.model.aoi.UserAOI;

public class UserAOICron implements RequestStreamHandler {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(UserAOICron.class);

    private final AnalyticsClient client;
	
	public UserAOICron() {
        ObjectMapper mapper = new ObjectMapper();
		GeoJsonModule gjsonMod = new GeoJsonModule();
		mapper.registerModules(gjsonMod);
		mapper.enable(SerializationFeature.INDENT_OUTPUT);
		client = new AnalyticsClient(mapper);
	}

	// TODO this loop will blow out with too many users/a more complex AOI generator
    // I suggest moving the 'chron' component to its own function and then call this (remotely via API) from there
	@Override
	public void handleRequest(InputStream input, OutputStream output, Context context) throws IOException {
		long lastExecution = System.currentTimeMillis() - (1000 * 60 * 60 * Integer.parseInt(System.getenv("PERIOD_HOURS")));
        Set<String> users = client.getUsers();
		List<UserAOI> createdAOIs = new ArrayList<UserAOI>();
        for (String user : users) {
        	UserHeatmapDataSource heatmapData = new UserHeatmapDataSource(user, client, lastExecution);
        	LOGGER.debug("Fetching heatmap data for " + user);
        	heatmapData.populate();
        	 
        	//GeojsonDataSource gjsonData = new GeojsonDataSource(user);
        	//UserAOI aoi = AOIDataSource.aggregate(user, heatmapData, gjsonData);
        	LOGGER.debug("Aggregating AOI for  " + user);
        	UserAOI aoi = AOIDataSource.aggregate(user, heatmapData);
        	createdAOIs.add(aoi);
        }
        LOGGER.debug("Updating new AOIs");
        client.postAOIs(createdAOIs);
		
	}
	
	

	
	
    public static void main(String[] args) {
		UserAOICron cron = new UserAOICron();
		try {
			cron.handleRequest(null, null, null);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
