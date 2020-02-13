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

import java.awt.geom.Point2D;

import org.joml.Matrix4d;
import org.joml.Vector2d;
import org.joml.Vector3d;
import org.locationtech.jts.geom.Coordinate;

import com.leidoslabs.holeshot.imaging.coord.ImageCoordinate;
import com.leidoslabs.holeshot.imaging.coord.ImageScale;

/**
 * Representation of Screen coordinate with ELTCoordinate functionality
 *
 */
public class ScreenELTCoordinate extends ELTCoordinate<Vector2d> {
   public ScreenELTCoordinate(ImageWorld world, Vector2d coordinate, ImageScale imageScale) {
      super(world, coordinate, imageScale);
   }

   @Override
   public Coordinate getGeodeticCoordinate() {
      return new ImageCoordinate(getCameraModel(), getR0ImageCoordinate(), ImageScale.forRset(0)).getGeodeticCoordinate();
   }

   @Override
   public Vector3d getOpenGLCoordinate() {
      Vector2d screen = getSourceCoordinate();
      Vector3d openGL = new Vector3d();
      final Matrix4d openGLToScreen = openGLToScreen();

      openGLToScreen.unproject(screen.x,
            getImageWorld().getCurrentViewport().getHeight()-screen.y-1.0,
            0.0,
            getData(getImageWorld().getCurrentViewport()),
            openGL);
      openGL.z = -getImageScale().getRset();

      return openGL;
   }

   @Override
   public Point2D getR0ImageCoordinate() {
      return new OpenGLELTCoordinate(getImageWorld(), getOpenGLCoordinate(), getImageScale()).getR0ImageCoordinate();
   }

   @Override
   public Vector2d getScreenCoordinate() {
      return getSourceCoordinate();
   }

}
