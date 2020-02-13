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
