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

package com.leidoslabs.holeshot.chipper;

import java.awt.Dimension;
import java.awt.Rectangle;
import java.net.URL;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.Response;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.leidoslabs.holeshot.chipper.request.ChipGeoCenterRequest;
import com.leidoslabs.holeshot.chipper.request.ChipGeoEnvelopeRequest;
import com.leidoslabs.holeshot.chipper.request.ChipRequest;
import com.leidoslabs.holeshot.chipper.request.ImageChipRequest;

@Path("/chipper")
/**
 * REST API for ImageChipper. Provides get implementations for chipping by
 * providing imagespace bounding box, geo center coord + radius, or geographic
 * envelope
 */
public class ImageChipperService {
	private static final Logger LOGGER = LoggerFactory.getLogger(ImageChipperService.class);

	private final static int CACHE_MAX_AGE_IN_SECS = 60 * 60 * 24 * 365; // ONE YEAR
	private final static CacheControl CACHE_CONTROL = CacheControl
			.valueOf(String.format("public, max-age=%d", CACHE_MAX_AGE_IN_SECS));

	@GET
	@Produces({ "image/png" })
	@Path("/chip")
	/**
	 * Chip image by image space bounding box
	 * @param imageMetadataURL
	 * @param chipBoundingBox         chip's bounding box
	 * @param chipOutputDimension
	 * @param lockHistogramToOverview
	 * @return
	 */
	public Response chip(@QueryParam("url")URL imageMetadataURL,
			@QueryParam("bbox")Rectangle chipBoundingBox,
			@QueryParam("outDim") Dimension chipOutputDimension,
			@DefaultValue("false") @QueryParam("lockHistogramToOverview") boolean lockHistogramToOverview) {
		
		ChipRequest req = new ImageChipRequest(imageMetadataURL, chipBoundingBox, chipOutputDimension, lockHistogramToOverview);
		return Response.ok(req.getChip()).cacheControl(CACHE_CONTROL).build();
	}

	@GET
	@Produces({ "image/png" })
	@Path("/chipByGeoPoint")
	/**
	 * Chip image by world space center coordinate + radius
	 * @param imageMetadataURL
	 * @param chipGeoCenter
	 * @param radius
	 * @param chipOutputDimension
	 * @param lockHistogramToOverview
	 * @return
	 */
	public Response chip(@QueryParam("url")URL imageMetadataURL,
			@QueryParam("center")Coordinate chipGeoCenter,
			@QueryParam("radius")double radius,
			@QueryParam("outDim")Dimension chipOutputDimension,
			@DefaultValue("false") @QueryParam("lockHistogramToOverview") boolean lockHistogramToOverview) {
		ChipRequest req = new ChipGeoCenterRequest(imageMetadataURL, chipGeoCenter, radius, chipOutputDimension, lockHistogramToOverview);
		return Response.ok(req.getChip()).cacheControl(CACHE_CONTROL).build();
	}

	@GET
	@Produces({ "image/png" })
	@Path("/chipByGeoEnvelope")
	/**
	 * Chip image by world space envelope
	 * @param imageMetadataURL
	 * @param envelope
	 * @param chipOutputDimension
	 * @param warpToFit
	 * @param lockHistogramToOverview
	 * @return
	 */
	public Response chip(@QueryParam("url")URL imageMetadataURL,
			@QueryParam("envelope")Envelope envelope,
			@QueryParam("outDim") Dimension chipOutputDimension,
			@DefaultValue("false") @QueryParam("lockHistogramToOverview")boolean lockHistogramToOverview
			) {
		ChipRequest req = new ChipGeoEnvelopeRequest(imageMetadataURL, envelope, chipOutputDimension, lockHistogramToOverview);
		return Response.ok(req.getChip()).cacheControl(CACHE_CONTROL).build();
	}

}
