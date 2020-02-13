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
package com.leidoslabs.holeshot.imaging.photogrammetry.warp;

import java.awt.geom.Point2D;

/**
 * This class provides static methods to compute polynomial coefficients
 * for image warping.
 *
 * polyCoeff() accepts an array of tie points
 * and computes the coefficients of the warping
 * polynomial.  The coefficients are computed using
 * generalized least squares.
 *
 * polyEval() evaluates the polynomial.
 *
 * The warping polynomial F(x,y) associates to each
 * point (x,y) in the first image a corresponding
 * point F(x,y) in the second image.  The point F(x,y)
 * has the form (f1(x,y),f2(x,y)) where f1(x,y) is the
 * x-component, and f2(x,y) the y-component.  f1(x,y)
 * and f2(x,y) are polynomials in the independent
 * variables x and y.  Their form depends on their
 * degree.  If they are linear (degree 1), then they
 * have the form
 *     f1(x,y) = a1 + a2*x + a3*y
 *     f2(x,y) = b1 + b2*x + b3*y
 * If they are quadratic (degree 2), then they have
 * the form
 *     f1(x,y) = a1 + a2*x + a3*y + a4*x*x + a5*x*y
 *                  + a6*y*y
 *     f2(x,y) = b1 + b2*x + b3*y + b4*x*x + b5*x*y
 *                  + b6*y*y
 * If they are cubic (degree 3), then they have the
 * form
 *     f1(x,y) = a1 + a2*x + a3*y + a4*x*x + a5*x*y
 *                  + a6*y*y + a7*x*x*x + a8*x*x*y
 *                  + a9*x*y*y + 10*y*y*y
 *     f2(x,y) = b1 + b2*x + b3*y + b4*x*x + b5*x*y
 *                  + b6*y*y + b7*x*x*x + b8*x*x*y
 *                  + b9*x*y*y + 10*y*y*y
 * And so on.  The caller specifies the degree.
 * Function PolyCoeff computes the coefficients (a's
 * and b's) and returns these coefficients along with
 * the chi-square error (a measure of goodness-of-fit
 * of the tie points to the polynomial, equal to the
 * sum of the squared differences between each tie
 * point and the value predicted by the polynomial.)
 *
 */
public class PolyWarp
{

  public static final int LINEAR_WARP = 1;
  public static final int QUASILINEAR_WARP = -1;
  public static final int QUADRATIC_WARP = 2;
  public static final int CUBIC_WARP = 3;

  private static final boolean DEBUG = false;


  private int myDegree;
  private TiePointPair[] myTiePoints;
  private WarpCoefficientSet myCoeffs;
  private double myMaxError = 0.0;
  private double myChiSquaredError = 0.0;


  /**
   * Constructs a PolyWarp. The tie points are stored, along with the
   * coefficients, so that a warp may be easily evaluated multiple times in
   * sequence.
   */
  public PolyWarp(int degree, TiePointPair[] tiePoints)
  {
    myDegree = degree;
    myTiePoints = tiePoints;
    myCoeffs = polyCoeff(myDegree, myTiePoints);
    double[] errors = myCoeffs.getErrors();

    //myMaxError = ArrayUtils.maxValue(errors);
    // +++
    myMaxError = 0;
    for(int i=0; i<tiePoints.length; i++)
    {
      myMaxError = Math.max(errors[i], myMaxError);
    }
    // +++

    myChiSquaredError = myCoeffs.getChisqr();
  }




  // NEW

  // MDP: Added Jan 2006 for computation of Affine Transform from 4 points
  public WarpCoefficientSet getCoeffs()
  {
    return myCoeffs;
  }







  /**
   * Constructs the invers warp of this PolyWarp. This is done by constructing
   * a new set of tie points containing the original tie points in reverse
   * order, and building a new PolyWarp from these tiePoints. The degree is
   * assumed to be the same as the degree of this PolyWarp.
   */
  public PolyWarp invertWarp()
  {
    TiePointPair[] invertedTiePoints = invertTies(myTiePoints);
    PolyWarp invertedWarp = new PolyWarp(myDegree, invertedTiePoints);
    return invertedWarp;
  }

