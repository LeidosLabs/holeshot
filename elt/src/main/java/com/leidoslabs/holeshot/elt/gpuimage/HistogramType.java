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

package com.leidoslabs.holeshot.elt.gpuimage;

import java.util.Arrays;
import java.util.List;

/**
 * Enum for histogram type, includes compatible vertex shaders
 */
public enum HistogramType {
  RED(Arrays.asList(Shaders.RED_SAMPLING_SHADER)),
  GREEN(Arrays.asList(Shaders.GREEN_SAMPLING_SHADER)),
  BLUE(Arrays.asList(Shaders.BLUE_SAMPLING_SHADER)),
//  LUMINANCE(Arrays.asList(Shaders.LUMINANCE_SAMPLING_SHADER)),
  RGB(Arrays.asList(Shaders.RED_SAMPLING_SHADER,
                    Shaders.GREEN_SAMPLING_SHADER,
                    Shaders.BLUE_SAMPLING_SHADER));

  private List<String> vertexShaders;

  private HistogramType(List<String> vertexShaders) {
    this.vertexShaders = vertexShaders;
  }

  public List<String> getVertexShaders() {
    return vertexShaders;
  }

  public static class Shaders {
    // Names of the shader files, without extensions
    public static final String PASSTHROUGH_VERTEX_SHADER = "PassthroughVertexShader.vp";
    public static final String ALL_FOR_ONE_SHADER = "AllForOneShader.vp";
    public static final String EFIRST_SHADER = "eFirstShader.fp";
    public static final String EMIN_SHADER = "eMinShader.fp";
    public static final String EMAX_SHADER = "eMaxShader.fp";
    public static final String ELAST_SHADER = "eLastShader.fp";
//    public static final String PASSTHROUGH_GEOMETRY_SHADER = "PassthroughGeometryShader.gp";
//    public static final String CUMULATIVE_GEOMETRY_SHADER = "HistogramCumulativeShader.gp";
    public static final String RED_SAMPLING_SHADER = "HistogramRedSampling.vp";
    public static final String GREEN_SAMPLING_SHADER = "HistogramGreenSampling.vp";
    public static final String BLUE_SAMPLING_SHADER = "HistogramBlueSampling.vp";
//    public static final String LUMINANCE_SAMPLING_SHADER = "HistogramLuminanceSampling";
    public static final String HISTOGRAM_ACCUMULATION_SHADER = "HistogramAccumulation_GL.fp";
    public static final String CUMULATIVE_HISTOGRAM_SHADER = "CumulativeHistogramShader.fp";
    public static final String SUMMED_AREA_HORIZONTAL_PHASE_SHADER = "SummedAreaHorizontalPhase.fp";
    public static final String SUMMED_AREA_VERTICAL_PHASE_SHADER = "SummedAreaVerticalPhase.fp";
    public static final String EQUALIZATION_SHADER = "EqualizationShader.fp";
    public static final String DRA_PARAMETERS_SHADER = "DRAParametersShader.fp";
    public static final String TTC_SHADER = "ToneTransferCurveShader.fp";
  }
};