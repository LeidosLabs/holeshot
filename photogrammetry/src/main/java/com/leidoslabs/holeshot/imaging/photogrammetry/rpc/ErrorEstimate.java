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
 * Representing the RPC Error associated with a camera model (RPC_ERR and RPC_BIAS)
 * Created by parrise on 2/22/17.
 */
public class ErrorEstimate implements Cacheable {

   private double bias;
   private double random;

    /**
     * Create a new ErrorEstimate from image metadata
     * @param bias The RMS bias error in meters per horizontal axis of all points in the image (negative if unknown)
     * @param random The RMS random error in meters per horizontal axis of each point in the image (negative if unknown)
     */
   public ErrorEstimate(double bias,double random) {
      this.bias = bias;
      this.random = random;
   }

    /**
     * Create a new ErrorEstimate with 0.0 bias error and 0.0 random error
     */
   public ErrorEstimate() {
      this(0.0, 0.0);
   }

   public long getSizeInBytes() {
      return CacheableUtil.getDefault().getSizeInBytesForObjects(bias, random);
   }

    /**
     * @return bias The RMS bias error in meters per horizontal axis of all points in the image (negative if unknown)
     */
   public double getBias() {
      return bias;
   }

    /**
     * @return The RMS random error in meters per horizontal axis of each point in the image (negative if unknown)
     */
   public double getRandom() {
      return random;
   }

    /**
     * @param bias The RMS bias error in meters per horizontal axis of all points in the image (negative if unknown)
     */
   public void setBias(double bias) {
      this.bias = bias;
   }

    /**
     * @param random The RMS bias error in meters per horizontal axis of all points in the image (negative if unknown)
     */
   public void setRandom(double random) {
      this.random = random;
   }
}
