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
package com.leidoslabs.holeshot.imaging.photogrammetry.rpc;

import java.awt.geom.Point2D;
import org.apache.commons.math3.optim.InitialGuess;
import org.apache.commons.math3.optim.MaxEval;
import org.apache.commons.math3.optim.PointValuePair;
import org.apache.commons.math3.optim.nonlinear.scalar.GoalType;
import org.apache.commons.math3.optim.nonlinear.scalar.ObjectiveFunction;
import org.apache.commons.math3.optim.nonlinear.scalar.noderiv.NelderMeadSimplex;
import org.apache.commons.math3.optim.nonlinear.scalar.noderiv.SimplexOptimizer;
import org.image.common.cache.CacheableUtil;
import com.leidoslabs.holeshot.imaging.photogrammetry.CameraModel;
import com.leidoslabs.holeshot.imaging.photogrammetry.Surface;
import org.locationtech.jts.geom.Coordinate;

/**
 * Implementation of CameraModel that uses a Rational Polynomial Coefficient model to perform transformations
 * between the image and world coordinates
 * Created by parrise on 2/22/17.
 */
public class RPCCameraModel extends CameraModel {

  private final RationalCameraPolynomial rpcSample;
  private final RationalCameraPolynomial rpcLine;
  private final NormalizationCoefficients normalization;
  private final ErrorEstimate error;

    /**
     * Create a null camera model without setting any coefficients
     */
  public RPCCameraModel() {
     this.rpcSample = new RationalCameraPolynomial();
     this.rpcLine = new RationalCameraPolynomial();
     this.normalization = new NormalizationCoefficients();
     this.error = new ErrorEstimate();
  }

    /**
     * Create a camera model with the given polynomial coefficients
     * @param rpcSample The polynomial ratio that is solved to determine sample coordinate mapping for the image
     * @param rpcLine The polynomial ratio that is solved to determine line coordinate mapping for the image
     * @param normalization The normalization coefficients from the camera model
     * @param error The error estimates from the camera model
     */
  public RPCCameraModel(RationalCameraPolynomial rpcSample, RationalCameraPolynomial rpcLine, NormalizationCoefficients normalization, ErrorEstimate error) {
    this.rpcSample = rpcSample;
    this.rpcLine = rpcLine;
    this.normalization = normalization;
    this.error = error;
  }

  @Override
  public Point2D worldToImage(Coordinate worldPoint, Point2D imagePoint) {
    double[] sampleLine = internalWorldToImage(worldPoint.x,worldPoint.y,worldPoint.z);
    if (imagePoint == null) {
      imagePoint = new Point2D.Double(sampleLine[0], sampleLine[1]);
    } else {
      imagePoint.setLocation(sampleLine[0], sampleLine[1]);
    }
    return imagePoint;
  }

  @Override
  public Coordinate imageToWorld(Point2D imagePoint, Surface surf, Coordinate worldPoint) {
    SimplexOptimizer optim = new SimplexOptimizer(1E-10, 1E-10);
    PointValuePair result = optim.optimize(
        new MaxEval(1000),
        new ObjectiveFunction(lonlat -> {
          double[] sampleLine = internalWorldToImage(lonlat[0],lonlat[1],surf.getElevation(lonlat[0],lonlat[1]));
          double dx = imagePoint.getX() - sampleLine[0];
          double dy = imagePoint.getY() - sampleLine[1];
          return Math.sqrt(dx * dx + dy * dy);
        }),
        GoalType.MINIMIZE,
        new NelderMeadSimplex(new double[]{ 0.2, 0.2 }),
        new InitialGuess(new double[]{normalization.getLonOff(), normalization.getLatOff()}));

    double[] closestWorldPoint = result.getPoint();

    if (worldPoint == null) {
      worldPoint = new Coordinate(closestWorldPoint[0],closestWorldPoint[1],closestWorldPoint[2]);
    } else {
      worldPoint.setOrdinate(0,closestWorldPoint[0]);
      worldPoint.setOrdinate(1,closestWorldPoint[1]);
      worldPoint.setOrdinate(2,getDefaultElevation());
    }
    return worldPoint;
  }

  @Override
  public double getDefaultElevation() {
    return this.normalization.getHtOff();
  }

  @Override
  public void setDefaultElevation(double elevation) {

  }

  @Override
  public Coordinate getReferencePoint() {
    return new Coordinate(normalization.getLonOff(),normalization.getLatOff(),normalization.getHtOff());
  }

    /**
     * @return the sample polynomial ratio for this model
     */
  public RationalCameraPolynomial getRpcSample() {
    return rpcSample;
  }

    /**
     * @return The line polynomial ratio for this model
     */
  public RationalCameraPolynomial getRpcLine() {
    return rpcLine;
  }

    /**
     * @return the normalization coefficients of this model
     */
  public NormalizationCoefficients getNormalization() {
    return normalization;
  }

    /**
     * @return the error estimates of this model
     */
  public ErrorEstimate getError() {
    return error;
  }

  private double[] internalWorldToImage(double lon,double lat,double elev) {
    double normLon = this.normalization.normalizeLon(lon);
    double normLat = this.normalization.normalizeLat(lat);
    double normElev = this.normalization.normalizeElev(elev);
    double samp = this.rpcSample.calculateValue(normLon,normLat,normElev);
    double line = this.rpcLine.calculateValue(normLon,normLat,normElev);
    double[] sampleLine = new double[2];
    sampleLine[0] = this.normalization.deNormalizeSample(samp);
    sampleLine[1] = this.normalization.deNormalizeLine(line);
    return sampleLine;
  }

  @Override
  public long getSizeInBytes() {
    return super.getSizeInBytes() + CacheableUtil.getDefault().getSizeInBytesForObjects(rpcSample, rpcLine, normalization, error);
  }  
}
