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

/**
 * This class encapsulates the data that form a set of warp coefficients.
 */
public class WarpCoefficientSet
{
  private int      myDegree;    // the degree of the polynomial
  private int      myNumberOfCoeffs;   // num of coeffs = (deg+1)*(deg+2)/2
  private double[] myA;         // first (f1) set of coefficients (a's)
  private double[] myB;         // second (f2) set of coefficients (b's)
  private double[] myErrors;    // errors for each point
  private double   myChisqr;    // chi squared error

  private double   myNumTiePts; // num tie pts (length of myErrors)
  private double   myMaxErr;    // the maximum error over all tie points

    /**
     * Constructor
     * @param degree the degree of the polynomial
     * @param numberOfCoeffs num of coeffs = (deg+1)*(deg+2)/2
     * @param numTiePts num tie pts (length of myErrors)
     * @param a first (f1) set of coefficients (a's)
     * @param b second (f2) set of coefficients (b's)
     * @param errors errors for each point
     * @param chisqr chi squared error
     */
  public WarpCoefficientSet(int degree, int numberOfCoeffs, int numTiePts,
    double[] a,  double[] b, double[] errors, double chisqr)
  {
    myDegree = degree;
    myNumberOfCoeffs = numberOfCoeffs;
    myA = a;
    myB = b;
    myErrors = errors;
    myChisqr = chisqr;

    // MDP: added 03/31/04
    myMaxErr = 0;
    for (int i=0; i<numTiePts; i++)  {
      if (myMaxErr < errors[i]) myMaxErr = errors[i];
    }
    myNumTiePts = numTiePts;

  }

    /**
     * @return the degree of the polynomial
     */
  public int getDegree()
  {
    return myDegree;
  }

    /**
     * @return num of coeffs = (deg+1)*(deg+2)/2
     */
  public int getNumberOfCoeffs()
  {
    return myNumberOfCoeffs;
  }

    /**
     * @return first (f1) set of coefficients (a's)
     */
  public double[] getACoeffs()
  {
    return myA;
  }

    /**
     * @return second (f2) set of coefficients (b's)
     */
  public double[] getBCoeffs() {
      return myB;
  }

    /**
     * @return errors for each point
     */
  public double[] getErrors()
  {
    return myErrors;
  }

    /**
     * @return chi squared error
     */
  public double getChisqr()
  {
    return myChisqr;
  }

    /**
     * @return the maximum error over all tie points
     */
  public double getMaxErr()
  {
    return myMaxErr;
  }

    /**
     * @return num tie pts (length of myErrors)
     */
  public double getNumTiePts()
  {
    return myNumTiePts;
  }

}
