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

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.Optional;

import com.google.common.collect.ImmutableMap;

/**
 * Represents an RPC00B TRE from an images metadata
 */
public class RPCBTre {
   private static final String TAG_NAME = "RPC00B";
   
   private Optional<Boolean> success;
   private Optional<Double> errorBias;
   private Optional<Double> errorRand;
   private Optional<Integer> lineOffset;
   private Optional<Integer> sampleOffset;
   private Optional<Double> geodeticLatOffset;
   private Optional<Double> geodeticLonOffset;
   private Optional<Integer> geodeticHeightOffset;
   private Optional<Integer> lineScale;
   private Optional<Integer> sampleScale;
   private Optional<Double> geodeticLatScale;
   private Optional<Double> geodeticLonScale;
   private Optional<Integer> geodeticHeightScale;
   private Optional<double[]> lineNumeratorCoeff;
   private Optional<double[]> lineDenominatorCoeff;
   private Optional<double[]> sampleNumeratorCoeff;
   private Optional<double[]> sampleDenomoniatorCoeff;
   
   
   private RPCBTre() {
      success = Optional.empty();
      errorBias = Optional.empty();
      errorRand = Optional.empty();
      lineOffset = Optional.empty();
      sampleOffset = Optional.empty();
      geodeticLatOffset = Optional.empty();
      geodeticLonOffset = Optional.empty();
      geodeticHeightOffset = Optional.empty();
      lineScale = Optional.empty();
      sampleScale = Optional.empty();
      geodeticLatScale = Optional.empty();
      geodeticLonScale = Optional.empty();
      geodeticHeightScale = Optional.empty();
      lineNumeratorCoeff = Optional.empty();
      lineDenominatorCoeff = Optional.empty();
      sampleNumeratorCoeff = Optional.empty();
      sampleDenomoniatorCoeff = Optional.empty();
   }

    /**
     * Builder class for creating an RPCBTre, an object representation of an images RPC00B TRE
     */
   public static class Builder {
      private RPCBTre result;

        /**
         * Generate a new builder to begin constructing an RPCBTre
         */
      public Builder() {
         result = new RPCBTre();
      }

        /**
         * @return the RPCBTre that was built
         */
      public RPCBTre build() {
         return result;
      }

        /**
         * @param success If metadata was retrieved successfully
         * @return this
         */
      public Builder setSuccess(boolean success) {
         result.success = Optional.of(success);
         return this;
      }

        /**
         * Set the RMS bias error in meters per horizontal axis of all points in the image
         * @param errorBias ERR_BIAS
         * @return this
         */
      public Builder setErrorBias(double errorBias) {
         result.errorBias = Optional.of(errorBias);
         return this;
      }

        /**
         * Set the RMS random error in meters per horizontal axis of each point in the image
         * @param errorRand ERR_RAND
         * @return this
         */
      public Builder setErrorRand(double errorRand) {
         result.errorRand = Optional.of(errorRand);
         return this;
      }

        /**
         * Set the Line Offset, for (de)normalization
         * @param lineOffset LINE_OFF
         * @return this
         */
      public Builder setLineOffset(int lineOffset) {
         result.lineOffset = Optional.of(lineOffset);
         return this;
      }

        /**
         * Set this Sample Offset, for (de)normalization
         * @param sampleOffset SAMP_OFF
         * @return this
         */
      public Builder setSampleOffset(int sampleOffset) {
         result.sampleOffset = Optional.of(sampleOffset);
         return this;
      }

        /**
         * Set the Geodetic Latitude Offset, for normalization
         * @param geodeticLatOffset LAT_OFF
         * @return this
         */
      public Builder setGeodeticLatOffset(double geodeticLatOffset) {
         result.geodeticLatOffset = Optional.of(geodeticLatOffset);
         return this;
      }

        /**
         * Set the Geodetic Longitude Offset, for normalization
         * @param geodeticLonOffset LONG_OFF
         * @return this
         */
      public Builder setGeodeticLonOffset(double geodeticLonOffset) {
         result.geodeticLonOffset = Optional.of(geodeticLonOffset);
         return this;
      }

        /**
         * Set the Geodetic Height Offset, for normalization
         * @param geodeticHeightOffset HEIGHT_OFF
         * @return this
         */
      public Builder setGeodeticHeightOffset(int geodeticHeightOffset) {
         result.geodeticHeightOffset = Optional.of(geodeticHeightOffset);
         return this;
      }

        /**
         * Set the Line Scale, for (de)normalization
         * @param lineScale LINE_SCALE
         * @return this
         */
      public Builder setLineScale(int lineScale) {
         result.lineScale = Optional.of(lineScale);
         return this;
      }

        /**
         * Set the Sample Scale, for (de)normalization
         * @param sampleScale SAMP_SCALE
         * @return this
         */
      public Builder setSampleScale(int sampleScale) {
         result.sampleScale = Optional.of(sampleScale);
         return this;
      }

        /**
         * Set the Geodetic Latitude Scale, for normalization
         * @param geodeticLatScale LAT_SCALE
         * @return this
         */
      public Builder setGeodeticLatScale(double geodeticLatScale) {
         result.geodeticLatScale = Optional.of(geodeticLatScale);
         return this;
      }

        /**
         * Set the Geodetic Longitude Scale, for normalization
         * @param geodeticLonScale LONG_SCALE
         * @return this
         */
      public Builder setGeodeticLonScale(double geodeticLonScale) {
         result.geodeticLonScale = Optional.of(geodeticLonScale);
         return this;
      }

