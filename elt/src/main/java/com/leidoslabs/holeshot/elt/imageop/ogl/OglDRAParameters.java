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

import static org.lwjgl.opengl.GL11.GL_ALPHA_TEST;
import static org.lwjgl.opengl.GL11.GL_BLEND;
import static org.lwjgl.opengl.GL11.GL_COLOR_BUFFER_BIT;
import static org.lwjgl.opengl.GL11.GL_DEPTH_BUFFER_BIT;
import static org.lwjgl.opengl.GL11.GL_ONE;
import static org.lwjgl.opengl.GL11.GL_POINTS;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_2D;
import static org.lwjgl.opengl.GL11.glBindTexture;
import static org.lwjgl.opengl.GL11.glClear;
import static org.lwjgl.opengl.GL11.glClearColor;
import static org.lwjgl.opengl.GL11.glDisable;
import static org.lwjgl.opengl.GL11.glEnable;
import static org.lwjgl.opengl.GL11.glViewport;
import static org.lwjgl.opengl.GL13.GL_TEXTURE0;
import static org.lwjgl.opengl.GL13.GL_TEXTURE3;
import static org.lwjgl.opengl.GL13.GL_TEXTURE4;
import static org.lwjgl.opengl.GL13.GL_TEXTURE5;
import static org.lwjgl.opengl.GL13.glActiveTexture;
import static org.lwjgl.opengl.GL14.GL_CONSTANT_ALPHA;
import static org.lwjgl.opengl.GL14.GL_MAX;
import static org.lwjgl.opengl.GL14.GL_MIN;
import static org.lwjgl.opengl.GL14.glBlendColor;
import static org.lwjgl.opengl.GL14.glBlendFuncSeparate;
import static org.lwjgl.opengl.GL15.GL_ARRAY_BUFFER;
import static org.lwjgl.opengl.GL15.glBindBuffer;
import static org.lwjgl.opengl.GL15.glDeleteBuffers;
import static org.lwjgl.opengl.GL20.glBlendEquationSeparate;
import static org.lwjgl.opengl.GL20.glUniform1f;
import static org.lwjgl.opengl.GL20.glUniform1i;
import static org.lwjgl.opengl.GL20.glUniform2iv;
import static org.lwjgl.opengl.GL20.glUseProgram;
import static org.lwjgl.opengl.GL30.glBindVertexArray;
import static org.lwjgl.opengl.GL31.glDrawArraysInstanced;

import java.awt.Dimension;
import java.awt.Rectangle;
import java.io.IOException;
import java.nio.FloatBuffer;
import java.util.Arrays;

import org.apache.commons.lang3.tuple.Pair;
import org.image.common.util.CloseableUtils;
import org.lwjgl.BufferUtils;

import com.google.common.collect.Streams;
import com.leidoslabs.holeshot.elt.ELTDisplayContext;
import com.leidoslabs.holeshot.elt.gpuimage.GLInternalFormat;
import com.leidoslabs.holeshot.elt.gpuimage.HistogramType;
import com.leidoslabs.holeshot.elt.gpuimage.ShaderProgram;
import com.leidoslabs.holeshot.elt.gpuimage.VertexArrayObject;
import com.leidoslabs.holeshot.elt.imagechain.Framebuffer;
import com.leidoslabs.holeshot.elt.imagechain.PingPongFramebuffer;
import com.leidoslabs.holeshot.elt.imageop.CumulativeHistogram;
import com.leidoslabs.holeshot.elt.imageop.DRAParameters;
import com.leidoslabs.holeshot.elt.imageop.Histogram;


/**
 * OpenGL operation for retrieving parameters for DRA
 */
class OglDRAParameters extends OglAbstractImageOp implements DRAParameters {
   private static final Dimension FB_DIMENSION = new Dimension(1,1);

   // This is the well-known value that the position sampler is set/found in by the vertex shader.
   private static final boolean DEBUG = false;
   protected static final int POSITION_ATTRIBUTE = 0;
   public static final float DEFAULT_MAX_ADJUST_PER_FRAME = 5E-05f;

   private CumulativeHistogram cumulativeHistogram;
   private ShaderProgram eFirstShader;
   private ShaderProgram eMinShader;
   private ShaderProgram eMaxShader;
   private ShaderProgram eLastShader;

   private VertexArrayObject cumulativeHistogramVAO;

   private Histogram histogram;
   private PingPongFramebuffer eFirstFramebuffer;
   private PingPongFramebuffer eMinFramebuffer;
   private PingPongFramebuffer eMaxFramebuffer;
   private PingPongFramebuffer eLastFramebuffer;

   private float[] debugFramebufferData;

   private FloatBuffer debugFramebufferBuffer;
   private final FloatBuffer pointInstance;
   private float maxAdjustPerFrame;
   private boolean phasedDRAAdjustmentEnabled;


