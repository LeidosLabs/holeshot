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
import com.leidoslabs.holeshot.imaging.photogrammetry.rpc.RPCCameraModel;

/**
 * A GeointCoordinate representing a point on Earth
 */
public class GeodeticCoordinate extends GeointCoordinate<Coordinate> {

    /**
     * Create a new Geodetic Coordinate representing a point on a Earth
     * @param cameraModel The camera model which relates this point to a pixel of an image
     * @param coordinate The x: longitude y: latitude coordinate of this point
     * @param imageScale The ImageScale object to track and convert between different rSets
     */
  public GeodeticCoordinate(CameraModel cameraModel, Coordinate coordinate, ImageScale imageScale) {
    super(cameraModel, coordinate, imageScale);
  }

    /**
     * Return the Longitude - Latitude coordinate this object was built with
     * @return The source coordinate
     */
  @Override
  public Coordinate getGeodeticCoordinate() {
      return getSourceCoordinate();
  }

    /**
     * Get the pixel location within the full resolution image that corresponds
     * to this GeodeticCoordinate's Longitude - Latitude source coordinate
     * @return A point in the full resolution image space
     */
  @Override
  public Point2D getR0ImageCoordinate() {
     // TODO: RGR- This is a bit of kludge to get past the immediate problem of bogus input elevations screwing up
     // camera model calculations.  We're just going to set all given elevations to the height offset.  Need to rethink this
     // to see if it's appropriate in the future.
     final Coordinate sourceCoordinate = new Coordinate(getSourceCoordinate());
     final CameraModel cameraModel = getCameraModel();
     if (cameraModel instanceof RPCCameraModel) {
        final RPCCameraModel rpcCameraModel = (RPCCameraModel)cameraModel;
        sourceCoordinate.z = rpcCameraModel.getNormalization().getHtOff();
     }
    return cameraModel.worldToImage(sourceCoordinate);
  }
}
