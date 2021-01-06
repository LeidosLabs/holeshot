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

import org.joml.Vector2ic;
import org.joml.Vector3dc;
import org.locationtech.jts.geom.Coordinate;

import com.leidoslabs.holeshot.elt.tileserver.TileserverImage;
import com.leidoslabs.holeshot.elt.viewport.ImageWorld;
import com.leidoslabs.holeshot.imaging.coord.ImageCoordinate;
import com.leidoslabs.holeshot.imaging.coord.ImageScale;
import com.leidoslabs.holeshot.imaging.photogrammetry.CameraModel;

/**
 * Representation of Image Coordinate with ELTCoordinate functionality
 */
public class ImageELTCoordinate extends ELTCoordinate<Point2D> {
   private final ImageCoordinate imageCoordinate;
   private final TileserverImage image;
   
   public ImageELTCoordinate(TileserverImage image, ImageWorld imageWorld, ImageCoordinate imageCoordinate) {
      super(imageWorld, imageCoordinate.getSourceCoordinate());
      this.imageCoordinate = imageCoordinate;
      this.image = image;
   }

   public ImageELTCoordinate(TileserverImage image, ImageWorld imageWorld, Point2D coordinate, ImageScale scale) {
      this(image, imageWorld, new ImageCoordinate(image.getCameraModel(), coordinate, scale));
   }

   @Override
   public Coordinate getGeodeticCoordinate() {
      return imageCoordinate.getGeodeticCoordinate();
   }

   @Override
   public Vector3dc getOpenGLCoordinate() {
	   return getImageWorld().geodeticToClip(getGeodeticCoordinate());
   }

   public Point2D getRsetImageCoordinate() {
      return getSourceCoordinate();
   }

   public Point2D getR0ImageCoordinate() {
      return imageCoordinate.getR0ImageCoordinate();
   }

   @Override
   public Vector2ic getScreenCoordinate() {
	   return getImageWorld().geodeticToScreen(getGeodeticCoordinate());
   }
   
	@Override
	public Coordinate getProjectedCoordinate() {
		return getImageWorld().imageToProjected(image, getSourceCoordinate());
	}


}
