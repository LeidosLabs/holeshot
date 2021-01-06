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

import static org.lwjgl.opengl.GL11.glViewport;

import java.awt.Dimension;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;

import org.image.common.util.CloseableUtils;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;

import com.leidoslabs.holeshot.elt.ELTDisplayContext;
import com.leidoslabs.holeshot.elt.ImageFramebuffer;
import com.leidoslabs.holeshot.elt.Interpolation;
import com.leidoslabs.holeshot.elt.gpuimage.GLInternalFormat;
import com.leidoslabs.holeshot.elt.imagechain.Framebuffer;
import com.leidoslabs.holeshot.elt.imagechain.ImageChain;
import com.leidoslabs.holeshot.elt.imageop.Mosaic;
import com.leidoslabs.holeshot.elt.imageop.RawImage;
import com.leidoslabs.holeshot.elt.imageop.ToneTransferCurve;
import com.leidoslabs.holeshot.elt.tileserver.TileserverImage;
import com.leidoslabs.holeshot.elt.utils.GeometryUtils;
import com.leidoslabs.holeshot.elt.viewport.ImageWorld;
import com.leidoslabs.holeshot.elt.viewport.ViewportImageListener;

public class OglMosaic extends OglAbstractImageOpAccumulator implements Mosaic {

	private final Interpolation DEFAULT_INTERPOLATION = Interpolation.CATMULL;
	private final ImageWorld imageWorld;
	private final ELTDisplayContext eltDisplayContext;
	private Framebuffer mosaicFramebuffer;
	private List<ImageFramebuffer> images;
	private final Consumer<Mosaic> initializeCallback;
	private final List<ViewportImageListener> imageListeners;
	private ImageFramebuffer lastCenterImage;
	private Interpolation interpolation;
	private boolean framebufferExternallyManaged;

	//	private QuadDrawVAO quadDrawVAO;


	public OglMosaic(ImageWorld imageWorld, ELTDisplayContext displayContext, Consumer<Mosaic> initializeCallback ) {
		super();
		this.imageWorld = imageWorld;
		this.eltDisplayContext = displayContext;
		this.images = new ArrayList<>();
		this.initializeCallback = initializeCallback;
		this.imageListeners = new ArrayList<ViewportImageListener>();
		this.lastCenterImage = null;
		this.interpolation = DEFAULT_INTERPOLATION;
		this.framebufferExternallyManaged = false;
	}

	@Override
	public void addImage(ImageFramebuffer image) {
		image.setInterpolation(interpolation);
		this.images.add(image);
	}

	@Override
	public Framebuffer getResultFramebuffer() {
		return this.mosaicFramebuffer;
	}

	public void enableDRA(boolean enabled) {
		images.stream().forEach(i->i.enableDRA(enabled));
	}
	@Override
	public boolean isDRAEnabled() {
		return images.stream().allMatch(i->i.isDRAEnabled());
	}
	private Dimension getSize() {
		return mosaicFramebuffer.getSize();
	}

	private int getWidth() {
		return getSize().width;
	}

	private int getHeight() {
		return getSize().height;
	}

	private void doRender(float zOrder, ImageChain imageChain) throws Exception {
		OglToneTransferCurve ttc = imageChain.getLastImageOp(OglToneTransferCurve.class);
		ttc.setResultFramebuffer(mosaicFramebuffer);
		ttc.setZOrder(zOrder);

		imageChain.render(null);
	}

	public void setTTC(Consumer<? super ToneTransferCurve> action) {
		images.stream().map(i->i.getImageChain().getFirstImageOpOfType(ToneTransferCurve.class)).forEach(action);
	}

