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
package com.leidoslabs.holeshot.imaging.photogrammetry;

import org.locationtech.jts.geom.Coordinate;

/**
 * This class defines the surface in 3-D world coordinates in which to project points from the
 * image. The surface must be a true function: a vertical line through a given point in the (x,y)
 * plane intersects the surface in at most one point.
 */
public abstract class Surface {

  /**
   * Given a point (x,y) returns z such that the point (x,y,z) lies on the surface
   */
  public abstract double getElevation(double x, double y);

  /**
   * Updates the z ordinate of teh coordinate such that (geo.x, geo.y, geo.z) lies on the surface.
   */
  public void updateElevation(Coordinate geo) {
    geo.setOrdinate(2,getElevation(geo.x,geo.y));
  }

}

