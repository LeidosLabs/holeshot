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
import java.util.HashMap;
import java.util.Map;

import org.image.common.util.CloseableUtils;

import com.leidoslabs.holeshot.elt.gpuimage.GLInternalFormat;
import com.leidoslabs.holeshot.elt.gpuimage.HistogramType;
import com.leidoslabs.holeshot.elt.gpuimage.QuadDrawVAO;
import com.leidoslabs.holeshot.elt.gpuimage.ShaderProgram;
import com.leidoslabs.holeshot.elt.gpuimage.Texture;
import com.leidoslabs.holeshot.elt.imagechain.Framebuffer;
import com.leidoslabs.holeshot.elt.imageop.DynamicRangeAdjust;
import com.leidoslabs.holeshot.elt.imageop.ToneTransferCurve;
import com.leidoslabs.holeshot.elt.imageop.ogl.ImageChainSettings.ImageSource;

/**
 * Final OpenGL operation in Image Chain. Uses a sensor specific lookup table
 * to DRA'd pixels
 */
public class OglToneTransferCurve extends OglAbstractImageOp implements ToneTransferCurve {

   private ShaderProgram shader;
   private Framebuffer toneCorrectedFramebuffer;

   private Texture ttcTexture;
   private QuadDrawVAO quadDrawVAO;
   private ImageSource ttcImageSource;
   private ImageChainSettings imageChainSettings;

   private static final Map<String, int[]> ttcCurves = new HashMap<String, int[]>();

   public OglToneTransferCurve() {
   }

   @Override
   public Framebuffer getResultFramebuffer() {
      return this.toneCorrectedFramebuffer;
   }

   private Dimension getSize() {
      return getResultFramebuffer().getSize();
   }

   private int getWidth() {
      return getSize().width;
   }

   private int getHeight() {
      return getSize().height;
   }

   @Override
   protected void doRender() throws IOException {
      try {
         initialize();

         toneCorrectedFramebuffer.clearBuffer();

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

         this.quadDrawVAO.draw();

         glUseProgram(0);

      } catch (URISyntaxException e) {
         throw new IOException(e);
      } finally {
         // Cleanup State
         toneCorrectedFramebuffer.unbind();
      }

   }

   private void initialize() throws IOException, URISyntaxException {
      final Dimension viewportDimensions = getViewportDimensions();

      if (toneCorrectedFramebuffer == null) {
         toneCorrectedFramebuffer = new Framebuffer(getViewportDimensions(), GLInternalFormat.GlInternalFormatRGBA32F, getELTDisplayContext());
         this.shader = new ShaderProgram(HistogramType.class, HistogramType.Shaders.PASSTHROUGH_VERTEX_SHADER, HistogramType.Shaders.TTC_SHADER);
         this.quadDrawVAO = new QuadDrawVAO(QuadDrawVAO.FULL_UNIFORM_QUAD, 0);
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
      imageChainSettings.adjustSub(d);
   }

   @Override
   public void adjustContrast(float d) {
      imageChainSettings.adjustMul(d);
   }

   @Override
   public void adjustGamma(float d) {
      imageChainSettings.adjustGamma(d);
   }

   @Override
   public void resetManualAdjustments() {
      imageChainSettings = new ImageChainSettings(ttcImageSource.getSettings());
   }

   @Override
   public void close() throws IOException {
      super.close();
      CloseableUtils.close(shader, toneCorrectedFramebuffer, ttcTexture, quadDrawVAO);
   }
   @Override
   public void reset() {
      clearFramebuffer(0.0f, 0.0f, 0.0f, 1.0f, toneCorrectedFramebuffer);
   }

}
