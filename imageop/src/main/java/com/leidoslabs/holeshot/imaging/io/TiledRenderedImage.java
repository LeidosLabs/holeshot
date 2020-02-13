/*
 * Licensed to The Leidos Corporation under one or more contributor license agreements.  
 * See the NOTICE file distributed with this work for additional information regarding copyright ownership.
 * The Leidos Corporation licenses this file to You under the Apache License, Version 2.0
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
package com.leidoslabs.holeshot.imaging.io;

import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.awt.image.SampleModel;
import java.awt.image.WritableRaster;
import java.io.IOException;
import java.util.Vector;

import javax.imageio.ImageReadParam;
import javax.imageio.ImageReader;
import javax.media.jai.JAI;

import com.leidoslabs.holeshot.imaging.nitf.RenderedImageSegment;

/**
 * Our Tiled Implementation of RenderedImage. Creatable from an ImageReader
 */
public class TiledRenderedImage implements RenderedImage {

   private final javax.media.jai.TileCache tileCache;
   private final ImageReader imageReader;
   private final int imageIndex;
   private Rectangle bounds;
   private final int tileWidth;
   private final int tileHeight;
   private final ColorModel colorModel;
   private final SampleModel sampleModel;

   public TiledRenderedImage(ImageReader imageReader) throws IOException {
      this(imageReader, 0);
   }
   /**
    * Instantiate TIeldRenderedImage from an ImageReader and index to read
    * @param imageReader
    * @param imageIndex
    * @throws IOException
    */
   public TiledRenderedImage(ImageReader imageReader, int imageIndex) throws IOException {
      this.tileCache =  JAI.getDefaultInstance().getTileCache();
      this.imageReader = imageReader;
      this.imageIndex = imageIndex;

      this.bounds = new Rectangle(0, 0,
            imageReader.getWidth(imageIndex),
            imageReader.getHeight(imageIndex));
      this.tileWidth = imageReader.getTileWidth(imageIndex);
      this.tileHeight = imageReader.getTileHeight(imageIndex);
      this.colorModel = imageReader.getRawImageType(imageIndex).getColorModel();
      this.sampleModel = imageReader.getRawImageType(imageIndex).getSampleModel();
   }

   @Override
   public Vector<RenderedImage> getSources() {
      return null;
   }

   @Override
   public Object getProperty(String name) {
      return java.awt.Image.UndefinedProperty;
   }

   @Override
   public String[] getPropertyNames() {
      return null;
   }

   @Override
   public ColorModel getColorModel() {
      return colorModel;
   }

   @Override
   public SampleModel getSampleModel() {
      return sampleModel;
   }

   @Override
   public int getWidth() {
      return bounds.width;
   }

   @Override
   public int getHeight() {
      return bounds.height;
   }

   @Override
   public int getMinX() {
      return bounds.x;
   }

   @Override
   public int getMinY() {
      return bounds.y;
   }

   @Override
   public int getNumXTiles() {
      return (bounds.width + tileWidth - 1) / tileWidth;

   }

   @Override
   public int getNumYTiles() {
      return (bounds.height + tileHeight - 1) / tileHeight;
   }

   @Override
   public int getMinTileX() {
      return 0;
   }

   @Override
   public int getMinTileY() {
      return 0;
   }

   @Override
   public int getTileWidth() {
      return tileWidth;
   }

   @Override
   public int getTileHeight() {
      return tileHeight;
   }

   @Override
   public int getTileGridXOffset() {
      return 0;
   }

   @Override
   public int getTileGridYOffset() {
      return 0;
   }

   private boolean validTile(int tileX, int tileY) {
      return tileX >= getMinTileX() && tileX < (getMinTileX() + getNumXTiles()) &&
            tileY >= getMinTileY() && tileY < (getMinTileY() + getNumYTiles());
   }


