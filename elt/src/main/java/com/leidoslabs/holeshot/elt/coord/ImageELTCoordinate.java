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

import org.joml.Vector2d;
import org.joml.Vector3d;
import org.locationtech.jts.geom.Coordinate;

import com.leidoslabs.holeshot.elt.utils.GeometryUtils;
import com.leidoslabs.holeshot.imaging.coord.ImageCoordinate;
import com.leidoslabs.holeshot.imaging.coord.ImageScale;

/**
 * Representation of Image Coordinate with ELTCoordinate functionality
 */
public class ImageELTCoordinate extends ELTCoordinate<Point2D> {

   private final ImageCoordinate imageCoordinate;
   public ImageELTCoordinate(ImageWorld world, ImageCoordinate imageCoordinate) {
      super(world, imageCoordinate.getSourceCoordinate(), imageCoordinate.getImageScale());
      this.imageCoordinate = imageCoordinate;
   }

   public ImageELTCoordinate(ImageWorld imageWorld, Point2D coordinate, ImageScale scale) {
      this(imageWorld, new ImageCoordinate(imageWorld.getTopTile().getImage().getCameraModel(), coordinate, scale));
   }

   @Override
   public Coordinate getGeodeticCoordinate() {
      return imageCoordinate.getGeodeticCoordinate();
   }

   @Override
   public Vector3d getOpenGLCoordinate() {
      return new Vector3d(getImage().imageToOpenGL(GeometryUtils.toVector2d(getR0ImageCoordinate())), -getImageScale().getRset());
   }

   @Override
   public Point2D getRsetImageCoordinate() {
      return imageCoordinate.getRsetImageCoordinate();
   }

   @Override
   public Point2D getR0ImageCoordinate() {
      return imageCoordinate.getR0ImageCoordinate();
   }

   @Override
   public Vector2d getScreenCoordinate() {
      return new OpenGLELTCoordinate(getImageWorld(), getOpenGLCoordinate(), getImageScale()).getScreenCoordinate();
   }

}
