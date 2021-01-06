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

package com.leidoslabs.holeshot.elt.imagechain;

import java.io.Closeable;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.image.common.util.CloseableUtils;

import com.leidoslabs.holeshot.elt.ELTDisplayContext;
import com.leidoslabs.holeshot.elt.Interpolation;
import com.leidoslabs.holeshot.elt.gpuimage.HistogramType;
import com.leidoslabs.holeshot.elt.imageop.ImageOpFactory;
import com.leidoslabs.holeshot.elt.imageop.ImageOpPrimitive;
import com.leidoslabs.holeshot.elt.imageop.RawImage;
import com.leidoslabs.holeshot.elt.layers.Renderer;
import com.leidoslabs.holeshot.elt.tileserver.TileserverImage;
import com.leidoslabs.holeshot.elt.utils.ReverseListSpliterator;
import com.leidoslabs.holeshot.elt.viewport.ImageWorld;

/**
 * The ImageChain is an ordered sequence of events to manipulate the framebuffer prior to rendering. Each subsequent step
 * can access the results of previous steps in order to build upon the output.
 */
public class ImageChain extends Renderer<Void> implements Closeable {
	private final LinkedList<ImageOpPrimitive> imageOps;

	private ImageChain(LinkedList<ImageOpPrimitive> imageOps, ImageWorld imageWorld, ELTDisplayContext eltDisplayContext) {
		super(imageWorld, eltDisplayContext);
		this.imageOps = imageOps;
		this.imageOps.stream().forEach(ic->ic.setImageChain(this));
	}

	public List<ImageOpPrimitive> getImageOps() {
		return imageOps;
	}

	public TileserverImage getImage() {
		TileserverImage image = null;

		RawImage rawImageOp = getFirstImageOpOfType(RawImage.class);
		if (rawImageOp != null) {
			image = rawImageOp.getImage();
		}
		return image;
	}

	public <T extends ImageOpPrimitive> T getFirstImageOpOfType(Class<T> clazz) {
		return imageOps.stream().filter(o->clazz.isInstance(o)).map(o->(T)o).findFirst().orElse(null);
	}

	public Stream<ImageOpPrimitive> getPreviousImageOpsAsStream(ImageOpPrimitive currentImageOp) {
		return StreamSupport.stream(new ReverseListSpliterator<ImageOpPrimitive>(imageOps, currentImageOp), false);
	}

	public Stream<ImageOpPrimitive> getPreviousImageOpsAsStream() {
		return StreamSupport.stream(new ReverseListSpliterator<ImageOpPrimitive>(imageOps), false);
	}

	@SuppressWarnings("unchecked")
	public <T extends ImageOpPrimitive> Stream<T> getPreviousImageOpsOfTypeAsStream(ImageOpPrimitive currentImageOp, Class<T> previousImageOpClass) {
		return getPreviousImageOpsAsStream(currentImageOp).filter(i->previousImageOpClass.isInstance(i)).map(i->(T)i);
	}

	@SuppressWarnings("unchecked")
	public <T extends ImageOpPrimitive> Stream<T> getPreviousImageOpsOfTypeAsStream(Class<T> previousImageOpClass) {
		return getPreviousImageOpsAsStream().filter(i->previousImageOpClass.isInstance(i)).map(i->(T)i);
	}

	public <T extends ImageOpPrimitive> List<T> getPreviousImageOps(ImageOpPrimitive currentImageOp, Class<T> previousImageOpClass) {
		return getPreviousImageOpsOfTypeAsStream(currentImageOp, previousImageOpClass).collect(Collectors.toList());
	}

	/**
	 * Returns the ImageOp instance of previousImageOpClass type that occurred most recently in the image chain prior to
	 * current imageop.
	 * @param currentImageOp The place in the chain to start searching (Typically the current step, i.e. 'this')
	 * @param previousImageOpClass The imageop type you are looking for
	 * @param <T> The type of the returned class, will be equal to the type of the previous ImageOp
	 * @return The first found instance, else null if the operation isn't found earlier in the chain
	 */
	public <T extends ImageOpPrimitive> T getPreviousImageOp(ImageOpPrimitive currentImageOp, Class<T> previousImageOpClass) {
		return getPreviousImageOpsOfTypeAsStream(currentImageOp, previousImageOpClass).findFirst().orElse(null);
	}

