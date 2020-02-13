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
 * Class containing static utlity methods for dealing with datums (i.e. conversions, etc.).
 *
 * Abbreviations:
 * <ol>
 * <li> GRS = Geocentric Reference System
 * </ol>
 * <p>
 *
 * Note this class was based on datum-conversion.c and earth-constants.h files
 * in the TargetJr Consortium at GE-CRD in 1997.
 */
public class DatumUtil {

  /**
   * GRS-80 major axis in meters, the equatorial radius of the Earth
   */
  public final static double GRS80_A = 6378137.0;
  /**
   * GRS-80 minor axis in meters, the semiminor axis of the elipsoid
   */
  public final static double GRS80_B = 6356752.3141;
  /**
   * Description of the Field
   */
  public final static double GRS80_E = 0.08181921805;

  /**
   * Convert a Geodetic latitude value into a Geocentric latitude value
   *
   * @param geodeticLat in radians
   * @param axisA       Major axis of earth in meters
   * @param axisB       Minor axis of earth in meters
   * @return Description of the Return Value
   */
  private static double geodeticToGeocentric(double geodeticLat, double axisA, double axisB) {
    return Math.atan((axisB / axisA) * (axisB / axisA) * Math.tan(geodeticLat));
  }

  /**
   * Converts a Coordinate in a GEODETIC coordinate system to a Coordinate in
   * GRS coordinates.
   *
   * Geodetic coordinates specify a location on the Earth's oblate
   * (non-spherical) surface. Geodetic latitude is defined as the angle between
   * the equatorial plane and a line normal to the surface at that location.
   * Geodetic longitude is the angular distance between the location's meridian
   * and the Greenwich meridian.
   *
   * Geocentric coordinates relate to a reference system where the origin is the
   * center of the Earth. Geocentric latitude is defined by the angle between
   * the equatorial plane and a line from the local position to the intersection
   * of the axis of rotation with the equatorial plane. Geodetic longitude and
   * geocentric longitude are the same because they share the same reference
   * meridian and axis.
   *
   * An alternate way of doing this computation would be:
   * <ol>
   * <li> N = A/sqrt(1 - e*e*sin(lat)*sin(lat))
   * <li> x = (N + el)cos(lat)cos(lon)
   * <li> y = (N + el)cos(lat)sin(lon)
   * <li> getElevation = (N(1-e*e) + el) sin(lat)\
   * </ol>
   *
   * @param geodeticPoint Lat/Long in radians and elev in meters
   * @param axisA         Major axis of earth in meters
   * @param axisB         Minor axis of earth in meters
   * @return Coordinate X/Y/Z in meters
   */
  public static Coordinate geodeticToGeocentric(Coordinate geodeticPoint, double axisA, double axisB) {

    double geocentricLat = geodeticToGeocentric(geodeticPoint.y, axisA, axisB);

    // Compute the sin and cos of the latitude
    double s = Math.sin(geocentricLat);
    double c = Math.cos(geocentricLat);

    // Compute the distance to the centre of the earth
    double localRadius = (axisA * axisB) / Math.sqrt(axisB * axisB * c * c + axisA * axisA * s * s);

    return new Coordinate(
        (localRadius * c + geodeticPoint.z * Math.cos(geodeticPoint.y)) * Math.cos(geodeticPoint.x),
        (localRadius * c + geodeticPoint.z * Math.cos(geodeticPoint.y)) * Math.sin(geodeticPoint.x),
        (localRadius * s + geodeticPoint.z * Math.sin(geodeticPoint.y)));
  }
}

