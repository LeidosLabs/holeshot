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

package com.leidoslabs.holeshot.elt.coord;

import org.joml.Matrix4d;
import org.joml.Vector2d;
import org.joml.Vector3d;

import com.leidoslabs.holeshot.elt.tileserver.TileRef;
import com.leidoslabs.holeshot.elt.tileserver.TileserverImage;
import com.leidoslabs.holeshot.imaging.coord.GeointCoordinate;
import com.leidoslabs.holeshot.imaging.coord.ImageScale;

/**
 * Abstract representation of a Geointcoordinate in ELT.
 * Uses ImageWorld to enable implementations of conversions between coordinate types 
 * @param <C>
 */
public abstract class ELTCoordinate<C> extends GeointCoordinate<C> {
   private ImageWorld imageWorld;

   protected ELTCoordinate(ImageWorld imageWorld, C sourceCoordinate, ImageScale imageScale) {
      super(imageWorld.getTopTile().getImage().getCameraModel(), sourceCoordinate, imageScale);
      this.imageWorld = imageWorld;
   }
   public abstract Vector3d getOpenGLCoordinate();
   public abstract Vector2d getScreenCoordinate();

   protected ImageWorld getImageWorld() {
      return this.imageWorld;
   }
   protected TileRef getTopTile() {
      return this.imageWorld.getTopTile();
   }
   protected TileserverImage getImage() {
      return getTopTile().getImage();
   }
   /**
    * @return openGL to screen transformation 
    */
   protected Matrix4d openGLToScreen() {
      return getImageWorld().getCurrentProjection().mul(getImageWorld().getCurrentModel());
   }
}
