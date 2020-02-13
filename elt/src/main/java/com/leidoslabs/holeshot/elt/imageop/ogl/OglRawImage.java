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

import static org.lwjgl.opengl.GL11.glEnable;
import static org.lwjgl.opengl.GL11.glViewport;
import static org.lwjgl.opengl.GL13.GL_MULTISAMPLE;

import java.awt.Dimension;
import java.io.IOException;

import org.image.common.util.CloseableUtils;

import com.leidoslabs.holeshot.elt.ELTImageTexture;
import com.leidoslabs.holeshot.elt.gpuimage.GLInternalFormat;
import com.leidoslabs.holeshot.elt.imagechain.Framebuffer;
import com.leidoslabs.holeshot.elt.imageop.RawImage;
import com.leidoslabs.holeshot.elt.tileserver.TileRef;

/**
 * Gets image from top tile, and loads into openGL frame buffer.
 * First stage in image chain
 *
 */
class OglRawImage extends OglAbstractImageOp implements RawImage {

   private Framebuffer rawImageFramebuffer;
   private ELTImageTexture topTexture;
   private boolean progressiveRender;
   private boolean fullyRendered;

   public OglRawImage(boolean progressiveRender) {
      this.progressiveRender = progressiveRender;
      topTexture = null;
   }

   @Override
   protected void doRender() throws IOException {
      updateFramebuffer();
      drawImageToFramebuffer();
   }

   @Override
   public Framebuffer getResultFramebuffer() {
      return rawImageFramebuffer;
   }

   private void updateFramebuffer() throws IOException {
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
   private void drawImageToFramebuffer() throws IOException {
      fullyRendered = false;
      final TileRef newTopTile = getImageWorld().getTopTile();
      final TileRef oldTopTile = (topTexture == null) ? null : topTexture.getTileRef();
      if (!newTopTile.equals(oldTopTile)) {
         topTexture = ELTImageTexture.getTexture(getImageChain().getELTDisplayContext(), newTopTile, this.getImageWorld());
      }
      if (topTexture != null) {
         if (getImageWorld().getPercentVisible() < 1.0) {
            rawImageFramebuffer.clearBuffer();
         }
         rawImageFramebuffer.bind();

         glViewport(0, 0, getWidth(), getHeight());


         fullyRendered = topTexture.draw(progressiveRender, getImageWorld());
         rawImageFramebuffer.unbind();
      }
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
      clearFramebuffer(0.0f, 0.0f, 0.0f, 1.0f, rawImageFramebuffer);
   }

}
