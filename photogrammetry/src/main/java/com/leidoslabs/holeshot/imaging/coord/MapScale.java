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
package com.leidoslabs.holeshot.imaging.coord;

import org.locationtech.jts.geom.Coordinate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.primitives.Doubles;

/**
 * @author robertsrg
 *
 */
public class MapScale {
	private static final Logger LOGGER = LoggerFactory.getLogger(MapScale.class);

	public static final double EARTH_RADIUS_METERS = 6378137.0;
	public static final double EARTH_CIRCUMFERENCE_METERS = 2.0 * Math.PI * EARTH_RADIUS_METERS;
	public static final double TOP_MAP_TILE_PIXELS = 256.0;
	public static final double EARTH_CIRCUMFERENCE_METERS_PER_PIXEL = EARTH_CIRCUMFERENCE_METERS / TOP_MAP_TILE_PIXELS;

	public static final double MAX_ZOOM = 25.0;
	public static final double MIN_ZOOM = 0.0;
	public static final double DEFAULT_ZOOM = MIN_ZOOM;



	private double zoom;

	public MapScale() {
		this(DEFAULT_ZOOM);
	}
	public MapScale(double zoom) {
		setZoom(zoom);
	}
	public MapScale(MapScale scale) {
		setZoom(scale.zoom);
	}

	public void set(MapScale scale) {
		setZoom(scale.zoom);
	}

	public double getProjectionRenderSize() {
		return TOP_MAP_TILE_PIXELS * Math.pow(2.0, zoom);
	}

	public double getGSD(Coordinate latLon) {
		return getMeters(latLon, 1.0);
	}

	public static MapScale mapScaleForGSD(Coordinate latLon, double gsd) {
		return new MapScale(getZoom(latLon, 1.0, gsd));
	}

	/**
	 * @return the zoom
	 */
	public double getZoom() {
		return zoom;
	}

	public double zoomBy(double zoomDelta) {
		return setZoom(zoom + zoomDelta);
	}

	/**
	 * @param zoom the zoom to set
	 */
	public double setZoom(double zoom) {
		if (zoom != this.zoom) { 
			this.zoom = Doubles.constrainToRange(zoom, MIN_ZOOM,  MAX_ZOOM);

			System.out.println(String.format("Attempted to set zoom to %f, set it to %f", zoom, this.zoom));
		}
		return this.zoom;
	}

	public double getMeters(Coordinate latLon, double pixels) {
		return pixels * getMetersPerPixel(latLon, zoom);
	}

	public static double getMetersPerPixel(Coordinate latLon, double zoom) {
		return EARTH_CIRCUMFERENCE_METERS_PER_PIXEL * Math.cos(Math.toRadians(latLon.y)) / Math.pow(2.0, zoom);
	}

	public static double getZoom(Coordinate latLon, double pixels, double meters) {
		return Math.log(Math.cos(Math.toRadians(latLon.y)) * pixels * EARTH_CIRCUMFERENCE_METERS_PER_PIXEL / meters) / Math.log(2.0);
	}
	
	public static void main(String[] args) {
		Coordinate coord = new Coordinate(55.27, 27.19, 0.0);

		for (int i=0;i<25;++i) {
			MapScale mapScale = new MapScale((double)i);
			double gsd = mapScale.getGSD(coord);
//			double gsd = getMetersPerPixel(coord,i);
			System.out.println(String.format("%d == %f", i, gsd));
		}
	}

}
