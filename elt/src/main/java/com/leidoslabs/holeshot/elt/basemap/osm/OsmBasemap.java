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
package com.leidoslabs.holeshot.elt.basemap.osm;

import static org.lwjgl.opengl.GL11.glViewport;

import java.awt.Dimension;
import java.io.IOException;
import java.util.Iterator;
import java.util.Set;

import org.joml.Vector2ic;
import org.locationtech.jts.geom.Polygon;

import com.leidoslabs.holeshot.elt.ELTDisplayContext;
import com.leidoslabs.holeshot.elt.basemap.Basemap;
import com.leidoslabs.holeshot.elt.coord.MapScale;
import com.leidoslabs.holeshot.elt.gpuimage.GLInternalFormat;
import com.leidoslabs.holeshot.elt.imagechain.Framebuffer;
import com.leidoslabs.holeshot.elt.viewport.ImageProjection;
import com.leidoslabs.holeshot.elt.viewport.ImageWorld;

/**
 * @author robertsrg
 *
 */
public class OsmBasemap extends Basemap {
	private boolean isFullyRendered;
	private Framebuffer resultFB;
	/**
	 * @param imageWorld
	 * @param eltDisplayContext
	 */
	public OsmBasemap(ImageWorld imageWorld, ELTDisplayContext eltDisplayContext) {
		super(imageWorld, eltDisplayContext);
	}
	
	@Override
	public void render(Void data) throws Exception {
		updateFramebuffer();
		resultFB.clearBuffer(0.0f, 0.0f, 0.0f, 1.0f);
		
		isFullyRendered = true;
		
		if (!(getImageWorld().getProjection() instanceof ImageProjection)) {
			Set<OSMTile> tilesToRender = getTilesToRender();
			Iterator<OSMTile> iter = tilesToRender.iterator();
			resultFB.bind();

			Vector2ic screenSize = getImageWorld().getScreenSize();
			glViewport(0, 0, screenSize.x(), screenSize.y());
			while (iter.hasNext()) {
				OSMTile tile = iter.next();

				boolean rendered = false;
				for (OSMTile ancestor = tile; !rendered && ancestor != null; ancestor = ancestor.getParentTile()) {
					OSMTileTexture texture = OSMTileTexture.getTexture(getELTDisplayContext(), ancestor);
					if (texture != null) {
						if (ancestor != tile) {
							isFullyRendered = false;
						}
						rendered = true;
						texture.draw(getImageWorld(), tile);
					} 
				}
			}
			resultFB.unbind();
		}
	}
	
	public boolean isFullyRendered() {
		return isFullyRendered;
	}
	
	public Framebuffer getResultFramebuffer() {
		return resultFB;
	}

	/**
	 * @param world
	 * @return
	 */
	private Set<OSMTile> getTilesToRender() {
		final ImageWorld world = getImageWorld();
		return OSMTile.getOverlappingTiles(world);
	}
	
	private void updateFramebuffer() throws Exception {
		final Dimension viewportDimensions = getViewportDimensions();
		final GLInternalFormat internalFormat = GLInternalFormat.GlInternalFormatRGBA8;

		if (resultFB == null) {
			resultFB = new Framebuffer(viewportDimensions, internalFormat, getELTDisplayContext());
		} else {
			resultFB.reset(viewportDimensions, internalFormat);
		}
	}
	

	
}
