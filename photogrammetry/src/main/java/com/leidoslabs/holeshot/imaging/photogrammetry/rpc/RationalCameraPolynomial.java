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
 * Ratio of polynomials representing transformation from a 3D point on earth (latitude, longitude, elevation) to a
 * sample or line point (x or y, respectively) in an image
 * Created by parrise on 2/22/17.
 */
public class RationalCameraPolynomial implements Cacheable {

   private final CameraPolynomial numerator;
   private final CameraPolynomial denominator;

    /**
     * Create a camera polynomial ratio with the given numerator and denominator polynomials
     * @param numerator
     * @param denominator
     */
   public RationalCameraPolynomial(CameraPolynomial numerator, CameraPolynomial denominator) {
      this.numerator = numerator;
      this.denominator = denominator;
   }

    /**
     * Create a null camera polynomial ratio
     */
   public RationalCameraPolynomial() {
      this(new CameraPolynomial(), new CameraPolynomial());
   }

    /**
     * Find the line or sample point in the image of the given point on earth by evaluating the polynomial ratio
     * at that point
     * @param x longitude (decimal degrees)
     * @param y latitude (decimal degrees)
     * @param z elevation (meters)
     * @return Either the sample or line coordinate, depending on the coefficients used to generate this polynomial
     */
   public double calculateValue(double x,double y,double z) {
      double den = this.denominator.calculateValue(x,y,z);
      if (Math.abs(den) < Double.MIN_VALUE) {
         return Double.MAX_VALUE;
      }
      return this.numerator.calculateValue(x,y,z) / den;
   }

   public long getSizeInBytes() {
      return CacheableUtil.getDefault().getSizeInBytesForObjects(numerator, denominator);
   }

    /**
     * @return This polynomial ratio's polynomial numerator
     */
   public CameraPolynomial getNumerator() {
      return numerator;
   }

    /**
     * @return This polynomial ratio's polynomial denominator
     */
   public CameraPolynomial getDenominator() {
      return denominator;
   }
}