   public OglDRAParameters(boolean phasedDRA) {
      pointInstance = BufferUtils.createFloatBuffer(4);
      phasedDRAAdjustmentEnabled = phasedDRA;
      maxAdjustPerFrame = DEFAULT_MAX_ADJUST_PER_FRAME;
   }

   @Override
   public Framebuffer getResultFramebuffer() {
      return getEFirstFramebuffer().getDestination();
   }
   @Override
   public PingPongFramebuffer getEMaxFramebuffer() {
      return this.eMaxFramebuffer;
   }
   @Override
   public PingPongFramebuffer getEMinFramebuffer() {
      return this.eMinFramebuffer;
   }
   @Override
   public PingPongFramebuffer getEFirstFramebuffer() {
      return this.eFirstFramebuffer;
   }
   @Override
   public PingPongFramebuffer getELastFramebuffer() {
      return this.eLastFramebuffer;
   }

   @Override
   protected void doRender() throws IOException {
      initialize();

      runShader(eFirstShader, eFirstFramebuffer, null, GL_MIN, getImage().getMaxPixelValue());
      runShader(eLastShader, eLastFramebuffer, null, GL_MAX, 0.0f);
      runShader(eMinShader, eMinFramebuffer, Pair.of("eFirstTexture", eFirstFramebuffer), GL_MIN, getImage().getMaxPixelValue());
      runShader(eMaxShader, eMaxFramebuffer, Pair.of("eLastTexture", eLastFramebuffer), GL_MAX, 0.0f);

      if (DEBUG) {
         dumpResults();
      }
   }

   private void dumpResults() {
      final String eFirst = rgbToString(getResult(eFirstFramebuffer));
      final String eLast = rgbToString(getResult(eLastFramebuffer));
      final String eMin = rgbToString(getResult(eMinFramebuffer));
      final String eMax = rgbToString(getResult(eMaxFramebuffer));
      System.out.println(String.format("eFirst = %s eLast = %s eMin = %s eMax = %s", eFirst, eLast, eMin, eMax));
   }
   private static String rgbToString(float[] rgb) {
      return Arrays.toString(rgb);
   }
   private float[] getResult(PingPongFramebuffer fb) {
      fb.getDestination().readFramebuffer(debugFramebufferBuffer, debugFramebufferData);
      return Arrays.copyOf(debugFramebufferData, 3);
   }

   private void runShader(ShaderProgram shader, PingPongFramebuffer framebuffer, Pair<String, PingPongFramebuffer> inputTexture, int blendEquation, float clearColor) {
      try {
         // Bind to buffer for Writing
         framebuffer.swap();
         framebuffer.getDestination().bind();

         glViewport(0, 0,  getWidth(), getHeight());

         glEnable(GL_ALPHA_TEST);
         glClearColor(clearColor, clearColor, clearColor, 1.0f);
         glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

         glEnable(GL_BLEND);
         glBlendFuncSeparate(GL_ONE, GL_ONE, GL_CONSTANT_ALPHA, GL_CONSTANT_ALPHA);
         glBlendEquationSeparate(blendEquation, GL_MAX);
         glBlendColor(0.0f, 0.0f, 0.0f, 1.0f);

         glBindVertexArray(cumulativeHistogramVAO.getVao());

         glActiveTexture(GL_TEXTURE3);
         glBindTexture(GL_TEXTURE_2D, cumulativeHistogram.getResultFramebuffer().getTexture().getId());
         glActiveTexture(GL_TEXTURE0);

         if (inputTexture != null) {
            glActiveTexture(GL_TEXTURE4);
            glBindTexture(GL_TEXTURE_2D, inputTexture.getRight().getDestination().getTexture().getId());
            glActiveTexture(GL_TEXTURE0);
         }

         glActiveTexture(GL_TEXTURE5);
         glBindTexture(GL_TEXTURE_2D, framebuffer.getSource().getTexture().getId());
         glActiveTexture(GL_TEXTURE0);

         shader.useProgram();

         // Set the texture that the vertex shader should read from
         glUniform1i(shader.getUniformLocation("cumulativeHistogram"), 3);
         if (inputTexture != null) {
            glUniform1i(shader.getUniformLocation(inputTexture.getLeft()), 4);
         }
         glUniform1i(shader.getUniformLocation("lastResult"), 5);
         glUniform2iv(shader.getUniformLocation("fbDim"), new int[] { FB_DIMENSION.width, FB_DIMENSION.height});
         final Dimension histDim = getCumulativeHistogram().getResultFramebuffer().getSize();
         glUniform2iv(shader.getUniformLocation("histDim"), new int[] { histDim.width, histDim.height});


         glUniform1i(shader.getUniformLocation("histogramDownsampling"), histogram.getDownsamplingFactor());

         glUniform1i(shader.getUniformLocation("buckets"), histogram.getBuckets());
         glUniform1i(shader.getUniformLocation("maxPixel"), getImage().getMaxPixelValue());

         final ImageChainSettings ic = getImageSource().getSettings();
         glUniform1f(shader.getUniformLocation("ic_pmin"), ic.getpMin());
         glUniform1f(shader.getUniformLocation("ic_pmax"), ic.getpMax());
         glUniform1f(shader.getUniformLocation("maxAdjustPerFrame"), phasedDRAAdjustmentEnabled ? maxAdjustPerFrame : -1.0f);

         // Indicate at which points in the texture that the vertex shader should sample at
         glDrawArraysInstanced(GL_POINTS, 0, 1, getImage().getMaxPixelValue());

         glUseProgram(0);
      } finally {
         glUseProgram(0);

         glBindVertexArray(0);
         glBindBuffer(GL_ARRAY_BUFFER, 0);
         framebuffer.getDestination().unbind();
         glDisable(GL_BLEND);

      }

   }

