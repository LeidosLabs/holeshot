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

package com.leidoslabs.holeshot.elt.coord;

import org.joml.Vector2ic;
import org.joml.Vector3dc;
import org.locationtech.jts.geom.Coordinate;

import com.leidoslabs.holeshot.elt.viewport.ImageWorld;
import com.leidoslabs.holeshot.imaging.coord.GeodeticCoordinate;

/**
 * Representation of Geodetic Coordinate with ELTCoordinate functionality
 */
public class GeodeticELTCoordinate extends ELTCoordinate<Coordinate> {
	public GeodeticELTCoordinate(ImageWorld windowViewport, GeodeticCoordinate geodeticCoordinate) {
		this(windowViewport, geodeticCoordinate.getSourceCoordinate());
	}
	public GeodeticELTCoordinate(ImageWorld windowViewport, Coordinate coordinate) {
		super(windowViewport, coordinate);
	}

	@Override
	public Coordinate getGeodeticCoordinate() {
		return getSourceCoordinate();
	}

	@Override
	public Vector3dc getOpenGLCoordinate() {
		return getImageWorld().geodeticToClip(getSourceCoordinate());
	}

	@Override
	public Vector2ic getScreenCoordinate() {
		return getImageWorld().geodeticToScreen(getSourceCoordinate());
	}

	@Override
	public Coordinate getProjectedCoordinate() {
		return getImageWorld().geodeticToProjected(getSourceCoordinate());
	}
}
