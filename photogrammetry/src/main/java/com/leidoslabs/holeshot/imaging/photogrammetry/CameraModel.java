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

import java.awt.geom.Point2D;
import org.image.common.cache.Cacheable;
import org.locationtech.jts.geom.Coordinate;

/**
 * Abstract class that defines the camera model
 * A camera model is a transformation between (pixel, scan) <=> (lat, lon , height) for a given satellite image
 */
public abstract class CameraModel implements java.io.Serializable, Cacheable {

  private static double SQRT_OF_TWO = Math.sqrt(2.0);

  /**
   * Map world/geo point to image point. The world/geo point is often a WGS84
   * lat/long/elevation point (units of decimal degrees and meters), but please
   * note that the units can vary depending on a particular CameraModel
   * implementation (e.g. a particular CameraModel might require MGRS coordinate
   * units instead).
   *
   * This version of the worldToImage method is useful if you already have a
   * Point2D allocated. For efficiency this can be helpful.
   *
   * @param worldPoint source world point to convert to an image space point
   * @param imagePoint destination image point; must already be allocated.
   * @return returns the reference to the result image point
   */
  public abstract Point2D worldToImage(Coordinate worldPoint, Point2D imagePoint);

  /**
   * Map world/geo point to image point The world/geo point is often a WGS84
   * lat/long/elevation point (units of decimal degrees and meters), but please
   * note that the units can vary depending on a particular CameraModel
   * implementation (e.g. a particular CameraModel might require MGRS coordinate
   * units instead).
   *
   * @param worldPoint source world point to convert to an image space point
   * @return destination image space point
   */
  public Point2D worldToImage(Coordinate worldPoint) {
    Point2D result = new Point2D.Double();
    return worldToImage(worldPoint, result);
  }

  /**
   * Map image point to surface in world.
   *
   * This version of the imageToWorld method is useful if you already have a
   * Coordinate allocated. For efficiency this can be helpful.
   *
   * @param imagePoint source image point to convert to a world space point
   * @param surf       world surface which can be null! If it is null, a default surface within the
   *                   camera model will be used ( the default surface is not necessarily zero, but
   *                   could be an average height of the overall image).
   * @param worldPoint destination world space point; must already be allocated
   * @return destination world space point
   */
  public abstract Coordinate imageToWorld(Point2D imagePoint, Surface surf, Coordinate worldPoint);

  /**
   * Map image point to surface in world.
   *
   * @param imagePoint source image point to convert to a world space point
   * @param surf       world surface which can be null! If it is null, a default surface within the
   *                   camera model will be used ( the default surface is not necessarily zero, but
   *                   could be an average height of the overall image).
   * @return destination world space point
   */
  public Coordinate imageToWorld(Point2D imagePoint, Surface surf) {
    Coordinate result = new Coordinate();
    return imageToWorld(imagePoint, surf, result);
  }

  /**
   * Map image point to a Default Surface in world (i.e. a ConstantElevationSurface
   * instance), as defined by the getDefaultSurface() method.
   *
   * This version of the worldToImage method is useful if you already have a
   * Point2D allocated. For efficiency this can be helpful.
   *
   * @param imagePoint source image point to convert to a world space point
   * @param worldPoint destination world point; must already be allocated
   * @return destination world space point
   */
  public Coordinate imageToWorld(Point2D imagePoint, Coordinate worldPoint) {
    worldPoint.setCoordinate(imageToWorld(imagePoint, getDefaultSurface(), worldPoint));
    return worldPoint;
  }

  /**
   * Map image point to a Default Surface in world (i.e. a ConstantElevationSurface
   * instance), as defined by the getDefaultSurface() method.
   *
   * @param imagePoint source image point to convert to a world space point
   * @return destination world space point
   */
  public Coordinate imageToWorld(Point2D imagePoint) {
    Coordinate result = new Coordinate();
    return imageToWorld(imagePoint, result);
  }

  /**
   * Retrieves the default (or average) elevation, if one is known.
   *
   * @return null if no default elevation is known.
   */
  public abstract double getDefaultElevation();

  /**
   * Sets the default (or average) elevation
   *
   * @param elevation The value to set the default elevation to
   */
  public abstract void setDefaultElevation(double elevation);

