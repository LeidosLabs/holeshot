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

import org.locationtech.jts.geom.Coordinate;

/**
 * A GeointCoordinate representing a point on Earth
 */
public class GeodeticCoordinate extends GeointCoordinate<Coordinate> {

    /**
     * Create a new Geodetic Coordinate representing a point on a Earth
     * @param coordinate The x: longitude y: latitude coordinate of this point
     */
  public GeodeticCoordinate(Coordinate coordinate) {
    super(coordinate);
  }

    /**
     * Return the Longitude - Latitude coordinate this object was built with
     * @return The source coordinate
     */
  @Override
  public Coordinate getGeodeticCoordinate() {
      return getSourceCoordinate();
  }

}
