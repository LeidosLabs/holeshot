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

package com.leidoslabs.holeshot.elt.layers;

import java.io.Closeable;
import java.io.IOException;
import java.net.URL;
import java.util.Collection;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;

import org.image.common.util.CloseableUtils;

import com.leidoslabs.holeshot.elt.ELTCanvas;
import com.leidoslabs.holeshot.elt.ELTDisplayContext;
import com.leidoslabs.holeshot.elt.ImageFramebuffer;
import com.leidoslabs.holeshot.elt.Interpolation;
import com.leidoslabs.holeshot.elt.imagechain.Framebuffer;
import com.leidoslabs.holeshot.elt.imageop.ImageOpFactory;
import com.leidoslabs.holeshot.elt.imageop.Mosaic;
import com.leidoslabs.holeshot.elt.imageop.ToneTransferCurve;
import com.leidoslabs.holeshot.elt.tileserver.TileserverImage;
import com.leidoslabs.holeshot.elt.viewport.ImageWorld;
import com.leidoslabs.holeshot.elt.viewport.ViewportImageListener;

/**
 * Base (Image) Layer of ELTCanvas. Uses a Renderer (typically ImageChain)
 * to render a TileServerImage
 */
public class BaseLayer extends Renderer<Void> implements Closeable {
	private final Mosaic mosaic;
	private final ImageOpFactory imageOpFactory;
	private final boolean isProgressiveRender;

	public BaseLayer(ELTCanvas eltCanvas) {
		this(eltCanvas.getImageWorld(), eltCanvas.getELTDisplayContext(), eltCanvas.getImageOpFactory(), eltCanvas.isProgressiveRender());
	}
	public BaseLayer(ImageWorld imageWorld, ELTDisplayContext displayContext, ImageOpFactory imageOpFactory, boolean progressiveRender) {
		super(imageWorld, displayContext);
		this.imageOpFactory = imageOpFactory;
		this.isProgressiveRender = progressiveRender;
		this.mosaic = imageOpFactory.mosaic(imageWorld, displayContext, m->this.setFramebuffer(m.getResultFramebuffer()));
	}

	public void addImage(TileserverImage tileserverImage) throws IOException, InterruptedException, ExecutionException {
		final ImageFramebuffer imageFb = new ImageFramebuffer(imageOpFactory, getImageWorld(), getELTDisplayContext(), isProgressiveRender, tileserverImage);
		addImage(imageFb);
	}
	public void addImage(URL imageMetadataURL) throws IOException, InterruptedException, ExecutionException {
		final ImageFramebuffer imageFb = new ImageFramebuffer(imageOpFactory, getImageWorld(), getELTDisplayContext(), isProgressiveRender, imageMetadataURL);
		addImage(imageFb);
	}
	private void addImage(ImageFramebuffer imageFb) {
		mosaic.addImage(imageFb);
	}

	public void addImages(Collection<URL> imageMetadataURLs) throws IOException, InterruptedException, ExecutionException {
		for (URL url: imageMetadataURLs) {
			addImage(url);
		}
	}
	
	public boolean isDRAEnabled() {
		return mosaic.isDRAEnabled();
	}
	public void enableDRA(boolean enabled) {
		mosaic.enableDRA(enabled);
	}
	public void resetImageChain() {
		mosaic.reset();
	}
	public void setTTC(Consumer<? super ToneTransferCurve> action) {
		mosaic.setTTC(action);
	}

	/**
	 * Render tile server image, and return output from buffer
	 * @return FrameBuffer from rendered TileServerImage
	 */
	public Framebuffer draw() {
		try {
			render(null);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return getResultFramebuffer();
	}

	public boolean isFullyRendered() {
		return mosaic.isFullyRendered();
	}

	@Override
	public void close() throws IOException {
		CloseableUtils.close(mosaic);
	}

	@Override
	public void render(Void data) throws Exception {
		mosaic.render();
	}

	public TileserverImage[] getImages() {
		return mosaic.getImages();
	}

	public void addViewportImageListener(ViewportImageListener imageListener) {
		mosaic.addViewportImageListener(imageListener);
	}

	public void setMultiImageMode() {
		mosaic.setMultiImageMode();
	}
	public void setSingleImageMode(TileserverImage singleImage) {
		mosaic.setSingleImageMode(singleImage);
	}
	public Interpolation getInterpolation() {
		return mosaic.getInterpolation();
	}
	public void setInterpolation(Interpolation interpolation) {
		mosaic.setInterpolation(interpolation);
	}
	/**
	 * 
	 */
	public void clearAllImages() {
		mosaic.clearAllImages();
	}
	/**
	 * 
	 */
	public void resetFullyRendered() {
	   mosaic.resetFullyRendered();
	}
	/**
	 * @param framebuffer
	 */
	public void setSourceFramebuffer(Framebuffer framebuffer) {
		mosaic.setResultFramebuffer(framebuffer);
	}
	
}
