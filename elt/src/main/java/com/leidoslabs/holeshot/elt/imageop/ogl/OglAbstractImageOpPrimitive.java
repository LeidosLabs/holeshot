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
import java.awt.Rectangle;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.leidoslabs.holeshot.elt.ELTDisplayContext;
import com.leidoslabs.holeshot.elt.imagechain.ImageChain;
import com.leidoslabs.holeshot.elt.imageop.ImageOpPrimitive;
import com.leidoslabs.holeshot.elt.tileserver.TileRef;
import com.leidoslabs.holeshot.elt.tileserver.TileserverImage;
import com.leidoslabs.holeshot.elt.utils.GeometryUtils;
import com.leidoslabs.holeshot.elt.viewport.ImageWorld;


/**
 * Abstract type for openGL Image operations
 */
abstract class OglAbstractImageOpPrimitive extends OglAbstractImageOp implements ImageOpPrimitive {
   private static final Logger LOGGER = LoggerFactory.getLogger(OglAbstractImageOpPrimitive.class);

   private ImageChain imageChain;

   protected OglAbstractImageOpPrimitive() {
	   super();
   }

   protected ImageChain getImageChain() {
	   return imageChain;
   }
   
   public Dimension getViewportDimensions() {
	   Rectangle viewport = getImageWorld().getCurrentViewport();
	   return GeometryUtils.toDimension(viewport);
   }

   /**
    * Sets ImageChain
    */
   public void setImageChain(ImageChain imageChain) {
      this.imageChain = imageChain;
   }
   protected ImageWorld getImageWorld() {
      return imageChain.getImageWorld();
   }
   protected TileRef getTopTile() {
      return getImage().getTopTile();
   }
   protected TileserverImage getImage() {
      return imageChain.getImage();
   }

   /**
    * @return maximum pixel value in image
    */
   protected int getMaxPixelValue() {
      return (int)Math.pow(2.0,  getImage().getBitsPerPixel());
   }

   protected ELTDisplayContext getELTDisplayContext() {
      return imageChain.getELTDisplayContext();
   }
}
