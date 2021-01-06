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
import java.awt.Rectangle;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URL;
import java.util.concurrent.ExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.leidoslabs.holeshot.chipper.ImageChipper;


public class ImageChipRequest extends ChipRequest {
	private static final Logger LOGGER = LoggerFactory.getLogger(ImageChipRequest.class);

	private Rectangle chipRegion;

	public ImageChipRequest(URL metadataUrl, Rectangle chipRegion, Dimension chipOutputDimension, boolean lockHistogramToOverview) {
		super(metadataUrl, chipOutputDimension, lockHistogramToOverview);
		setChipRegion(chipRegion);
	}

	public Rectangle getChipRegion() {
		return chipRegion;
	}

	public void setChipRegion(Rectangle chipRegion) {
		this.chipRegion = chipRegion;
	}

	protected void validateRequest() throws IOException {
		super.validateRequest();

		// Ensure output dim is non null with positive dims
		if (this.chipRegion == null || this.chipRegion.width <= 0
				|| this.chipRegion.height <= 0
				|| !this.getImage().getR0ImageRectangle().intersects(this.chipRegion)) {
			throw new IllegalArgumentException("invalid chipRegion");
		}
	}

	@Override
	public void chip(ImageChipper chipper, OutputStream os) throws InterruptedException, ExecutionException, Exception {
		String logStr = String.format("chip Image Region: %s outdim: %s", this.chipRegion.toString(), this.getChipOutputDimension().toString());
		LOGGER.debug(logStr);
		chipper.chip(chipRegion, this.getChipOutputDimension(), os, this.isLockHistogramToOverview());
	}

}
