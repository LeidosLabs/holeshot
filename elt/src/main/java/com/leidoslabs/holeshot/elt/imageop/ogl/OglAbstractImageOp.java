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

import java.awt.Dimension;
import java.io.IOException;
import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.leidoslabs.holeshot.elt.ELTDisplayContext;
import com.leidoslabs.holeshot.elt.coord.ImageWorld;
import com.leidoslabs.holeshot.elt.imagechain.Framebuffer;
import com.leidoslabs.holeshot.elt.imagechain.ImageChain;
import com.leidoslabs.holeshot.elt.imagechain.PingPongFramebuffer;
import com.leidoslabs.holeshot.elt.imageop.ImageOp;
import com.leidoslabs.holeshot.elt.imageop.ogl.ImageChainSettings.ImageSource;
import com.leidoslabs.holeshot.elt.tileserver.TileRef;
import com.leidoslabs.holeshot.elt.tileserver.TileserverImage;


/**
 * Abstract type for openGL Image operations
 */
abstract class OglAbstractImageOp implements ImageOp {
   private static final Logger LOGGER = LoggerFactory.getLogger(OglAbstractImageOp.class);

   private ImageChain imageChain;
   private boolean renderingEnabled;

   protected OglAbstractImageOp() {
      setRenderingEnabled(true);
   }

   protected ImageChain getImageChain() {
      return imageChain;
   }
   
   /**
    * Sets ImageChain
    */
   public void setImageChain(ImageChain imageChain) {
      this.imageChain = imageChain;
   }
   protected ImageWorld getImageWorld() {
      return this.imageChain.getImageWorld();
   }
   protected TileRef getTopTile() {
      return getImageWorld().getTopTile();
   }
   protected TileserverImage getImage() {
      return getTopTile().getImage();
   }
   /**
    * @return maximum pixel value in image
    */
   protected int getMaxPixelValue() {
      return (int)Math.pow(2.0,  getImage().getBitsPerPixel());
   }
   
   protected ImageSource getImageSource() {
      // Need to identify the actual imagesource.  metadata.json doesn't currently have it.
//      ImageSource result = ImageSource.UNSPECIFIED;
      ImageSource result = ImageSource.WORLDVIEW_1;
      return result;
   }

   /**
    * @return viewport dimensino
    */
   protected Dimension getViewportDimensions() {
      return new Dimension((int)getImageWorld().getCurrentViewport().getWidth(), (int)getImageWorld().getCurrentViewport().getHeight());
   }

   /**
    * Currently returns true always
    */
   public boolean isFullyRendered() {
      return true;
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

   protected ELTDisplayContext getELTDisplayContext() {
      return imageChain.getELTDisplayContext();
   }

   protected abstract void doRender() throws IOException;

   @Override
   /**
    * If rendering is enabled, will call internal render method, which typically
    * sets up and executes the relevant vertex shader
    */
   public void render() throws IOException {
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
      Arrays.stream(fb).forEach(f->clearFramebuffer(clearRed, clearGreen, clearBlue, clearAlpha, f.getSource(), f.getDestination()));
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
      Arrays.stream(fb).forEach(f-> f.clearBuffer(clearRed, clearGreen, clearBlue, clearAlpha));
   }

}
