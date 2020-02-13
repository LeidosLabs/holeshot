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

import com.leidoslabs.holeshot.imaging.coord.GeodeticCoordinate;
import com.leidoslabs.holeshot.imaging.coord.ImageCoordinate;
import com.leidoslabs.holeshot.imaging.coord.ImageScale;

/**
 * Representation of Geodetic Coordinate with ELTCoordinate functionality
 */
public class GeodeticELTCoordinate extends ELTCoordinate<Coordinate> {

   private final GeodeticCoordinate geodeticCoordinate;

  public GeodeticELTCoordinate(ImageWorld world, GeodeticCoordinate geodeticCoordinate) {
    super(world, geodeticCoordinate.getSourceCoordinate(), geodeticCoordinate.getImageScale());
    this.geodeticCoordinate = geodeticCoordinate;
  }
  public GeodeticELTCoordinate(ImageWorld imageWorld, Coordinate coordinate, ImageScale scale) {
     this(imageWorld, new GeodeticCoordinate(imageWorld.getTopTile().getImage().getCameraModel(), coordinate, scale));
  }

  @Override
  public Coordinate getGeodeticCoordinate() {
      return geodeticCoordinate.getSourceCoordinate();
  }

  @Override
  public Vector3d getOpenGLCoordinate() {
    return getELTImageCoordinate().getOpenGLCoordinate();
  }


  @Override
  public Point2D getR0ImageCoordinate() {
     return geodeticCoordinate.getR0ImageCoordinate();
  }

  @Override
  public Vector2d getScreenCoordinate() {
     return getELTImageCoordinate().getScreenCoordinate();
  }

  private ImageCoordinate getImageCoordinate() {
     return new ImageCoordinate(getCameraModel(), getR0ImageCoordinate(), ImageScale.forRset(0));
  }
  private ImageELTCoordinate getELTImageCoordinate() {
     return new ImageELTCoordinate(getImageWorld(), getImageCoordinate());

  }

}