	public <T extends ImageOpPrimitive> T getLastImageOp(Class<T> previousImageOpClass) {
		return getPreviousImageOpsOfTypeAsStream(previousImageOpClass).findFirst().orElse(null);
	}

	public ImageOpPrimitive getLastImageOp() {
		return this.imageOps.getLast();
	}

	/**
	 * Evaluate the image chain by calling the render() function on each ImageOp in the chain in sequence.
	 * @throws IOException
	 */
	@Override
	public void render(Void ignored) throws Exception {
		for (ImageOpPrimitive imageOp: imageOps) {
			imageOp.render();
		}
	}

	/**
	 * Return the last non-null result framebuffer from the image chain, if one exists. Null if none exists.
	 * @return Final framebuffer output of image chain
	 */
	public Framebuffer getResultFramebuffer() {
		return StreamSupport.stream(new ReverseListSpliterator<ImageOpPrimitive>(imageOps), false)
				.map(i->i.getResultFramebuffer())
				.filter(b->b != null)
				.findFirst().orElse(null);
	}

	/**
	 * Builder pattern for performing a sequence of ImageChain operations
	 */
	public static class ImageChainBuilder {
		private final LinkedList<ImageOpPrimitive> imageOps;
		private final ImageOpFactory imageOpFactory;
		private final ImageWorld imageWorld;
		private final ELTDisplayContext eltDisplayContext;

		public ImageChainBuilder(ImageOpFactory imageOpFactory, ImageWorld imageWorld, ELTDisplayContext eltDisplayContext) {
			this.imageOpFactory = imageOpFactory;
			this.imageOps = new LinkedList<ImageOpPrimitive>();
			this.imageWorld = imageWorld;
			this.eltDisplayContext = eltDisplayContext;
		}

		public ImageChain build() {
			return new ImageChain(this.imageOps, this.imageWorld, this.eltDisplayContext);
		}

		public ImageChainBuilder rawImage(TileserverImage image, boolean progressiveRender) {
			this.imageOps.add(imageOpFactory.rawImage(image, progressiveRender));
			return this;
		}
		public ImageChainBuilder rawImage(boolean progressiveRender) {
			this.imageOps.add(imageOpFactory.rawImage(progressiveRender));
			return this;
		}
		public ImageChainBuilder interpolated(Interpolation interpolation) {
			this.imageOps.add(imageOpFactory.interpolated(interpolation));
			return this;
		}
		public ImageChainBuilder cumulativeHistogram() {
			this.imageOps.add(imageOpFactory.cumulativeHistogram());
			return this;
		}
		public ImageChainBuilder dynamicRangeAdjust() {
			this.imageOps.add(imageOpFactory.dynamicRangeAdjust());
			return this;
		}
		public ImageChainBuilder histogram(HistogramType histogramType) {
			this.imageOps.add(imageOpFactory.histogram(histogramType));
			return this;
		}
		public ImageChainBuilder summedArea() {
			this.imageOps.add(imageOpFactory.summedArea());
			return this;
		}
		public ImageChainBuilder draParameters(boolean phasedDRA) {
			this.imageOps.add(imageOpFactory.draParameters(phasedDRA));
			return this;
		}
		public ImageChainBuilder toneTransferCurve() throws IOException {
			this.imageOps.add(imageOpFactory.toneTransferCurve());
			return this;
		}
	}

	@Override
	public boolean isFullyRendered() {
		return !(imageOps.stream().filter(imageOp-> !imageOp.isFullyRendered()).findFirst().isPresent());
	}

	@Override
	public void close() throws IOException {
		super.close();
		CloseableUtils.close(imageOps.stream().toArray(ImageOpPrimitive[]::new));
	}

	public void reset() {
		this.imageOps.forEach(i->i.reset());
	}
}
