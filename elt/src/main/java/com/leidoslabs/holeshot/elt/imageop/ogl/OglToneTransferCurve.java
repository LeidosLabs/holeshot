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
import static org.lwjgl.opengl.GL11.GL_TEXTURE_2D;
import static org.lwjgl.opengl.GL11.glBindTexture;
import static org.lwjgl.opengl.GL11.glViewport;
import static org.lwjgl.opengl.GL12.GL_CLAMP_TO_EDGE;
import static org.lwjgl.opengl.GL13.GL_TEXTURE0;
import static org.lwjgl.opengl.GL13.GL_TEXTURE1;
import static org.lwjgl.opengl.GL13.GL_TEXTURE2;
import static org.lwjgl.opengl.GL13.glActiveTexture;
import static org.lwjgl.opengl.GL20.glUniform1f;
import static org.lwjgl.opengl.GL20.glUniform1i;
import static org.lwjgl.opengl.GL20.glUniform2iv;
import static org.lwjgl.opengl.GL20.glUseProgram;

import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import org.image.common.util.CloseableUtils;
import org.locationtech.jts.geom.Coordinate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.leidoslabs.holeshot.elt.gpuimage.GLInternalFormat;
import com.leidoslabs.holeshot.elt.gpuimage.HistogramType;
import com.leidoslabs.holeshot.elt.gpuimage.QuadDrawVAO;
import com.leidoslabs.holeshot.elt.gpuimage.ShaderProgram;
import com.leidoslabs.holeshot.elt.gpuimage.Texture;
import com.leidoslabs.holeshot.elt.imagechain.Framebuffer;
import com.leidoslabs.holeshot.elt.imageop.DynamicRangeAdjust;
import com.leidoslabs.holeshot.elt.imageop.RawImage;
import com.leidoslabs.holeshot.elt.imageop.ToneTransferCurve;
import com.leidoslabs.holeshot.elt.imageop.ogl.ImageChainSettings.ImageSource;
import com.leidoslabs.holeshot.elt.tileserver.TileserverImage;
import com.leidoslabs.holeshot.elt.viewport.ImageWorld;

/**
 * Final OpenGL operation in Image Chain. Uses a sensor specific lookup table
 * to DRA'd pixels
 */
public class OglToneTransferCurve extends OglAbstractImageOpPrimitive implements ToneTransferCurve {
	private static final Logger LOGGER = LoggerFactory.getLogger(OglToneTransferCurve.class);

	private static final String SHADER_KEY = OglToneTransferCurve.class.getName();
	private Framebuffer toneCorrectedFramebuffer;

	private Texture ttcTexture;
	private ImageSource ttcImageSource;
	private ImageChainSettings imageChainSettings;
	private boolean isResultFBExternallyManaged;
	private float zOrder;

	private static final Map<String, int[]> ttcCurves = new HashMap<String, int[]>();
	
	public OglToneTransferCurve() {
		this.isResultFBExternallyManaged = false;
		setZOrder(0.0f);
	}

	@Override
	public Framebuffer getResultFramebuffer() {
		return this.toneCorrectedFramebuffer;
	}

	public void setResultFramebuffer(Framebuffer resultFramebuffer) {
		this.isResultFBExternallyManaged = (resultFramebuffer != null);
		this.toneCorrectedFramebuffer = resultFramebuffer;
	}

	private Dimension getSize() {
		return getResultFramebuffer().getSize();
	}

	/**
	 * @return the zOrder
	 */
	public float getZOrder() {
		return zOrder;
	}

	/**
	 * @param zOrder the zOrder to set
	 */
	public void setZOrder(float zOrder) {
		this.zOrder = zOrder;
	}

	private int getWidth() {
		return getSize().width;
	}

	private int getHeight() {
		return getSize().height;
	}

