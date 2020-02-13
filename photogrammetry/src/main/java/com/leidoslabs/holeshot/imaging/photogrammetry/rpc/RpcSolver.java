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
import java.util.Arrays;

import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;
import org.apache.commons.math3.linear.SingularValueDecomposition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.leidoslabs.holeshot.imaging.photogrammetry.CameraModel;
import com.leidoslabs.holeshot.imaging.photogrammetry.Surface;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;

/**
 * Find coefficients for an RPC Model given either a grid of control points or a camera model from
 * which to compute such a grid
 */
public class RpcSolver {
   private static final Logger LOGGER = LoggerFactory.getLogger(RpcSolver.class);
   
   private static final int STARTING_GRID_SIZE = 8;
   
   private static final double DBL_EPSILON =  Math.ulp(1.0);
   private static final double FLT_EPSILON = Math.ulp(1.0f);
   
   
   private RPCCameraModel rpcCameraModel;

   private boolean theUseElevationFlag;
   private boolean theHeightAboveMSLFlag;
   @SuppressWarnings("unused")
   private double theMeanResidual;
   private double theMaxResidual;

   public RpcSolver(boolean useElevation,
         boolean useHeightAboveMSLFlag)
   {
      theUseElevationFlag   = useElevation;
      theHeightAboveMSLFlag = useHeightAboveMSLFlag;
      theMeanResidual = 0.0;
      theMaxResidual = 0.0;
      rpcCameraModel = null;
   }

    /**
     * Solves for coefficients by first creating a discrete regular grid of xSamples x ySamples control points within
     * the given imageBounds using the given cameraModel as the surface function. The camera model should be capable
     * of estimating the geodetic coordinates for image coordinates within the envelope using the imageToWorld function.
     * It need not perform the inverse worldToImage function. If theUseElevationFlag is set, the two argument
     * version of imageToWorld will be used with a null surface.
     * @param imageBounds The bounds for the control point grid, the given cameraModel should be valid everywhere within
     * @param cameraModel The function to calculate the expected value at each grid point
     * @param xSamples The number of x grid points to create
     * @param ySamples The number of y grid points to create
     * @param shiftTo0Flag Not currently used
     */
   public void solveCoefficients(Envelope imageBounds,
         CameraModel cameraModel,
         int xSamples,
         int ySamples,
         boolean shiftTo0Flag)
   {
      if(ySamples < 1) ySamples = STARTING_GRID_SIZE;
      if(xSamples < 1) xSamples = STARTING_GRID_SIZE;
      
      final int numSamples = xSamples * ySamples;
      
      Coordinate[] theGroundPoints = new Coordinate[numSamples];
      Point2D[] theImagePoints = new Point2D[numSamples];

      double w = imageBounds.getWidth();
      double h = imageBounds.getHeight();
      
      double Dx = w / (xSamples - 1);
      double Dy = h / (ySamples - 1);

      final Coordinate ul = new Coordinate(imageBounds.getMinX(), imageBounds.getMinY());
      

      Point2D.Double dpt = new Point2D.Double();
      for(int y = 0; y < ySamples; ++y)
      {
         dpt.y = (double)y*Dy + ul.y;
         for(int x = 0; x < xSamples; ++x)
         {
            dpt.x = (double)x*Dx + ul.x;
            
            Coordinate gpt;
            if (theUseElevationFlag) {
               gpt = cameraModel.imageToWorld(dpt);
            } else {
               gpt = cameraModel.imageToWorld(dpt, (Surface)null);
            }
            
            if (Double.isNaN(gpt.x) || Double.isNaN(gpt.y))
               continue;
            
            if (Double.isNaN(gpt.z)) {
               gpt.z = 0.0;
            }
            
            if (theHeightAboveMSLFlag) {
               throw new UnsupportedOperationException("Haven't implemented getHeightAboveMSL in CameraModel yet");
               //               double height = ossimElevManager::instance()->getHeightAboveMSL(gpt);
               //               if(ossim::isnan(h) == false)
               //               {
               //                  gpt.height(h);
               //               }
            }
            final int pointIndex = y * xSamples + x;
            theImagePoints[pointIndex] = new Point2D.Double(dpt.x, dpt.y);
            theGroundPoints[pointIndex] = gpt;
         }
      }
      solveCoefficients(theImagePoints,
            theGroundPoints);
   }