	@Override
	protected void doRender() throws Exception {
		try {
			initialize();

			if (mosaicFramebuffer != null) {
				glViewport(0, 0,  getWidth(), getHeight());

				Geometry uncoveredViewport = imageWorld.getProjectedViewport();
				Point viewportProjectedCenter = uncoveredViewport.getCentroid();

				ImageFramebuffer newCenterImage = null;
				Iterator<ImageFramebuffer> fbIter = images.iterator();
				// TODO: Need a faster way of getting to the relevant images for the viewport.
				int zOrder = 0;
				int numberImages = images.size();
				while (fbIter.hasNext() && !uncoveredViewport.isEmpty()) {
					final ImageFramebuffer imageFB = fbIter.next();
					if (imageFB.isEnabled()) {
						final TileserverImage image = imageFB.getImage();
						final Polygon imageFootprint = imageWorld.getProjectedImage(image);

						final Geometry imageContribution = imageFootprint.intersection(uncoveredViewport);
						if (!imageContribution.isEmpty()) {
							if (viewportProjectedCenter.within(imageContribution)) {
								newCenterImage = imageFB;
							}
							uncoveredViewport = uncoveredViewport.difference(imageFootprint);
							doRender((float)(zOrder++)/(float)numberImages, imageFB.getImageChain());
						}
					}
				}
				notifyImageListeners(newCenterImage);
			}
		} catch (URISyntaxException e) {
			throw new IOException(e);
		} finally {
		}

	}

	/**
	 * @param newCenterImage
	 */
	private void notifyImageListeners(ImageFramebuffer newCenterImage) {
		if (newCenterImage != lastCenterImage) {
			imageListeners.forEach(c->c.centerImageChanged(newCenterImage == null ? null : newCenterImage.getImage()));
			lastCenterImage = newCenterImage;
		}
	}
	
	public void setResultFramebuffer(Framebuffer framebuffer) {
		this.mosaicFramebuffer = framebuffer;
		this.framebufferExternallyManaged = true;
	}

	private void initialize() throws Exception {
		final Dimension viewportDimensions = getViewportDimensions();

		if (!framebufferExternallyManaged) {
			if (mosaicFramebuffer == null) {
				mosaicFramebuffer = new Framebuffer(getViewportDimensions(), GLInternalFormat.GlInternalFormatRGBA32F, eltDisplayContext);
			} else if (!mosaicFramebuffer.getSize().equals(viewportDimensions)) {
				mosaicFramebuffer.reset(viewportDimensions);
			}
			mosaicFramebuffer.clearBuffer(0.0f, 0.0f, 0.0f, 0.0f);
		}
		initializeCallback.accept(this);
	}

	@Override
	public void close() throws IOException {
		super.close();
		CloseableUtils.close(mosaicFramebuffer);
	}
	@Override
	public void reset() {
		images.stream().forEach(i->i.getImageChain().reset());
		if (mosaicFramebuffer != null) {
			clearFramebuffer(0.0f, 0.0f, 0.0f, 0.0f, mosaicFramebuffer);
		}
	}

	@Override
	public Dimension getViewportDimensions() {
		return GeometryUtils.toDimension(imageWorld.getCurrentViewport());
	}

	@Override
	public TileserverImage[] getImages() {
		return images.stream().map(i->i.getImage()).toArray(TileserverImage[]::new);
	}

	@Override
	public void addViewportImageListener(ViewportImageListener imageListener) {
		imageListeners.add(imageListener);
	}

	@Override
	public void setMultiImageMode() {
		images.forEach(i->i.setEnabled(true));
	}
	@Override
	public void setSingleImageMode(TileserverImage singleImage) {
		images.forEach(i->i.setEnabled(singleImage == i.getImage()));
	}

	@Override
	public Interpolation getInterpolation() {
		return interpolation;
	}

	@Override
	public void setInterpolation(Interpolation interpolation) {
		this.interpolation = interpolation;
		images.forEach(i->i.setInterpolation(interpolation));
	}

	@Override
	public void clearAllImages() {
		images.clear();
	}
	
	@Override
	public boolean isFullyRendered() {
		return images.stream().allMatch(i->i.isFullyRendered());
	}
}