  /**
   * Determine a reference point that is within the bounds of the world space.
   *
   * @return a reference point that is presumed to be within the bounds of the image
   */
  public abstract Coordinate getReferencePoint();

  /**
   * This up-is-up function calculates the angle by which the image needs to be
   * rotated to make vertical objects (such as buildings) stand right side up.
   * It outputs the angle in degrees by which the image should be rotated
   * clockwise.
   *
   * It works by mapping two points into the image. The first point is the base
   * of an imaginary building, and the second point is the top of the imaginary
   * building. Given these two points in the image, it is easy to rotate the
   * image to its correct up-is-up orientation. Just draw an arrow from the
   * first point (the base of the building) to the second point (the top.) Then
   * rotate the image until the arrow points vertically upward. The most
   * difficult part of the algorithm is calculating correctly the angle between
   * this arrow and the vertical.
   *
   * The step-by-step operation of the algorithm, which one can follow in the
   * code, is as follows. Explanatory comments are interspersed with these
   * steps.
   *
   * Step 1. Define a geographical point to be the base of the imaginary
   * building. This will be a point at the nominal center of the image.
   *
   * Step 2. Define a geographical point to be the top of the building. This
   * will be at an elevation constant value above the basePoint.
   *
   * Note that the line from the first point to the second point is a vertical
   * line in 3-D space. It represents a vertical edge of the building.
   *
   * Step 3. Use the RPC coefficients to project both of these points into the
   * image. The resulting image coordinates are (samp_1, row_1) and (samp_2,
   * row_2), respectively.
   *
   * Step 4. Define the vector (or arrow) in the image that defines the edge of
   * the building. Denoting this vector (x,y), it is calculated by
   * <ul>
   * <li> x = samp_2 - samp_1
   * <li> y = row_2 - row_1
   * </ul>
   * <p>
   * Remember that the vector (x, y) represents the vertical edge of the
   * building. In other words, it is the "up" vector. It is now necessary to
   * calculate how far this vector must be rotated clockwise in order to make it
   * point straight upwards. In other words, the angle between this vector (x,
   * y) and the vector (0, -1), measured in the clockwise direction, must be
   * calculated. The vector (0, -1) points directly upwards because images are
   * displayed according to the standard image processing convention of the
   * x-coordinate increasing to the right and the y-coordinate increasing
   * downwards (instead of upwards as one learns in high school geometry.)
   *
   * Step 5. Normalize the vector (x, y) by dividing x and y by r, where r is
   * the length of the vector. That is, r is the square root of x-squared plus
   * y-squared. This normalization does not change the direction in which the
   * vector (x, y) points. It just scales its length to 1. It is necessary for
   * the trigonometry that follows.
   *
   * Step 6. Let theta be the arc cosine of y. This is the standard
   * trigonometric method for finding the angle between the vector (x, y) and
   * the vector that points down. (Down, because of the image convention that
   * the y-coordinate increases in the downwards direction.) Thus, theta is the
   * angle between the vector (x, y) and the "down" vector. (Once we rotate it
   * to this down position, a second rotation of 180 degrees will point it
   * straight up.)
   *
   * Trigonometric functions in C, Java, and other programming languages measure
   * angles in units of radians instead of degrees. We convert these angles to
   * degrees.
   *
   * If the "up" vector in the image points to the right, then the angle theta
   * represents a clockwise rotation. However, if the "up" vector points to the
   * left, then the angle represents a counterclockwise rotation. In the former
   * case, x is positive, while in the latter case, x is negative. Thus, to
   * rotate the "up" vector (x, y) so that it points straight up, we must rotate
   * it by an angle of 180 + theta degrees if x is positive. (The addition of
   * 180 rotates it from pointing downwards to pointing upwards.) If, however, x
   * is negative, then we must rotate it by an angle of 180 - theta degrees. The
   * negative sign in front of theta turns the clockwise rotation into a
   * counterclockwise rotation, and the addition of 180 degrees rotates the
   * vector from pointing straight down to pointing straight up. This defines
   * the last step of the algorithm, which is...
   *
   * Step 7. If x is positive, then replace theta with 180 + theta. Otherwise,
   * replace it with 180 - theta.
   *
   * Theta is the angle in degrees by which the image must be rotated in a
   * clockwise direction to make vertical objects stand straight up.
   *
   * This algorithm was originally written by Mark Pritt in C.
   *
   * @return the angle in degrees by which the image should be rotated clockwise
   */
  public double getUpIsUp() {
    double r = 0.0;
    double x = 0.0;
    double y = 0.0;
    double theta = 0.0;
    double epsilon = 1.0e-10;
    final double elevationVectorLength = 1000;

    Coordinate baseWorldPoint = getReferencePoint();
    Coordinate topWorldPoint = new Coordinate(baseWorldPoint.x,
        baseWorldPoint.y,
        baseWorldPoint.z + elevationVectorLength);
    Point2D baseImagePoint = worldToImage(baseWorldPoint);
    Point2D topImagePoint = worldToImage(topWorldPoint);

    x = topImagePoint.getX() - baseImagePoint.getX();
    y = topImagePoint.getY() - baseImagePoint.getY();

    r = baseImagePoint.distance(topImagePoint);

    // Check for nadir image (one where we are looking directly down
    // on a building)
    if (r < epsilon) {
      // We don't want to rotate a nadir image
      return 0;
    }
    x = x / r;
    y = y / r;

    /* find up_is_up rotation angle */

    theta = Math.toDegrees(Math.acos(y));
    if (x > 0) {
      theta = 180.0 + theta;
    } else {
      theta = 180.0 - theta;
    }

    return theta;
  }

