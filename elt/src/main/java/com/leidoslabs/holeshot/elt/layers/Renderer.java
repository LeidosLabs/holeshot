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

package com.leidoslabs.holeshot.elt.layers;

import java.awt.Dimension;
import java.io.Closeable;
import java.io.IOException;

import org.image.common.util.CloseableUtils;

import com.leidoslabs.holeshot.elt.ELTDisplayContext;
import com.leidoslabs.holeshot.elt.imagechain.Framebuffer;
import com.leidoslabs.holeshot.elt.utils.GeometryUtils;
import com.leidoslabs.holeshot.elt.viewport.ImageWorld;

/**
 * Abstract class representing a Renderer of some data. Provides a resultant
 * FrameBuffer.
 * Known implementations: ImageChain, PLacemarkRenderer, PolygonRenderer
 * @param <T>
 */
public abstract class Renderer<T> implements Closeable {

	private final ImageWorld imageWorld;
	private final ELTDisplayContext eltDisplayContext;
	private Framebuffer fb;

	/**
	 * Constructor
	 * @param imageWorld viewport model for ELTCanvas
	 * @param eltDisplayContext 
	 */
	public Renderer(ImageWorld imageWorld, ELTDisplayContext eltDisplayContext) {
		this.imageWorld = imageWorld;
		this.eltDisplayContext = eltDisplayContext;
	}

	public ImageWorld getImageWorld() {
		return this.imageWorld;
	}

	public Dimension getViewportDimensions() {
		return GeometryUtils.toDimension(imageWorld.getCurrentViewport());
	}

	public void setFramebuffer(Framebuffer fb) {
		this.fb = fb;
	}

	/**
	 * @return Resultant FrameBuffer
	 */
	public Framebuffer getResultFramebuffer() {
		return fb;
	}
	public ELTDisplayContext getELTDisplayContext() {
		return eltDisplayContext;
	}

	public abstract void render(T data) throws Exception;

	public boolean isFullyRendered() {
		return true;
	}

	@Override
	public void close() throws IOException {
		CloseableUtils.close(fb);
	}
}