    /**
     * Solve for coefficients given the grid of image points and ground control points. The grids should
     * match in length and span the image space for an accurate result model
     * @param imagePoints The grid points in image coordinates
     * @param groundControlPoints Those same points, calculated in geodetic coordinates (decimal degrees)
     */
   public void solveCoefficients(Point2D[] imagePoints,
                                 Coordinate[] groundControlPoints)
   {
      
      
      if(imagePoints.length != groundControlPoints.length)
      {
         throw new IllegalArgumentException("Must have same number of imagePoints and ground control points");
      }
      
      final int numPoints = imagePoints.length;

      // we will first create f which holds the result of f(x,y,z).
      // This basically holds the cooresponding image point for each
      // ground control point.  One for x and a second array for y
      //
      final double[] fx = new double[numPoints];
      final double[] fy = new double[numPoints];

      //  Holds the x, y, z vectors
      //
      final double[] x = new double[numPoints];
      final double[] y = new double[numPoints];
      final double[] z = new double[numPoints];

      // compute the image bounds for the given image points
      //
      Envelope rect = new Envelope();
      Arrays.stream(imagePoints).map(p-> new Coordinate(p.getX(), p.getY())).forEach(coord -> rect.expandToInclude(coord));
      
      // get the widtha dn height that will be used
      // in data normalization
      //
      double w = rect.getWidth();
      double h = rect.getHeight();

      // setup scales for normalization
      //double xScale = w/2.0;
      //double yScale = h/2.0;

      // get the shift for the cneter of the data
      Coordinate centerImagePoint = rect.centre();
      

      double latSum=0.0;
      double lonSum=0.0;
      double heightSum=0.0;

      // find the center ground  Use elevation only if its enabled
      //
      
      for (int c=0; c<numPoints;++c) {
         Coordinate currentGCP = groundControlPoints[c];
         if(!Double.isNaN(currentGCP.y))
         {
            latSum += currentGCP.y;
         }
         if(!Double.isNaN(currentGCP.x))
         {
            lonSum += currentGCP.x;
         }
         if(!Double.isNaN(currentGCP.z))
         {
            if(theUseElevationFlag)
            {
               heightSum += currentGCP.z;
            }
         }
      }
      
      // set the center ground for the offset
      //
      Coordinate centerGround = new Coordinate(
            lonSum/numPoints,
            latSum/numPoints,
            heightSum/numPoints);
      
      // set up ground scales and deltas for normalization
      //
      double deltaLat       = 0.0;
      double deltaLon       = 0.0;
      double deltaHeight    = 0.0;
      double maxDeltaLat    = 0.0;
      double maxDeltaLon    = 0.0;
      double maxDeltaHeight = 0.0;
      double heightTest       = 0.0;


      for (int c=0; c<numPoints; ++c) {
         final Coordinate currentGCP = groundControlPoints[c];
         final Point2D currentIP = imagePoints[c];

         deltaLat = (currentGCP.y - centerGround.y);
         deltaLon = (currentGCP.x - centerGround.x);
         if(!Double.isNaN(currentGCP.z))
         {
            if(theUseElevationFlag)
            {
               deltaHeight = currentGCP.z - centerGround.z;
               heightTest  = currentGCP.z;
            }
            else
            {
               deltaHeight = 0.0;
               heightTest  = 0.0;
            }
         }
         else
         {
            deltaHeight = 0.0;
         }
         fx[c] = (currentIP.getX() - centerImagePoint.x)/(w/2.0);
         fy[c] = (currentIP.getY() - centerImagePoint.y)/(h/2.0);

         x[c] = deltaLon;
         y[c] = deltaLat;
         z[c] = deltaHeight;
         
         
         if(Math.abs(deltaLat) > maxDeltaLat) maxDeltaLat          = Math.abs(deltaLat);
         if(Math.abs(deltaLon) > maxDeltaLon) maxDeltaLon          = Math.abs(deltaLon);
         if(Math.abs(heightTest) > maxDeltaHeight) maxDeltaHeight  = Math.abs(heightTest);
      }
      
      boolean elevationEnabled = theUseElevationFlag;

      if(Math.abs(maxDeltaHeight) < FLT_EPSILON ) elevationEnabled = false;
      // if max delta is less than a meter then set it to a meter.
      if(maxDeltaHeight < 1.0) maxDeltaHeight = 1.0;

      // set the height scale to something pretty large
      if(!elevationEnabled)
      {
         maxDeltaHeight = 1.0/DBL_EPSILON;
         maxDeltaHeight = 10000.0;
         centerGround.z = 0.0;
      }
      // normalize the ground points
      for(int c = 0; c < numPoints; ++c)
      {
         x[c] /= maxDeltaLon;
         y[c] /= maxDeltaLat;
         z[c] /= maxDeltaHeight;
      }
      rpcCameraModel = RPCCameraModelFactory.buildNullCameraModel();
      final NormalizationCoefficients norm = rpcCameraModel.getNormalization();
      norm.setLineOff(centerImagePoint.y);
      norm.setSampOff(centerImagePoint.x);
      norm.setLineScale(h/2.0);
      norm.setSampScale(w/2.0);

      norm.setLatScale(maxDeltaLat);
      norm.setLonScale(maxDeltaLon);
      norm.setHtScale(maxDeltaHeight);
      
      norm.setLatOff(centerGround.y);
      norm.setLonOff(centerGround.x);
      norm.setHtOff(centerGround.z);

      if(Double.isNaN(norm.getHtOff()))
      {
         norm.setHtOff(0.0);
      }

      // now lets solve the coefficients
      //

      // perform a least squares fit for sample values found in f
      // given the world values with variables x, y, z
      //
      RealMatrix coeffxVec = solveCoefficients(fx, x, y, z);
      
      // perform a least squares fit for line values found in f
      // given the world values with variables x, y, z
      //
      RealMatrix coeffyVec = solveCoefficients(fy, x, y, z);
      
      // there are 20 numerator coefficients
      // and 19 denominator coefficients
      // I believe that the very first one for the
      // denominator coefficients is fixed at 1.0
      //
      final double[] lineNumCoeff = rpcCameraModel.getRpcLine().getNumerator().getCoefficients();
      final double[] lineDenCoeff = rpcCameraModel.getRpcLine().getDenominator().getCoefficients();
      final double[] sampNumCoeff = rpcCameraModel.getRpcSample().getNumerator().getCoefficients();
      final double[] sampDenCoeff = rpcCameraModel.getRpcSample().getDenominator().getCoefficients();
      
      lineNumCoeff[0] = coeffyVec.getEntry(0,0);
      lineDenCoeff[0] = 1.0;
      sampNumCoeff[0] = coeffxVec.getEntry(0,0);
      sampDenCoeff[0] = 1.0;
      for (int i=1;i<20;++i) {
         lineNumCoeff[i] = coeffyVec.getEntry(i, 0);
         lineDenCoeff[i] = coeffyVec.getEntry(i+19, 0);
         sampNumCoeff[i] = coeffxVec.getEntry(i, 0);
         sampDenCoeff[i] = coeffxVec.getEntry(i+19, 0);
      }
      
      // now lets compute the RMSE for the given control points by feeding it
      // back through the modeled RPC
      //
      double  sumSquareError = 0.0;
      int idx = 0;

      theMaxResidual = 0.0;
      
      // std::cout << "ground offset height = " << theGroundOffset.height()
      //           << "Height scale         = " << theHeightScale << std::endl;
      for (idx = 0; idx<numPoints; idx++)
      {
         Point2D evalPt = evalPoint(groundControlPoints[idx]);

         final RealVector evalPtVec = new ArrayRealVector(new double[] { evalPt.getX(), evalPt.getY() });
         final Point2D imagePoint = imagePoints[idx];
         final RealVector imagePointVec = new ArrayRealVector(new double[] { imagePoint.getX(), imagePoint.getY() });
         final double len = evalPtVec.subtract(imagePointVec).getNorm();
         
         theMaxResidual = Math.max(theMaxResidual,  len);
         sumSquareError += (len * len);
      }
      
      // set the error
      theMeanResidual = Math.sqrt(sumSquareError / numPoints);
   }
   
