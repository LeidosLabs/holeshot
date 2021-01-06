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

import com.leidoslabs.holeshot.analytics.common.model.aoi.AOIElement;
import com.leidoslabs.holeshot.analytics.common.model.aoi.GeojsonAOI;

public class GeojsonDataSource extends AOIDataSource<GeojsonAOI> {

	public GeojsonDataSource(String userID) {
		super(userID);
	}

	@Override
	public AOIElement toElement(GeojsonAOI source) {
		// placeholder code so we calculate some importance based on size (heuristic: smaller -> more important)
		// Normalized so an area equal to 1/1000 the surface of the earth has importance 1, clamped to [0.01, 50]
		double importance = Math.min(Math.max(0.01, .041253 / source.getPolygon().getArea()), 50);
		return new AOIElement(source.getPolygon(), (float) importance, source.getStartDate(), source.getEndDate());
	}

	@Override
	List<GeojsonAOI> fetch() throws IOException {
		List<GeojsonAOI> gjsons = new ArrayList<GeojsonAOI>();
		gjsons.add(new GeojsonAOI(this.getClass().getResourceAsStream("airport.json"), System.currentTimeMillis()));
		gjsons.add(new GeojsonAOI(this.getClass().getResourceAsStream("bandaar.json"), System.currentTimeMillis()));
		return gjsons;
	}

}
