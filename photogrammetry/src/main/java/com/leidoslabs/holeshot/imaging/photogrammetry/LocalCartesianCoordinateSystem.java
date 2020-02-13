package com.leidoslabs.holeshot.imaging.photogrammetry;

import org.locationtech.jts.geom.Coordinate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class provides conversion routines for coordinates in a local 3D Cartesian coordinate system
 * anchored at a specific point on the WGS84 datum. The X axis is assumed to be pointing East, the
 * Y axis points North and the Z axis corresponds to elevation.
 *
 * Note this class was based on lvcs.c in the TargetJr Consortium at GE-CRD, 1997.
 *
 * Created by parrise on 2/22/17.
 */
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
public class LocalCartesianCoordinateSystem {

  private static final Logger LOGGER = LoggerFactory.getLogger(LocalCartesianCoordinateSystem.class);

  private Coordinate originGeodeticRadians;
  private double latScale;
  private double lonScale;
  private double elevScale;

  /**
   * This method constructs a cartesian coordinate system that is located at a specific point on
   * the WGS84 datum. The origin coordinate in assumed to be in decimal degrees of longitude
   * and latitude with an elevation from the datum expressed in meters.
   *
   * @param originGeodeticDegrees origin of this coordinate system
   */
  public LocalCartesianCoordinateSystem(Coordinate originGeodeticDegrees) {

    if (Math.abs(originGeodeticDegrees.x) > 360.0 || Math.abs(originGeodeticDegrees.y) > 90.0) {
      LOGGER.error("Invalid value for Cartesian coordinate system origin: {}. Valid ranges are [-360.0,360.0] and [-90.0,90.0].", originGeodeticDegrees);
      throw new IllegalArgumentException("Invalid value for Cartesian coordinate system origin: " + originGeodeticDegrees);
    }

    this.originGeodeticRadians = new Coordinate(
        Math.toRadians(originGeodeticDegrees.x),
        Math.toRadians(originGeodeticDegrees.y),
        originGeodeticDegrees.z);

    computeScaleValues();
  }

  /**
   * This method converts a local Cartesian point into a geodetic point on the WGS84 datum.
   *
   * @param localCoord the local cartesian point (x,y,z) in meters
   * @return the geodetic point (lon, lat, elev) in degrees and meters
   */
  public Coordinate localToGlobal(Coordinate localCoord) {

    return new Coordinate(
        Math.toDegrees(localCoord.x * this.lonScale + this.originGeodeticRadians.x),
        Math.toDegrees(localCoord.y * this.latScale + this.originGeodeticRadians.y),
        localCoord.z * this.elevScale + this.originGeodeticRadians.z
    );
  }

  /**
   * This method converts a geodetic point on the WGS84 datum to the local Carteisan coordinate
   * system.
   *
   * @param globalCoord the geodetic point (lon, lat, elev) in degrees and meters
   * @return the local cartesian point (x,y,z) in meters
   */
  public Coordinate globalToLocal(Coordinate globalCoord) {

    return new Coordinate(
        (Math.toRadians(globalCoord.x) - this.originGeodeticRadians.x) / this.lonScale,
        (Math.toRadians(globalCoord.y) - this.originGeodeticRadians.y) / this.latScale,
        (globalCoord.z - this.originGeodeticRadians.z) / this.elevScale
    );
  }

  /**
   * Generate the scale values based on the geodetic origin.
   *
   * Units are radians and meters. LatScale and LongScale are in radians/meter.
   *
   * This method assumes we are in WGS84 and uses its relationship to GRS80 to
   * determine the angular-to-length scales.
   */
  private void computeScaleValues() {
    final double smallStep = 1.0e-6; // assumed to be in radians

    Coordinate grsOrigin =
        DatumUtil.geodeticToGeocentric(originGeodeticRadians, DatumUtil.GRS80_A, DatumUtil.GRS80_B);

    Coordinate geodeticOffsetLat =
        new Coordinate(originGeodeticRadians.x, originGeodeticRadians.y + smallStep, originGeodeticRadians.z);

    Coordinate grsOffsetLat =
        DatumUtil.geodeticToGeocentric(geodeticOffsetLat, DatumUtil.GRS80_A, DatumUtil.GRS80_B);

    // latScale in radians/meter
    latScale =
        smallStep / Math.sqrt(
            Math.pow((grsOrigin.x - grsOffsetLat.x), 2)
                + Math.pow((grsOrigin.y - grsOffsetLat.y), 2)
                + Math.pow((grsOrigin.z - grsOffsetLat.z), 2));

    Coordinate geodeticOffsetLon =
        new Coordinate(originGeodeticRadians.x + smallStep, originGeodeticRadians.y, originGeodeticRadians.z);

    Coordinate grsOffsetLon =
        DatumUtil.geodeticToGeocentric(geodeticOffsetLon, DatumUtil.GRS80_A, DatumUtil.GRS80_B);

    // lonScale in radians/meter
    lonScale =
        smallStep / Math.sqrt(
            Math.pow((grsOrigin.x - grsOffsetLon.x), 2)
                + Math.pow((grsOrigin.y - grsOffsetLon.y), 2)
                + Math.pow((grsOrigin.z - grsOffsetLon.z), 2));

    elevScale = 1.0; // default
  }

}
