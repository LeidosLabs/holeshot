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
import java.util.function.Consumer;

import com.leidoslabs.holeshot.elt.ELTDisplayContext;
import com.leidoslabs.holeshot.elt.Interpolation;
import com.leidoslabs.holeshot.elt.gpuimage.HistogramType;
import com.leidoslabs.holeshot.elt.imageop.CumulativeHistogram;
import com.leidoslabs.holeshot.elt.imageop.DRAParameters;
import com.leidoslabs.holeshot.elt.imageop.DynamicRangeAdjust;
import com.leidoslabs.holeshot.elt.imageop.Histogram;
import com.leidoslabs.holeshot.elt.imageop.ImageOpFactory;
import com.leidoslabs.holeshot.elt.imageop.Interpolated;
import com.leidoslabs.holeshot.elt.imageop.Mosaic;
import com.leidoslabs.holeshot.elt.imageop.RawImage;
import com.leidoslabs.holeshot.elt.imageop.SummedArea;
import com.leidoslabs.holeshot.elt.imageop.ToneTransferCurve;
import com.leidoslabs.holeshot.elt.tileserver.TileserverImage;
import com.leidoslabs.holeshot.elt.viewport.ImageWorld;

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
	public RawImage rawImage(TileserverImage image, boolean progressiveRender) {	
		return new OglRawImage(image, progressiveRender);
	}

	@Override
	public RawImage rawImage(boolean progressiveRender) {
		return new OglRawImage(progressiveRender);
	}
	
	@Override
	public Interpolated interpolated(Interpolation interpolation) {
		return new OglInterpolated(interpolation);
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

	@Override
	public Mosaic mosaic(ImageWorld imageWorld, ELTDisplayContext displayContext, Consumer<Mosaic> initializeCallback) {
		return new OglMosaic(imageWorld, displayContext, initializeCallback);
	}   
}
