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

import static org.lwjgl.opengl.GL11.GL_COLOR_BUFFER_BIT;
import static org.lwjgl.opengl.GL11.GL_NEAREST;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_2D;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_BORDER_COLOR;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_WRAP_S;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_WRAP_T;
import static org.lwjgl.opengl.GL11.glTexParameterfv;
import static org.lwjgl.opengl.GL11.glTexParameteri;
import static org.lwjgl.opengl.GL11.glViewport;
import static org.lwjgl.opengl.GL13.GL_CLAMP_TO_BORDER;
import static org.lwjgl.opengl.GL13.GL_TEXTURE0;
import static org.lwjgl.opengl.GL13.GL_TEXTURE1;
import static org.lwjgl.opengl.GL13.glActiveTexture;
import static org.lwjgl.opengl.GL20.glUniform1i;
import static org.lwjgl.opengl.GL20.glUniform2iv;
import static org.lwjgl.opengl.GL20.glUseProgram;
import static org.lwjgl.opengl.GL30.GL_DRAW_FRAMEBUFFER;
import static org.lwjgl.opengl.GL30.GL_READ_FRAMEBUFFER;
import static org.lwjgl.opengl.GL30.glBindFramebuffer;
import static org.lwjgl.opengl.GL30.glBlitFramebuffer;

import java.awt.Dimension;
import java.awt.Rectangle;
import java.io.IOException;
import java.nio.FloatBuffer;

import org.image.common.util.CloseableUtils;
import org.lwjgl.BufferUtils;

import com.leidoslabs.holeshot.elt.gpuimage.GLInternalFormat;
import com.leidoslabs.holeshot.elt.gpuimage.HistogramAccess;
import com.leidoslabs.holeshot.elt.gpuimage.HistogramType;
import com.leidoslabs.holeshot.elt.gpuimage.QuadDrawVAO;
import com.leidoslabs.holeshot.elt.gpuimage.ShaderProgram;
import com.leidoslabs.holeshot.elt.imagechain.Framebuffer;
import com.leidoslabs.holeshot.elt.imagechain.PingPongFramebuffer;
import com.leidoslabs.holeshot.elt.imageop.Histogram;
import com.leidoslabs.holeshot.elt.imageop.SummedArea;

/**
 * OpenGL ImageOp to calculate a summed area table.
 */
class OglSummedArea extends OglAbstractImageOp implements SummedArea {

   final static int HISTOGRAM_BANDS = 3;
   private static final float[] BLACK = new float[] {0.0f, 0.0f, 0.0f, 1.0f};
   private static final int DEFAULT_PIXEL_LOOKBACK = 16;
   private Histogram histogram;
   private ShaderProgram horizontalPhaseShader;
   private int pixelLookback;

   private QuadDrawVAO quadDrawVAO;
   private float[] summedArea;

   private FloatBuffer summedAreaBuffer;
   private PingPongFramebuffer summedAreaFramebuffer;

   private ShaderProgram verticalPhaseShader;

   public OglSummedArea() {
      this.pixelLookback = DEFAULT_PIXEL_LOOKBACK;
   }


   public int getPixelLookback() {
      return pixelLookback;
   }


   @Override
   public Framebuffer getResultFramebuffer() {
      return summedAreaFramebuffer.getDestination();
   }

   @Override
   protected void doRender() throws IOException {
      final Histogram histogram = getHistogram();
      initialize(histogram);

      this.blit(histogram.getResultFramebuffer(), summedAreaFramebuffer.getDestination());
      summedAreaFramebuffer.getSource().clearBuffer();

      glViewport(0, 0, getWidth(), getHeight());

      HistogramAccess histogramAccess = new HistogramAccess(histogram.getResultFramebuffer().getSize(),
            histogram.getBuckets(), getImage().getMaxPixelValue());

      scan(horizontalPhaseShader, getWidth(), histogramAccess, "HORIZ", histogram);
      scan(verticalPhaseShader, getHeight(), histogramAccess, "VERT", histogram);

      if (OglHistogram.DEBUG) {
         readSummedArea();

         // Dump the histogram to STDOUT for debugging purposes
         dumpSummedArea(true);
      }
   }

   public void setPixelLookback(int pixelLookback) {
      this.pixelLookback = pixelLookback;
   }

   private void blit(Framebuffer fbSrc, Framebuffer fbDest) {
      this.blit(fbSrc, fbDest, fbSrc.getRectangle(), fbDest.getRectangle());
   }

