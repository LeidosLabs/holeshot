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

package com.leidoslabs.holeshot.imaging.photogrammetry;

import org.locationtech.jts.geom.Coordinate;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Created by parrise on 2/23/17.
 */
public class LocalCartesianCoordinateSystemTest {


  @Test
  public void localToGlobal_nominal() throws Exception {

    LocalCartesianCoordinateSystem lccs = new LocalCartesianCoordinateSystem(
        new Coordinate(-0.130491,51.510397,0)
    );

    Coordinate offsetNorth = lccs.localToGlobal(new Coordinate(0,5000,5));
    assertEquals(-0.130491,offsetNorth.x,1E-6);
    assertEquals(51.5553375,offsetNorth.y,1E-6);
    assertEquals(5.0, offsetNorth.z,1E-6);

    Coordinate globalEast = lccs.localToGlobal(new Coordinate(5000, 0, -10));
    assertEquals(-0.0584705,globalEast.x,1E-6);
    assertEquals(51.510397,globalEast.y,1E-6);
    assertEquals(-10.0, globalEast.z,1E-6);

  }

  @Test
  public void globalToLocal_nominal() throws Exception {

    LocalCartesianCoordinateSystem lccs = new LocalCartesianCoordinateSystem(
        new Coordinate(-0.130491,51.510397,0)
    );

    Coordinate localNorth = lccs.globalToLocal(new Coordinate(-0.130491,51.5553375,5));
    assertEquals(0,localNorth.x,1E-1);
    assertEquals(5000,localNorth.y,1E-1);
    assertEquals(5.0, localNorth.z,1E-1);

    Coordinate localEast = lccs.globalToLocal(new Coordinate(-0.0584705, 51.510397, -10));
    assertEquals(5000,localEast.x,1E-1);
    assertEquals(0,localEast.y,1E-1);
    assertEquals(-10.0, localEast.z,1E-1);
  }
}