   // RGR - NEED to create an RPCCameraModel and continuosly re-evaluate it the way that the new ossimRpcSolver.cpp does. 

   private Point2D evalPoint(Coordinate gpt) {
      Point2D result;
      if (rpcCameraModel == null) {
         result = new Point2D.Double(Double.NaN, Double.NaN);
      } else {
         result = rpcCameraModel.worldToImage(gpt);
      }
      return result;
   }
   
   public RPCCameraModel getRPCCameraModel() {
      return rpcCameraModel;
   }


   private RealMatrix solveCoefficients(
         double[] f,
         double[] x,
         double[] y,
         double[] z )
   {
      // this is an iterative  linear least square fit.  We really probably need
      // a nonlinear fit instead
      //
      int idx = 0;
      
      RealMatrix m;
      
      RealMatrix r = MatrixUtils.createColumnRealMatrix(f);
      RealMatrix tempCoeff;
      RealMatrix weights = MatrixUtils.createRealIdentityMatrix(f.length);
      RealMatrix denominator = MatrixUtils.createRealMatrix(20, 1);
      
      double residualValue = 1.0/FLT_EPSILON;
      int iterations = 0;
      
      RealMatrix w2;
      do
      {
         w2 = weights.multiply(weights);

         // sets up the matrix to hold the system of
         // equations
         m = setupSystemOfEquations(
               r,
               x,
               y,
               z);

         // solve the least squares solution.  Note: the invert is used
         // to do a Singular Value Decomposition for the inverse since the
         // matrix is more than likely singular.  Slower but more robust
         //
         tempCoeff = invert(m.transpose().multiply(w2).multiply(m)).multiply(m.transpose()).multiply(w2).multiply(r);

         // set up the weight matrix by using the denominator
         //
         for(idx = 0; idx < 19; ++idx)
         {
            denominator.setEntry(idx+1,0, tempCoeff.getEntry(20+idx, 0));
         }
         denominator.setEntry(0, 0, 1.0);
         
         setupWeightMatrix(
               weights,
               denominator,
               r,
               x,
               y,
               z);

         // compute the residual
         //
         RealMatrix residual = m.transpose().multiply(w2).multiply(m.multiply(tempCoeff).subtract(r));

         // now get the innerproduct
         //
         RealMatrix tempRes = residual.transpose().multiply(residual);
         residualValue = tempRes.getEntry(0, 0);

         ++iterations;

      } while((residualValue >FLT_EPSILON) && (iterations < 10));
      return tempCoeff;
   }

   private RealMatrix invert(RealMatrix m)
   {
      // decompose m.t*m which is stored in Temp into the singular values and vectors.
      //
     
      SingularValueDecomposition svd = new SingularValueDecomposition(m);
      RealMatrix u = svd.getU();
      RealMatrix v = svd.getV();
      RealMatrix d = svd.getS();
      
      // invert the diagonal
      // this is just doing the reciprocal for all diagonal components and store back in
      // d.  this computes d inverse.
      //
      for(int idx=0; idx < d.getColumnDimension(); ++idx)
      {
         final double diagValue = d.getEntry(idx,  idx);
         if(diagValue > FLT_EPSILON)
         {
            d.setEntry(idx, idx, 1.0/diagValue);
         }
         else
         {
            d.setEntry(idx, idx, 0.0);
         }
      }

      //compute inverse of decomposed m;
      return v.multiply(d).multiply(u.transpose());
   }

