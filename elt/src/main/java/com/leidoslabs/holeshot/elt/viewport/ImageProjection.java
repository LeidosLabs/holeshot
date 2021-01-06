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

import java.awt.geom.Point2D;

import org.apache.sis.measure.Range;
import org.joml.Vector2dc;
import org.locationtech.jts.geom.Coordinate;
import org.opengis.util.FactoryException;

import com.leidoslabs.holeshot.elt.coord.Angle;
import com.leidoslabs.holeshot.elt.coord.MapScale;
import com.leidoslabs.holeshot.elt.coord.Radians;
import com.leidoslabs.holeshot.elt.tileserver.TileserverImage;
import com.leidoslabs.holeshot.elt.utils.GeometryUtils;
import com.leidoslabs.holeshot.imaging.coord.ImageScale;

/**
 * @author robertsrg
 *
 */
public class ImageProjection extends WGS84Projection {
	private final TileserverImage image;
	private MapScale topScale;

	public ImageProjection(TileserverImage image) throws FactoryException {
		super();
		this.image = image;
		setTopScale();
	}
	
	public TileserverImage getImage() {
		return image;
	}
	
	@Override
	public double getGSD(Coordinate latLon) {
		return image.getCameraModel().getGSD(GeometryUtils.toPoint2D(latLon));
	}

	@Override
	public Coordinate transformTo(Coordinate latLon) {
        latLon.setZ(image.getCameraModel().getDefaultElevation());
		return GeometryUtils.toCoordinate(image.getCameraModel().worldToImage(latLon));
	}

	@Override
	public Coordinate transformFrom(Coordinate latLon) {
		return image.getCameraModel().imageToWorld(GeometryUtils.toPoint(latLon));
	}

	@Override
	public Range<Double> getXAxisRange() {
		final int width = image.getR0ImageWidth();
		return new Range<>(Double.class, 0.0, true, (double)width, false);
	}

	@Override
	public Range<Double> getYAxisRange() {
		final int height = image.getR0ImageHeight();
		return new Range<>(Double.class, 0.0, true, (double)height, false);
	}

	@Override
	public String getName() {
		return image.getName();
	}

	@Override
	public Angle getProjectionAngleOffNorth() {
		return new Radians(image.getAngleOffNorth());
	}

	@Override
	public double getXAxisDirection() {
		return 1.0;
	}
	@Override
	public double getYAxisDirection() {
		return -1.0;
	}

	private void setTopScale() {
		ImageScale topLevelImageScale = ImageScale.forRset(image.getMaxRLevel());
		Vector2dc topLevelImage = GeometryUtils.toVector2d(topLevelImageScale.scaleDownToRset(new Point2D.Double(image.getR0ImageWidth(), image.getR0ImageHeight())));
		double maxDim = getMaxAxisSpan();
		MapScale fullProjectionScale = getFullProjectionZoom(GeometryUtils.toCoordinate(image.getGeodeticBounds().getCentroid()));
		double scaleDiff = Math.log(Math.max(topLevelImage.x(), topLevelImage.y())/maxDim) / Math.log(0.5);
		topScale = new MapScale(fullProjectionScale.getZoom() - scaleDiff);
	
	}
	
	public MapScale getTopScale() {
		return topScale;
	}
	
	
}
