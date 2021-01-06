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

package com.leidoslabs.holeshot.elt.imageop;

import java.io.IOException;
import java.util.function.Consumer;

import com.leidoslabs.holeshot.elt.ELTDisplayContext;
import com.leidoslabs.holeshot.elt.Interpolation;
import com.leidoslabs.holeshot.elt.gpuimage.HistogramType;
import com.leidoslabs.holeshot.elt.tileserver.TileserverImage;
import com.leidoslabs.holeshot.elt.viewport.ImageWorld;

/**
 * Factory pattern for ImageOp
 */
public interface ImageOpFactory {
  public CumulativeHistogram cumulativeHistogram();
  public DynamicRangeAdjust dynamicRangeAdjust();
  public Histogram histogram(HistogramType histogramType);
  public RawImage rawImage(TileserverImage image, boolean progressiveRender);
  public RawImage rawImage(boolean progressiveRender);
  public Interpolated interpolated(Interpolation interpolation);
  public SummedArea summedArea();
  public DRAParameters draParameters(boolean phasedDRA);
  public ToneTransferCurve toneTransferCurve() throws IOException;
  
  public Mosaic mosaic(ImageWorld imageWorld, ELTDisplayContext displayContext, Consumer<Mosaic> initializeCallback);
}
