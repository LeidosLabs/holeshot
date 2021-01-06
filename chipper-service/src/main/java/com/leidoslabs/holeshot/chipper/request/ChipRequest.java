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
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URL;
import java.util.concurrent.ExecutionException;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.StreamingOutput;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

import com.leidoslabs.holeshot.chipper.ImageChipper;
import com.leidoslabs.holeshot.elt.tileserver.TileserverImage;

public abstract class ChipRequest {
	private static final Logger LOGGER = LoggerFactory.getLogger(ChipRequest.class);

	private Dimension chipOutputDimension;
	private boolean lockHistogramToOverview;
	private URL metadataUrl;
	private TileserverImage img;

	public ChipRequest(URL metadataUrl, Dimension chipOutputDimension, boolean lockHistogramToOverview) {
		this.metadataUrl = metadataUrl;
		this.chipOutputDimension = chipOutputDimension;
		this.lockHistogramToOverview = lockHistogramToOverview;
	}

	public ChipRequest(URL metadataUrl, Dimension chipOutputDimension) {
		this(metadataUrl, chipOutputDimension, false);
	}

	public Dimension getChipOutputDimension() {
		return chipOutputDimension;
	}

	public boolean isLockHistogramToOverview() {
		return lockHistogramToOverview;
	}

	public URL getMetadataUrl() {
		return metadataUrl;
	}
	
	public TileserverImage getImage() {
		return this.img;
	}

	
	protected void validateRequest() throws IOException {
		// Ensure output dim is non null with positive dims with positive area
		if (this.chipOutputDimension == null || this.chipOutputDimension.width <= 0 || this.chipOutputDimension.height <= 0) {
			throw new IllegalArgumentException("invalid chip output dimension");
		}
		// We attempt to create TileServerImage to validate URL before we borrow from pool
		this.img = ImageChipper.getTileserverImage(this.getMetadataUrl());
	}

	/**
	 * Perform the appropriate type of ImageChipper function on given chipper and write to os
	 * @param os
	 * @throws InterruptedException
	 * @throws ExecutionException
	 * @throws Exception
	 */
	public abstract void chip(ImageChipper chipper, OutputStream os) throws InterruptedException, ExecutionException, Exception;

	/**
	 * We perform validation on a chip request before borrowing so we don't waste resources on a bad request.
	 * validation typically includes checking metadata url, valid output dimensions, and for a valid chip region. 
	 * @return ImageChipper borrowed from pool
	 * @throws IOException invalid chipper request
	 * @throws Exception something went wrong when borrowing from chipper pool
	 */
	public final ImageChipper validateAndBorrow() throws IOException, Exception{
		this.validateRequest();
		return ImageChipper.borrow(this.img);
	}
	
	
	
	public final StreamingOutput getChip() {
		return new StreamingOutput() {
			public void write(OutputStream os) throws IOException, WebApplicationException {
				ImageChipper chipper = null;
				try {
					chipper = ChipRequest.this.validateAndBorrow();
					ChipRequest.this.chip(chipper, os);
				} catch (IllegalArgumentException e) {
					LOGGER.error("Illegal Argument error:", e);
					throw new WebApplicationException(Response.status(Status.BAD_REQUEST).build());
				} catch (FileNotFoundException e) {
					LOGGER.error("Bad tileserver url/resource not found:", e);
					throw new WebApplicationException(Response.status(Status.NOT_FOUND).build());
				} catch (ExecutionException | InterruptedException e) {
					try {
						// if we encounter an error from chipper's executor service, its possible the chippers state was corrupted so we destroy and recreate that chipper
						LOGGER.error("Runtime error during chip: Destroying and recreating current chipper object", e);
						ImageChipper.recreate(chipper);
					} catch (Exception e1) {
						handleFailedRecreate(e1);
					}
					chipper = null;
					throw new WebApplicationException(Response.serverError().build());
				} catch (Exception e) {
					LOGGER.error("Unknown exception: ", e);
					throw new WebApplicationException(Response.serverError().build());
				} finally {
					if (chipper != null) {
						chipper.returnObject();
					}
				}
			}
		};
	}
	
	/**
	 * If we are unable to destroy and recreate a corrupted chipper object,
	 * we attempt to close and recreate a new chipper pool
	 * @param e recreation exception
	 * @throws WebApplicationException guaranteed
	 */
	private void handleFailedRecreate(Exception e) throws WebApplicationException {
		try {
			LOGGER.error("Something went wrong when invalidating corrupted chipper. Recreating pool", e);
			ImageChipper.recreatePool();
		} catch (Exception e1) {
			Marker fatalMark = MarkerFactory.getMarker("FATAL");
			LOGGER.error(fatalMark, "Failed to recreate chipper pool, expect a full failure", e1);
		}
		throw new WebApplicationException(Response.serverError().build());
	}
}