   private RealMatrix setupSystemOfEquations(
         RealMatrix f,
         double[] x,
         double[] y,
         double[] z)
   {
      RealMatrix equations = MatrixUtils.createRealMatrix(f.getRowDimension(), 39);

      for(int idx = 0; idx < f.getRowDimension();++idx)
      {
         equations.setEntry(idx,0, 1.0);
         equations.setEntry(idx,1,x[idx]);
         equations.setEntry(idx,2,y[idx]);
         equations.setEntry(idx,3,z[idx]);
         equations.setEntry(idx,4,x[idx]*y[idx]);
         equations.setEntry(idx,5,x[idx]*z[idx]);
         equations.setEntry(idx,6,y[idx]*z[idx]);
         equations.setEntry(idx,7,x[idx]*x[idx]);
         equations.setEntry(idx,8,y[idx]*y[idx]);
         equations.setEntry(idx,9,z[idx]*z[idx]);
         equations.setEntry(idx,10,x[idx]*y[idx]*z[idx]);
         equations.setEntry(idx,11,x[idx]*x[idx]*x[idx]);
         equations.setEntry(idx,12,x[idx]*y[idx]*y[idx]);
         equations.setEntry(idx,13,x[idx]*z[idx]*z[idx]);
         equations.setEntry(idx,14,x[idx]*x[idx]*y[idx]);
         equations.setEntry(idx,15,y[idx]*y[idx]*y[idx]);
         equations.setEntry(idx,16,y[idx]*z[idx]*z[idx]);
         equations.setEntry(idx,17,x[idx]*x[idx]*z[idx]);
         equations.setEntry(idx,18,y[idx]*y[idx]*z[idx]);
         equations.setEntry(idx,19,z[idx]*z[idx]*z[idx]);
         equations.setEntry(idx,20,-f.getEntry(idx, 0)*x[idx]);
         equations.setEntry(idx,21,-f.getEntry(idx, 0)*y[idx]);
         equations.setEntry(idx,22,-f.getEntry(idx, 0)*z[idx]);
         equations.setEntry(idx,23,-f.getEntry(idx, 0)*x[idx]*y[idx]);
         equations.setEntry(idx,24,-f.getEntry(idx, 0)*x[idx]*z[idx]);
         equations.setEntry(idx,25,-f.getEntry(idx, 0)*y[idx]*z[idx]);
         equations.setEntry(idx,26,-f.getEntry(idx, 0)*x[idx]*x[idx]);
         equations.setEntry(idx,27,-f.getEntry(idx, 0)*y[idx]*y[idx]);
         equations.setEntry(idx,28,-f.getEntry(idx, 0)*z[idx]*z[idx]);
         equations.setEntry(idx,29,-f.getEntry(idx, 0)*x[idx]*y[idx]*z[idx]);
         equations.setEntry(idx,30,-f.getEntry(idx, 0)*x[idx]*x[idx]*x[idx]);
         equations.setEntry(idx,31,-f.getEntry(idx, 0)*x[idx]*y[idx]*y[idx]);
         equations.setEntry(idx,32,-f.getEntry(idx, 0)*x[idx]*z[idx]*z[idx]);
         equations.setEntry(idx,33,-f.getEntry(idx, 0)*x[idx]*x[idx]*y[idx]);
         equations.setEntry(idx,34,-f.getEntry(idx, 0)*y[idx]*y[idx]*y[idx]);
         equations.setEntry(idx,35,-f.getEntry(idx, 0)*y[idx]*z[idx]*z[idx]);
         equations.setEntry(idx,36,-f.getEntry(idx, 0)*x[idx]*x[idx]*z[idx]);
         equations.setEntry(idx,37,-f.getEntry(idx, 0)*y[idx]*y[idx]*z[idx]);
         equations.setEntry(idx,38,-f.getEntry(idx, 0)*z[idx]*z[idx]*z[idx]);
      }
      return equations;
   }