  /**
   * Inverts the given array of tie points, switching the order of ties
   */
  private static TiePointPair[] invertTies(TiePointPair[] ties)
  {
    TiePointPair[] inverted = new TiePointPair[ties.length];
    for(int i=0; i<ties.length; i++)
    {
      inverted[i] = new TiePointPair(ties[i].getPointInSecondImage(),
                                     ties[i].getPointInFirstImage());
    }
    return inverted;
  }

  /**
   * Evaluates this PolyWarp for the given point, returning the resulting point.
   */
  public Point2D eval(Point2D fromPoint)
  {
    double x = polyEval(myCoeffs.getACoeffs(), myDegree, fromPoint.getX(), fromPoint.getY());
    double y = polyEval(myCoeffs.getBCoeffs(), myDegree, fromPoint.getX(), fromPoint.getY());
    Point2D mappedPoint = new Point2D.Double(x,y);   // +++ was Point2D.Double
    return mappedPoint;
  }

  /**
   * Gets the maximum error of all tiePoints in this warp.
   */
  public double getMaxError()
  {
    return myMaxError;
  }

  /**
   * Gets the chi-squared error of all tiePoints defining this warp
   */
  public double getChiSquaredError()
  {
    return myChiSquaredError;
  }

  /**
   * Accepts an array of tie points and computes the polynomial coefficients.
   * The value of num_tie_points must be >= (degree + 1)*(degree + 2)/2 and
   * coeffs->num will be set equal to (degree + 1)*(degree + 2)/2.
   * The return value from this function is 0 if there was no error.
   *
   * 01/22/98: changed PolyCoeff so it accepts degree=1.  This gives
   * a "quasi-linear" polynomial with the terms 1, x, y, xy.  Exactly
   * four tie points are needed for an exact fit.  Ex: four image corners.
   *
   */
  public static WarpCoefficientSet polyCoeff(
    int                 degree,         // input: degree of poly
    TiePointPair[]      tiePoints)      // input: tie points
  {
    int      i, j, k;
    int      mfit;              // number of polynomial coefficients
    double[] terms;
    double   sum, d=0.0;
    double   xError, yError, dError;

    int numTiePoints = tiePoints.length;

                                // compute mfit
    if(degree == QUASILINEAR_WARP) // quasi-linear fit
    {
      mfit = 4;
    }
    else
    {
      mfit = (degree + 1)*(degree + 2)/2;
    } // endif

                                // geo.ExcpBadDegree = degree 'degree' < -1
    //assert degree >= -1;
    if (degree < -1) {
      Object[] args = new Object[1];
      args[0] = new Integer(degree);
      throw new RuntimeException("IllegalDegreeExcp");
    }
    // need at least 'mfit' tie points
    //assert numTiePoints >= mfit;
    if (numTiePoints < mfit) {
      Object[]args = new Object[3];
      args[0] = new Integer(numTiePoints);
      args[1] = new Integer(degree);
      args[2] = new Integer(mfit);
      throw new RuntimeException("NotEnoughTiePtsExcp");
    }

    int[] indx = new int[mfit];
    double[][] covar = new double[mfit][mfit];
    double[] coefa = new double[mfit];
    double[] coefb = new double[mfit];
    double[] errors = new double[numTiePoints];
    double chisqr;

    /* first set of coefficients */
    for(j=0; j<mfit; j++) {
      coefa[j] = 0.0;
      for(k=0; k<mfit; k++) {
        covar[j][k] = 0.0;
      } // endfor k
    } // endfor j

    for(i=0; i<numTiePoints; i++) {
      double x1 = tiePoints[i].getPointInFirstImage().getX();
      double y1 = tiePoints[i].getPointInFirstImage().getY();
      double x2 = tiePoints[i].getPointInSecondImage().getX();

      if(degree == -1)
      {
        terms = fillInQuasiLinearTerms(mfit, x1, y1);
      }
      else
      {
        terms = fillInTerms(mfit, degree, x1, y1);
      }

      for(j=0; j<mfit; j++) {
        coefa[j] += terms[j]*x2;
        for(k=0; k<mfit; k++) {
          covar[j][k] += terms[j]*terms[k];
        } // endfor k
      } // endfor j
    } // endfor i

    d = Matrix.decomposeLU(covar, mfit, indx);
    Matrix.backSubstituteLU(covar, mfit, indx, coefa);

    /* second set of coefficients */
    for(j=0; j<mfit; j++) {
      coefb[j] = 0.0;
      for(k=0; k<mfit; k++) {
        covar[j][k] = 0.0;
      } // endfor k
    } // endfor j

    for(i=0; i<numTiePoints; i++) {
      double x1 = tiePoints[i].getPointInFirstImage().getX();
      double y1 = tiePoints[i].getPointInFirstImage().getY();
      double y2 = tiePoints[i].getPointInSecondImage().getY();

      if(degree == -1)
      {
        terms = fillInQuasiLinearTerms(mfit, x1, y1);
      }
      else
      {
        terms = fillInTerms(mfit, degree, x1, y1);
      } // endif
      for(j=0; j<mfit; j++) {
        coefb[j] += terms[j]*y2;
        for(k=0; k<mfit; k++) {
          covar[j][k] += terms[j]*terms[k];
        } // endfor k
      } // endfor j
    } // endfor i
    d = Matrix.decomposeLU(covar, mfit, indx);
    Matrix.backSubstituteLU(covar, mfit, indx, coefb);

    /* chi-squared error term */
    for(chisqr=0.0, i=0; i<numTiePoints; i++) {
      double x1 = tiePoints[i].getPointInFirstImage().getX();
      double y1 = tiePoints[i].getPointInFirstImage().getY();
      double x2 = tiePoints[i].getPointInSecondImage().getX();
      double y2 = tiePoints[i].getPointInSecondImage().getY();

      if(degree == -1)
      {
        terms = fillInQuasiLinearTerms(mfit, x1, y1);
      }
      else
      {
        terms = fillInTerms(mfit, degree, x1, y1);
      } // endif

      for(sum=0.0, j=0; j<mfit; j++)
        sum += coefa[j]*terms[j];
      xError = x2 - sum;
      chisqr += (xError*xError);
      for(sum=0.0, j=0; j<mfit; j++)
        sum += coefb[j]*terms[j];
      yError = y2 - sum;
      chisqr += (yError*yError);
      dError = Math.sqrt(xError*xError + yError*yError);
      errors[i] = dError;
    }

    return(new WarpCoefficientSet(degree, mfit, numTiePoints, coefa, coefb, errors, chisqr));

  } // polyCoeff()

