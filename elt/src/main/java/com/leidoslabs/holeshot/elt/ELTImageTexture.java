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

package com.leidoslabs.holeshot.elt;

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
import static org.lwjgl.opengl.GL13.GL_TEXTURE1;
import static org.lwjgl.opengl.GL13.glActiveTexture;
import static org.lwjgl.opengl.GL20.glUniform1i;
import static org.lwjgl.opengl.GL20.glUniformMatrix4fv;
import static org.lwjgl.opengl.GL20.glUseProgram;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.FloatBuffer;
import java.util.Collection;
import java.util.Deque;
import java.util.LinkedList;
import java.util.concurrent.ExecutionException;

import org.image.common.util.CloseableUtils;
import org.joml.Matrix4dc;
import org.joml.Vector2d;
import org.joml.Vector2dc;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Polygon;
import org.lwjgl.BufferUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.primitives.Doubles;
import com.leidoslabs.holeshot.elt.gpuimage.GLInternalFormat;
import com.leidoslabs.holeshot.elt.gpuimage.QuadDrawVAO;
import com.leidoslabs.holeshot.elt.gpuimage.ShaderProgram;
import com.leidoslabs.holeshot.elt.gpuimage.Texture;
import com.leidoslabs.holeshot.elt.gpuimage.TextureCache;
import com.leidoslabs.holeshot.elt.gpuimage.TextureTracker;
import com.leidoslabs.holeshot.elt.gpuimage.TextureTracker.TextureID;
import com.leidoslabs.holeshot.elt.tileserver.CoreImage;
import com.leidoslabs.holeshot.elt.tileserver.TileRef;
import com.leidoslabs.holeshot.elt.tileserver.TileserverImage;
import com.leidoslabs.holeshot.elt.utils.GeometryUtils;
import com.leidoslabs.holeshot.elt.utils.KeyedLIFOExecutorService;
import com.leidoslabs.holeshot.elt.viewport.ImageWorld;
import com.leidoslabs.holeshot.imaging.coord.ImageScale;

/**
 * Representation of an OpenGL texture for a tile image
 */
public class ELTImageTexture extends Texture {
	private static final int EXECUTOR_POOL_SIZE = Integer.parseInt(System.getProperty("java.util.concurrent.ForkJoinPool.common.parallelism", "4"));
	private static final Logger LOGGER = LoggerFactory.getLogger(ELTImageTexture.class);
	private static final KeyedLIFOExecutorService executorService = KeyedLIFOExecutorService.newFixedThreadPool(EXECUTOR_POOL_SIZE);
	private static final String SHADER_KEY = ELTImageTexture.class.getName();

	private TileRef tileRef;
	private FloatBuffer mvpBuffer;
	private TextureID textureID;
	
	private ELTImageTexture(TileRef tileRef, GLInternalFormat internalFormat,
			BufferedImage textureData, ELTDisplayContext eltDisplayContext) throws IOException, InterruptedException, ExecutionException {
		super(tileRef.getSize(), internalFormat, GL_LINEAR, GL_CLAMP_TO_EDGE, eltDisplayContext, textureData);

		this.textureID = eltDisplayContext.getTextureTracker().createTextureID();
		this.tileRef = tileRef;
		this.mvpBuffer = BufferUtils.createFloatBuffer(16);
	}

	/**
	 * @return ELTTextureImage's TileRef
	 */
	public TileRef getTileRef() {
		return tileRef;
	}


	private static BufferedImage imageToTextureData(TileRef tileRef) throws IOException {
		final CoreImage coreImage = tileRef.getTileImage();
		return coreImage == null ? null : coreImage.getBufferedImage();
	}

	private static BufferedImage getTextureData(TileRef tileRef) throws IOException {
		BufferedImage textureData = imageToTextureData(tileRef);
		return textureData;
	}

