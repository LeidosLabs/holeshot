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

import static java.util.stream.Collectors.groupingBy;
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
import static org.lwjgl.opengl.GL13.GL_TEXTURE1;
import static org.lwjgl.opengl.GL13.glActiveTexture;
import static org.lwjgl.opengl.GL14.GL_CONSTANT_ALPHA;
import static org.lwjgl.opengl.GL14.GL_FUNC_ADD;
import static org.lwjgl.opengl.GL14.glBlendEquation;
import static org.lwjgl.opengl.GL14.glBlendFuncSeparate;
import static org.lwjgl.opengl.GL15.GL_ARRAY_BUFFER;
import static org.lwjgl.opengl.GL15.glBindBuffer;
import static org.lwjgl.opengl.GL15.glDeleteBuffers;
import static org.lwjgl.opengl.GL20.glUniform1i;
import static org.lwjgl.opengl.GL20.glUniform2iv;
import static org.lwjgl.opengl.GL20.glUseProgram;
import static org.lwjgl.opengl.GL30.glBindVertexArray;
import static org.lwjgl.opengl.GL31.glDrawArraysInstanced;

import java.awt.Dimension;
import java.io.Closeable;
import java.io.IOException;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.apache.commons.lang3.tuple.Pair;
import org.image.common.util.CloseableUtils;
import org.joml.Vector3f;
import org.lwjgl.BufferUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.leidoslabs.holeshot.elt.gpuimage.GLInternalFormat;
import com.leidoslabs.holeshot.elt.gpuimage.HistogramType;
import com.leidoslabs.holeshot.elt.gpuimage.ShaderProgram;
import com.leidoslabs.holeshot.elt.gpuimage.VertexArrayObject;
import com.leidoslabs.holeshot.elt.imagechain.Framebuffer;
import com.leidoslabs.holeshot.elt.imageop.Histogram;
import com.leidoslabs.holeshot.elt.imageop.ImageOp;
import com.leidoslabs.holeshot.elt.imageop.Interpolated;
import com.leidoslabs.holeshot.elt.utils.Vector3Collector;

/**
 * OpenGL Histogram operation. Computes histogram from raw image
 * First operation in image chain
 */
class OglHistogram extends OglAbstractImageOpPrimitive implements Histogram {
   private static final Logger LOGGER = LoggerFactory.getLogger(OglHistogram.class);

   // Enable additional debug logging
   protected static boolean DEBUG = false;

   protected static final int HISTOGRAM_BANDS = 3;

   // This is the well-known value that the position sampler is set/found in by the vertex shader.
   protected static final int POSITION_ATTRIBUTE = 0;

   // TODO: See about adjusting this based on the capabilities of the GPU.   Setting this to "1" on my laptop was driving my FPS down to the point where smooth roaming
   // wasn't possible.
//   private static final int DEFAULT_DOWNSAMPLING_FACTOR = 16;
    private static final int DEFAULT_DOWNSAMPLING_FACTOR = 1;

   private static final int MAX_HISTOGRAM_WIDTH = 4096;

   private int downsamplingFactor;

   private float[] histogram;
   private FloatBuffer histogramBuffer;
   private Framebuffer histogramFramebuffer;

   private static final HistogramType HISTOGRAM_TYPE = HistogramType.RGB;

   private ImageOp interpolated;


   private FloatBuffer rawImageBuffer;
   private float[] rawImageBuf;

   private VertexArrayObject rawImageVAO;

   private final FloatBuffer pointInstance;

   public OglHistogram(HistogramType histogramType) {
      this.pointInstance = BufferUtils.createFloatBuffer(4);

      this.downsamplingFactor = DEFAULT_DOWNSAMPLING_FACTOR;
   }

   public void destroy() {
      glDeleteBuffers(rawImageVAO.getVao());
   }
   public int getBuckets() {
      return getImage().getMaxPixelValue();
   }

   public int getDownsamplingFactor() {
      return downsamplingFactor;
   }
   public int getMaxTextureSize() {
      return Math.min(getBuckets(), MAX_HISTOGRAM_WIDTH);
   }
   public int getNumRows() {
      return (getBuckets()-1) / getMaxTextureSize() + 1;
   }

