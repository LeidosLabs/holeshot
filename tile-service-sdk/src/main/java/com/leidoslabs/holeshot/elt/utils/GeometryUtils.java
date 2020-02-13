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
package com.leidoslabs.holeshot.elt.utils;

import java.awt.Point;
import java.awt.geom.Point2D;
import org.apache.commons.math3.util.Precision;
import org.joml.Matrix3x2dc;
import org.joml.Vector2d;
import org.joml.Vector3d;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.GeometryFactory;

public class GeometryUtils {
  private GeometryFactory geometryFactory;

  public GeometryUtils(GeometryFactory geometryFactory) {
    this.geometryFactory = geometryFactory;
  }

  public static boolean equals(Envelope e1, Envelope e2, double epsilon) {
    return Precision.equals(e1.getMinX(), e2.getMinX(), epsilon) &&
        Precision.equals(e1.getMinY(), e2.getMinY(), epsilon) &&
        Precision.equals(e1.getMaxX(), e2.getMaxX(), epsilon) &&
        Precision.equals(e1.getMaxY(), e2.getMaxY(), epsilon);
  }

  //  public double distanceBetween(DrawContext dc, Polygon geodeticPoly, Point point) {
  //    Polygon cartesianPoly =
  //        geometryFactory.createPolygon(Arrays.stream(geodeticPoly.getCoordinates())
  //            .map(c -> computePointFromPosition(c, dc.getGlobe(), dc.getVerticalExaggeration()))
  //            .toArray(Coordinate[]::new));
  //    return point.distance(cartesianPoly);
  //
  //  }
  //

  /**
   * Transform rectangle to quadrilateral  
   * @param rect
   * @param topLeft
   * @param topRight
   * @param bottomLeft
   * @param bottomRight
   * @return
   */
  public static double[] rectToQuad(Envelope rect,
      Coordinate topLeft,
      Coordinate topRight,
      Coordinate bottomLeft,
      Coordinate bottomRight) {
    return rectToQuad(rect, topLeft.x, topLeft.y, topRight.x, topRight.y, bottomLeft.x, bottomLeft.y, bottomRight.x, bottomRight.y);
  }

  public static double[] rectToQuad(Envelope rect,
      double x1a, double y1a,
      double x2a, double y2a,
      double x3a, double y3a,
      double x4a, double y4a) {
    double X = rect.getMinX();
    double Y = rect.getMinY();
//    double Y = rect.getMaxY();
    double W = rect.getWidth();
    double H = rect.getHeight();

    double y21 = y2a - y1a;
    double y32 = y3a - y2a;
    double y43 = y4a - y3a;
    double y14 = y1a - y4a;
    double y31 = y3a - y1a;
    double y42 = y4a - y2a;

    double a = -H*(x2a*x3a*y14 + x2a*x4a*y31 - x1a*x4a*y32 + x1a*x3a*y42);
    double b = W*(x2a*x3a*y14 + x3a*x4a*y21 + x1a*x4a*y32 + x1a*x2a*y43);
    double c = H*X*(x2a*x3a*y14 + x2a*x4a*y31 - x1a*x4a*y32 + x1a*x3a*y42) - H*W*x1a*(x4a*y32 - x3a*y42 + x2a*y43) - W*Y*(x2a*x3a*y14 + x3a*x4a*y21 + x1a*x4a*y32 + x1a*x2a*y43);

    double d = H*(-x4a*y21*y3a + x2a*y1a*y43 - x1a*y2a*y43 - x3a*y1a*y4a + x3a*y2a*y4a);
    double e = W*(x4a*y2a*y31 - x3a*y1a*y42 - x2a*y31*y4a + x1a*y3a*y42);
    double f = -(W*(x4a*(Y*y2a*y31 + H*y1a*y32) - x3a*(H + Y)*y1a*y42 + H*x2a*y1a*y43 + x2a*Y*(y1a - y3a)*y4a + x1a*Y*y3a*(-y2a + y4a)) - H*X*(x4a*y21*y3a - x2a*y1a*y43 + x3a*(y1a - y2a)*y4a + x1a*y2a*(-y3a + y4a)));

    double g = H*(x3a*y21 - x4a*y21 + (-x1a + x2a)*y43);
    double h = W*(-x2a*y31 + x4a*y31 + (x1a - x3a)*y42);
    double i = W*Y*(x2a*y31 - x4a*y31 - x1a*y42 + x3a*y42) + H*(X*(-(x3a*y21) + x4a*y21 + x1a*y43 - x2a*y43) + W*(-(x3a*y2a) + x4a*y2a + x2a*y3a - x4a*y3a - x2a*y4a + x3a*y4a));

    final double kEpsilon = 0.0001;


    if(Math.abs(i) < kEpsilon)
    {
      i = kEpsilon* (i > 0 ? 1.0 : -1.0);
    }
    return new double[] {a/i, d/i, 0, g/i, b/i, e/i, 0, h/i, 0, 0, 1, 0, c/i, f/i, 0, 1.0};
  }