   private void setupWeightMatrix(
         RealMatrix result,
         RealMatrix coefficients,
         RealMatrix f,
         double[] x,
         double[] y,
         double[] z)
   {
      RealMatrix row = MatrixUtils.createRealMatrix(1, coefficients.getRowDimension());

      for(int idx = 0; idx < (int)f.getRowDimension(); ++idx)
      {
         row.setEntry(0,0,1);
         row.setEntry(0,1,x[idx]);
         row.setEntry(0,2,y[idx]);
         row.setEntry(0,3,z[idx]);
         row.setEntry(0,4,x[idx]*y[idx]);
         row.setEntry(0,5,x[idx]*z[idx]);
         row.setEntry(0,6,y[idx]*z[idx]);
         row.setEntry(0,7,x[idx]*x[idx]);
         row.setEntry(0,8,y[idx]*y[idx]);
         row.setEntry(0,9,z[idx]*z[idx]);
         row.setEntry(0,10,x[idx]*y[idx]*z[idx]);
         row.setEntry(0,11,x[idx]*x[idx]*x[idx]);
         row.setEntry(0,12,x[idx]*y[idx]*y[idx]);
         row.setEntry(0,13,x[idx]*z[idx]*z[idx]);
         row.setEntry(0,14,x[idx]*x[idx]*y[idx]);
         row.setEntry(0,15,y[idx]*y[idx]*y[idx]);
         row.setEntry(0,16,y[idx]*z[idx]*z[idx]);
         row.setEntry(0,17,x[idx]*x[idx]*z[idx]);
         row.setEntry(0,18,y[idx]*y[idx]*z[idx]);
         row.setEntry(0,19,z[idx]*z[idx]*z[idx]);

         result.setEntry(idx, idx, 0.0);
         for(int idx2 = 0; idx2 < row.getColumnDimension(); ++idx2)
         {
            result.setEntry(idx, idx, result.getEntry(idx,  idx) + row.getEntry(0, idx2)*coefficients.getEntry(idx2,0));
         }

         if(result.getEntry(idx,idx) > FLT_EPSILON)
         {
            result.setEntry(idx, idx, 1.0/result.getEntry(idx, idx));
         }
      }
   }

