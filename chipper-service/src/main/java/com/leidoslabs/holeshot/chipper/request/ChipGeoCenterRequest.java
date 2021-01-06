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

package com.leidoslabs.holeshot.chipper.request;

import java.awt.Dimension;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URL;
import java.util.concurrent.ExecutionException;

import org.locationtech.jts.geom.Coordinate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.leidoslabs.holeshot.chipper.ImageChipper;

public class ChipGeoCenterRequest extends ChipRequest {
	private static final Logger LOGGER = LoggerFactory.getLogger(ChipGeoCenterRequest.class);
	
	
	private Coordinate chipGeoCenter;
	private double radiusInMeters;

	public ChipGeoCenterRequest(URL metadataUrl, Coordinate chipGeoCenter, double radiusInMeters,
			Dimension chipOutputDimension, boolean lockHistogramToOverview) {
		super(metadataUrl, chipOutputDimension, lockHistogramToOverview);
		setChipGeoCenter(chipGeoCenter);
		setRadiusInMeters(radiusInMeters);

	}

	public Coordinate getChipGeoCenter() {
		return chipGeoCenter;
	}

	public void setChipGeoCenter(Coordinate chipGeoCenter) {
		this.chipGeoCenter = chipGeoCenter;
	}

	public double getRadiusInMeters() {
		return radiusInMeters;
	}

	public void setRadiusInMeters(double radiusInMeters) {
		this.radiusInMeters = radiusInMeters;
	}

	protected void validateRequest() throws IOException {
		super.validateRequest();

		// Ensure chip center is a valid geo coordinate, and that radius is positive
		if (this.chipGeoCenter == null || Math.abs(this.chipGeoCenter.getX()) > 180
				|| Math.abs(this.chipGeoCenter.getX()) > 90) {
			throw new IllegalArgumentException("invalid geo center coordinates");
		}
		if (radiusInMeters <= 0) {
			throw new IllegalArgumentException("invalid radius");
		}
	}

	@Override
	public void chip(ImageChipper chipper, OutputStream os) throws InterruptedException, ExecutionException, Exception {
		String logStr = String.format("chipByGeo Center. Center: %s radius: %f outdim: %s", this.chipGeoCenter.toString(), radiusInMeters, this.getChipOutputDimension().toString());
		LOGGER.debug(logStr);
		chipper.chipByGeo(this.chipGeoCenter, this.radiusInMeters, this.getChipOutputDimension(), os, this.isLockHistogramToOverview());
	}
}