   @Override
   public Framebuffer getResultFramebuffer() {
      return histogramFramebuffer;
   }

   //  private int first = 0;

   @Override
   public void doRender() throws Exception {

      try {
         initializeFramebuffer();
         getHistogramBuffer().clearBuffer(0.0f, 0.0f, 0.0f, 1.0f);

         // Bind to the Histogram Buffer for Writing
         getHistogramBuffer().bind();

         //      // Set the Viewport to the Histogram Size
         glViewport(0, 0, getHistogramWidth(), getHistogramHeight());

         glEnable(GL_ALPHA_TEST);
         glClearColor(0f, 0f, 0f, 1f);
         glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

         // Enable Blending. Red, Green, and Blue are written by different Shader Programs
         // so blending is necessary to collect the results of all of them in a single buffer
         glEnable(GL_BLEND);

         glBlendEquation(GL_FUNC_ADD);
         glBlendFuncSeparate(GL_ONE,  GL_ONE, GL_ONE, GL_CONSTANT_ALPHA);


         // Bind the Array Buffer that holds the vertices that are sent down to the vertex shader
         final Framebuffer rawImageFB = getInterpolatedFramebuffer();
         final Dimension rawImageFBSize = rawImageFB.getSize();
         allocateVAO(rawImageFB);

         glBindVertexArray(rawImageVAO.getVao());

         glActiveTexture(GL_TEXTURE1);
         glBindTexture(GL_TEXTURE_2D, getInterpolatedFramebuffer().getTexture().getId());
         glActiveTexture(GL_TEXTURE0);

         final int pointsToSample = (rawImageFBSize.width * rawImageFBSize.height) / downsamplingFactor;

         // Iterate over the shader programs to collect the histogram for each of the images HISTOGRAM_BANDS.
         List<ShaderProgram> shaderStates = createShaderPrograms();
         for (ShaderProgram shaderState : shaderStates) {
            shaderState.useProgram();

            glUniform1i(shaderState.getUniformLocation("rawImageTexture"), 1);
            glUniform2iv(shaderState.getUniformLocation("fbDim"), new int[] { getHistogramWidth(), getHistogramHeight()});
            glUniform1i(shaderState.getUniformLocation("buckets"), getBuckets());
            glUniform1i(shaderState.getUniformLocation("maxPixel"), getMaxPixelValue());
            glUniform2iv(shaderState.getUniformLocation("rawImageTextureDim"), new int[] { rawImageFBSize.width, rawImageFBSize.height});
            glUniform1i(shaderState.getUniformLocation("pointsToSample"), pointsToSample);


            // Indicate at which points in the texture that the vertex shader should sample at
            glDrawArraysInstanced(GL_POINTS, 0, 1, pointsToSample);

            glUseProgram(0);
         }

         if (DEBUG) {
            readHistogram();

            // Dump the histogram to STDOUT for debugging purposes
            System.out.println("BEGIN HISTOGRAM");
            dumpHistogram(true);
            System.out.println("END HISTOGRAM");

            readRawImage();
            System.out.println("BEGIN RAW IMAGE");
            dumpRawImage(true);
            System.out.println("END RAW IMAGE");
         }
      } finally {
         glBindVertexArray(0);
         glBindBuffer(GL_ARRAY_BUFFER, 0);
         // Cleanup State
         getHistogramBuffer().unbind();
         glDisable(GL_BLEND);
      }
   }
   private void readRawImage() {
      final Dimension size = interpolated.getResultFramebuffer().getSize();
      final int numBands = Math.max(getImage().getNumBands(), 3);
      final int bufLength = size.height * size.width * numBands;
      if (rawImageBuf == null || rawImageBuf.length != bufLength) {
         this.rawImageBuf = new float[bufLength];
         this.rawImageBuffer = BufferUtils.createFloatBuffer(rawImageBuf.length);
      }
      getInterpolatedFramebuffer().readFramebuffer(rawImageBuffer, rawImageBuf);
   }
   private void dumpRawImage(boolean ignoreZeros) {
      OglHistogram.dump3DArray(rawImageBuf, ignoreZeros, "RAW IMAGE", -1);
   }