   @Override
   /**
    * Given a specified tile's x/y, computes corresponding pixel location in image, and reads the
    * region into a Raster an caches result
    */
   public synchronized Raster getTile(int tileX, int tileY) {
      Raster result = null;
      try {
         if (validTile(tileX, tileY)) {
            result = (tileCache == null ? null : tileCache.getTile(this, tileX, tileY));
            if (result == null) {
               Rectangle tileRect = new Rectangle(tileXToX(tileX), tileYToY(tileY), getTileWidth(), getTileHeight());
               ImageReadParam param = imageReader.getDefaultReadParam();
               param.setSourceRegion(tileRect);
               BufferedImage bi = imageReader.read(imageIndex, param);
               WritableRaster ras = bi.getRaster();
               result = ras.createWritableChild(0, 0,  ras.getWidth(), ras.getHeight(), tileRect.x,  tileRect.y,  null);

               if (result != null && tileCache != null) {
                  tileCache.add(this, tileX, tileY, result);
               }
            }
         }
      } catch (IOException e) {
         e.printStackTrace();
      }
      return result;
   }


   @Override
   public Raster getData() {
      return getData(bounds);
   }

   @Override
   /**
    * Given a rectangle, returns a Raster with the image data of the intersection between the rectangle
    * and the tiles it covers
    */
   public synchronized Raster getData(Rectangle rect) {
      WritableRaster result = null;

      final Rectangle bounds = new Rectangle(getMinX(), getMinY(), getWidth(), getHeight());
      final Rectangle imageIntersection = bounds.intersection(rect);

      if (!imageIntersection.isEmpty()) {

         final int minTileX = Math.max(XToTileX(imageIntersection.x), getMinTileX());
         final int maxTileX = Math.min(XToTileX(imageIntersection.x + imageIntersection.width - 1), getMinTileX() + getNumXTiles() - 1);
         final int minTileY = Math.max(YToTileY(imageIntersection.y), getMinTileY());
         final int maxTileY = Math.min(YToTileY(imageIntersection.y + imageIntersection.height - 1), getMinTileY() + getNumYTiles() - 1);

         for (int tileX = minTileX; tileX <= maxTileX; ++tileX) {
            for (int tileY = minTileY; tileY <= maxTileY; ++tileY) {
               final Raster fullTile = getTile(tileX, tileY);
               final Rectangle tileRect = new Rectangle(tileXToX(tileX), tileYToY(tileY), getTileWidth(), getTileHeight());
               final Rectangle tileIntersection = imageIntersection.intersection(tileRect);
               Raster tileIntersectionRaster = fullTile.createChild(tileIntersection.x, tileIntersection.y, tileIntersection.width, tileIntersection.height, 0, 0, null);

               if (result == null) {
                  result = fullTile.createCompatibleWritableRaster(imageIntersection);
               }

               result.setDataElements(tileIntersection.x, tileIntersection.y, tileIntersectionRaster);
            }
         }
      }

      return result;
   }

   public synchronized int XToTileX(int x) {
      return RenderedImageSegment.XToTileX(x, this.getTileGridXOffset(), this.getTileWidth());
   }

   public synchronized int YToTileY(int y) {
      return RenderedImageSegment.YToTileY(y, this.getTileGridYOffset(), this.getTileHeight());
   }

   public synchronized int tileXToX(int tx) {
      return RenderedImageSegment.tileXToX(tx, this.getTileGridXOffset(), this.getTileWidth());
   }

   public synchronized int tileYToY(int ty) {
      return RenderedImageSegment.tileYToY(ty, this.getTileGridYOffset(), this.getTileHeight());
   }

   public synchronized WritableRaster copyData(WritableRaster raster) {
      WritableRaster result;
      if (raster == null) {
         result = (WritableRaster)getData(new Rectangle(0, 0, getWidth(), getHeight()));
      } else {
         Raster src = getData(raster.getBounds());
         raster.setRect(src);
         result = raster;
      }
      return result;
   }
}
