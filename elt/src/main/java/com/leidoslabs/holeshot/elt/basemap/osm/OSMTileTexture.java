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

import static org.lwjgl.opengl.GL11.GL_LINEAR;
import static org.lwjgl.opengl.GL11.GL_NEAREST;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_2D;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_MAG_FILTER;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_MIN_FILTER;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_WRAP_S;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_WRAP_T;
import static org.lwjgl.opengl.GL11.glBindTexture;
import static org.lwjgl.opengl.GL11.glTexParameteri;
import static org.lwjgl.opengl.GL12.GL_CLAMP_TO_EDGE;
import static org.lwjgl.opengl.GL13.GL_TEXTURE0;
import static org.lwjgl.opengl.GL13.glActiveTexture;
import static org.lwjgl.opengl.GL20.glUniform1i;
import static org.lwjgl.opengl.GL20.glUniformMatrix4fv;
import static org.lwjgl.opengl.GL20.glUseProgram;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.FloatBuffer;
import java.util.concurrent.ExecutionException;

import org.image.common.util.CloseableUtils;
import org.joml.Matrix4dc;
import org.joml.Vector2d;
import org.joml.Vector2dc;
import org.locationtech.jts.geom.Coordinate;
import org.lwjgl.BufferUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.leidoslabs.holeshot.elt.ELTDisplayContext;
import com.leidoslabs.holeshot.elt.ELTImageTexture;
import com.leidoslabs.holeshot.elt.gpuimage.GLInternalFormat;
import com.leidoslabs.holeshot.elt.gpuimage.QuadDrawVAO;
import com.leidoslabs.holeshot.elt.gpuimage.ShaderProgram;
import com.leidoslabs.holeshot.elt.gpuimage.Texture;
import com.leidoslabs.holeshot.elt.gpuimage.TextureCache;
import com.leidoslabs.holeshot.elt.gpuimage.TextureTracker.TextureID;
import com.leidoslabs.holeshot.elt.tileserver.CoreImage;
import com.leidoslabs.holeshot.elt.utils.KeyedLIFOExecutorService;
import com.leidoslabs.holeshot.elt.viewport.ImageWorld;

public class OSMTileTexture extends Texture {
	private static final int EXECUTOR_POOL_SIZE = Integer.parseInt(System.getProperty("java.util.concurrent.ForkJoinPool.common.parallelism", "4"));
	private static final Logger LOGGER = LoggerFactory.getLogger(OSMTileTexture.class);
	private static final KeyedLIFOExecutorService executorService = KeyedLIFOExecutorService.newFixedThreadPool(EXECUTOR_POOL_SIZE);
	private static final String SHADER_KEY = OSMTileTexture.class.getName();

	private final OSMTile tile;
	private final FloatBuffer mvpBuffer;
	private final TextureID textureID;
	
	private OSMTileTexture(OSMTile tile, GLInternalFormat internalFormat,
			BufferedImage textureData, ELTDisplayContext eltDisplayContext) throws IOException, InterruptedException, ExecutionException {
		super(tile.getSize(), internalFormat, GL_LINEAR, GL_CLAMP_TO_EDGE, eltDisplayContext, textureData);

		this.textureID = eltDisplayContext.getTextureTracker().createTextureID();
		this.tile = tile;
		this.mvpBuffer = BufferUtils.createFloatBuffer(16);
	}

	public OSMTile getTile() {
		return tile;
	}
	
	private static BufferedImage imageToTextureData(OSMTile tile) throws IOException {
		final CoreImage coreImage = OSMTileCache.getInstance().getTile(tile, false, false);
		return coreImage == null ? null : coreImage.getBufferedImage();
	}

	private static BufferedImage getTextureData(OSMTile tile) throws IOException {
		BufferedImage textureData = imageToTextureData(tile);
		return textureData;
	}

	/**
	 * Retrieves image texture. If not in Cache, retrieves texture data from 
	 * Tile Image, and updates cache key.
	 * @param eltDisplayContext
	 * @param tile
	 * @param imageWorld
	 * @return Image Texture, retrieved by tile key and image world hash.
	 * @throws IOException
	 */
	public static OSMTileTexture getTexture(ELTDisplayContext eltDisplayContext, OSMTile tile)
			throws IOException {
		OSMTileTexture texture;
		final String textureKey = String.format("%d-%d-%d-%d", (int)tile.getZoom().getZoom(), tile.getXTile(), tile.getYTile(), eltDisplayContext.getContextHandle());

		synchronized (textureKey.intern()) {

			texture = (OSMTileTexture)TextureCache.getInstance().getTexture(textureKey);

			if (texture == null && !executorService.isInProcess(textureKey)) {
				executorService.submit(textureKey, () -> {
					try {
						// Necessary when rendering multiple windows/OpenGL Contexts
						BufferedImage textureData = getTextureData(tile);
						if (textureData != null) {
							eltDisplayContext.asyncExec(() -> {
								try {
									synchronized (textureKey.intern()) {
										if (!TextureCache.getInstance().containsKey(textureKey)) {
											eltDisplayContext.setContextThread();
											OSMTileTexture newTexture = new OSMTileTexture(tile,
													getInternalFormat(tile), textureData, eltDisplayContext);
											TextureCache.getInstance().setTexture(textureKey, newTexture);
										}
									}
								} catch (Throwable e) {
									e.printStackTrace();
								}
							});
						}
					} catch (Throwable e) {
						e.printStackTrace();
					}
				});
			}
		}
		return texture;
	}