        /**
         * Set the Geodetic Height Scale, for normalization
         * @param geodeticHeightScale HEIGHT_SCALE
         * @return this
         */
      public Builder setGeodeticHeightScale(int geodeticHeightScale) {
         result.geodeticHeightScale = Optional.of(geodeticHeightScale);
         return this;
      }

        /**
         * Set the 20 Line Numerator Coefficients for the polynomial in the numerator of the row equation
         * @param lineNumeratorCoeff LINE_NUM_COEFF 1-20
         * @return this
         */
      public Builder setLineNumeratorCoeff(double[] lineNumeratorCoeff) {
         result.lineNumeratorCoeff = Optional.of(lineNumeratorCoeff);
         return this;
      }

        /**
         * Set the 20 Line Denominator Coefficients for the polynomial in the denominator of the row equation
         * @param lineDenominatorCoeff LINE_DEN_COEFF 1-20
         * @return this
         */
      public Builder setLineDenominatorCoeff(double[] lineDenominatorCoeff) {
         result.lineDenominatorCoeff = Optional.of(lineDenominatorCoeff);
         return this;
      }

        /**
         * Set the 20 Sample Numerator Coefficients for the polynomial in the numerator of the column equation
         * @param sampleNumeratorCoeff
         * @return this
         */
      public Builder setSampleNumeratorCoeff(double[] sampleNumeratorCoeff) {
         result.sampleNumeratorCoeff = Optional.of(sampleNumeratorCoeff);
         return this;
      }

        /**
         * Set the 20 Sample Denominator Coeefficients for the polynomial in teh denominator of the column equation
         * @param sampleDenomoniatorCoeff
         * @return this
         */
      public Builder setSampleDenomoniatorCoeff(double[] sampleDenomoniatorCoeff) {
         result.sampleDenomoniatorCoeff = Optional.of(sampleDenomoniatorCoeff);
         return this;
      }
   }

    /**
     * @return Text printout of the RPCB TRE values
     */
   @Override
   public String toString() {
      String result = null;
      try (StringWriter sw = new StringWriter()) {
         try (PrintWriter pw = new PrintWriter(sw)) {
            ImmutableMap<String, Optional<?>> values = new ImmutableMap.Builder<String, Optional<?>>()
                  .put("success", success)
                  .put("errorBias", errorBias)
                  .put("errorRand", errorRand)
                  .put("lineOffset", lineOffset)
                  .put("sampleOffset", sampleOffset)
                  .put("geodeticLatOffset", geodeticLatOffset)
                  .put("geodeticLonOffset", geodeticLonOffset)
                  .put("geodeticHeightOffset", geodeticHeightOffset)
                  .put("lineScale", lineScale)
                  .put("sampleScale", sampleScale)
                  .put("geodeticLatScale", geodeticLatScale)
                  .put("geodeticLonScale", geodeticLonScale)
                  .put("geodeticHeightScale", geodeticHeightScale)
                  .put("lineNumeratorCoeff", lineNumeratorCoeff)
                  .put("lineDenominatorCoeff", lineDenominatorCoeff)
                  .put("sampleNumeratorCoeff", sampleNumeratorCoeff)
                  .put("sampleDenomoniatorCoeff", sampleDenomoniatorCoeff)
                  .build();
            
            values.entrySet().stream()
            .filter(v->v.getValue().isPresent())
            .map(v-> {
               String stringValue;
                  Object value = v.getValue().get();
                  if (value.getClass().isArray()) {
                     stringValue = Arrays.toString((double[])value);
                  } else {
                     stringValue = value.toString();
                  }
                  return String.join(" == ", v.getKey(), stringValue);
            })
            .forEach(s-> pw.println(s));
         }
         result = sw.toString();
      } catch (IOException e1) {
         e1.printStackTrace();
      }
      return result;
   }

    /**
     * @return The name of this TRE (tagged record extension), "RPC00B"
     */
   public String getTagName() {
      return TAG_NAME;
   }

   public Optional<Boolean> getSuccess() {
      return success;
   }

   public Optional<Double> getErrorBias() {
      return errorBias;
   }

   public Optional<Double> getErrorRand() {
      return errorRand;
   }

   public Optional<Integer> getLineOffset() {
      return lineOffset;
   }

   public Optional<Integer> getSampleOffset() {
      return sampleOffset;
   }

   public Optional<Double> getGeodeticLatOffset() {
      return geodeticLatOffset;
   }

   public Optional<Double> getGeodeticLonOffset() {
      return geodeticLonOffset;
   }

   public Optional<Integer> getGeodeticHeightOffset() {
      return geodeticHeightOffset;
   }

   public Optional<Integer> getLineScale() {
      return lineScale;
   }

   public Optional<Integer> getSampleScale() {
      return sampleScale;
   }

   public Optional<Double> getGeodeticLatScale() {
      return geodeticLatScale;
   }

   public Optional<Double> getGeodeticLonScale() {
      return geodeticLonScale;
   }

   public Optional<Integer> getGeodeticHeightScale() {
      return geodeticHeightScale;
   }

   public Optional<double[]> getLineNumeratorCoeff() {
      return lineNumeratorCoeff;
   }

   public Optional<double[]> getLineDenominatorCoeff() {
      return lineDenominatorCoeff;
   }

   public Optional<double[]> getSampleNumeratorCoeff() {
      return sampleNumeratorCoeff;
   }

   public Optional<double[]> getSampleDenomoniatorCoeff() {
      return sampleDenomoniatorCoeff;
   }
}