    /**
     * @return an RPC00B TRE style metadata object built from the camera model
     */
   public RPCBTre getNitfRpcBTag()
   {
      final NormalizationCoefficients norm = rpcCameraModel.getNormalization();
      final ErrorEstimate error = rpcCameraModel.getError();
      final RationalCameraPolynomial sample = rpcCameraModel.getRpcSample();
      final RationalCameraPolynomial line = rpcCameraModel.getRpcLine();
      
      return new RPCBTre.Builder()
            .setSuccess(true)
            .setErrorBias(error.getBias())
            .setErrorRand(error.getRandom())
            .setLineOffset((int)norm.getLineOff())
            .setSampleOffset((int)norm.getSampOff())
            .setGeodeticLatOffset(norm.getLatOff())
            .setGeodeticLonOffset(norm.getLonOff())
            .setGeodeticHeightOffset((int)norm.getHtOff())
            .setLineScale((int)norm.getLineScale())
            .setSampleScale((int)norm.getSampScale())
            .setGeodeticLatScale(norm.getLatScale())
            .setGeodeticLonScale(norm.getLonScale())
            .setGeodeticHeightScale((int)norm.getHtScale())
            .setLineNumeratorCoeff(line.getNumerator().getCoefficients())
            .setLineDenominatorCoeff(line.getDenominator().getCoefficients())
            .setSampleNumeratorCoeff(sample.getNumerator().getCoefficients())
            .setSampleDenomoniatorCoeff(sample.getDenominator().getCoefficients())
            .build();
   }
   
//   public static void main(String[] args) {
//      double[][] imagePoints = 
//         {{0.0, 0.0},
//          {705.9166666666666, 0.0},
//          {1411.8333333333333, 0.0},
//          {2117.75, 0.0},
//          {2823.6666666666665, 0.0},
//          {3529.5833333333335, 0.0},
//          {4235.5, 0.0},
//          {4941.416666666667, 0.0},
//          {5647.333333333333, 0.0},
//          {6353.25, 0.0},
//          {7059.166666666667, 0.0},
//          {7765.083333333333, 0.0},
//          {0.0, 1931.1666666666665},
//          {705.9166666666666, 1931.1666666666665},
//          {1411.8333333333333, 1931.1666666666665},
//          {2117.75, 1931.1666666666665},
//          {2823.6666666666665, 1931.1666666666665},
//          {3529.5833333333335, 1931.1666666666665},
//          {4235.5, 1931.1666666666665},
//          {4941.416666666667, 1931.1666666666665},
//          {5647.333333333333, 1931.1666666666665},
//          {6353.25, 1931.1666666666665},
//          {7059.166666666667, 1931.1666666666665},
//          {7765.083333333333, 1931.1666666666665},
//          {0.0, 3862.333333333333},
//          {705.9166666666666, 3862.333333333333},
//          {1411.8333333333333, 3862.333333333333},
//          {2117.75, 3862.333333333333},
//          {2823.6666666666665, 3862.333333333333},
//          {3529.5833333333335, 3862.333333333333},
//          {4235.5, 3862.333333333333},
//          {4941.416666666667, 3862.333333333333},
//          {5647.333333333333, 3862.333333333333},
//          {6353.25, 3862.333333333333},
//          {7059.166666666667, 3862.333333333333},
//          {7765.083333333333, 3862.333333333333},
//          {0.0, 5793.5},
//          {705.9166666666666, 5793.5},
//          {1411.8333333333333, 5793.5},
//          {2117.75, 5793.5},
//          {2823.6666666666665, 5793.5},
//          {3529.5833333333335, 5793.5},
//          {4235.5, 5793.5},
//          {4941.416666666667, 5793.5},
//          {5647.333333333333, 5793.5},
//          {6353.25, 5793.5},
//          {7059.166666666667, 5793.5},
//          {7765.083333333333, 5793.5},
//          {0.0, 7724.666666666666},
//          {705.9166666666666, 7724.666666666666},
//          {1411.8333333333333, 7724.666666666666},
//          {2117.75, 7724.666666666666},
//          {2823.6666666666665, 7724.666666666666},
//          {3529.5833333333335, 7724.666666666666},
//          {4235.5, 7724.666666666666},
//          {4941.416666666667, 7724.666666666666},
//          {5647.333333333333, 7724.666666666666},
//          {6353.25, 7724.666666666666},
//          {7059.166666666667, 7724.666666666666},
//          {7765.083333333333, 7724.666666666666},
//          {0.0, 9655.833333333334},
//          {705.9166666666666, 9655.833333333334},
//          {1411.8333333333333, 9655.833333333334},
//          {2117.75, 9655.833333333334},
//          {2823.6666666666665, 9655.833333333334},
//          {3529.5833333333335, 9655.833333333334},
//          {4235.5, 9655.833333333334},
//          {4941.416666666667, 9655.833333333334},
//          {5647.333333333333, 9655.833333333334},
//          {6353.25, 9655.833333333334},
//          {7059.166666666667, 9655.833333333334},
//          {7765.083333333333, 9655.833333333334},
//          {0.0, 11587.0},
//          {705.9166666666666, 11587.0},
//          {1411.8333333333333, 11587.0},
//          {2117.75, 11587.0},
//          {2823.6666666666665, 11587.0},
//          {3529.5833333333335, 11587.0},
//          {4235.5, 11587.0},
//          {4941.416666666667, 11587.0},
//          {5647.333333333333, 11587.0},
//          {6353.25, 11587.0},
//          {7059.166666666667, 11587.0},
//          {7765.083333333333, 11587.0},
//          {0.0, 13518.166666666668},
//          {705.9166666666666, 13518.166666666668},
//          {1411.8333333333333, 13518.166666666668},
//          {2117.75, 13518.166666666668},
//          {2823.6666666666665, 13518.166666666668},
//          {3529.5833333333335, 13518.166666666668},
//          {4235.5, 13518.166666666668},
//          {4941.416666666667, 13518.166666666668},
//          {5647.333333333333, 13518.166666666668},
//          {6353.25, 13518.166666666668},
//          {7059.166666666667, 13518.166666666668},
//          {7765.083333333333, 13518.166666666668},
//          {0.0, 15449.333333333332},
//          {705.9166666666666, 15449.333333333332},
//          {1411.8333333333333, 15449.333333333332},
//          {2117.75, 15449.333333333332},
//          {2823.6666666666665, 15449.333333333332},
//          {3529.5833333333335, 15449.333333333332},
//          {4235.5, 15449.333333333332},
//          {4941.416666666667, 15449.333333333332},
//          {5647.333333333333, 15449.333333333332},
//          {6353.25, 15449.333333333332},
//          {7059.166666666667, 15449.333333333332},
//          {7765.083333333333, 15449.333333333332},
//          {0.0, 17380.5},
//          {705.9166666666666, 17380.5},
//          {1411.8333333333333, 17380.5},
//          {2117.75, 17380.5},
//          {2823.6666666666665, 17380.5},
//          {3529.5833333333335, 17380.5},
//          {4235.5, 17380.5},
//          {4941.416666666667, 17380.5},
//          {5647.333333333333, 17380.5},
//          {6353.25, 17380.5},
//          {7059.166666666667, 17380.5},
//          {7765.083333333333, 17380.5},
//          {0.0, 19311.666666666668},
//          {705.9166666666666, 19311.666666666668},
//          {1411.8333333333333, 19311.666666666668},
//          {2117.75, 19311.666666666668},
//          {2823.6666666666665, 19311.666666666668},
//          {3529.5833333333335, 19311.666666666668},
//          {4235.5, 19311.666666666668},
//          {4941.416666666667, 19311.666666666668},
//          {5647.333333333333, 19311.666666666668},
//          {6353.25, 19311.666666666668},
//          {7059.166666666667, 19311.666666666668},
//          {7765.083333333333, 19311.666666666668},
//          {0.0, 21242.833333333332},
//          {705.9166666666666, 21242.833333333332},
//          {1411.8333333333333, 21242.833333333332},
//          {2117.75, 21242.833333333332},
//          {2823.6666666666665, 21242.833333333332},
//          {3529.5833333333335, 21242.833333333332},
//          {4235.5, 21242.833333333332},
//          {4941.416666666667, 21242.833333333332},
//          {5647.333333333333, 21242.833333333332},
//          {6353.25, 21242.833333333332},
//          {7059.166666666667, 21242.833333333332},
//          {7765.083333333333, 21242.833333333332}};
//         double[][] groundControlPoints = 
//         {{31.081596633889422, 30.082985966165545, 0.0},
//          {31.08943028437483, 30.08310011419883, 0.0},
//          {31.097263979700426, 30.08321379572157, 0.0},
//          {31.105097719682767, 30.083327010724226, 0.0},
//          {31.112931504138437, 30.083439759197294, 0.0},
//          {31.120765332884005, 30.083552041131313, 0.0},
//          {31.128599205736027, 30.08366385651686, 0.0},
//          {31.136433122511075, 30.08377520534455, 0.0},
//          {31.144267083025674, 30.083886087605038, 0.0},
//          {31.15210108709638, 30.08399650328902, 0.0},
//          {31.159935134539733, 30.08410645238722, 0.0},
//          {31.16776922517225, 30.084215934890423, 0.0},
//          {31.081939473482905, 30.0652195775662, 0.0},
//          {31.089771726089424, 30.065333644419784, 0.0},
//          {31.09760402350259, 30.065447245094415, 0.0},
//          {31.10543636553913, 30.065560379580557, 0.0},
//          {31.113268752015745, 30.06567304786872, 0.0},
//          {31.12110118274914, 30.06578524994945, 0.0},
//          {31.12893365755601, 30.065896985813332, 0.0},
//          {31.13676617625306, 30.066008255450992, 0.0},
//          {31.14459873865697, 30.066119058853094, 0.0},
//          {31.152431344584414, 30.06622939601035, 0.0},
//          {31.160263993852073, 30.06633926691348, 0.0},
//          {31.168096686276602, 30.066448671553285, 0.0},
//          {31.082282007105057, 30.04745313737017, 0.0},
//          {31.09011286307912, 30.047567123072355, 0.0},
//          {31.097943763826354, 30.047680642927073, 0.0},
//          {31.105774709163608, 30.047793696924785, 0.0},
//          {31.113605698907737, 30.047906285056015, 0.0},
//          {31.121436732875566, 30.048018407311314, 0.0},
//          {31.12926781088395, 30.048130063681267, 0.0},
//          {31.137098932749705, 30.048241254156533, 0.0},
//          {31.14493009828966, 30.048351978727762, 0.0},
//          {31.15276130732062, 30.048462237385667, 0.0},
//          {31.160592559659417, 30.048572030121015, 0.0},
//          {31.16842385512283, 30.048681356924583, 0.0},
//          {31.082624234984497, 30.029686645598158, 0.0},
//          {31.09045369557162, 30.029800550177246, 0.0},
//          {31.098283200898475, 30.02991398924022, 0.0},
//          {31.106112750782053, 30.030026962777562, 0.0},
//          {31.113942345039327, 30.03013947077979, 0.0},
//          {31.121771983487285, 30.030251513237477, 0.0},
//          {31.129601665942904, 30.030363090141226, 0.0},
//          {31.13743139222313, 30.030474201481677, 0.0},
//          {31.14526116214494, 30.030584847249507, 0.0},
//          {31.153090975525284, 30.03069502743545, 0.0},
//          {31.1609208321811, 30.030804742030245, 0.0},
//          {31.168750731929336, 30.03091399102471, 0.0},
//          {31.08296615734957, 30.011920102270928, 0.0},
//          {31.090794223794333, 30.012033925755173, 0.0},
//          {31.09862233494543, 30.012147284054535, 0.0},
//          {31.106450490619995, 30.012260177159533, 0.0},
//          {31.11427869063514, 30.012372605060676, 0.0},
//          {31.12210693480798, 30.01248456774855, 0.0},
//          {31.129935222955634, 30.012596065213764, 0.0},
//          {31.137763554895177, 30.012707097446967, 0.0},
//          {31.145591930443736, 30.012817664438856, 0.0},
//          {31.153420349418376, 30.012927766180155, 0.0},
//          {31.161248811636188, 30.013037402661634, 0.0},
//          {31.169077316914258, 30.013146573874096, 0.0},
//          {31.083307774428306, 29.994153507409226, 0.0},
//          {31.09113444797437, 29.99426724982685, 0.0},
//          {31.098961166193416, 29.994380527390724, 0.0},
//          {31.10678792890271, 29.99449334009136, 0.0},
//          {31.11461473591951, 29.994605687919297, 0.0},
//          {31.122441587061065, 29.994717570865117, 0.0},
//          {31.130268482144608, 29.994828988919455, 0.0},
//          {31.138095420987398, 29.99493994207295, 0.0},
//          {31.145922403406647, 29.995050430316304, 0.0},
//          {31.153749429219594, 29.995160453640274, 0.0},
//          {31.161576498243445, 29.99527001203562, 0.0},
//          {31.169403610295426, 29.99537910549316, 0.0},
//          {31.08364908644845, 29.976386861033838, 0.0},
//          {31.091474368338545, 29.976500522413026, 0.0},
//          {31.099299694868314, 29.976613719269483, 0.0},
//          {31.107125065855165, 29.97672645159373, 0.0},
//          {31.114950481116473, 29.976838719376317, 0.0},
//          {31.12277594046964, 29.976950522607822, 0.0},
//          {31.13060144373204, 29.97706186127889, 0.0},
//          {31.138426990721047, 29.977172735380176, 0.0},
//          {31.146252581254032, 29.9772831449024, 0.0},
//          {31.15407821514835, 29.977393089836312, 0.0},
//          {31.161903892221357, 29.977502570172682, 0.0},
//          {31.169729612290396, 29.977611585902345, 0.0},
//          {31.08399009363746, 29.958620163165534, 0.0},
//          {31.091813985113387, 29.95873374353446, 0.0},
//          {31.099637921195725, 29.958846859711556, 0.0},
//          {31.10746190170202, 29.958959511687336, 0.0},
//          {31.115285926449783, 29.95907169945238, 0.0},
//          {31.123109995256545, 29.959183422997278, 0.0},
//          {31.13093410793981, 29.959294682312667, 0.0},
//          {31.13875826431711, 29.959405477389232, 0.0},
//          {31.146582464205927, 29.95951580821768, 0.0},
//          {31.15440670742377, 29.95962567478877, 0.0},
//          {31.16223099378812, 29.959735077093303, 0.0},
//          {31.170055323116465, 29.95984401512211, 0.0},
//          {31.08433079622247, 29.94085341382512, 0.0},
//          {31.09215329852512, 29.940966913211913, 0.0},
//          {31.09997584540096, 29.941079948737663, 0.0},
//          {31.107798436667668, 29.941192520392892, 0.0},
//          {31.115621072142893, 29.941304628168186, 0.0},
//          {31.123443751644306, 29.94141627205415, 0.0},
//          {31.131266474989552, 29.94152745204142, 0.0},
//          {31.13908924199627, 29.9416381681207, 0.0},
//          {31.146912052482108, 29.941748420282707, 0.0},
//          {31.154734906264697, 29.941858208518205, 0.0},
//          {31.162557803161654, 29.94196753281799, 0.0},
//          {31.170380742990616, 29.942076393172915, 0.0},
//          {31.08467119443034, 29.923086613033416, 0.0},
//          {31.09249230879968, 29.923200031466177, 0.0},
//          {31.10031346770902, 29.923312986368565, 0.0},
//          {31.108134670976195, 29.923425477731122, 0.0},
//          {31.115955918418976, 29.92353750554442, 0.0},
//          {31.12377720985517, 29.92364906979909, 0.0},
//          {31.131598545102563, 29.923760170485785, 0.0},
//          {31.139419923978927, 29.923870807595193, 0.0},
//          {31.147241346302046, 29.923980981118053, 0.0},
//          {31.155062811889685, 29.92409069104514, 0.0},
//          {31.162884320559602, 29.924199937367256, 0.0},
//          {31.170705872129552, 29.92430872007527, 0.0},
//          {31.08501128848764, 29.905319760811253, 0.0},
//          {31.0928310161627, 29.905433098318056, 0.0},
//          {31.10065078834464, 29.90554597262504, 0.0},
//          {31.1084706048514, 29.905658383722756, 0.0},
//          {31.116290465500914, 29.9057703316018, 0.0},
//          {31.1241103701111, 29.90588181625279, 0.0},
//          {31.131930318499894, 29.905992837666396, 0.0},
//          {31.1397503104852, 29.906103395833327, 0.0},
//          {31.147570345884947, 29.90621349074431, 0.0},
//          {31.15539042451701, 29.90632312239015, 0.0},
//          {31.163210546199306, 29.906432290761646, 0.0},
//          {31.171030710749722, 29.906540995849664, 0.0},
//          {31.08535107862063, 29.887552857179468, 0.0},
//          {31.093169420839537, 29.887666113788352, 0.0},
//          {31.10098780753223, 29.887778907527863, 0.0},
//          {31.10880623851679, 29.88789123838856, 0.0},
//          {31.116624713611277, 29.888003106361033, 0.0},
//          {31.12444323263376, 29.888114511435937, 0.0},
//          {31.132261795402297, 29.88822545360393, 0.0},
//          {31.140080401734927, 29.888335932855732, 0.0},
//          {31.147899051449706, 29.88844594918209, 0.0},
//          {31.155717744364665, 29.888555502573798, 0.0},
//          {31.163536480297843, 29.888664593021687, 0.0},
//          {31.171355259067262, 29.888773220516626, 0.0}};
//         
//         Point2D[] imagePoints2D = Arrays.stream(imagePoints).map(d->new Point2D.Double(d[0], d[1])).toArray(Point2D.Double[]::new);
//         Coordinate[] groundControlCoords = Arrays.stream(groundControlPoints).map(d->new Coordinate(d[0], d[1], d[2])).toArray(Coordinate[]::new);
//         
//         RpcSolver rpcSolver = new RpcSolver(false, false);
//         rpcSolver.solveCoefficients(imagePoints2D, groundControlCoords);
//         
//         RPCCameraModel rpcCamera = rpcSolver.getRPCCameraModel();
//         
//         Point2D[] imageCornerPoints = new Point2D[] {
//               new Point2D.Double(0.0, 0.0),
//               new Point2D.Double(0.0, 23174),
//               new Point2D.Double(8471.0, 23174.0),
//               new Point2D.Double(8471.0, 0.0)
//         };
//         
//         for (int i = 0; i<4; ++i) {
//            Coordinate worldCornerPoint = rpcCamera.imageToWorld(imageCornerPoints[i]);
//            System.out.println(String.format("%s == %s", imageCornerPoints[i].toString(), worldCornerPoint.toString()));
//         }
//         
//         RPCBTre rpcBTag = rpcSolver.getNitfRpcBTag();
//         System.out.println(rpcBTag.toString());
//   }
}