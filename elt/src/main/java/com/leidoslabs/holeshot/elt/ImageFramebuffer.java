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

package com.leidoslabs.holeshot.elt;

import java.io.Closeable;
import java.io.IOException;
import java.net.URL;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableSet;
import com.leidoslabs.holeshot.elt.gpuimage.HistogramType;
import com.leidoslabs.holeshot.elt.imagechain.ImageChain;
import com.leidoslabs.holeshot.elt.imagechain.ImageChain.ImageChainBuilder;
import com.leidoslabs.holeshot.elt.imageop.CumulativeHistogram;
import com.leidoslabs.holeshot.elt.imageop.DRAParameters;
import com.leidoslabs.holeshot.elt.imageop.Histogram;
import com.leidoslabs.holeshot.elt.imageop.ImageOpFactory;
import com.leidoslabs.holeshot.elt.imageop.Interpolated;
import com.leidoslabs.holeshot.elt.imageop.SummedArea;
import com.leidoslabs.holeshot.elt.layers.Renderer;
import com.leidoslabs.holeshot.elt.tileserver.TileserverImage;
import com.leidoslabs.holeshot.elt.viewport.ImageWorld;

/**
 * ELT (Electronic Light Table) Canvas allows for the loading of imagery from the HOLESHOT Tileserver. It handles the:
 * <ul>
 *    <li>Fetching and caching of imagery tiles from the server</li>
 *    <li>Mosaicing of image tiles within OpenGL canvas</li>
 *    <li>SIPS processing for histogram equalization of the imagery</li>
 *    <li>Roaming, Zooming, Rotation of imagery</li> 
 *    <li>The creation and visualization of KML vector layers over top of the imagery</li>
 * </ul>
 * 
 * 
 * Drop-in replacement for SWT's {@link org.eclipse.swt.opengl.GLCanvas} class.
 * <p>
 * It supports creating OpenGL 3.0 and 3.2 core/compatibility contexts as well as multisampled framebuffers.
 * 
 * @author robertsrg
 *
 */
@SuppressWarnings("serial")
public class ImageFramebuffer extends Renderer<Void> implements Closeable {
	private static final Logger LOGGER = LoggerFactory.getLogger(ImageFramebuffer.class);

	private boolean enabled;

	/**
	 * The currently loaded image
	 */
	private TileserverImage tileserverImage;


	/**
	 * List of ImageOps involved in DRA(Dynamic Range Adjustment) calculations
	 */
	private static final Set<Class> ACTIVE_DRA_IMAGEOPS = ImmutableSet.of(Histogram.class, CumulativeHistogram.class, SummedArea.class, DRAParameters.class);

	private final ImageChain imageChain;


	public ImageFramebuffer (ELTCanvas canvas, URL imageMetadataURL) throws IOException, InterruptedException, ExecutionException {
		this (canvas, new TileserverImage(imageMetadataURL));
	}
	public ImageFramebuffer (ELTCanvas canvas, TileserverImage image) throws IOException, InterruptedException, ExecutionException {
		this (canvas.getImageOpFactory(), canvas.getImageWorld(), canvas.getELTDisplayContext(), canvas.isProgressiveRender(), image);
	}
	public ImageFramebuffer(ImageOpFactory imageOpFactory, ImageWorld imageWorld, ELTDisplayContext displayContext, boolean isProgressiveRender, URL imageMetadataURL) throws IOException, InterruptedException, ExecutionException  {
		this(imageOpFactory, imageWorld, displayContext, isProgressiveRender, new TileserverImage(imageMetadataURL));
	}

	public ImageFramebuffer(ImageOpFactory imageOpFactory, ImageWorld imageWorld, ELTDisplayContext displayContext, boolean isProgressiveRender, TileserverImage image) throws IOException, InterruptedException, ExecutionException  {
		super(imageWorld, displayContext);

		this.enabled = true;

		setImage(image);

		this.imageChain = new ImageChainBuilder(imageOpFactory, imageWorld, displayContext)
				.rawImage(tileserverImage, isProgressiveRender)
				.interpolated(Interpolation.CATMULL)
				.histogram(HistogramType.RGB)
				.summedArea()
				.cumulativeHistogram()
				.draParameters(false)
				.dynamicRangeAdjust()
				.toneTransferCurve()
				.build();

	}
	
	private Interpolated getInterpolated() {
		return imageChain.getFirstImageOpOfType(Interpolated.class);
	}
	public Interpolation getInterpolation() {
	    Interpolated interpolatedOp = getInterpolated();
	    return (interpolatedOp == null) ? Interpolation.NEAREST : interpolatedOp.getInterpolation();
	}
	public void setInterpolation(Interpolation interpolation) {
	    Interpolated interpolatedOp = getInterpolated();
	    if (interpolatedOp != null) {
	    	interpolatedOp.setInterpolation(interpolation);
	    }
	}
	
	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}
	public boolean isEnabled() {
		return enabled;
	}

	public ImageChain getImageChain() {
		return imageChain;
	}
	public void enableDRA() {
		enableDRA(true);
	}
	public void disableDRA() {
		enableDRA(false);
	}

	public void enableDRA(boolean enabled) {
		imageChain.getImageOps().stream()
		.filter(i->ACTIVE_DRA_IMAGEOPS.stream().anyMatch(ai->ai.isInstance(i)))
		.forEach(i->i.setRenderingEnabled(enabled));
	}

	/**
	 *  Given metadata url, set a new Tileserverimage. Resets ImageWorld, ImageChain, etc
	 * @param image new TileServerImage
	 * @param resetViewport
	 * @throws IOException
	 * @throws InterruptedException
	 * @throws ExecutionException
	 */
	private void setImage(TileserverImage image) throws IOException, InterruptedException, ExecutionException {
		tileserverImage = image;
	}

	/**
	 * @return Canvas's current TileServerImage
	 */
	public TileserverImage getImage() {
		return tileserverImage;
	}

	/**
	 * @return Locked check on fullyRendered
	 */
	public boolean isFullyRendered() {
		return imageChain.isFullyRendered();
	}

	@Override
	public void close() throws IOException {
	}

	@Override
	public void render(Void data) throws Exception {
		this.imageChain.render(data);
	}
	/**
	 * @return
	 */
	public boolean isDRAEnabled() {
		return imageChain.getImageOps().stream()
				.filter(i->ACTIVE_DRA_IMAGEOPS.stream().anyMatch(ai->ai.isInstance(i)))
				.allMatch(i->i.isRenderingEnabled());
	}
}
