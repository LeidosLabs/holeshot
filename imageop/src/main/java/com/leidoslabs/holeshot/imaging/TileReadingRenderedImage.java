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
package com.leidoslabs.holeshot.imaging;

import com.leidoslabs.holeshot.imaging.cache.TileCache;

import java.awt.*;
import java.awt.image.ColorModel;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.awt.image.SampleModel;
import java.awt.image.WritableRaster;
import java.io.IOException;
import java.util.HashMap;
import java.util.Vector;

/**
 * This class represents an image that uses a TileReader to load tiles as needed to satisfy
 * calls to the RenderedImage interface. Tiles can be cached in a TileCache to prevent reading
 * them from the TileReader more than once.
 */
public class TileReadingRenderedImage implements RenderedImage {

  protected int minX;
  protected int minY;
  protected int width;
  protected int height;
  protected int tileWidth;
  protected int tileHeight;
  protected int tileGridXOffset;
  protected int tileGridYOffset;
  protected SampleModel sampleModel;
  protected ColorModel colorModel;
  protected final HashMap<String,Object> properties = new HashMap<>();

  private final TileReader tileReader;
  private final TileCache tileCache;

  public TileReadingRenderedImage(TileReader tileReader, TileCache tileCache) {
    this.tileReader = tileReader;
    this.tileCache = tileCache;
  }

  @Override
  public Vector<RenderedImage> getSources() {
    // TODO: Consider if the TileReadingRenderedImage.getSources() method should return something.
    return null;
  }

  @Override
  public Object getProperty(String name) {
    return properties.get(name);
  }

  @Override
  public String[] getPropertyNames() {
    return properties.keySet().toArray(new String[properties.size()]);
  }

  /**
   * Returns the ColorModel of the image. This implementation returns the
   * TileReader's color model, which can change over time.
   */
  @Override
  public ColorModel getColorModel() {
    return (tileReader != null) ? tileReader.getColorModel() : colorModel;
  }

  /**
   * Returns the SampleModel of the image. This implementation returns the
   * TileReader's sample model, which can change over time.
   */
  @Override
  public SampleModel getSampleModel() {
    return (tileReader != null) ? tileReader.getSampleModel() : sampleModel;
  }

  @Override
  public int getWidth() {
    return this.width;
  }

  @Override
  public int getHeight() {
    return this.height;
  }

  @Override
  public int getMinX() {
    return this.minX;
  }

  @Override
  public int getMinY() {
    return this.minY;
  }

  /**
   * Returns the number of tiles in the x direction.
   *
   * @return the number of tiles in the x direction.
   */
  @Override
  public int getNumXTiles() {
    int x = this.getMinX();
    int tx = this.getTileGridXOffset();
    int tw = this.getTileWidth();
    return XToTileX(x + this.getWidth() - 1, tx, tw) - XToTileX(x, tx, tw) + 1;
  }

  /**
   * Returns the number of tiles in the y direction.
   *
   * @return the number of tiles in the y direction.
   */
  @Override
  public int getNumYTiles() {
    int y = this.getMinY();
    int ty = this.getTileGridYOffset();
    int th = this.getTileHeight();
    return YToTileY(y + this.getHeight() - 1, ty, th) - YToTileY(y, ty, th) + 1;
  }

  /**
   * Returns the minimum tile index in the x direction.
   *
   * @return the minimum tile index in the x direction.
   */
  @Override
  public int getMinTileX() {
    return XToTileX(this.getMinX(), this.getTileGridXOffset(), this.getTileWidth());
  }

  /**
   * Returns the minimum tile index in the y direction.
   *
   * @return the minimum tile index in the y direction.
   */
  @Override
  public int getMinTileY() {
    return YToTileY(this.getMinY(), this.getTileGridYOffset(), this.getTileHeight());
  }

  /**
   * Returns the tile width in pixels.
   *
   * @return the tile width in pixels.
   */
  @Override
  public int getTileWidth() {
    return this.tileWidth;
  }

  /**
   * Returns the tile height in pixels.
   *
   * @return the tile height in pixels.
   */
  @Override
  public int getTileHeight() {
    return this.tileHeight;
  }

  /**
   * Returns the x offset of the tile grid relative to the origin,
   * For example, the x coordinate of the location of tile
   * (0,&nbsp;0).
   *
   * @return the x offset of the tile grid.
   */
  @Override
  public int getTileGridXOffset() {
    return this.tileGridXOffset;
  }

  @Override
  public int getTileGridYOffset() {
    return this.tileGridYOffset;
  }

  @Override
  public Raster getTile(int tileX, int tileY) {
    Raster result = (tileCache == null ? null : tileCache.getTile(this, tileX, tileY));
    if (result == null) {
      try {
        result = tileReader.readTile(tileX, tileY);
        if (result != null && tileCache != null) {
          tileCache.add(this, tileX, tileY, result);
        }
      } catch (IOException ioe) {
        ioe.printStackTrace();
        result = null;
      }
    }

    return result;
  }

  @Override
  public Raster getData() {
    return this.getData(null);
  }

  @Override
  public Raster getData(Rectangle region) {
    throw new UnsupportedOperationException("TODO: Implement TileReadingRenderedImage.getData(Rectangle)");
  }

  @Override
  public WritableRaster copyData(WritableRaster raster) {
    throw new UnsupportedOperationException("TODO: Implement TileReadingRenderedImage.copyData(WritableRaster");
  }


  /**
   * @param x x position (in pixels)
   * @param tileGridXOffset
   * @param tileWidth
   * @return tile index in x direction (i.e, 0 indexed column number)
   */
  public static int XToTileX(int x, int tileGridXOffset, int tileWidth) {
    x -= tileGridXOffset;
    if (x < 0) {
      x += 1 - tileWidth;
    }
    return x / tileWidth;
  }

  /**
   * @param y y position (in pixels)
   * @param tileGridYOffset
   * @param tileHeight
   * @return tile index in y direction (i.e, 0 indexed row number)
   */
  public static int YToTileY(int y, int tileGridYOffset, int tileHeight) {
    y -= tileGridYOffset;
    if (y < 0) {
      y += 1 - tileHeight;
    }

    return y / tileHeight;
  }

  public int XToTileX(int x) {
    return XToTileX(x, this.getTileGridXOffset(), this.getTileWidth());
  }

  public int YToTileY(int y) {
    return YToTileY(y, this.getTileGridYOffset(), this.getTileHeight());
  }

  public static int tileXToX(int tx, int tileGridXOffset, int tileWidth) {
    return tx * tileWidth + tileGridXOffset;
  }

  public static int tileYToY(int ty, int tileGridYOffset, int tileHeight) {
    return ty * tileHeight + tileGridYOffset;
  }

  public int tileXToX(int tx) {
    return tileXToX(tx, this.getTileGridXOffset(), this.getTileWidth());
  }

  public int tileYToY(int ty) {
    return tileYToY(ty, this.getTileGridYOffset(), this.getTileHeight());
  }
}
