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
import static org.lwjgl.opengl.GL11.GL_TEXTURE_2D;
import static org.lwjgl.opengl.GL11.glBindTexture;
import static org.lwjgl.opengl.GL11.glEnable;
import static org.lwjgl.opengl.GL11.glViewport;
import static org.lwjgl.opengl.GL13.GL_TEXTURE0;
import static org.lwjgl.opengl.GL13.GL_TEXTURE1;
import static org.lwjgl.opengl.GL13.GL_TEXTURE2;
import static org.lwjgl.opengl.GL13.GL_TEXTURE3;
import static org.lwjgl.opengl.GL13.GL_TEXTURE4;
import static org.lwjgl.opengl.GL13.GL_TEXTURE5;
import static org.lwjgl.opengl.GL13.GL_TEXTURE6;
import static org.lwjgl.opengl.GL13.glActiveTexture;
import static org.lwjgl.opengl.GL20.glUniform1f;
import static org.lwjgl.opengl.GL20.glUniform1i;
import static org.lwjgl.opengl.GL20.glUniform2iv;
import static org.lwjgl.opengl.GL20.glUseProgram;

import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.FloatBuffer;

import javax.imageio.ImageIO;

import org.image.common.util.CloseableUtils;
import org.lwjgl.BufferUtils;

import com.leidoslabs.holeshot.elt.gpuimage.GLInternalFormat;
import com.leidoslabs.holeshot.elt.gpuimage.HistogramType;
import com.leidoslabs.holeshot.elt.gpuimage.QuadDrawVAO;
import com.leidoslabs.holeshot.elt.gpuimage.ShaderProgram;
import com.leidoslabs.holeshot.elt.imagechain.Framebuffer;
import com.leidoslabs.holeshot.elt.imageop.CumulativeHistogram;
import com.leidoslabs.holeshot.elt.imageop.DRAParameters;
import com.leidoslabs.holeshot.elt.imageop.DynamicRangeAdjust;
import com.leidoslabs.holeshot.elt.imageop.Histogram;
import com.leidoslabs.holeshot.elt.imageop.RawImage;

/**
 * OpenGL DRA operation. Penultimate image operation in the Image Chain
 */
class OglDynamicRangeAdjust extends OglAbstractImageOp implements DynamicRangeAdjust {
   private DRAParameters draParameters;
   private ShaderProgram shader;
   private float[] equalizedImage;
   private FloatBuffer equalizedImageBuffer;
   private Framebuffer equalizedImageFramebuffer;
   private ImageChainSettings imageChainSettings;
   private CumulativeHistogram cumulativeHistogram;

   private Histogram histogram;

   private QuadDrawVAO quadDrawVAO;

   private RawImage rawImage;

   public OglDynamicRangeAdjust() {
   }

   @Override
   public Framebuffer getResultFramebuffer() {
      return this.equalizedImageFramebuffer;
   }

   @Override
   protected void doRender() throws IOException {
      try {
         // this.dumpGLError("ENTER DRA");
         initialize();

         final Histogram histogram = getHistogram();
         final Rectangle histogramRect = histogram.getResultFramebuffer().getRectangle();
         final double percentVisible = getImageWorld().getPercentVisible();
         equalizedImageFramebuffer.clearBuffer();

         // Bind to the Equalized Image Buffer for Writing
         equalizedImageFramebuffer.bind();

         glEnable(GL_ALPHA_TEST);

         //glTexEnvi(GL_TEXTURE_ENV, GL_TEXTURE_ENV_MODE, GL_REPLACE);
         glViewport(0, 0,  getWidth(), getHeight());

         final RawImage rawImage = getRawImage();

         glActiveTexture(GL_TEXTURE1);
         glBindTexture(GL_TEXTURE_2D, rawImage.getResultFramebuffer().getTexture().getId());
         glActiveTexture(GL_TEXTURE0);

         glActiveTexture(GL_TEXTURE2);
         glBindTexture(GL_TEXTURE_2D, cumulativeHistogram.getResultFramebuffer().getTexture().getId());
         glActiveTexture(GL_TEXTURE0);

         glActiveTexture(GL_TEXTURE3);
         glBindTexture(GL_TEXTURE_2D, draParameters.getEFirstFramebuffer().getDestination().getTexture().getId());
         glActiveTexture(GL_TEXTURE0);

         glActiveTexture(GL_TEXTURE4);
         glBindTexture(GL_TEXTURE_2D, draParameters.getEMinFramebuffer().getDestination().getTexture().getId());
         glActiveTexture(GL_TEXTURE0);

         glActiveTexture(GL_TEXTURE5);
         glBindTexture(GL_TEXTURE_2D, draParameters.getEMaxFramebuffer().getDestination().getTexture().getId());
         glActiveTexture(GL_TEXTURE0);

         glActiveTexture(GL_TEXTURE6);
         glBindTexture(GL_TEXTURE_2D, draParameters.getELastFramebuffer().getDestination().getTexture().getId());
         glActiveTexture(GL_TEXTURE0);

         shader.useProgram();

         // Set the texture that the vertex shader should read from
         glUniform1i(shader.getUniformLocation("rawImage"), 1);
         glUniform1i(shader.getUniformLocation("cumulativeHistogram"), 2);
         glUniform1i(shader.getUniformLocation("eFirstTexture"), 3);
         glUniform1i(shader.getUniformLocation("eMinTexture"), 4);
         glUniform1i(shader.getUniformLocation("eMaxTexture"), 5);
         glUniform1i(shader.getUniformLocation("eLastTexture"), 6);
         glUniform2iv(shader.getUniformLocation("fbDim"), new int[] { getWidth(), getHeight() });
         glUniform2iv(shader.getUniformLocation("histFBDim"), new int[] { histogramRect.width, histogramRect.height});

         glUniform1f(shader.getUniformLocation("visibleImageArea"), (float)(percentVisible * getWidth() * getHeight()));
         glUniform1i(shader.getUniformLocation("histogramDownsampling"), histogram.getDownsamplingFactor());

         glUniform1i(shader.getUniformLocation("buckets"), histogram.getBuckets());
         glUniform1i(shader.getUniformLocation("maxPixel"), getImage().getMaxPixelValue());

         glUniform1f(shader.getUniformLocation("ic_a"), imageChainSettings.getA());
         glUniform1f(shader.getUniformLocation("ic_b"), imageChainSettings.getB());

         this.quadDrawVAO.draw();

         glUseProgram(0);


         //dumpImage();

         if (OglHistogram.DEBUG) {
            readEqualizedImage();

            // Dump the histogram to STDOUT for debugging purposes
            dumpEqualizedImage(false);
         }

         // dumpGLError("EXIT DRA");

      } finally {
         // Cleanup State
         equalizedImageFramebuffer.unbind();
      }

   }
   private void dumpEqualizedImage(boolean ignoreZeros) {
      OglHistogram.dump3DArray(equalizedImage, ignoreZeros, "EQUALIZED", -1);
   }
   private DRAParameters getDRAParameters() {
      if (draParameters == null) {
         draParameters = this.getImageChain().getPreviousImageOp(this, DRAParameters.class);
      }
      return draParameters;
   }
   private int getHeight() {
      return getSize().height;
   }

