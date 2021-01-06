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

import static org.lwjgl.opengl.GL11.GL_LINEAR;
import static org.lwjgl.opengl.GL11.GL_NEAREST;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_2D;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_MAG_FILTER;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_MIN_FILTER;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_WRAP_S;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_WRAP_T;
import static org.lwjgl.opengl.GL11.glBindTexture;
import static org.lwjgl.opengl.GL11.glTexParameteri;
import static org.lwjgl.opengl.GL11.glViewport;
import static org.lwjgl.opengl.GL12.GL_CLAMP_TO_EDGE;
import static org.lwjgl.opengl.GL13.GL_TEXTURE0;
import static org.lwjgl.opengl.GL13.GL_TEXTURE2;
import static org.lwjgl.opengl.GL13.glActiveTexture;
import static org.lwjgl.opengl.GL20.glUniform1i;
import static org.lwjgl.opengl.GL20.glUniform2iv;
import static org.lwjgl.opengl.GL20.glUseProgram;

import java.awt.Dimension;
import java.io.IOException;
import java.util.Set;

import org.image.common.util.CloseableUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableSet;
import com.leidoslabs.holeshot.elt.Interpolation;
import com.leidoslabs.holeshot.elt.gpuimage.GLInternalFormat;
import com.leidoslabs.holeshot.elt.gpuimage.HistogramType;
import com.leidoslabs.holeshot.elt.gpuimage.QuadDrawVAO;
import com.leidoslabs.holeshot.elt.gpuimage.ShaderProgram;
import com.leidoslabs.holeshot.elt.imagechain.Framebuffer;
import com.leidoslabs.holeshot.elt.imageop.Interpolated;
import com.leidoslabs.holeshot.elt.imageop.RawImage;

/**
 * OpenGL Interpolated
 */
class OglInterpolated extends OglAbstractImageOpPrimitive implements Interpolated {
    private static final Logger LOGGER = LoggerFactory.getLogger(OglInterpolated.class);
	Set<Interpolation> HARDWARE_INTERPOLATIONS = ImmutableSet.of(Interpolation.NEAREST, Interpolation.LINEAR);
	private static final String SHADER_KEY = OglInterpolated.class.getName();
	private Framebuffer interpolatedFramebuffer;
	private QuadDrawVAO quadDrawVAO;
	private Interpolation interpolation;
	private RawImage rawImage;
	
	public OglInterpolated(Interpolation interpolation) {
		setInterpolation(interpolation);
	}



	@Override
	public Framebuffer getResultFramebuffer() {
		Framebuffer result;
		if (isHardwareInterpolation()) {
			result = getRawImage().getResultFramebuffer();
		} else {
			result = interpolatedFramebuffer;
		}
		return result;
	}
	
	private boolean isHardwareInterpolation() {
		return HARDWARE_INTERPOLATIONS.contains(interpolation);
	}

	@Override
	protected void doRender() throws Exception {
		try {
			initialize();

			if (!isHardwareInterpolation()) { 
				Dimension viewportDimensions = getViewportDimensions();
				
				interpolatedFramebuffer.clearBuffer();

				// Bind to the Equalized Image Buffer for Writing
				interpolatedFramebuffer.bind();

//				glEnable(GL_ALPHA_TEST);

				glViewport(0, 0,  viewportDimensions.width, viewportDimensions.height);
				
				glActiveTexture(GL_TEXTURE2);
				
				glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
				glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);

				glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
				glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
				
				glBindTexture(GL_TEXTURE_2D, getRawImage().getResultFramebuffer().getTexture().getId());
				glActiveTexture(GL_TEXTURE0);

				
				ShaderProgram shader = getELTDisplayContext().getShader(SHADER_KEY, HistogramType.class, HistogramType.Shaders.PASSTHROUGH_VERTEX_SHADER, HistogramType.Shaders.INTERPOLATED_FRAGMENT_SHADER);

				shader.useProgram();

				// Set the texture that the vertex shader should read from
				glUniform1i(shader.getUniformLocation("rawImage"), 2);
				glUniform1i(shader.getUniformLocation("interpolation"), interpolation.ordinal());
				glUniform2iv(shader.getUniformLocation("fbDim"), new int[] { viewportDimensions.width, viewportDimensions.height});

				this.quadDrawVAO.draw();

				glUseProgram(0);
			}
		} finally {
			interpolatedFramebuffer.unbind();
		}
	}
	private RawImage getRawImage() {
		if (rawImage == null) {
			rawImage = getImageChain().getPreviousImageOp(this, RawImage.class);
		}
		return rawImage;
	}


	private void initialize() throws Exception {
		final Dimension viewportDimensions = getViewportDimensions();
		if (interpolatedFramebuffer == null) {
			RawImage rawImage = getRawImage();
			GLInternalFormat internalFormat = rawImage.getResultFramebuffer().getTexture().getInternalFormat();
			interpolatedFramebuffer = new Framebuffer(viewportDimensions, internalFormat, getELTDisplayContext());
			this.quadDrawVAO = new QuadDrawVAO(QuadDrawVAO.FULL_UNIFORM_QUAD, 0);
		} else {
			interpolatedFramebuffer.reset(viewportDimensions);
		}
	}

	@Override
	public void close() throws IOException {
		super.close();
		CloseableUtils.close(interpolatedFramebuffer, quadDrawVAO);
	}

	@Override
	public void reset() {
		clearFramebuffer(0.0f, 0.0f, 0.0f, 1.0f, interpolatedFramebuffer);
	}
	public Interpolation getInterpolation() {
		return interpolation;
	}
	public void setInterpolation(Interpolation interpolation) {
		this.interpolation = interpolation;
	}

}