	@Override
	protected void doRender() throws Exception {
		try {
			initialize();

			if (!isResultFBExternallyManaged) {
				toneCorrectedFramebuffer.clearBuffer();
			}

			// Bind to the Equalized Image Buffer for Writing
			toneCorrectedFramebuffer.bind();

			glViewport(0, 0,  getWidth(), getHeight());
			
			final DynamicRangeAdjust dra = getImageChain().getPreviousImageOp(this, OglDynamicRangeAdjust.class);
			Framebuffer draFB = dra.getResultFramebuffer();
			glActiveTexture(GL_TEXTURE1);
			glBindTexture(GL_TEXTURE_2D, draFB.getTexture().getId());
			glActiveTexture(GL_TEXTURE0);
			glActiveTexture(GL_TEXTURE2);
			this.ttcTexture.bind();
			glActiveTexture(GL_TEXTURE0);

			
			ShaderProgram shader = getELTDisplayContext().getShader(SHADER_KEY, HistogramType.class, HistogramType.Shaders.PASSTHROUGH_ZORDER_VERTEX_SHADER, HistogramType.Shaders.TTC_SHADER);

			shader.useProgram();

			// Set the texture that the vertex shader should read from
			glUniform1i(shader.getUniformLocation("inputImage"), 1);
			glUniform1i(shader.getUniformLocation("uTonalTransferSampler"), 2);

			glUniform2iv(shader.getUniformLocation("fbDim"), new int[] { draFB.getSize().width, draFB.getSize().height});
			glUniform1i(shader.getUniformLocation("buckets"), getImage().getMaxPixelValue());
			glUniform1i(shader.getUniformLocation("maxPixel"), getImage().getMaxPixelValue());

			glUniform1f(shader.getUniformLocation("ic_sub"), imageChainSettings.getSub());
			glUniform1f(shader.getUniformLocation("ic_mul"), imageChainSettings.getMul());
			glUniform1f(shader.getUniformLocation("ic_gamma"), imageChainSettings.getGamma());

			glUniform1f(shader.getUniformLocation("zOrder"), zOrder);
			
			ImageWorld imageWorld = getImageWorld();
			TileserverImage image = getImage();
			
			Coordinate[] clipBounds = imageWorld.getClipImage(image).getCoordinates();
			final double[][] vertices = 
					Arrays.stream(new int[] { 3, 0, 2, 1 })
					.mapToObj(i->clipBounds[i])
					.map(c->new double[] {c.getX(), c.getY(), 0.0})
					.toArray(double[][]::new);

			try (QuadDrawVAO vao = new QuadDrawVAO(vertices, 0)) {
				vao.draw();
			}
			glUseProgram(0);

		} catch (URISyntaxException e) {
			throw new IOException(e);
		} finally {
			toneCorrectedFramebuffer.unbind();
		}
	}

	private void initialize() throws Exception {
		final Dimension viewportDimensions = getViewportDimensions();

		if (toneCorrectedFramebuffer == null) {
			toneCorrectedFramebuffer = new Framebuffer(getViewportDimensions(), GLInternalFormat.GlInternalFormatRGBA32F, getELTDisplayContext());
		} else if (!toneCorrectedFramebuffer.getSize().equals(viewportDimensions)) {
			toneCorrectedFramebuffer.reset(viewportDimensions);
		}


		final ImageSource newImageSource = getImageSource();
		if (ttcTexture == null || newImageSource != ttcImageSource) {
			ttcImageSource = newImageSource;
			resetManualAdjustments();

			BufferedImage ttcImage = new BufferedImage(256, 256, BufferedImage.TYPE_USHORT_GRAY);
			ttcImage.getRaster().setSamples(0, 0, 256, 256, 0, readTTCCurve());
			this.ttcTexture = new Texture(new Dimension(256, 256), GLInternalFormat.GlInternalFormatR32FUS, GL_LINEAR, GL_CLAMP_TO_EDGE, getELTDisplayContext(), ttcImage);
		}
	}
	private static synchronized int[] readTTCCurve(String ttcResourceName) throws IOException, URISyntaxException {
		int[] ttcCurve = ttcCurves.get(ttcResourceName);
		if (ttcCurve == null) {
			try (BufferedReader buffer = new BufferedReader(new InputStreamReader(OglToneTransferCurve.class.getClassLoader().getResourceAsStream(ttcResourceName)))) {
				ttcCurve = buffer.lines().map(String::trim)
						.filter(s -> s.length() > 0 && !s.equals("#") && s.charAt(0) != 'm' && !s.contains(" "))
						.mapToInt(s -> Integer.parseInt(s, 10)).toArray();
				ttcCurves.put(ttcResourceName, ttcCurve);
			}
		}
		return ttcCurve;
	}
	private int[] readTTCCurve() throws IOException, URISyntaxException {
		return readTTCCurve(getTTCResourceName());
	}

	private String getTTCResourceName() {
		final ImageChainSettings settings = ttcImageSource.getSettings();
		return String.format("ttc/ttc_family_%d_16_to_16_%d.dat", settings.getTTCFamily(), settings.getTTCMember() );
	}


	@Override
	public void adjustBrightness(float d) {
		if (imageChainSettings != null) {
			imageChainSettings.adjustSub(d);
		}
	}

	@Override
	public void adjustContrast(float d) {
		if (imageChainSettings != null) {
			imageChainSettings.adjustMul(d);
		}
	}

	@Override
	public void adjustGamma(float d) {
		if (imageChainSettings != null) {
			imageChainSettings.adjustGamma(d);
		}
	}

	@Override
	public void resetManualAdjustments() {
		if (ttcImageSource != null) {
			imageChainSettings = new ImageChainSettings(ttcImageSource.getSettings());
		}
	}

	@Override
	public void close() throws IOException {
		super.close();
		CloseableUtils.close(toneCorrectedFramebuffer, ttcTexture);

		if (!isResultFBExternallyManaged) {
			CloseableUtils.close(toneCorrectedFramebuffer);
		}
	}
	@Override
	public void reset() {
		clearFramebuffer(0.0f, 0.0f, 0.0f, 0.0f, toneCorrectedFramebuffer);
	}

}
