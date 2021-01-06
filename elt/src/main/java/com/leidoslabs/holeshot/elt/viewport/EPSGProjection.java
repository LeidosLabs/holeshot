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

import java.util.Collection;

import org.apache.sis.geometry.DirectPosition2D;
import org.apache.sis.measure.Range;
import org.apache.sis.referencing.CRS;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.opengis.geometry.DirectPosition;
import org.opengis.geometry.MismatchedDimensionException;
import org.opengis.metadata.extent.Extent;
import org.opengis.metadata.extent.GeographicBoundingBox;
import org.opengis.metadata.extent.GeographicExtent;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;
import org.opengis.util.FactoryException;

import com.leidoslabs.holeshot.elt.coord.Angle;
import com.leidoslabs.holeshot.elt.coord.MapScale;
import com.leidoslabs.holeshot.elt.coord.Radians;

/**
 * @author robertsrg
 *
 */
public abstract class EPSGProjection extends WGS84Projection {
	private static final Angle DEFAULT_ANGLE_OFF_NORTH = new Radians(0.0);
	private final MathTransform wgsToTarget;
	private final MathTransform targetToWGS;
	private final CoordinateReferenceSystem targetCRS;
	private final Range<Double> xAxisRange;
	private final Range<Double> yAxisRange;
	
	protected EPSGProjection(String targetCRSString) {
		super();
		try {
			targetCRS = CRS.forCode(targetCRSString);
			wgsToTarget = CRS.findOperation(WGS84,  targetCRS,  null).getMathTransform();
			targetToWGS = CRS.findOperation(targetCRS, WGS84,  null).getMathTransform();
			final Envelope projEnv = getProjectedRegion(); 
			xAxisRange = new Range<Double>(Double.class, projEnv.getMinX(), true, projEnv.getMaxX(), true);
			yAxisRange = new Range<Double>(Double.class, projEnv.getMinY(), true, projEnv.getMaxY(), true);
		} catch (FactoryException e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}
	
	public Envelope getGeodeticDomainOfValidity() {
		Envelope domainOfValidity = null;
		final Extent extent = targetCRS.getDomainOfValidity();
		if (extent != null) {
			Collection<? extends GeographicExtent> geographicElements = extent.getGeographicElements();
			if (geographicElements != null) {
				GeographicBoundingBox geoBbox = geographicElements.stream().filter(e->GeographicBoundingBox.class.isInstance(e)).map(e->(GeographicBoundingBox)e).findFirst().orElse(null);
				if (geoBbox != null) {
					domainOfValidity = new Envelope(geoBbox.getWestBoundLongitude(), geoBbox.getEastBoundLongitude(), geoBbox.getSouthBoundLatitude(), geoBbox.getNorthBoundLatitude());
				}
			}
		}
		return domainOfValidity;
	}
		
	private Envelope getProjectedRegion() {
		Envelope domainOfValidity = getGeodeticDomainOfValidity();
		Coordinate llWorld = new Coordinate(domainOfValidity.getMinX(), domainOfValidity.getMinY());
		Coordinate urWorld = new Coordinate(domainOfValidity.getMaxX(), domainOfValidity.getMaxY());
		Coordinate llProj = transformTo(llWorld);
		Coordinate urProj = transformTo(urWorld);
		return new Envelope(llProj.x, urProj.x, llProj.y, urProj.y);
	}
	
	@Override
	public Range<Double> getXAxisRange() {
		return xAxisRange;
	}
	@Override
	public Range<Double> getYAxisRange() {
		return yAxisRange;
	}
	@Override
	public String getName() {
		return targetCRS.getName().toString();
	}

	@Override
	public Coordinate transformTo(Coordinate latLon) {
		Coordinate result = null;
		DirectPosition position;
		try {
			position = wgsToTarget.transform(new DirectPosition2D(WGS84, latLon.y, latLon.x), null);
			result = new Coordinate(position.getOrdinate(0), position.getOrdinate(1), 0.0);
		} catch (MismatchedDimensionException | TransformException e) {
			e.printStackTrace();
		}
		return result;
	}
	@Override
	public Coordinate transformFrom(Coordinate latLon) {
		Coordinate result = null;
		DirectPosition position;
		try {
			position = targetToWGS.transform(new DirectPosition2D(targetCRS, latLon.x, latLon.y), null);
			result = new Coordinate(position.getOrdinate(1), position.getOrdinate(0), 0.0);
		} catch (MismatchedDimensionException | TransformException e) {
			e.printStackTrace();
		}
		return result;
	}
	
	@Override
	public Angle getProjectionAngleOffNorth() {
		return DEFAULT_ANGLE_OFF_NORTH;
	}	
	@Override
	public double getXAxisDirection() {
		return 1.0;
	}
	@Override
	public double getYAxisDirection() {
		return 1.0;
	}
	@Override
	public double getGSD(Coordinate latLon) {
		// TODO: This only works for Web Mercator (Which is the only non-image projection we currently have).  Need to generalize
		return MapScale.FULL_MAP_SCALE.getGSD(latLon);
	}
}
