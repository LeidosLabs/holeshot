/*
 * Licensed to The Leidos Corporation under one or more contributor license agreements.
 * See the NOTICE file distributed with this work for additional information regarding copyright ownership.
 * The Leidos Corporation licenses this file to You under the Apache License, Version 2.0
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
package com.leidoslabs.holeshot.imaging.coord;

import java.awt.geom.Point2D;

import org.locationtech.jts.geom.Coordinate;

import com.leidoslabs.holeshot.imaging.photogrammetry.CameraModel;

/**
 * GeointCoordinate utilzing Point2D as the source coordinate
 */
public class ImageCoordinate extends GeointCoordinate<Point2D> {

    /**
     * @param cameraModel The camera model of the image this coordinate is in
     * @param coordinate The source coordinate
     * @param imageScale The ImageScale object to track image scale and convert between different rSets
     */
  public ImageCoordinate(CameraModel cameraModel, Point2D coordinate, ImageScale imageScale) {
    super(cameraModel, coordinate, imageScale);
  }

  @Override
  public Coordinate getGeodeticCoordinate() {
    return getCameraModel().imageToWorld(getR0ImageCoordinate());
  }

  @Override
  public Point2D getRsetImageCoordinate() {
    return getSourceCoordinate();
  }

  @Override
  public Point2D getR0ImageCoordinate() {
    return scaleUpToR0(getSourceCoordinate());
  }
}
