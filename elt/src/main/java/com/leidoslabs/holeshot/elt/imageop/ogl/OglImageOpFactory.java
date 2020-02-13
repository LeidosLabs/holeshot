/*
 * Licensed to Leidos, Inc. under one or more contributor license agreements.  
 * See the NOTICE file distributed with this work for additional information regarding copyright ownership.
 * Leidos, Inc. licenses this file to You under the Apache License, Version 2.0
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

package com.leidoslabs.holeshot.elt.imageop.ogl;

import java.io.IOException;

import com.leidoslabs.holeshot.elt.gpuimage.HistogramType;
import com.leidoslabs.holeshot.elt.imageop.*;

/**
 * Factory class for OpenGL Image operations
 */
public class OglImageOpFactory implements ImageOpFactory {
   public OglImageOpFactory() {
   }

   @Override
   public CumulativeHistogram cumulativeHistogram() {
      return new OglCumulativeHistogram();
   }

   @Override
   public DynamicRangeAdjust dynamicRangeAdjust() {
      return new OglDynamicRangeAdjust();
   }

   @Override
   public Histogram histogram(HistogramType histogramType) {
      return new OglHistogram(histogramType);
   }

   @Override
   public RawImage rawImage(boolean progressiveRender) {
      return new OglRawImage(progressiveRender);
   }

   @Override
   public SummedArea summedArea() {
      return new OglSummedArea();
   }

   @Override
   public DRAParameters draParameters(boolean phasedDRA) {
      return new OglDRAParameters(phasedDRA);
   }

   @Override
   public ToneTransferCurve toneTransferCurve() throws IOException {
      return new OglToneTransferCurve();
   }
}
