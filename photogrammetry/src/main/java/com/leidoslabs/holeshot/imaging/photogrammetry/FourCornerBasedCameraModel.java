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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.leidoslabs.holeshot.imaging.photogrammetry.warp.PolyWarp;
import com.leidoslabs.holeshot.imaging.photogrammetry.warp.TiePointPair;
import com.leidoslabs.holeshot.imaging.photogrammetry.warp.WarpCoefficientSet;
import org.locationtech.jts.geom.Coordinate;


/**
 * This CameraModel is derived from the FourCornerLLCS. It provides
 * a CameraModel interface to the warp computed from a set of four
 * lat/long corners and their corresponding image corners.
 *
 * @author D. Crehan
 * @author Mike Nidel
 * @version $Revision: 1.3 $ $Date: 2006/01/09 17:20:00 $
 *
 */
public class FourCornerBasedCameraModel
  extends CameraModel
{
  private final static int NUM_CORNERS = 4;

  private double myDefaultElevation = 0;

  private WarpCoefficientSet myCoeffsFromLatLongToSource;
  private WarpCoefficientSet myCoeffsFromSourceToLatLong;

  private static Logger ourLogger = LoggerFactory.getLogger(FourCornerBasedCameraModel.class);

  /**
   * Constructor.
   *
   * @param imageWidth the width of the image
   * @param imageHeight the height of the image
   * @param worldCorners an array of Coordinate representing the
   *   lat/long location of each corner of the image, starting with the
   *   origin (upper-left) in source space and continuing clockwise
   * @param origin The origin of this rectangle
   */
  public FourCornerBasedCameraModel(int imageWidth,
                                    int imageHeight,
                                    Coordinate[] worldCorners,
                                    Point2D origin)
  {
    ourLogger.trace("FourCornerBasedCameraModel.ctor");
    TiePointPair[] tpp = new TiePointPair[NUM_CORNERS];

                                // get coefficients from source to lat/long
    tpp[0] = new TiePointPair(
      origin,
      new Point2D.Double(worldCorners[0].x, worldCorners[0].y)
      );
    tpp[1] = new TiePointPair(
      new Point2D.Double(origin.getX() + (double)imageWidth,
                         origin.getY()),
      new Point2D.Double(worldCorners[1].x, worldCorners[1].y)
      );
    tpp[2] = new TiePointPair(
      new Point2D.Double(origin.getX() + (double)imageWidth,
                         origin.getY() + (double)imageHeight),
      new Point2D.Double(worldCorners[2].x, worldCorners[2].y)
      );
    tpp[3] = new TiePointPair(
      new Point2D.Double(origin.getX(),
                         origin.getY() + (double)imageHeight),
      new Point2D.Double(worldCorners[3].x, worldCorners[3].y)
      );

    myCoeffsFromSourceToLatLong = PolyWarp.polyCoeff(-1, tpp);


                                // get coefficients from lat/long to source
    tpp[0] = new TiePointPair(
      new Point2D.Double(worldCorners[0].x, worldCorners[0].y),
      origin
      );
    tpp[1] = new TiePointPair(
      new Point2D.Double(worldCorners[1].x, worldCorners[1].y),
      new Point2D.Double(origin.getX() + (double)imageWidth,
                         origin.getY() )
      );
    tpp[2] = new TiePointPair(
      new Point2D.Double(worldCorners[2].x, worldCorners[2].y),
      new Point2D.Double(origin.getX() + (double)imageWidth,
                         origin.getY() + (double)imageHeight)
      );
    tpp[3] = new TiePointPair(
      new Point2D.Double(worldCorners[3].x, worldCorners[3].y),
      new Point2D.Double(origin.getX(),
                         origin.getY() + (double)imageHeight)
      );

    myCoeffsFromLatLongToSource = PolyWarp.polyCoeff(-1, tpp);

  } // end FourCornerBasedCameraModel()


  /**************************************************************
   * {@inheritDoc}
   **************************************************************/
  public Coordinate imageToWorld(Point2D imagePoint, Surface surf, Coordinate worldPoint)
  {
    if (surf == null)
      surf = getDefaultSurface();

    double sample = imagePoint.getX();
    double line = imagePoint.getY();

    double lon = PolyWarp.polyEval(
      myCoeffsFromSourceToLatLong.getACoeffs(),
      myCoeffsFromSourceToLatLong.getDegree(),
      sample,
      line);
    double lat = PolyWarp.polyEval(
      myCoeffsFromSourceToLatLong.getBCoeffs(),
      myCoeffsFromSourceToLatLong.getDegree(),
      sample,
      line);

    return new Coordinate(lon, lat, surf.getElevation(lon, lat));

  } // end imageToWorld()


  /**************************************************************
   * {@inheritDoc}
   **************************************************************/
  public Point2D worldToImage(Coordinate worldPoint, Point2D imagePoint)
  {
    double lon = worldPoint.x;
    double lat = worldPoint.y;

    double sample = PolyWarp.polyEval(
      myCoeffsFromLatLongToSource.getACoeffs(),
      myCoeffsFromLatLongToSource.getDegree(),
      lon,
      lat);
    double line = PolyWarp.polyEval(
      myCoeffsFromLatLongToSource.getBCoeffs(),
      myCoeffsFromLatLongToSource.getDegree(),
      lon,
      lat);

    return new Point2D.Double(sample, line);

  } // end worldToImage()


  /**************************************************************
   * {@inheritDoc}
   **************************************************************/
  public double getDefaultElevation() {
    return myDefaultElevation;
  }


  /**************************************************************
   * {@inheritDoc}
   **************************************************************/
  public void setDefaultElevation(double elevation) {
    myDefaultElevation = elevation;
  }


  /**************************************************************
   * {@inheritDoc}
   **************************************************************/
  public Coordinate getReferencePoint() {
    // assume 0,0 on the image will be in the image
    Point2D imagePoint = new Point2D.Double(0.0, 0.0);
    return imageToWorld(imagePoint);
  }

} // end FourCornerBasedCameraModel