   protected void readHistogram() {
      if (histogram == null || histogram.length != getHistogramWidth() * getHistogramHeight() * HISTOGRAM_BANDS) {
         this.histogram = new float[getHistogramWidth() * getHistogramHeight() * HISTOGRAM_BANDS];
         this.histogramBuffer = BufferUtils.createFloatBuffer(histogram.length);
      }

      getHistogramBuffer().readFramebuffer(histogramBuffer, histogram);
   }

   private void allocateVAO(Framebuffer framebuffer) {
      if (rawImageVAO == null) {
         rawImageVAO = new VertexArrayObject(pointInstance, new Dimension(1, 1), POSITION_ATTRIBUTE);
      }
   }
   private List<ShaderProgram> createShaderPrograms() throws IOException {
      List<ShaderProgram> programs = new ArrayList<ShaderProgram>();
      for (String vertexShader: HISTOGRAM_TYPE.getVertexShaders()) {
         programs.add(getELTDisplayContext().getShader(String.format("%s::%s",OglHistogram.class.getName(), vertexShader),HistogramType.class, vertexShader, HistogramType.Shaders.HISTOGRAM_ACCUMULATION_SHADER));
      }
      return programs;
   }

   private void dumpHistogram(boolean ignoreZeros) {
      dump3DArray(histogram, ignoreZeros, "HISTOGRAM", getBuckets());
   }


   private Framebuffer getHistogramBuffer() {
      return histogramFramebuffer;
   }

   private int getHistogramHeight() {
      return getHistogramSize().height;
   }

   private Dimension getHistogramSize() {
      return getHistogramBuffer().getSize();
   }

   private int getHistogramWidth() {
      return getHistogramSize().width;
   }
   private ImageOp getInterpolated() {
      if (interpolated == null) {
         interpolated = getImageChain().getPreviousImageOp(this,  Interpolated.class);
      }
      return interpolated;
   }
   private Framebuffer getInterpolatedFramebuffer() {
      return getInterpolated().getResultFramebuffer();
   }

   private void initializeFramebuffer() throws Exception {
      final int maxTextureSize = getMaxTextureSize();
      final int numRows = getNumRows();
      final Dimension histogramSize = new Dimension(maxTextureSize,numRows);

      if (this.histogramFramebuffer == null || !histogramSize.equals(histogramFramebuffer.getSize())) {
         this.histogramFramebuffer = new Framebuffer(histogramSize, GLInternalFormat.GlInternalFormatRGB32F, getELTDisplayContext());
      }
   }
   static void dump3DArray(float[] buffer, boolean ignoreZeros, String tag, int limit) {
      String results = vector3Stream(buffer, ignoreZeros, limit)
            .sorted((a,b) -> Integer.compare(a.getLeft(),b.getLeft()))
            .map(v -> String
                  .format("%d,%.10f,%.10f,%.10f", v.getLeft(), v.getRight().x, v.getRight().y, v.getRight().z))
            .collect(Collectors.joining("\n"));

      if (!results.isEmpty()) {
         LOGGER.debug(String.join("\n", "############################ BEGIN " + tag,
               results, "############################ END " + tag));
      } else {
         LOGGER.debug("############################ ZERO " + tag + " ENTRIES");
      }
   }
   static Stream<Pair<Integer, Vector3f>> vector3Stream(float[] buffer, boolean ignoreZeros, int limit) {
      return IntStream.range(0, buffer.length).mapToObj(i -> Pair.of(i, buffer[i]))
            .limit((limit > 0) ? limit * 3 : Integer.MAX_VALUE)
            .collect(groupingBy(i -> i.getLeft() / 3, new Vector3Collector())).values().stream()
            .filter(v -> !ignoreZeros || (v.getRight().x + v.getRight().y + v.getRight().z) != 0);
   }

   @Override
   public void close() throws IOException {
      super.close();
      CloseableUtils.close(histogramFramebuffer, rawImageVAO);
   }

   @Override
   public void reset() {
      clearFramebuffer(0.0f, 0.0f, 0.0f, 1.0f, histogramFramebuffer);
   }


}