  /**
   * 2D vector -> 3D coordinate with z=0
   * @param vec2D
   * @return
   */
  public static Coordinate toCoordinate(Vector2d vec2D) {
    return new Coordinate(vec2D.x, vec2D.y, 0.0);
  }

  /**
   * 3D vector -> 3D coordinate
   * @param vec3D
   * @return
   */
  public static Coordinate toCoordinate(Vector3d vec3D) {
     return new Coordinate(vec3D.x, vec3D.y, vec3D.z);
   }

  /**
   * Scale envelope evenly across both dimensions
   * @param env
   * @param scalar
   * @return
   */
  public static Envelope multiply(Envelope env, double scalar) {
    return multiply(env, scalar, scalar);
  }

  /**
   * Scale envelope
   * @param env
   * @param scalarX x dimension scaling factor
   * @param scalarY y dimension scaling factor
   * @return
   */
  public static Envelope multiply(Envelope env, double scalarX, double scalarY) {
    Envelope result = new Envelope(env);
    double growX = (env.getWidth() * (scalarX - 1.0)) / 2.0;
    double growY = (env.getHeight() * (scalarY - 1.0)) / 2.0;

    result.expandBy(growX, growY);
    return result;
  }

  /**
   * Translate envelope 
   * @param env
   * @param moveTo translation vector
   * @return
   */
  public static Envelope add(Envelope env,
      Vector3d moveTo) {
    return new Envelope(env.getMinX() + moveTo.x, env.getMaxX() + moveTo.x,
        env.getMinY() + moveTo.y, env.getMaxY() + moveTo.y);
  }

  /**
   * Point -> vector2D
   * @param point
   * @return
   */
  public static Vector2d toVector2d(
      java.awt.Point point) {
    return new Vector2d(point.getX(), point.getY());
  }

  /**
   * Point2D -> vector2D
   * @param point
   * @return
   */
  public static Vector2d toVector2d(
      Point2D point) {
    return new Vector2d(point.getX(), point.getY());
  }

  /**
   * Coordinate -> vector2D
   * @param coord
   * @return
   */
  public static Vector2d toVector2d(Coordinate coord) {
    return new Vector2d(coord.x, coord.y);
  }

  /**
   * Coordinate -> vector3D
   * @param coord
   * @return
   */
  public static Vector3d toVector3d(Coordinate coord) {
    return new Vector3d(coord.x, coord.y, coord.z);
  }

  /**
   * multiply envelope by 3x2 matrix
   * @param envelope
   * @param matrix
   * @return
   */
  public static Envelope multiply(Envelope envelope, Matrix3x2dc matrix) {
    Vector2d ll = new Vector2d(envelope.getMinX(), envelope.getMinY()).mulPosition(matrix);
    Vector2d ur = new Vector2d(envelope.getMaxX(), envelope.getMaxY()).mulPosition(matrix);
    return new Envelope(toCoordinate(ll), toCoordinate(ur));
  }

  /**
   * Coordinte -> Point
   * @param coordinate
   * @return
   */
  public static Point toPoint(Coordinate coordinate) {
    return new Point((int)Math.round(coordinate.x), (int)Math.round(coordinate.y));
  }
  /**
   * Point -> Coordinate
   * @param point
   * @return
   */
  public static Coordinate toCoordinate(Point2D point) {
     return new Coordinate(point.getX(), point.getY(), 0.0);
   }
}