   private CumulativeHistogram getCumulativeHistogram() {
      if (cumulativeHistogram == null) {
         cumulativeHistogram = this.getImageChain().getPreviousImageOp(this, CumulativeHistogram.class);
      }
      return cumulativeHistogram;
   }

   private Histogram getHistogram() {
      if (histogram == null) {
         histogram = this.getImageChain().getPreviousImageOp(this, Histogram.class);
      }
      return histogram;
   }
   private RawImage getRawImage() {
      if (rawImage == null) {
         return this.getImageChain().getPreviousImageOp(this, RawImage.class);
      }
      return rawImage;
   }

   private Dimension getSize() {
      return equalizedImageFramebuffer.getSize();
   }
   private int getWidth() {
      return getSize().width;
   }

   private void initialize() throws IOException {
      final Dimension viewportDimensions = getViewportDimensions();
      if (equalizedImageFramebuffer == null) {
         cumulativeHistogram = getCumulativeHistogram();
         draParameters = getDRAParameters();
         equalizedImageFramebuffer = new Framebuffer(viewportDimensions, GLInternalFormat.GlInternalFormatRGBA32F, getELTDisplayContext());
         this.shader = new ShaderProgram(HistogramType.class, HistogramType.Shaders.PASSTHROUGH_VERTEX_SHADER, HistogramType.Shaders.EQUALIZATION_SHADER);
         this.quadDrawVAO = new QuadDrawVAO(QuadDrawVAO.FULL_UNIFORM_QUAD, 0);
      } else if (!equalizedImageFramebuffer.getSize().equals(viewportDimensions)) {
         equalizedImageFramebuffer.reset(viewportDimensions);
      }
      imageChainSettings = getImageSource().getSettings();
   }
   private void readEqualizedImage() {
      final int imageBands = 3; // getImage().getNumBands();
      if (equalizedImage == null || equalizedImage.length != getWidth() * getHeight() * imageBands) {
         this.equalizedImage = new float[getWidth() * getHeight() * imageBands];
         this.equalizedImageBuffer = BufferUtils.createFloatBuffer(equalizedImage.length);
      }
      equalizedImageFramebuffer.readFramebuffer(equalizedImageBuffer, equalizedImage);
   }

   boolean first = true;
   private void dumpImage() {
      if (first) {
         try {
            first = false;
            readEqualizedImage();
            equalizedImageBuffer.rewind();

            final int width = getWidth();
            final int height = getHeight();
            BufferedImage image = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_INT_RGB);
            int[] rgbArray = new int[width*height];
            final int maxPixel = 256; // (int)Math.pow(2, 16);
            for(int y = 0; y < height; ++y) {
               for(int x = 0; x < width; ++x) {
                  int r = (int)(equalizedImageBuffer.get() * maxPixel) << 16;
                  int g = (int)(equalizedImageBuffer.get() * maxPixel) << 8;
                  int b = (int)(equalizedImageBuffer.get() * maxPixel);
                  int i = ((height - 1) - y) * width + x;
                  rgbArray[i] = r + g + b;
               }
            }

            image.setRGB(0,0,getWidth(), getHeight(), rgbArray, 0, getWidth());
            ImageIO.write(image, "png", new File("F:/fooEqualized.png"));
         } catch (IOException e) {
            e.printStackTrace();
         }

      }
   }
   @Override
   public void close() throws IOException {
      super.close();
      CloseableUtils.close(shader, equalizedImageFramebuffer, quadDrawVAO);
   }

   @Override
   public void reset() {
      clearFramebuffer(0.0f, 0.0f, 0.0f, 1.0f, equalizedImageFramebuffer);
   }

}
