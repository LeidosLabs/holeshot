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

package com.leidoslabs.holeshot.elt.imageop.ogl;

import static org.lwjgl.opengl.GL11.glViewport;

import java.awt.Dimension;
import java.io.IOException;

import org.image.common.util.CloseableUtils;

import com.leidoslabs.holeshot.elt.ELTImageTexture;
import com.leidoslabs.holeshot.elt.Interpolation;
import com.leidoslabs.holeshot.elt.gpuimage.GLInternalFormat;
import com.leidoslabs.holeshot.elt.imagechain.Framebuffer;
import com.leidoslabs.holeshot.elt.imagechain.ImageChain;
import com.leidoslabs.holeshot.elt.imageop.Interpolated;
import com.leidoslabs.holeshot.elt.imageop.RawImage;
import com.leidoslabs.holeshot.elt.tileserver.TileRef;
import com.leidoslabs.holeshot.elt.tileserver.TileserverImage;
import com.leidoslabs.holeshot.elt.viewport.ImageWorld;

/**
 * Gets image from top tile, and loads into openGL frame buffer.
 * First stage in image chain
 *
 */
class OglRawImage extends OglAbstractImageOpPrimitive implements RawImage {

	private Framebuffer rawImageFramebuffer;
	private ELTImageTexture topTexture;
	private boolean progressiveRender;
	private boolean fullyRendered;
	private TileserverImage image;
	private Interpolated interpolated;

	public OglRawImage(boolean progressiveRender) {
		this(null, progressiveRender);
	}
	
	public OglRawImage(TileserverImage image, boolean progressiveRender) {
		this.progressiveRender = progressiveRender;
		setImage(image);
	}
	
	@Override
	public void setImage(TileserverImage image) {
		this.fullyRendered = false;
		this.image = image;
		topTexture = null;
	}
	
	@Override
	public TileserverImage getImage() {
		return image;
	}

	@Override
	protected void doRender() throws Exception {
		updateFramebuffer();
		drawImageToFramebuffer();
	}

	@Override
	public Framebuffer getResultFramebuffer() {
		return rawImageFramebuffer;
	}

	private void updateFramebuffer() throws Exception {
		final Dimension viewportDimensions = getViewportDimensions();
		final int bpp = getImage().getBitsPerPixel();
		//      final int bands = getImage().getNumBands();


		final GLInternalFormat internalFormat =
				(bpp<=8) ? GLInternalFormat.GlInternalFormatRGBA8 : GLInternalFormat.GlInternalFormatRGBA16UI;

		if (rawImageFramebuffer == null) {
			rawImageFramebuffer = new Framebuffer(viewportDimensions, internalFormat, getELTDisplayContext());
		} else {
			rawImageFramebuffer.reset(viewportDimensions, internalFormat);
		}
	}

	private int getWidth() {
		return rawImageFramebuffer == null ? 0 : rawImageFramebuffer.getSize().width;
	}
	private int getHeight() {
		return rawImageFramebuffer == null ? 0 : rawImageFramebuffer.getSize().height;
	}
	private void drawImageToFramebuffer() throws Exception {
		final TileRef newTopTile = image.getTopTile();
		final TileRef oldTopTile = (topTexture == null) ? null : topTexture.getTileRef();
		
		final ImageChain imageChain = getImageChain();
		final ImageWorld imageWorld = imageChain.getImageWorld();

		fullyRendered = !newTopTile.getGeodeticBounds().intersects(imageWorld.getGeodeticViewport());
		
		if (!fullyRendered) {
			if (!newTopTile.equals(oldTopTile)) {
				topTexture = ELTImageTexture.getTexture(imageChain.getELTDisplayContext(), newTopTile);
			}
			if (topTexture != null) {
				if (getImageWorld().getPercentVisible(image) < 1.0) {
					rawImageFramebuffer.clearBuffer();
				}
				rawImageFramebuffer.bind();

				glViewport(0, 0, getWidth(), getHeight());

				fullyRendered = topTexture.draw(progressiveRender, getImageWorld(), getInterpolation());
				rawImageFramebuffer.unbind();
			}
		}
	}
	/**
	 * @return
	 */
	private Interpolation getInterpolation() {
		Interpolated interpolatedOp = getInterpolated();
		return (interpolatedOp == null) ? Interpolation.NEAREST : interpolatedOp.getInterpolation();
	}

	private Interpolated getInterpolated() {
		if (interpolated == null) {
			interpolated = getImageChain().getLastImageOp(Interpolated.class);
		}
		return interpolated;
	}	
	
	@Override
	public boolean isFullyRendered() {
		return fullyRendered;
	}

	@Override
	public void close() throws IOException {
		super.close();
		CloseableUtils.close(rawImageFramebuffer, topTexture);
	}

	@Override
	public void reset() {
		resetFullyRendered();
		if ( rawImageFramebuffer != null) {
			clearFramebuffer(0.0f, 0.0f, 0.0f, 0.0f, rawImageFramebuffer);
		}
	}

	@Override
	public void resetFullyRendered() {
		fullyRendered = false;
	}

}
