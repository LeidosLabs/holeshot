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
 * This class provides static methods to manipulate matricies,
 * including solving sets of linear equations.
 */
public class Matrix
{
  private final static double TINY = Double.MIN_VALUE;


  /**
   *  LU decomposition of a matrix
   *
   *  Use this in conjunction with backSubstituteLU() to solve linear
   *  equations or invert a matrix.  For example, to solve the linear
   *  set of equations
   *    A x  =  b
   *  do the following:
   *    int   n, indx[];
   *    double a[][], b[], d;
   *      .   .
   *      .   .
   *    a=...;
   *    b=...;
   *    indx=...;
   *      .   .
   *      .   .
   *    d = decomposeLU(a, n, indx);
   *    backSubstituteLU(a, n, indx, b);
   *
   *  Code description:
   *    Given the n x n matrix a[1..n][1..n], replace it by its LU
   *    decomposition.  The output is placed in a.  The vector indx[1..n]
   *    records the row permutation effected by the partial pivoting.
   *    d (return value) is +1 or -1 depending on whether the number of row
   *    interchanges was even or odd.
   *
   */
  public static double decomposeLU(double[][] a, int n, int[] indx)
  {
    int       i, imax=0, j, k;
    double    big, dum, sum, temp, d;
    double[]  vv;

    vv = new double[n];
    d = 1.0;
                                // loop over rows to get implicit scaling info
    for(i=0; i<n; i++)
    {
      big = 0.0;
      for(j=0; j<n; j++)  if((temp=Math.abs(a[i][j])) > big)  big = temp;
                                // check for nonzero largest element
                                // geo.ExcpSingularMatrix = singular matrix in Matrix.decomposeLU()
      if (big==0.0)
      {
        throw new RuntimeException("SingularExcp");
      }
       vv[i] = 1.0/big;          // save the scaling
    } // endfor i

                                // loop over columns (Crout's method)
    for(j=0; j<n; j++) {
      for(i=0; i<j; i++) {
        sum = a[i][j];
        for(k=0; k<i; k++) sum -= a[i][k]*a[k][j];
        a[i][j] = sum;
      } // endfor i
                                // initialize search for largest pivot element
      big = 0.0;
      for(i=j; i<n; i++) {
        sum = a[i][j];
        for(k=0; k<j; k++)  sum -= a[i][k]*a[k][j];
        a[i][j] = sum;
        if((dum=vv[i]*Math.abs(sum)) >= big) {
          big = dum;
          imax = i;
        } // endif
      } // endfor i
                                // need to interchange rows
      if(j != imax) {
        for(k=0; k<n; k++) {
          dum=a[imax][k];
          a[imax][k] = a[j][k];
          a[j][k] = dum;
        } // endfor k
                                // interchange parity of d
        d = -d;
        vv[imax] = vv[j];
      } // endif

      indx[j] = imax;
      if(a[j][j] == 0)  a[j][j] = TINY;
                                // divide by pivot element
      if(j != (n-1)) {
        dum = 1.0/a[j][j];
        for(i=j+1; i<n; i++)  a[i][j] *= dum;
      } // endif

    } // endfor j -  go back for the next column

    return d;
  } // end decomposeLU()

  /*
   *  Forward substitution and back substitution on an LU decomposed matrix.
   *  Use this in conjunction with function decomposeLU to solve linear systems
   *  or invert a matrix.
   *
   */
  public static void backSubstituteLU(double[][]a, int n, int[] indx, double[] b)
  {
    int    i, ii=-1, ip, j;
    double sum;
                                // forward substitution
    for(i=0; i<n; i++) {
      ip = indx[i];
      sum = b[ip];
      b[ip] = b[i];
      if(ii != -1)
      {
        for(j=ii; j<=i - 1; j++)  sum -= a[i][j]*b[j];
      }
      else if(sum != 0.0)
      {
        ii = i;
      } // endif
      b[i] = sum;
    } // endfor i

                                // back substitution
    for(i=n-1; i>=0; i--) {
      sum = b[i];
      for(j=i + 1; j<n; j++)
        sum -= a[i][j]*b[j];
                                // store a component of the solution vector x
      b[i] = sum/a[i][i];
    } // endfor i

  } // end backSubstituteLU()


  // add other matrix manipulation methods here as needed


} // end Matrix

