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

import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.util.Arrays;

import org.apache.commons.math3.util.Precision;
import org.joml.Matrix3x2dc;
import org.joml.Vector2d;
import org.joml.Vector2dc;
import org.joml.Vector2i;
import org.joml.Vector2ic;
import org.joml.Vector3d;
import org.joml.Vector3dc;
import org.joml.Vector4d;
import org.joml.Vector4dc;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Polygon;

public class GeometryUtils {
	public static final GeometryFactory GEOMETRY_FACTORY = new GeometryFactory();

	public static boolean equals(Envelope e1, Envelope e2, double epsilon) {
		return Precision.equals(e1.getMinX(), e2.getMinX(), epsilon) &&
				Precision.equals(e1.getMinY(), e2.getMinY(), epsilon) &&
				Precision.equals(e1.getMaxX(), e2.getMaxX(), epsilon) &&
				Precision.equals(e1.getMaxY(), e2.getMaxY(), epsilon);
	}

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
	public static Coordinate toCoordinate(Vector2dc vec2D) {
		return new Coordinate(vec2D.x(), vec2D.y(), 0.0);
	}

	public static Coordinate toCoordinate(Vector2ic vec2D) {
		return new Coordinate(vec2D.x(), vec2D.y(), 0.0);
	}
	
	/**
	 * 3D vector -> 3D coordinate
	 * @param vec3D
	 * @return
	 */
	public static Coordinate toCoordinate(Vector3dc vec3D) {
		return new Coordinate(vec3D.x(), vec3D.y(), vec3D.z());
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
			Vector3dc moveTo) {
		return new Envelope(env.getMinX() + moveTo.x(), env.getMaxX() + moveTo.x(),
				env.getMinY() + moveTo.y(), env.getMaxY() + moveTo.y());
	}

	/**
	 * Point -> vector2D
	 * @param point
	 * @return
	 */
	public static Vector2dc toVector2d(
			java.awt.Point point) {
		return new Vector2d(point.getX(), point.getY());
	}

	/**
	 * Point2D -> vector2D
	 * @param point
	 * @return
	 */
	public static Vector2dc toVector2d(
			Point2D point) {
		return new Vector2d(point.getX(), point.getY());
	}

	/**
	 * Coordinate -> vector2D
	 * @param coord
	 * @return
	 */
	public static Vector2dc toVector2d(Coordinate coord) {
		return new Vector2d(coord.x, coord.y);
	}

	public static Vector2ic toVector2i(Point point) {
		return new Vector2i(point.x, point.y);
	}

	/**
	 * Coordinate -> vector3D
	 * @param coord
	 * @return
	 */
	public static Vector3dc toVector3d(Coordinate coord) {
		return new Vector3d(coord.getX(), coord.getY(), coord.getZ());
	}

	/**
	 * multiply envelope by 3x2 matrix
	 * @param envelope
	 * @param matrix
	 * @return
	 */
	public static Envelope multiply(Envelope envelope, Matrix3x2dc matrix) {
		Vector2dc ll = new Vector2d(envelope.getMinX(), envelope.getMinY()).mulPosition(matrix);
		Vector2dc ur = new Vector2d(envelope.getMaxX(), envelope.getMaxY()).mulPosition(matrix);
		return new Envelope(toCoordinate(ll), toCoordinate(ur));
	}

	/**
	 * Coordinate -> Point
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

	public static Vector4dc toVector4d(Coordinate point) {
		return new Vector4d(point.getX(), point.getY(), point.getZ(), 1.0);
	}
	public static Coordinate toCoordinate(Vector4dc vector) {
		return new Coordinate(vector.x(), vector.y(), vector.z());
	}
	public static Vector3d toVector3d(Vector4dc vector) {
		return new Vector3d(vector.x(), vector.y(), vector.z());
	}
	public static Coordinate toCoordinate(org.locationtech.jts.geom.Point point) {
		return new Coordinate(point.getX(), point.getY(), 0.0);
	}

	public static Point2D toPoint2D(Vector2dc vec) {
		return new Point2D.Double(vec.x(), vec.y());
	}
	public static Point2D toPoint2D(Coordinate c) {
		return new Point2D.Double(c.x, c.y);
	}
	public static Dimension toDimension(Vector2dc vec) {
		return new Dimension((int)Math.round(vec.x()), (int)Math.round(vec.y()));
	}
	public static Dimension toDimension(Envelope envelope) {
		return new Dimension((int)Math.round(envelope.getWidth()), (int)Math.round(envelope.getHeight()));
	}
	public static Dimension toDimension(Rectangle rect) {
		return new Dimension(rect.width, rect.height);
	}
	public static Dimension toDimension(Vector2ic vec) {
		return new Dimension(vec.x(), vec.y());
	}
	public static Polygon toPolygon(Envelope env) {
		return GEOMETRY_FACTORY.createPolygon( Arrays.stream(new double[][] {
			{ env.getMinX(), env.getMinY() },
			{ env.getMaxX(), env.getMinY() },
			{ env.getMaxX(), env.getMaxY() },
			{ env.getMinX(), env.getMaxY() },
			{ env.getMinX(), env.getMinY() }}).map(c->new Coordinate(c[0], c[1],0.0)).toArray(Coordinate[]::new));
	}

	public static Vector2dc toVector2d(Rectangle rect) {
		return new Vector2d(rect.getWidth(), rect.getHeight());
	}
	public static Vector2dc toVector2d(Dimension size) {
		return new Vector2d(size.getWidth(), size.getHeight());
	}
}