  /* 012298 Pritt:  Added this function for when degree=-1 in PolyCoeffs */
  private static double[] fillInQuasiLinearTerms(int mfit, double x, double y)
  {
    double[] terms = new double[mfit];

    terms[0] = 1.0;
    terms[1] = x;
    terms[2] = y;
    terms[3] = x*y;

    return terms;
  } // fillInQuasiLinearTerms()

  private static double[] fillInTerms(int mfit, int degree, double x, double y)
  {
    double[] terms = new double[mfit];
    int    deg, i, n;

    for(n=0, deg=0; deg<=degree; deg++) {
      for(i=0; i<=deg; i++) {
        terms[n++] = Math.pow(x,deg-i)*Math.pow(y,i);
      }
    }

    return terms;
  } // end fillInTerms()

  /**
   * Evaluates a polynomial
   */
  public static double polyEval(double[] coeff, int degree, double x, double y)
  {
    int    deg, i, n=0;
    double val;

    if(DEBUG)
      System.out.println("   degree = "+degree+", coeffs[1] = "+coeff[1]+"\n");

    if(degree == QUASILINEAR_WARP)
    {
      val = coeff[0] + coeff[1]*x + coeff[2]*y + coeff[3]*x*y;
    }
    else
    {
      for(val=0.0, n=0, deg=0; deg<=degree; deg++) {  // MDP: Jan 2006: NEW - Corrected bug -- should be <=
        for(i=0; i<=deg; i++) {
          val += coeff[n++]*Math.pow(x,deg-i)*Math.pow(y,i);
        }
      }
    }


    return val;
  } // end polyEval()

