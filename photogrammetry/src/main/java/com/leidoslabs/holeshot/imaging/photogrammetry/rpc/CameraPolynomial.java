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

import org.image.common.cache.Cacheable;
import org.image.common.cache.CacheableUtil;

/**
 * Polynomial representation of the numerator or denominator of sample or line ratio in the RPC model
 * Created by parrise on 2/22/17.
 */
public class CameraPolynomial implements Cacheable {

   private double[] coefficients;

    /**
     * Constructor, builds a null polynomial to be populated later
     */
   public CameraPolynomial() {
      this(new double[20]);
   }

    /**
     * Construct a polynomial with the given coefficients
     * @param coefficients The 20 coefficients of this polynomial
     */
   public CameraPolynomial(double[] coefficients) {
      this.coefficients = coefficients;
   }

    /**
     * Evaluate the coefficients per RPC model to recover the corresponding numerator/denominator value at the given
     * point
     * @param x longitude
     * @param y latitude
     * @param z elevation
     * @return numerical value of the polynomial evaluated at the given point
     */
   public double calculateValue(double x, double y, double z) {
      double value = this.coefficients[0];
      value += this.coefficients[1] * x;
      value += this.coefficients[2] * y;
      value += this.coefficients[3] * z;
      value += this.coefficients[4] * x * y;
      value += this.coefficients[5] * x * z;
      value += this.coefficients[6] * y * z;
      value += this.coefficients[7] * x * x;
      value += this.coefficients[8] * y * y;
      value += this.coefficients[9] * z * z;
      value += this.coefficients[10] * x * y * z;
      value += this.coefficients[11] * x * x * x;
      value += this.coefficients[12] * x * y * y;
      value += this.coefficients[13] * x * z * z;
      value += this.coefficients[14] * x * x * y;
      value += this.coefficients[15] * y * y * y;
      value += this.coefficients[16] * y * z * z;
      value += this.coefficients[17] * x * x * z;
      value += this.coefficients[18] * y * y * z;
      value += this.coefficients[19] * z * z * z;

      return value;
   }

    /**
     * Get the coefficents for this polynomial
     * @return This polynomials internal array of coefficients
     */
   public double[] getCoefficients() {
      return coefficients;
   }

   public long getSizeInBytes() {
      return CacheableUtil.getDefault().getSizeInBytesForObjects(coefficients);
   }

}