  /**
   * This north-is-up function calculates the angle by which the image needs to be
   * rotated to make vertical objects (such as buildings) stand so the north
   * angle side is up. It outputs the angle in degrees by which the image should
   * be rotated clockwise.
   *
   * This is based on the same method as in getUpIsUp.
   *
   * @return the angle in degrees by which the image should be rotated clockwise
   */
  public double getNorthIsUp() {
    double r = 0.0, x = 0.0, y = 0.0, theta = 0.0;
    double epsilon = 1.0e-10;
    final double elevationVectorLength = 0.01;

    Coordinate baseWorldPoint = getReferencePoint();
    Coordinate northWorldPoint = new Coordinate(baseWorldPoint.x,
        baseWorldPoint.y + elevationVectorLength,
        baseWorldPoint.z);
    Point2D baseImagePoint = worldToImage(baseWorldPoint);
    Point2D northImagePoint = worldToImage(northWorldPoint);

    x = northImagePoint.getX() - baseImagePoint.getX();
    y = northImagePoint.getY() - baseImagePoint.getY();

    r = baseImagePoint.distance(northImagePoint);

    // Check for nadir image (one where we are looking directly down
    // on a building)
    if (r < epsilon) {
      // We don't want to rotate a nadir image
      return 0;
    }
    x = x / r;
    y = y / r;

    /* find north_is_up rotation angle */

    theta = Math.toDegrees(Math.acos(y));
    if (x > 0) {
      theta = 180.0 + theta;
    } else {
      theta = 180.0 - theta;
    }

    return theta;
  }

  /**
   * Determine the distance in pixels between two world space points
   *
   * @param pointA The first world space point (in lat/long/elev)
   * @param pointB The second world space point (in lat/long/elev)
   * @return A double value representing the distance in pixels between the two points.
   */
  public double getDistanceInPixels(Coordinate pointA, Coordinate pointB) {
    Point2D pointAImage = worldToImage(pointA);
    Point2D pointBImage = worldToImage(pointB);

    // determine distance between pixels in image space
    return Math.sqrt(((pointBImage.getX() - pointAImage.getX()) * (pointBImage.getX() - pointAImage.getX()))
        + ((pointBImage.getY() - pointAImage.getY()) * (pointBImage.getY() - pointAImage.getY())));
  }

