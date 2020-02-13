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

import static org.lwjgl.opengl.GL11.GL_ALPHA_TEST;
import static org.lwjgl.opengl.GL11.GL_LINEAR;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_2D;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_MAG_FILTER;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_MIN_FILTER;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_WRAP_S;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_WRAP_T;
import static org.lwjgl.opengl.GL11.glEnable;
import static org.lwjgl.opengl.GL11.glTexParameteri;
import static org.lwjgl.opengl.GL12.GL_CLAMP_TO_EDGE;
import static org.lwjgl.opengl.GL13.GL_TEXTURE0;
import static org.lwjgl.opengl.GL13.glActiveTexture;
import static org.lwjgl.opengl.GL20.glUniform1i;
import static org.lwjgl.opengl.GL20.glUniformMatrix4fv;
import static org.lwjgl.opengl.GL20.glUseProgram;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.FloatBuffer;
import java.util.Collection;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import org.image.common.util.CloseableUtils;
import org.locationtech.jts.geom.Polygon;
import org.lwjgl.BufferUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.leidoslabs.holeshot.elt.coord.ImageWorld;
import com.leidoslabs.holeshot.elt.gpuimage.GLInternalFormat;
import com.leidoslabs.holeshot.elt.gpuimage.QuadDrawVAO;
import com.leidoslabs.holeshot.elt.gpuimage.ShaderProgram;
import com.leidoslabs.holeshot.elt.gpuimage.Texture;
import com.leidoslabs.holeshot.elt.gpuimage.TextureCache;
import com.leidoslabs.holeshot.elt.gpuimage.TextureTracker;
import com.leidoslabs.holeshot.elt.gpuimage.TextureTracker.EvictionCallback;
import com.leidoslabs.holeshot.elt.tileserver.CoreImage;
import com.leidoslabs.holeshot.elt.tileserver.TileRef;
import com.leidoslabs.holeshot.elt.tileserver.TileserverImage;
import com.leidoslabs.holeshot.elt.utils.KeyedLIFOExecutorService;
import com.leidoslabs.holeshot.imaging.coord.ImageScale;

/**
 * Representation of an OpenGL texture for a tile image
 */
public class ELTImageTexture extends Texture {
   private static final int EXECUTOR_POOL_SIZE = Integer.parseInt(System.getProperty("java.util.concurrent.ForkJoinPool.common.parallelism", "4"));
   private static final Logger LOGGER = LoggerFactory.getLogger(ELTImageTexture.class);
   private static final KeyedLIFOExecutorService executorService = KeyedLIFOExecutorService.newFixedThreadPool(EXECUTOR_POOL_SIZE);

   private final ShaderProgram shaderProgram;
   private TileRef tileRef;
   private FloatBuffer modelBuffer;
   private FloatBuffer projectionBuffer;
   private int textureSlot;

