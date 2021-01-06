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

import org.apache.sis.measure.Range;
import org.apache.sis.referencing.CRS;
import org.joml.Vector2d;
import org.joml.Vector2dc;
import org.locationtech.jts.geom.Coordinate;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.util.FactoryException;

import com.leidoslabs.holeshot.elt.coord.Angle;
import com.leidoslabs.holeshot.elt.coord.MapScale;
import com.leidoslabs.holeshot.imaging.coord.GeodeticCoordinate;

/**
 * @author robertsrg
 *
 */
public abstract class WGS84Projection {
	public static final String WGS84_EPSG = "EPSG:4326";
	public static final CoordinateReferenceSystem WGS84 = getWGS84();
	
	public abstract Coordinate transformTo(Coordinate latLon);
	public abstract Coordinate transformFrom(Coordinate latLon);
	
	public abstract Range<Double> getXAxisRange();
	public abstract Range<Double> getYAxisRange();
	
	public abstract double getGSD(Coordinate latLon);
	
	/**
	 * 
	 * @return 1.0 for axis that gets bigger as it goes up, -1.0 for axis that gets bigger as it goes down.
	 */
	public abstract double getXAxisDirection();

	/**
	 * 
	 * @return 1.0 for axis that gets bigger as it goes up, -1.0 for axis that gets bigger as it goes down.
	 */
	public abstract double getYAxisDirection();
	
	
	public abstract Angle getProjectionAngleOffNorth();
	
	public double getXAxisSpan() {
		return getRangeSpan(getXAxisRange());
	}
	public double getYAxisSpan() {
		return getRangeSpan(getYAxisRange());
	}
	public double getMaxAxisSpan() {
		return Math.max(getXAxisSpan(), getYAxisSpan());
	}
	public GeodeticCoordinate getCentroid() {
		return new GeodeticCoordinate(transformFrom(new Coordinate(getCenter(getXAxisRange()), getCenter(getYAxisRange()), 0.0)));
	}
	private static double getCenter(Range<Double> range) {
		return range.getMinValue() + getRangeSpan(range) / 2.0;
	}
	
	public MapScale getFullProjectionZoom(Coordinate coordinate) {
		double projectionGSD = getGSD(coordinate);
		return MapScale.mapScaleForGSD(coordinate, projectionGSD);
	}
	
	public Vector2dc getFullProjectionSizeAtScale(Coordinate coordinate, MapScale mapscale) {
		MapScale fullProjectionZoom = getFullProjectionZoom(coordinate);
		double scaleFactor = Math.pow(0.5, fullProjectionZoom.getZoom() - mapscale.getZoom());
		Vector2dc scaledSpan = new Vector2d(getXAxisSpan(), getYAxisSpan()).mul(scaleFactor);
		return scaledSpan;
	}
	
	public abstract MapScale getTopScale();
	
	private static double getRangeSpan(Range<Double> range) {
		return range.getMaxValue().doubleValue() - range.getMinValue().doubleValue();
	}
	public abstract String getName();
	
	/**
	 * @return
	 */
	private static CoordinateReferenceSystem getWGS84() {
		CoordinateReferenceSystem crs = null;
		try {
			crs = CRS.forCode(WGS84_EPSG);
		} catch (FactoryException e) {
			e.printStackTrace();
		}
		return crs;
	}
	
	@Override
	public boolean equals(Object obj) {
		return (obj instanceof WGS84Projection) &&
				getName().equals(((WGS84Projection)obj).getName());
	}
	
}
