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

import java.util.function.Consumer;

import com.leidoslabs.holeshot.elt.ImageFramebuffer;
import com.leidoslabs.holeshot.elt.Interpolation;
import com.leidoslabs.holeshot.elt.imagechain.Framebuffer;
import com.leidoslabs.holeshot.elt.tileserver.TileserverImage;
import com.leidoslabs.holeshot.elt.viewport.ViewportImageListener;

public interface Mosaic extends ImageOpAccumulator {
	
	public void addImage(ImageFramebuffer image);
	public void clearAllImages();
	public void enableDRA(boolean enabled);
	public boolean isDRAEnabled();
	public void setTTC(Consumer<? super ToneTransferCurve> action);
	public TileserverImage[] getImages();
	public void addViewportImageListener(ViewportImageListener imageListener);
	public void setMultiImageMode();
	public void setSingleImageMode(TileserverImage singleImage);
	
	public Interpolation getInterpolation();
	public void setInterpolation(Interpolation interpolation);
	public void setResultFramebuffer(Framebuffer framebuffer);
}
