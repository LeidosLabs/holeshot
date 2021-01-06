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
import java.util.function.DoublePredicate;

import org.locationtech.jts.geom.Envelope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.leidoslabs.holeshot.chipper.ImageChipper;

public class ChipGeoEnvelopeRequest extends ChipRequest {
	private static final Logger LOGGER = LoggerFactory.getLogger(ChipGeoEnvelopeRequest.class);
	private Envelope bboxEnvelope;

	public ChipGeoEnvelopeRequest(URL metadataUrl, Envelope bboxEnvelope, Dimension chipOutputDimension, boolean lockHistogramToOverview) {
		super(metadataUrl, chipOutputDimension, lockHistogramToOverview);
		setBboxEnvelope(bboxEnvelope);
	}

	public Envelope getBboxEnvelope() {
		return bboxEnvelope;
	}

	public void setBboxEnvelope(Envelope bboxEnvelope) {
		this.bboxEnvelope = bboxEnvelope;
	}

	protected void validateRequest() throws IOException {
		super.validateRequest();
		DoublePredicate lonVal = lng -> Math.abs(lng) <= 180;
		DoublePredicate latVal = lat -> Math.abs(lat) <= 90;
		// Ensure bbox has valid geographic coordinates,
		// and we ensure our geographic envelope intersects our image.
		if (this.bboxEnvelope == null
				|| !lonVal.test(bboxEnvelope.getMinX()) || !latVal.test(bboxEnvelope.getMinY())
				|| !lonVal.test(bboxEnvelope.getMaxX()) || !latVal.test(bboxEnvelope.getMaxY())
				|| bboxEnvelope.getMinX() == bboxEnvelope.getMaxX()
				|| bboxEnvelope.getMinY() == bboxEnvelope.getMaxY()
				|| !this.getImage().getTopTile().getGeodeticBounds().getEnvelopeInternal().intersects(bboxEnvelope)) {
			throw new IllegalArgumentException("invalid geo envelope coordinates");
		}
	}

	@Override
	public void chip(ImageChipper chipper, OutputStream os) throws InterruptedException, ExecutionException, Exception {
		String logStr = String.format("chipByGeo Envelope. Envelope: %s outdim: %s", this.bboxEnvelope.toString(), this.getChipOutputDimension().toString());
		LOGGER.debug(logStr);
		chipper.chipByGeo(this.bboxEnvelope, this.getChipOutputDimension(), os, 
				this.isLockHistogramToOverview());
	}
}