  /**
   * Determine the distance in meters between two world space points
   *
   * @param pointA The first world space point (in lat/long/elev)
   * @param pointB The second world space point (in lat/long/elev)
   * @return A double value representing the distance in meters between the two points.
   */
  public double getDistanceInMeters(Coordinate pointA, Coordinate pointB) {

    LocalCartesianCoordinateSystem crs = new LocalCartesianCoordinateSystem(pointA);
    Coordinate pointBLocal = crs.globalToLocal(pointB);

    // compute distance between second point and origin (pointA). The result will be in meters.
    return Math.sqrt((pointBLocal.x * pointBLocal.x)
        + (pointBLocal.y * pointBLocal.y)
        + (pointBLocal.z * pointBLocal.z));
  }

  /**
   * Determine the distance in meters between two image space points
   *
   * @param pointA The first image space point (in pixels)
   * @param pointB The second image space point (in pixels)
   * @return A double value representing the distance in meters between the two points.
   */
  public double getDistanceInMeters(Point2D pointA, Point2D pointB) {
    return getDistanceInMeters(imageToWorld(pointA), imageToWorld(pointB));
  }

  /**
   * This GSD function calculate the Ground Sample Distance of an image in the
   * sample direction (X coordinate).
   *
   * @param startPoint The location to determine the GSD at
   * @return A double value representing the GSD as meters per pixel
   */
  public double getGSDSample(Point2D startPoint) {
    return getDistanceInMeters(startPoint, new Point2D.Double(startPoint.getX() + 100.0, startPoint.getY())) / 100.0;
  }

  /**
   * This GSD function calculate the Ground Sample Distance of an image in the
   * line direction (Y coordinate).
   *
   * @param startPoint The location to determine the GSD at
   * @return A double value representing the GSD as meters per pixel
   */
  public double getGSDLine(Point2D startPoint) {
    return getDistanceInMeters(startPoint, new Point2D.Double(startPoint.getX(), startPoint.getY() + 100.0)) / 100.0;
  }

  /**
   * This GSD function calculate the Ground Sample Distance of an image. This
   * method will compute an average value based on the diagonal of a pixel using
   * the getGSDSample() and getGSDLine() methods.
   *
   * @param startPoint The location to determine the GSD at
   * @return A double value representing the GSD as meters per pixel
   */
  public double getGSD(Point2D startPoint) {
    double gsdx = getGSDSample(startPoint);
    double gsdy = getGSDLine(startPoint);
    // divide by the square root of 2 because that is the length of the diagonal
    return Math.sqrt(gsdx * gsdx + gsdy * gsdy) / SQRT_OF_TWO;
  }

  /**
   * getDefaultSurface
   *
   * @return A constant elevation surface at the normalization height offset elevation.
   */
  protected Surface getDefaultSurface() {
    return new ConstantElevationSurface(getDefaultElevation());
  }

  /**
   * based on http://www.geom.uiuc.edu/docs/reference/CRC-formulas/node23.html
   * for the area of a general quadrilateral
   *
   * @param width  image width in pixels
   * @param height image height in pixels
   * @return the area of the image in square km
   */
  public double getImageAreaInSquareKM(int width, int height) {

    // get world corners
    Coordinate ul = imageToWorld(new Point2D.Double(0.0, 0.0));
    Coordinate ur = imageToWorld(new Point2D.Double((double) (width - 1), 0.0));
    Coordinate lr = imageToWorld(new Point2D.Double((double) (width - 1), (double) (height - 1)));
    Coordinate ll = imageToWorld(new Point2D.Double(0.0, (double) (height - 1)));

    // get distances of sides and diagonals in meters
    double top = getDistanceInMeters(ul, ur);
    double bot = getDistanceInMeters(ll, lr);
    double left = getDistanceInMeters(ul, ll);
    double right = getDistanceInMeters(ur, lr);
    double diag1 = getDistanceInMeters(ul, lr);
    double diag2 = getDistanceInMeters(ur, ll);

    // calc area in square meters
    double factor1 = 4.0 * diag1 * diag1 * diag2 * diag2;
    double factor2 = left * left + right * right - top * top - bot * bot;
    double areaInSqMeters = 0.25 * Math.sqrt(factor1 - (factor2 * factor2));

    return areaInSqMeters / 1000000.;
  }
  
  @Override
  public long getSizeInBytes() {
    // TODO Auto-generated method stub
    return 0;
  }

}