	private void internalDraw(ImageWorld imageWorld, OSMTile areaToRender) throws Exception {
		
		textureID.lock();
		try {
			// vertices in image space

			// ll, lr, ur, ul, ll
			Coordinate[] coordinates = areaToRender.getBoundingBoxProjected().getCoordinates();

			// These Coordinates define a triangle path for a quad across the area to render.
			// They are sensitive to changes into the order that ImageSpaceBounds is defined.
			final double PIXEL = 0.0;
			final double z = -1.0;
			double[][] vertices = new double[][] {
				{ coordinates[3].x, coordinates[3].y + PIXEL, z }, // ul
				{ coordinates[0].x, coordinates[0].y, z }, // ll
				{ coordinates[2].x + PIXEL, coordinates[2].y + PIXEL, z }, // ur
				{ coordinates[1].x + PIXEL, coordinates[1].y, z }  // lr
			};


			double zoomDiff = areaToRender.getZoom().getZoom() - tile.getZoom().getZoom();
			Vector2dc tileSize = new Vector2d(Math.pow(0.5, zoomDiff));
			Vector2dc zoomDiffScale = new Vector2d(Math.pow(2.0,  zoomDiff));
			Vector2dc currentTile = new Vector2d(tile.getXTile(), tile.getYTile());
			Vector2dc areaToRenderTile = new Vector2d(areaToRender.getXTile(), areaToRender.getYTile());

			Vector2dc minTexture = tileSize.mul(areaToRenderTile.sub(zoomDiffScale.mul(currentTile, new Vector2d()), new Vector2d()), new Vector2d());
			Vector2dc maxTexture = minTexture.add(tileSize, new Vector2d());

			double[][] textureQuad = new double[][] { 
				{ minTexture.x(), maxTexture.y() }, 
				{ minTexture.x(), minTexture.y()}, 
				{ maxTexture.x(), maxTexture.y() }, 
				{ maxTexture.x(), minTexture.y() } 
			};

			//		final TextureID textureID = TextureTracker.getInstance().borrowObject();

			if (textureID.isDirty()) {
				glActiveTexture(textureID.getID());
				bind();

				int filter = GL_NEAREST;
				glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, filter);
				glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, filter);

				glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
				glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);

				glActiveTexture(GL_TEXTURE0);
			}


			ShaderProgram shader = getELTDisplayContext().getShader(SHADER_KEY, ELTImageTexture.class, "ELTImageTexture.vp", "ELTImageTexture.fp");
			shader.useProgram();

			// TODO: RGR - This won't suffice for anything other than WebMercator
			final Matrix4dc imageMVP = imageWorld.getOpenGLMVPMatrix();
			imageMVP.get(this.mvpBuffer);
			glUniformMatrix4fv(shader.getUniformLocation("mvpMat"), false, this.mvpBuffer);
			glUniform1i(shader.getUniformLocation("imageTexture"), textureID.getID() - GL_TEXTURE0);


			try (QuadDrawVAO areaToRenderVAO = new QuadDrawVAO(vertices, 0, textureQuad, 1)) {
				//System.out.println("Drawing OSMTile vertices = " + Arrays.deepToString(vertices) + " textureQuad == " + Arrays.deepToString(textureQuad));
				areaToRenderVAO.draw();
			} finally {
				glUseProgram(0);
				glBindTexture(GL_TEXTURE_2D, 0);
			}
		} finally {
			textureID.unlock();
		}
	}

	/**
	 * Determines uncovered tiles and draws them with ImageTexture 
	 * @param progressiveRender
	 * @param imageWorld
	 * @return
	 * @throws IOException
	 */
	public boolean draw(ImageWorld imageWorld, OSMTile areaToRender) throws Exception {
		internalDraw(imageWorld, areaToRender);
		return true;
	}

	@Override
	public void close() throws IOException {
		super.close();
	}

	/**
	 * @param tile
	 * @return return GLInternalFormat of texture
	 */
	public static GLInternalFormat getInternalFormat(OSMTile tile) {
		return GLInternalFormat.GlInternalFormatRGB8;
	}
}