   private void blit(Framebuffer fbSrc, Framebuffer fbDest, Rectangle srcRect,
         Rectangle destRect) {
      glBindFramebuffer(GL_READ_FRAMEBUFFER, fbSrc.getFboId());
      glBindFramebuffer(GL_DRAW_FRAMEBUFFER, fbDest.getFboId());
      glBlitFramebuffer(srcRect.x, srcRect.y, srcRect.x + srcRect.width,
            srcRect.y + srcRect.height, destRect.x, destRect.y,
            destRect.x + destRect.width, destRect.y + destRect.height,
            GL_COLOR_BUFFER_BIT, GL_NEAREST);
   }

   private void dumpSummedArea(boolean ignoreZeros) {
      OglHistogram.dump3DArray(summedArea, ignoreZeros, "SUMMED AREA", getHistogram().getBuckets());
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
      return this.summedAreaFramebuffer.getSource().getSize();
   }

   private int getWidth() {
      return getSize().width;
   }

   private void initialize(Histogram histogram) throws IOException {
      if (this.horizontalPhaseShader == null) {
         this.horizontalPhaseShader = new ShaderProgram(HistogramType.class,
               HistogramType.Shaders.PASSTHROUGH_VERTEX_SHADER,
               HistogramType.Shaders.SUMMED_AREA_HORIZONTAL_PHASE_SHADER);
         this.verticalPhaseShader = new ShaderProgram(HistogramType.class,
               HistogramType.Shaders.PASSTHROUGH_VERTEX_SHADER,
               HistogramType.Shaders.SUMMED_AREA_VERTICAL_PHASE_SHADER);
         this.quadDrawVAO = new QuadDrawVAO(QuadDrawVAO.FULL_UNIFORM_QUAD, 0);
      }

      final Dimension summedAreaSize = new Dimension(histogram.getMaxTextureSize(), histogram.getNumRows());
      if (this.summedAreaFramebuffer == null || !this.summedAreaFramebuffer.getSource().getSize().equals(summedAreaSize)) {
         this.summedAreaFramebuffer = new PingPongFramebuffer(new Dimension(histogram.getMaxTextureSize(), histogram.getNumRows()), GLInternalFormat.GlInternalFormatRGBA32F, getELTDisplayContext());
      }

   }

   private void readSummedArea() {
      if (summedArea == null || summedArea.length != getWidth() * getHeight() * HISTOGRAM_BANDS) {
         this.summedArea = new float[getWidth() * getHeight() * HISTOGRAM_BANDS];
         this.summedAreaBuffer = BufferUtils.createFloatBuffer(summedArea.length);
      }

      summedAreaFramebuffer.getDestination().readFramebuffer(summedAreaBuffer, summedArea);
   }

   private void scan(ShaderProgram scanner, int width, HistogramAccess histogramAccess,
         String tag, Histogram histogram) {
      // Rectangle blitRect = null;
      final Rectangle histogramRect = this.summedAreaFramebuffer.getDestination().getRectangle();

      for (int i = 0; i < logOfBase(pixelLookback, width); ++i) {
         this.summedAreaFramebuffer.swap();

         // Bind to the Histogram Buffer for Writing
         this.summedAreaFramebuffer.getDestination().bind();

         glActiveTexture(GL_TEXTURE1);
         this.summedAreaFramebuffer.getSource().getTexture().bind();
         glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_BORDER);
         glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_BORDER);
         glTexParameterfv(GL_TEXTURE_2D, GL_TEXTURE_BORDER_COLOR, BLACK);
         glActiveTexture(GL_TEXTURE0);
         scanner.useProgram();

         glUniform1i(scanner.getUniformLocation("texture1"), 1);
         glUniform2iv(scanner.getUniformLocation("fbDim"), new int[] {histogramRect.width, histogramRect.height});
         glUniform1i(scanner.getUniformLocation("scanStart"), i);
         glUniform1i(scanner.getUniformLocation("pixelLookback"), pixelLookback);

         glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_BORDER);
         glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_BORDER);
         glTexParameterfv(GL_TEXTURE_2D, GL_TEXTURE_BORDER_COLOR, BLACK);

         this.quadDrawVAO.draw();
         glUseProgram(0);

         // Cleanup State
         this.summedAreaFramebuffer.getDestination().unbind();

      }
   }

   private static int logOfBase(int base, int num) {
      return (int) (Math.ceil(Math.log(num) / Math.log(base)));
   }

   @Override
   public void close() throws IOException {
      super.close();
      CloseableUtils.close(horizontalPhaseShader, quadDrawVAO, summedAreaFramebuffer, verticalPhaseShader);
   }

   @Override
   public void reset() {
      clearFramebuffer(0.0f, 0.0f, 0.0f, 1.0f, summedAreaFramebuffer);
   }


}