	/**
	 * Retrieves image texture. If not in Cache, retrieves texture data from 
	 * Tile Image, and updates cache key.
	 * @param eltDisplayContext
	 * @param tileRef
	 * @param imageWorld
	 * @return Image Texture, retrieved by tile key and image world hash.
	 * @throws IOException
	 */
	public static ELTImageTexture getTexture(ELTDisplayContext eltDisplayContext, TileRef tileRef)
			throws IOException {
		ELTImageTexture texture;
		final String textureKey = String.format("%s%d", tileRef.getKey(), eltDisplayContext.getContextHandle());

		synchronized(textureKey.intern()) {
			texture = (ELTImageTexture)TextureCache.getInstance().getTexture(textureKey);

			if (texture == null && !executorService.isInProcess(textureKey)) {
				executorService.submit(textureKey, () -> {
					try {
						BufferedImage textureData = getTextureData(tileRef);
						if (textureData != null) {
							eltDisplayContext.asyncExec(() -> {
								try {
									synchronized(textureKey.intern()) {
										if (!TextureCache.getInstance().containsKey(textureKey)) {
											// Necessary when rendering multiple windows/OpenGL Contexts
											eltDisplayContext.setContextThread();
											ELTImageTexture newTexture = new ELTImageTexture(tileRef,
													getInternalFormat(tileRef), textureData, eltDisplayContext);
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

	private static class TileRenderRequest {
		private ELTImageTexture imageTexture;
		private TileRef areaToRender;

		public TileRenderRequest(ELTImageTexture imageTexture, TileRef areaToRender) {
			super();
			this.imageTexture = imageTexture;
			this.areaToRender = areaToRender;
		}

		public ELTImageTexture getImageTexture() {
			return imageTexture;
		}

		public TileRef getAreaToRender() {
			return areaToRender;
		}
	}

	private int getTargetRset(ImageWorld imageWorld, TileRef tileRef) {
		final TileserverImage image = tileRef.getImage();
		final double unconstrainedRset = ImageWorld.getRset(image, imageWorld.getGeodeticCenter().getGeodeticCoordinate(), imageWorld.getMapScale());
		final int targetRset = (int)Doubles.constrainToRange(Math.floor(unconstrainedRset),
				0.0, image.getMaxRLevel());
		return targetRset;
	}

	private boolean isRsetVisible(ImageWorld imageWorld, TileRef tileRef, int targetRset) {
		final int tileRefRset = tileRef.getRset();
		return (tileRefRset >= targetRset);
	}

	private boolean isVisible(ImageWorld imageWorld, TileRef tileRef, Polygon projectedViewport, int targetRset) {
		return imageWorld.getProjectedTile(tileRef).intersects(projectedViewport) && 
				isRsetVisible(imageWorld, tileRef, targetRset);
	}


	private boolean getTilesToRender(TileRef topTile, ImageWorld imageWorld, Collection<TileRenderRequest> tilesToRender, boolean progressiveRender)
			throws IOException {
		final int targetRset = getTargetRset(imageWorld, topTile);
		return getTilesToRender(targetRset, topTile, imageWorld, tilesToRender, progressiveRender);
	}

	private boolean getTilesToRender(int targetRset, TileRef topTile, ImageWorld imageWorld, Collection<TileRenderRequest> tilesToRender, boolean progressiveRender)
			throws IOException {
		boolean isCovered = true;
		final Polygon projectedViewport = imageWorld.getProjectedViewport();
		final boolean topTileIsVisible = isVisible(imageWorld, topTile, projectedViewport, targetRset);
		final int currentRset = topTile.getRset();
		final boolean shouldLoadCurrent = progressiveRender || currentRset == targetRset;

		if (topTileIsVisible) {
			final int nextRset = currentRset- 1;

			boolean needToCover = true;
			if (nextRset >= targetRset) {
				Collection<TileRef> subTiles = topTile.createSubTiles();
				needToCover = false;
				for (TileRef subTile : subTiles) {
					needToCover |= !getTilesToRender(targetRset, subTile, imageWorld, tilesToRender, progressiveRender);
				}
			}

			// Attempt to cover any uncovered tiles
			if (needToCover) {
				isCovered = false;
				if (shouldLoadCurrent) {
					ELTImageTexture myTexture = ELTImageTexture.getTexture(getELTDisplayContext(), topTile);
					if (myTexture != null) {
						tilesToRender.add(new TileRenderRequest(myTexture, topTile));
						isCovered = true;
					} 
				}
			} 
			//			System.out.println(String.format("getTilesToRender targetRset = %d, topTile = %d shouldLoadCurrent = %b isCovered = %b needToCover = %b", targetRset, topTile.getRset(), shouldLoadCurrent, isCovered, needToCover));
		} 
		return isCovered;
	}

	private void internalDraw(TileRef areaToRender, ImageWorld imageWorld, Interpolation interpolation) throws Exception {
		textureID.lock();
		try {
			// vertices in image space
			// ll, lr, ur, ul, ll
			Coordinate[] coordinates = areaToRender.getImageSpaceBounds().getCoordinates();

			// These Coordinates define a triangle path for a quad across the area to render.
			// They are sensitive to changes into the order that ImageSpaceBounds is defined.
			final double PIXEL = 1.0;
			final int z = -areaToRender.getRset();
			double[][] vertices = new double[][] {
				{ coordinates[3].x, coordinates[3].y + PIXEL, z }, // ul
				{ coordinates[0].x, coordinates[0].y, z }, // ll
				{ coordinates[2].x + PIXEL, coordinates[2].y + PIXEL, z }, // ur
				{ coordinates[1].x + PIXEL, coordinates[1].y, z }  // lr
			};

			Rectangle areaToRenderR0 = areaToRender.getR0RectInImageSpace();
			Point areaToRenderR0UL = areaToRenderR0.getLocation();

			Rectangle tileRefR0 = tileRef.getR0RectInImageSpace();
			Point tileRefR0UL = tileRefR0.getLocation();

			Point offsetR0 = new Point(areaToRenderR0UL.x - tileRefR0UL.x, areaToRenderR0UL.y - tileRefR0UL.y);

			final int thisRset = tileRef.getRset();
			//		System.out.println("drawing from rset " + thisRset);

			ImageScale tileScale = ImageScale.forRset(thisRset);
			Point2D tileOffsetR0 = new Point2D.Double(offsetR0.getX(), offsetR0.getY());
			Vector2dc tileOffsetThisRset = GeometryUtils.toVector2d(tileScale.scaleDownToRset(tileOffsetR0));

			Point2D tileDimR0 = new Point2D.Double(areaToRenderR0.getWidth(), areaToRenderR0.getHeight());
			Vector2dc tileDimThisRset = GeometryUtils.toVector2d(tileScale.scaleDownToRset(tileDimR0));

			final Vector2dc tileSize = GeometryUtils.toVector2d(tileRef.getSize());
			Vector2d minTexture = tileOffsetThisRset.div(tileSize, new Vector2d());
			Vector2d maxTexture = tileOffsetThisRset.add(tileDimThisRset, new Vector2d()).div(tileSize, new Vector2d());
			double[][] textureQuad = new double[][] { 
				{ minTexture.x(), maxTexture.y() }, 
				{ minTexture.x(), minTexture.y()}, 
				{ maxTexture.x(), maxTexture.y() }, 
				{ maxTexture.x(), minTexture.y() } 
			};

			if (textureID.isDirty()) {
				glActiveTexture(textureID.getID());
				bind();

				int filter = interpolation == Interpolation.LINEAR ? GL_LINEAR : GL_NEAREST;
				glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, filter);
				glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, filter);

				glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
				glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);

				glActiveTexture(GL_TEXTURE0);
			}

			ShaderProgram shader = 	getELTDisplayContext().getShader(SHADER_KEY, ELTImageTexture.class, "ELTImageTexture.vp", "ELTImageTexture.fp");

			shader.useProgram();

			final Matrix4dc imageMVP = imageWorld.getOpenGLImageMVPMatrix(areaToRender.getImage());
			imageMVP.get(this.mvpBuffer);
			glUniformMatrix4fv(shader.getUniformLocation("mvpMat"), false, this.mvpBuffer);
			glUniform1i(shader.getUniformLocation("imageTexture"), textureID.getID() - GL_TEXTURE0);


			try (QuadDrawVAO areaToRenderVAO = new QuadDrawVAO(vertices, 0, textureQuad, 1)) {
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
	public boolean draw(boolean progressiveRender, ImageWorld imageWorld, Interpolation interpolated) throws Exception {
		Deque<TileRenderRequest> tilesToDraw = new LinkedList<TileRenderRequest>();
		boolean isCovered = getTilesToRender(tileRef, imageWorld, tilesToDraw, progressiveRender);

		for (TileRenderRequest request : tilesToDraw) {
			request.getImageTexture().internalDraw(request.getAreaToRender(), imageWorld, interpolated);
		}
		return isCovered;
	}

	@Override
	public void close() throws IOException {
		super.close();
	}

	/**
	 * @param tileRef
	 * @return return GLInternalFormat of texture
	 */
	public static GLInternalFormat getInternalFormat(TileRef tileRef) {
		final TileserverImage image = tileRef.getImage();
		final int bpp = image.getBitsPerPixel();
		final int bands = 3; //image.getNumBands();

		GLInternalFormat internalFormat = null;

		switch (bpp) {
		case 8:
			switch (bands) {
			case 1:
				internalFormat = GLInternalFormat.GlInternalFormatR8;
				break;
			case 3:
				internalFormat = GLInternalFormat.GlInternalFormatRGBA8;
				break;
			}
			break;
		case 16:
			switch (bands) {
			case 1:
				internalFormat = GLInternalFormat.GlInternalFormatR16UI;
				break;
			case 3:
				internalFormat = GLInternalFormat.GlInternalFormatRGBA16UI;
				break;
			}
			break;
		}

		if (internalFormat == null) {
			throw new UnsupportedOperationException(String.format("ELT doesn't currently support images with %d BPP and %d bands", bpp, bands));
		}
		return internalFormat;
	}
}