   private CumulativeHistogram getCumulativeHistogram() {
      if (cumulativeHistogram == null) {
         cumulativeHistogram = this.getImageChain().getPreviousImageOp(this, CumulativeHistogram.class);
      }
      return cumulativeHistogram;
   }
   private int getHeight() {
      return getSize().height;
   }

   private Histogram getHistogram() {
      if (histogram == null) {
         histogram = this.getImageChain().getPreviousImageOp(this, Histogram.class);
      }
      return histogram;
   }

   private Dimension getSize() {
      return FB_DIMENSION;
   }
   private int getWidth() {
      return getSize().width;
   }

   private void allocateVAO(Framebuffer framebuffer) {
      if (cumulativeHistogramVAO == null) {
         cumulativeHistogramVAO = new VertexArrayObject(pointInstance, new Dimension(1,1), POSITION_ATTRIBUTE);
      }
   }

   public void destroy() {
      glDeleteBuffers(cumulativeHistogramVAO.getVao());
   }


   private void initialize() throws IOException {
      if (eFirstFramebuffer == null) {
         histogram = getHistogram();
         cumulativeHistogram = getCumulativeHistogram();

         final ELTDisplayContext eltDisplayContext = getELTDisplayContext();
         eFirstFramebuffer = new PingPongFramebuffer(FB_DIMENSION, GLInternalFormat.GlInternalFormatRGB32F, eltDisplayContext);
         eMinFramebuffer = new PingPongFramebuffer(FB_DIMENSION, GLInternalFormat.GlInternalFormatRGB32F, eltDisplayContext);
         eMaxFramebuffer = new PingPongFramebuffer(FB_DIMENSION, GLInternalFormat.GlInternalFormatRGB32F, eltDisplayContext);
         eLastFramebuffer = new PingPongFramebuffer(FB_DIMENSION, GLInternalFormat.GlInternalFormatRGB32F, eltDisplayContext);

         //         Arrays.asList(eFirstFramebuffer, eMinFramebuffer, eMaxFramebuffer, eLastFramebuffer)
         //         .stream().forEach(fb->setFramebufferUninitialized(fb));

         this.eFirstShader = new ShaderProgram(HistogramType.class, HistogramType.Shaders.ALL_FOR_ONE_SHADER, HistogramType.Shaders.EFIRST_SHADER);
         this.eMinShader = new ShaderProgram(HistogramType.class, HistogramType.Shaders.ALL_FOR_ONE_SHADER, HistogramType.Shaders.EMIN_SHADER);
         this.eMaxShader = new ShaderProgram(HistogramType.class, HistogramType.Shaders.ALL_FOR_ONE_SHADER, HistogramType.Shaders.EMAX_SHADER);
         this.eLastShader = new ShaderProgram(HistogramType.class, HistogramType.Shaders.ALL_FOR_ONE_SHADER, HistogramType.Shaders.ELAST_SHADER);
         allocateVAO(cumulativeHistogram.getResultFramebuffer());

         debugFramebufferData = new float[3];
         debugFramebufferBuffer = BufferUtils.createFloatBuffer(debugFramebufferData.length);
      }
   }

   @Override
   public void close() throws IOException {
      super.close();

      CloseableUtils.close(
            eFirstShader,
            eMinShader,
            eMaxShader,
            eLastShader,
            cumulativeHistogramVAO,
            eFirstFramebuffer,
            eMinFramebuffer,
            eMaxFramebuffer,
            eLastFramebuffer
            );
   }

   @Override
   public void setMaxAdjustPerFrame(float maxAdjust) {
      this.maxAdjustPerFrame = maxAdjust;
   }
   @Override
   public void setPhasedDRAAdjustmentEnabled(boolean enabled) {
      this.phasedDRAAdjustmentEnabled = enabled;
   }

   @Override
   public void reset() {
      final float maxPixel = getImage().getMaxPixelValue();
      clearFramebuffer(0.0f, 0.0f, 0.0f, 1.0f, eMaxFramebuffer, eLastFramebuffer);
      clearFramebuffer(maxPixel, maxPixel, maxPixel, 1.0f, eFirstFramebuffer, eMinFramebuffer);

   }

}
