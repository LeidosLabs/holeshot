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

import java.io.IOException;
import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.leidoslabs.holeshot.elt.imagechain.Framebuffer;
import com.leidoslabs.holeshot.elt.imagechain.PingPongFramebuffer;
import com.leidoslabs.holeshot.elt.imageop.ImageOp;
import com.leidoslabs.holeshot.elt.imageop.ogl.ImageChainSettings.ImageSource;


/**
 * Abstract type for openGL Image operations
 */
abstract class OglAbstractImageOp implements ImageOp {
	private static final Logger LOGGER = LoggerFactory.getLogger(OglAbstractImageOp.class);

	private boolean renderingEnabled;

	protected OglAbstractImageOp() {
		setRenderingEnabled(true);
	}

	protected ImageSource getImageSource() {
		// TODO: Need to identify the actual imagesource.  metadata.json doesn't currently have it.
		// ImageSource result = ImageSource.UNSPECIFIED;
		ImageSource result = ImageSource.WORLDVIEW_1;
		return result;
	}

	/**
	 * Default is to return true.  Can be overridden in subclasses
	 */
	public boolean isFullyRendered() {
		return true;
	}

	@Override
	public void resetFullyRendered() {
	}

	/**
	 * 
	 */
	public boolean isRenderingEnabled() {
		return renderingEnabled;
	}
	public void setRenderingEnabled(boolean renderingEnabled) {
		this.renderingEnabled = renderingEnabled;
	}

	protected abstract void doRender() throws Exception;

	@Override
	/**
	 * If rendering is enabled, will call internal render method, which typically
	 * sets up and executes the relevant vertex shader
	 */
	public void render() throws Exception {
		if (isRenderingEnabled()) {
			doRender();
		}
	}

	@Override
	public void close() throws IOException {
	}

	/**
	 * Clear a number of framebuffers
	 * @param clearRed
	 * @param clearGreen
	 * @param clearBlue
	 * @param clearAlpha
	 * @param fb Varargs of PingPongFrameBuffers
	 */
	protected void clearFramebuffer(float clearRed, float clearGreen, float clearBlue, float clearAlpha, PingPongFramebuffer... fb) {
		Arrays.stream(fb).filter(f -> f!=null).forEach(f->clearFramebuffer(clearRed, clearGreen, clearBlue, clearAlpha, f.getSource(), f.getDestination()));
	}
	/**
	 * Clear a number of framebuffers
	 * @param clearRed
	 * @param clearGreen
	 * @param clearBlue
	 * @param clearAlpha
	 * @param fb
	 */
	protected void clearFramebuffer(float clearRed, float clearGreen, float clearBlue, float clearAlpha, Framebuffer... fb) {
		Arrays.stream(fb).filter(f -> f!=null).forEach(f-> f.clearBuffer(clearRed, clearGreen, clearBlue, clearAlpha));
	}
}
