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
 * For performing normalizing operations based on RPC normalization coefficients
 * Normalization, transforming to values between -1 and +1, is done to minimize
 * the introduction of errors during the calculations
 * Created by parrise on 2/22/17.
 */
public class NormalizationCoefficients implements Cacheable {

   private double sampOff;
   private double lineOff;
   private double lonOff;
   private double latOff;
   private double htOff;
   private double sampScale;
   private double lineScale;
   private double lonScale;
   private double latScale;
   private double htScale;

    /**
     * Constructor, initialize coefficients to all 0.0
     */
   public NormalizationCoefficients() {
      this(0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0);
   }

    /**
     * Constructor. Set the coefficients to the given values
     * @param sampOff Sample Offset SAMP_OFF
     * @param lineOff Line Offset LINE_OFF
     * @param lonOff Geodetic Longitude Offset LONG_OFF
     * @param latOff Geodetic Latitude Offset LAT_OFF
     * @param htOff Geodetic Height Offset HEIGHT_OFF
     * @param sampScale Sample Scale SAMP_SCALE
     * @param lineScale Line Scale LINE_SCALE
     * @param lonScale Geodetic Longitude Scale LONG_SCALE
     * @param latScale Geodetic Latitude Scale LAT_SCALE
     * @param htScale Geodetic Height Scale HEIGHT_SCALE
     */
   public NormalizationCoefficients(double sampOff, double lineOff, double lonOff, double latOff, double htOff,
         double sampScale, double lineScale, double lonScale, double latScale, double htScale) {
      this.sampOff = sampOff;
      this.lineOff = lineOff;
      this.lonOff = lonOff;
      this.latOff = latOff;
      this.htOff = htOff;
      this.sampScale = sampScale;
      this.lineScale = lineScale;
      this.lonScale = lonScale;
      this.latScale = latScale;
      this.htScale = htScale;    

   }


    /**
     * Perform normalization using the given values.
     * (value - offset) / scale
     * @param value The value to be normalized
     * @param offset The offset to translate by
     * @param scale The scale of the value
     * @return The normalized value, Double.MAX_VALUE if scale is <=0
     */
   public double normalize(double value,double offset,double scale) {
      if (Math.abs(scale) < Double.MIN_VALUE) {
         return Double.MAX_VALUE;
      }
      return (value - offset) / scale;
   }

    /**
     * Inverse operation of normalization
     * value * scale + offset
     * @param value The value to be denormalized
     * @param offset The offset to translate by
     * @param scale The scale of the value
     * @return The denormalized value
     */
   public double deNormalize(double value, double offset, double scale) {
      return value * scale + offset;
   }

    /**
     * Use the geodetic latitude offset and scale to normalize the given latitude
     * @param lat the latitude value to normalize
     * @return The normalized geodetic latitude
     */
   public double normalizeLat(double lat) {
      return this.normalize(lat,latOff,latScale);
   }

    /**
     * Use the geodetic longitude offset and scale to normalize the given longitude
     * @param lon the longitude value to normalize
     * @return The normalized geodetic longitude
     */
   public double normalizeLon(double lon) {
      return this.normalize(lon,lonOff,lonScale);
   }

    /**
     * Use the geodetic height offset and scale to normalize the given elevation
     * @param ht The elevation to normalize
     * @return The normalized elevation
     */
   public double normalizeElev(double ht) {
      return this.normalize(ht,htOff,htScale);
   }

    /**
     * Denormalize the given sample coordinate using the sample offset and scale coefficients
     * @param samp The sample coordinate to denormalize
     * @return A denormalized coordinate
     */
   public double deNormalizeSample(double samp) {
      return this.deNormalize(samp,sampOff,sampScale);
   }

    /**
     * Denormalize the given line coordinate using the line offset and scale coefficients
     * @param line The line coordinate to denormalize
     * @return A denormalized coordinate
     */
   public double deNormalizeLine(double line) {
      return this.deNormalize(line,lineOff,lineScale);
   }

    /**
     * SAMP_OFF
     * @return Sample Offset
     */
   public double getSampOff() {
      return sampOff;
   }

    /**
     * LINE_OFF
     * @return Line Offset
     */
   public double getLineOff() {
      return lineOff;
   }

    /**
     * LONG_OFF
     * @return Geodetic Longitude Offset
     */
   public double getLonOff() {
      return lonOff;
   }

    /**
     * LAT_OFF
     * @return Geodetic Latitude Offset
     */
   public double getLatOff() {
      return latOff;
   }

    /**
     * HEIGHT_OFF
     * @return Geodetic Height Offset
     */
   public double getHtOff() {
      return htOff;
   }

    /**
     * SAMP_SCALE
     * @return Sample Scale
     */
   public double getSampScale() {
      return sampScale;
   }

    /**
     * LINE_SCALE
     * @return Line Scale
     */
   public double getLineScale() {
      return lineScale;
   }

    /**
     * LONG_SCALE
     * @return Geodetic Longitude Scale
     */
   public double getLonScale() {
      return lonScale;
   }

    /**
     * LAT_SCALE
     * @return Geodetic Latitude Scale
     */
   public double getLatScale() {
      return latScale;
   }

    /**
     * HEIGHT_SCALE
     * @return Geodetic Height Scale
     */
   public double getHtScale() {
      return htScale;
   }

    /**
     * SAMP_OFF
     * @param sampOff Sample Offset
     */
   public void setSampOff(double sampOff) {
      this.sampOff = sampOff;
   }

    /**
     * LINE_OFF
     * @param lineOff Line Offset
     */
   public void setLineOff(double lineOff) {
      this.lineOff = lineOff;
   }

    /**
     * LONG_OFF
     * @param lonOff Geodetic Longitude Offset
     */
   public void setLonOff(double lonOff) {
      this.lonOff = lonOff;
   }

    /**
     * LAT_OFF
     * @param latOff Geodetic Latitude Offset
     */
   public void setLatOff(double latOff) {
      this.latOff = latOff;
   }

    /**
     * HEIGHT_OFF
     * @param htOff Geodetic Height Offset
     */
   public void setHtOff(double htOff) {
      this.htOff = htOff;
   }

    /**
     * SAMP_SCALE
     * @param sampScale Sample Scale
     */
   public void setSampScale(double sampScale) {
      this.sampScale = sampScale;
   }

    /**
     * LINE_SCALE
     * @param lineScale Line Scale
     */
   public void setLineScale(double lineScale) {
      this.lineScale = lineScale;
   }

    /**
     * LONG_SCALE
     * @param lonScale Geodetic Longitude Scale
     */
   public void setLonScale(double lonScale) {
      this.lonScale = lonScale;
   }

    /**
     * LAT_SCALE
     * @param latScale Geodetic Latitude Scale
     */
   public void setLatScale(double latScale) {
      this.latScale = latScale;
   }

    /**
     * HEIGHT_SCALE
     * @param htScale Geodetic Height Scale
     */
   public void setHtScale(double htScale) {
      this.htScale = htScale;
   }

   public long getSizeInBytes() {
      return CacheableUtil.getDefault().getSizeInBytesForObjects(sampOff, lineOff, lonOff, latOff, htOff, sampScale, lineScale, lonScale, latScale, htScale);
   }
}
