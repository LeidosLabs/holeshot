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
import java.util.List;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.spatial4j.context.jts.JtsSpatialContext;
import org.locationtech.spatial4j.io.GeohashUtils;
import org.locationtech.spatial4j.shape.Rectangle;
import org.locationtech.spatial4j.shape.jts.JtsGeometry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.leidoslabs.holeshot.analytics.common.client.AnalyticsClient;
import com.leidoslabs.holeshot.analytics.common.model.RequestSummary;
import com.leidoslabs.holeshot.analytics.common.model.aoi.AOIElement;

public class UserHeatmapDataSource extends AOIDataSource<RequestSummary>{
	
	private static final Logger LOGGER = LoggerFactory.getLogger(UserHeatmapDataSource.class);
	
	private final AnalyticsClient client;
	private final long date;
	
	public UserHeatmapDataSource(String userID, AnalyticsClient client, long date) {
		super(userID);
		this.client = client;
		this.date = date;
	}
	
	@Override
	List<RequestSummary> fetch() throws IOException {
		List<RequestSummary> result;
		try {
			result = client.getSummaries(date, super.userID);
		} catch (IOException e) {
			LOGGER.error("Error fetching data from UserHeatMap");
			throw e;
		}
		return result;
	}

	@Override
	public AOIElement toElement(RequestSummary source) {
		JtsSpatialContext sc = JtsSpatialContext.GEO;
		GeometryFactory gFactory = sc.getShapeFactory().getGeometryFactory();
		Rectangle rect = GeohashUtils.decodeBoundary(source.getLocation(), sc);
        Coordinate ll = new Coordinate(rect.getMinX(), rect.getMinY());
        Coordinate lr = new Coordinate(rect.getMaxX(), rect.getMinY());
        Coordinate ur = new Coordinate(rect.getMaxX(), rect.getMaxY());
        Coordinate ul = new Coordinate(rect.getMinX(), rect.getMaxY());
		Polygon poly = gFactory.createPolygon(new Coordinate[] {ll, lr, ur, ul, ll});
		
		JtsGeometry geom = new JtsGeometry(poly, sc, false, false);
		AOIElement result = new AOIElement(geom.getGeom(), source.getRequestCount(), source.getStartTime(), source.getEndTime());
		return result;
	}

}