  /*
  public static ErrorTerms computeErrorTerms(TiePointPair[] tiePoints, PolyWarp warp)
  {
    ErrorTerms result = new ErrorTerms();
    double maxError = 0.0;
    double chiSqrError = 0.0;
    for(int i=0; i<tiePoints.length; i++)
    {
      Point2D image1Point = tiePoints[i].getPointInFirstImage();
      Point2D image2PointRef = tiePoints[i].getPointInSecondImage();
      Point2D image2PointComp = warp.eval(image1Point);
      double error = image2PointRef.distance(image2PointComp);
      double errorSq = image2PointRef.distanceSq(image2PointComp);
      // maximum error term
      maxError = Math.max(error, maxError);
      // sum of squares of error terms
      chiSqrError += errorSq;
      System.err.println("PolyWarp: Image 1 point "+image1Point+" maps to "+
        image2PointComp);
      System.err.println("  actual point is "+image2PointRef+" error distance is "+error+" pixels");
    }
    result.maxError = maxError;
    result.chiSquaredError = chiSqrError;
    return result;
  }

  private static class ErrorTerms
  {
    double maxError;
    double chiSquaredError;
  }
  */


  // ----------------------------------------------------------------------
                                // test driver
/*
  public static void main( String[] args )
  {
    final int NUM_TIE_POINTS = 4;

    double latitude = 21.0;     // was 33
    double longitude = 51.0;

    int xsize = 200;
    int ysize = 300;

    double transx = 0.0;
    double transy = 0.0;

    ApplicationConfigurationBuilder.configure();

    TiePointPair[] tpp = new TiePointPair[NUM_TIE_POINTS];
    tpp[0] = new TiePointPair(
      new Point2D.Double( 20.0,  50.0),      // 50 was 170.
      new Point2D.Double(  0.0,   0.0)
      );
    tpp[1] = new TiePointPair(
      new Point2D.Double( 30.0,  50.0),
      new Point2D.Double((double)xsize - 1,   0.0)
      );
    tpp[2] = new TiePointPair(
      new Point2D.Double( 30.0,  60.0),
      new Point2D.Double((double)xsize - 1, (double)ysize - 1)
      );
    tpp[3] = new TiePointPair(
      new Point2D.Double( 20.0,  60.0),
      new Point2D.Double(  0.0, (double)ysize - 1)
      );


    WarpCoefficientSet coeffs = PolyWarp.polyCoeff(-1, tpp);

    transx = PolyWarp.polyEval(
      coeffs.getACoeffs(),
      coeffs.getDegree(),
      latitude,
      longitude);
    transy = PolyWarp.polyEval(
      coeffs.getBCoeffs(),
      coeffs.getDegree(),
      latitude,
      longitude);

    System.out.println(" PolyWarp Test Driver: "
      + " latitude = " + latitude
      + " longitude = " + longitude
      + " xsize = " + xsize
      + " ysize = " + ysize
      + " transx = " + transx
      + " transy = " + transy
      );

    try
    {
//      System.in.read();
    }
    catch(Exception e)
    {
      System.err.println(e);
    } // end try/catch

  } // end main()
*/

} // end PolyWarp
