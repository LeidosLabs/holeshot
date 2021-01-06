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
package com.leidoslabs.holeshot.elt.viewport;

import com.leidoslabs.holeshot.elt.coord.MapScale;

/**
 * @author robertsrg
 *
 */
public class WebMercatorProjection extends EPSGProjection {
	private static final String PSEUDO_MERCATOR_EPSG = "EPSG:3857";
	private static MapScale TOP_SCALE = null;
	
	public WebMercatorProjection() {
		super(PSEUDO_MERCATOR_EPSG);
	}
	
	@Override
	public MapScale getTopScale() {
		if (TOP_SCALE == null) {
			TOP_SCALE = new MapScale(0.0);
		}
		return TOP_SCALE;
	}
}