   private ELTImageTexture(TileRef tileRef, GLInternalFormat internalFormat,
         BufferedImage textureData, ELTDisplayContext eltDisplayContext) throws IOException {
      super(tileRef.getSize(), internalFormat, GL_LINEAR, GL_CLAMP_TO_EDGE, eltDisplayContext, textureData);

      this.tileRef = tileRef;
      this.modelBuffer = BufferUtils.createFloatBuffer(16);
      this.projectionBuffer = BufferUtils.createFloatBuffer(16);
      this.textureSlot = -1;
      this.shaderProgram = new ShaderProgram(ELTImageTexture.class, "ELTImageTexture.vp", "ELTImageTexture.fp");
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
   public static ELTImageTexture getTexture(ELTDisplayContext eltDisplayContext, TileRef tileRef, ImageWorld imageWorld)
         throws IOException {
      ELTImageTexture texture;
      final String textureKey = String.format("%s%d", tileRef.getKey(), System.identityHashCode(imageWorld));

      texture = TextureCache.getInstance().getTexture(textureKey);

      if (texture == null) {
         executorService.submit(textureKey, () -> {
            try {
               BufferedImage textureData = getTextureData(tileRef);
               if (textureData != null) {
                  eltDisplayContext.syncExec(() -> {
                     try {
                        // Necessary when rendering multiple windows/OpenGL Contexts
                        eltDisplayContext.setContextThread();
                        ELTImageTexture newTexture = new ELTImageTexture(tileRef,
                              getInternalFormat(tileRef), textureData, eltDisplayContext);
                        TextureCache.getInstance().setTexture(textureKey, newTexture);
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
   private boolean isRsetVisible(TileRef tileRef, double currentRsetZoom) {
      final double rset = (double)tileRef.getRset();
      boolean result = rset >= Math.floor(currentRsetZoom) || (rset == tileRef.getImage().getTopTile().getRset()) ;
      return result;
   }

   private boolean isVisible(TileRef tileRef, ImageWorld imageWorld) {
      final boolean isRsetVisible = isRsetVisible(tileRef, imageWorld.getImageScale().getRset());
      final boolean intersects = tileRef.getOpenGLSpaceBounds().intersects(imageWorld.getOpenGLViewport());
      boolean isVisible = isRsetVisible && intersects;
      return isVisible;
   }

   private Collection<TileRef> getTilesToRender(TileRef topTile, ImageWorld imageWorld, Collection<TileRenderRequest> tilesToRender, boolean progressiveRender)
         throws IOException {
      final Set<TileRef> uncoveredTiles = new HashSet<TileRef>();
      final boolean topTileIsVisible = isVisible(topTile, imageWorld);
      if (topTileIsVisible) {
         final int nextRset = topTile.getRset() - 1;
         final boolean highestResolution = imageWorld.getImageRset() == topTile.getRset();

         if (nextRset >= 0 && nextRset >= imageWorld.getImageRset()) {
            Collection<TileRef> subTiles = topTile.createSubTiles();
            for (TileRef subTile : subTiles) {
               uncoveredTiles.addAll(getTilesToRender(subTile, imageWorld, tilesToRender, progressiveRender));
            }
         } else if (progressiveRender || highestResolution) {
            uncoveredTiles.add(topTile);
         }

         // Attempt to cover any uncovered tiles
         if (!uncoveredTiles.isEmpty() &&
               (progressiveRender || highestResolution)) {
            ELTImageTexture myTexture = ELTImageTexture.getTexture(getELTDisplayContext(), topTile, imageWorld);
            if (myTexture != null) {
               // Cover all uncoveredTiles with this texture
               uncoveredTiles.stream().map(t -> new TileRenderRequest(myTexture, t)).forEach(t -> tilesToRender.add(t));
               uncoveredTiles.clear();
            }
         }
      }
      return uncoveredTiles;
   }

   private void internalDraw(TileRef areaToRender, ImageWorld imageWorld) throws IOException {
      imageWorld.getCurrentProjection().get(this.projectionBuffer);
      imageWorld.getCurrentModel().get(this.modelBuffer);

      if (textureSlot < 0) {
         textureSlot = TextureTracker.getInstance().getTexture(new EvictionCallback() {
            @Override
            public void evict(int textureID) {
               ELTImageTexture.this.textureSlot = -1;
            }
         });
         glActiveTexture(textureSlot);

         bind();
      } else {
         glActiveTexture(textureSlot);
      }
      glEnable(GL_ALPHA_TEST);

      glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
      glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
      glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
      glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);

      glActiveTexture(GL_TEXTURE0);

      shaderProgram.useProgram();
      glUniformMatrix4fv(shaderProgram.getUniformLocation("mvMat"), false, this.modelBuffer);
      glUniformMatrix4fv(shaderProgram.getUniformLocation("pMat"), false, this.projectionBuffer);

      glUniform1i(shaderProgram.getUniformLocation("imageTexture"), textureSlot - GL_TEXTURE0);

      final int z = -areaToRender.getRset();

      TileserverImage image = areaToRender.getImage();

      Rectangle r0Rectangle = areaToRender.getR0RectInImageSpace();
      Rectangle2D.Double r0OpenGLRectangle = image.imageToOpenGL(r0Rectangle);

      double[][] vertices = new double[][] { { r0OpenGLRectangle.getMinX(), r0OpenGLRectangle.getMaxY(), z },
         { r0OpenGLRectangle.getMinX(), r0OpenGLRectangle.getMinY(), z },
         { r0OpenGLRectangle.getMaxX(), r0OpenGLRectangle.getMaxY(), z },
         { r0OpenGLRectangle.getMaxX(), r0OpenGLRectangle.getMinY(), z } };

         Rectangle areaToRenderR0 = areaToRender.getR0RectInImageSpace();
         Rectangle tileRefR0 = tileRef.getR0RectInImageSpace();

         Point areaToRenderR0UL = areaToRenderR0.getLocation();
         Point tileRefR0UL = tileRefR0.getLocation();

         Point offsetR0 = new Point(areaToRenderR0UL.x - tileRefR0UL.x, areaToRenderR0UL.y - tileRefR0UL.y);

         final int thisRset = tileRef.getRset();

         double tileWidth = tileRef.getWidth();
         double tileHeight = tileRef.getHeight();
         areaToRender.getFullR0RectInImageSpace();

         ImageScale tileScale = ImageScale.forRset(thisRset);
         Point2D tileOffsetR0 = new Point2D.Double(offsetR0.getX(), offsetR0.getY());
         Point2D tileOffsetThisRset = tileScale.scaleDownToRset(tileOffsetR0);

         Point2D tileDimR0 = new Point2D.Double(areaToRenderR0.getWidth(), areaToRenderR0.getHeight());
         Point2D tileDimThisRset = tileScale.scaleDownToRset(tileDimR0);


         Rectangle adjustedTile = new Rectangle((int)tileOffsetThisRset.getX(), (int)tileOffsetThisRset.getY(),
               (int)tileDimThisRset.getX(), (int)tileDimThisRset.getY());

         double minX = adjustedTile.getMinX() / tileWidth;
         double minY = adjustedTile.getMinY() / tileHeight;
         double maxX = adjustedTile.getMaxX() / tileWidth;
         double maxY = adjustedTile.getMaxY() / tileHeight;

         double[][] textureQuad = new double[][] { { minX, maxY }, { minX, minY }, { maxX, maxY }, { maxX, minY } };

         try (QuadDrawVAO areaToRenderVAO = new QuadDrawVAO(vertices, 0, textureQuad, 1)) {
            areaToRenderVAO.draw();
         }
         glUseProgram(0);
   }

   /**
    * Determines uncovered tiles and draws them with ImageTexture 
    * @param progressiveRender
    * @param imageWorld
    * @return
    * @throws IOException
    */
   public boolean draw(boolean progressiveRender, ImageWorld imageWorld) throws IOException {
      Deque<TileRenderRequest> tilesToDraw = new LinkedList<TileRenderRequest>();
      Collection<TileRef> uncoveredTiles = getTilesToRender(tileRef, imageWorld, tilesToDraw, progressiveRender);

      for (TileRenderRequest request : tilesToDraw) {
         request.getImageTexture().internalDraw(request.getAreaToRender(), imageWorld);
      }
      return uncoveredTiles.isEmpty();
   }

   @Override
   public void close() throws IOException {
      super.close();
      try {
         getELTDisplayContext().asyncExec(() ->
         {
            try {
               CloseableUtils.close(shaderProgram);
            } catch (IOException e) {
               LOGGER.error(e.getMessage(), e);
            }
         }
         );
      } catch (InterruptedException | ExecutionException e) {
         LOGGER.error(e.getMessage(), e);
         throw new IOException(e);
      }
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
