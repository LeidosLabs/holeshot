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

import static org.lwjgl.opengl.GL11.GL_TEXTURE_2D;
import static org.lwjgl.opengl.GL11.glBindTexture;
import static org.lwjgl.opengl.GL13.GL_TEXTURE0;
import static org.lwjgl.opengl.GL13.GL_TEXTURE1;
import static org.lwjgl.opengl.GL13.glActiveTexture;
import static org.lwjgl.opengl.GL20.glUniform1i;
import static org.lwjgl.opengl.GL20.glUniform2iv;
import static org.lwjgl.opengl.GL20.glUseProgram;

import java.awt.Dimension;
import java.io.Closeable;
import java.io.IOException;
import java.nio.FloatBuffer;

import org.image.common.util.CloseableUtils;
import org.lwjgl.BufferUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.leidoslabs.holeshot.elt.gpuimage.GLInternalFormat;
import com.leidoslabs.holeshot.elt.gpuimage.HistogramType;
import com.leidoslabs.holeshot.elt.gpuimage.QuadDrawVAO;
import com.leidoslabs.holeshot.elt.gpuimage.ShaderProgram;
import com.leidoslabs.holeshot.elt.imagechain.Framebuffer;
import com.leidoslabs.holeshot.elt.imageop.CumulativeHistogram;
import com.leidoslabs.holeshot.elt.imageop.Histogram;
import com.leidoslabs.holeshot.elt.imageop.SummedArea;

/**
 * Given previously computed histogram, uses a shader to compute cumulative histogram
 */
class OglCumulativeHistogram extends OglAbstractImageOpPrimitive implements CumulativeHistogram {
	private static final Logger LOGGER = LoggerFactory.getLogger(OglCumulativeHistogram.class);

	private static final String SHADER_KEY = OglCumulativeHistogram.class.getName();
	private float[] cumulativeHistogram;
	private FloatBuffer cumulativeHistogramBuffer;
	private Framebuffer cumulativeHistogramFramebuffer;
	private Histogram histogram;
	private QuadDrawVAO quadDrawVAO;

	private SummedArea summedArea;

	public OglCumulativeHistogram() {}

	@Override
	public Framebuffer getResultFramebuffer() {
		return cumulativeHistogramFramebuffer;
	}

	@Override
	protected void doRender() {
		try {
			final Histogram histogram = getHistogram();
			Dimension histogramSize = histogram.getResultFramebuffer().getSize();

			initializeFramebuffer(histogram);

			// Bind to the Histogram Buffer for Writing
			cumulativeHistogramFramebuffer.bind();

			// Setup the current raw image framebuffer as the texture for the vertex shaders to read from.
			// Assign it to GL_TEXTURE1 and
			// set the uniform parameter so that the vertex shaders know which texture to read from.


			glActiveTexture(GL_TEXTURE1);
			glBindTexture(GL_TEXTURE_2D,
					getSummedArea().getResultFramebuffer().getTexture().getId());
			glActiveTexture(GL_TEXTURE0);

			ShaderProgram shader = getELTDisplayContext().getShader(SHADER_KEY, HistogramType.class,
					HistogramType.Shaders.PASSTHROUGH_VERTEX_SHADER,
					HistogramType.Shaders.CUMULATIVE_HISTOGRAM_SHADER);
					
			shader.useProgram();

			// Set the texture that the vertex shader should read from
			glUniform1i(shader.getUniformLocation("summedAreaData"), 1);
			glUniform2iv(shader.getUniformLocation("fbDim"), new int[] {histogramSize.width, histogramSize.height});

			glUniform1i(shader.getUniformLocation("buckets"), histogram.getBuckets());
			glUniform1i(shader.getUniformLocation("maxPixel"), getMaxPixelValue());

			//      shader.uniform(gl4, bucketsUniformData);
			//      shader.uniform(gl4, maxPixelUniformData);

			this.quadDrawVAO.draw();

			glUseProgram(0);

			if (OglHistogram.DEBUG) {
				readCumulativeHistogram();

				// Dump the histogram to STDOUT for debugging purposes
				System.out.println("BEGIN CUMULATIVE HISTOGRAM");
				dumpCumulativeHistogram(false);
				System.out.println("END CUMULATIVE HISTOGRAM");
			}

		} catch (Throwable e) {
			e.printStackTrace();
		} finally {
			// Cleanup State
			this.cumulativeHistogramFramebuffer.unbind();
		}
	}

	private void dumpCumulativeHistogram(boolean ignoreZeros) {
		OglHistogram.dump3DArray(cumulativeHistogram, ignoreZeros, "CUMULATIVE HISTOGRAM",
				getHistogram().getBuckets());
	}

	private Histogram getHistogram() {
		if (histogram == null) {
			histogram = this.getImageChain().getPreviousImageOp(this, Histogram.class);
		}
		return histogram;
	}

	private SummedArea getSummedArea() {
		if (summedArea == null) {
			summedArea = this.getImageChain().getPreviousImageOp(this, SummedArea.class);
		}
		return summedArea;
	}

	private void initializeFramebuffer(Histogram histogram) throws Exception {
		if (this.quadDrawVAO == null) {
			this.quadDrawVAO = new QuadDrawVAO(QuadDrawVAO.FULL_UNIFORM_QUAD, 0);
		}
		final int maxTextureSize = histogram.getMaxTextureSize();
		final int numRows = histogram.getNumRows();
		final Dimension cumulativeHistogramSize = new Dimension(maxTextureSize, numRows);

		if (this.cumulativeHistogramFramebuffer == null || !cumulativeHistogramFramebuffer.getSize().equals(cumulativeHistogramSize)) {
			this.cumulativeHistogramFramebuffer = new Framebuffer(cumulativeHistogramSize, GLInternalFormat.GlInternalFormatRGBA32F, getELTDisplayContext());
		}
	}

	private void readCumulativeHistogram() {
		final Histogram histogram = getHistogram();
		Dimension histogramSize = histogram.getResultFramebuffer().getSize();

		if (cumulativeHistogram == null || cumulativeHistogram.length != histogramSize.getWidth()
				* histogramSize.getHeight() * OglHistogram.HISTOGRAM_BANDS) {
			this.cumulativeHistogram = new float[histogramSize.width * histogramSize.height
			                                     * OglHistogram.HISTOGRAM_BANDS];
			this.cumulativeHistogramBuffer = BufferUtils.createFloatBuffer(cumulativeHistogram.length);
		}

		cumulativeHistogramFramebuffer.readFramebuffer(cumulativeHistogramBuffer, cumulativeHistogram);
	}
	@Override
	public void close() throws IOException {
		CloseableUtils.close(cumulativeHistogramFramebuffer,
				quadDrawVAO);
	}

	@Override
	public void reset() {
		clearFramebuffer(0.0f, 0.0f, 0.0f, 1.0f, cumulativeHistogramFramebuffer);
	}



}
