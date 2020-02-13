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

/**
 * Defines a constant elevation from an Earth ellipsoid.
 */
public class ConstantElevationSurface extends Surface {

  /**
   * Elevation (in meters) in relation to the Earth ellipsoid (if the value is negative, then it
   * is below the ellipsoid).
   */
  private double elevation;

  /**
   * Constructor which initializes elevation to zero
   */
  public ConstantElevationSurface() {
    elevation = 0.0;
  }

  /**
   * Constructor which initializes to given elevation.
   */
  public ConstantElevationSurface(double elevation) {
    this.elevation = elevation;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public double getElevation(double x, double y) {
    return elevation;
  }

  public void setElevation(double elevation) {
    this.elevation = elevation;
  }

  public double getElevation() {
    return elevation;
  }

}
