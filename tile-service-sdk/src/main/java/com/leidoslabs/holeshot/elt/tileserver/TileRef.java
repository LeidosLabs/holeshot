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
package com.leidoslabs.holeshot.elt.tileserver;

import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.image.common.cache.CacheableUtil;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.spatial4j.shape.impl.RectangleImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.leidoslabs.holeshot.elt.utils.GeometryUtils;
import com.leidoslabs.holeshot.imaging.coord.ImageScale;

public class TileRef extends AbstractTileRef<TileRef> {
   private static final Logger LOGGER = LoggerFactory.getLogger(TileRef.class);
   private TileserverImage image;
   private Dimension nextRsetRowCols;
   private double[][] textureCoordinates;
   
   /**
    * Initialize TileRef without band
    * @param image
    * @param rset
    * @param x
    * @param y
    */
   public TileRef(TileserverImage image, int rset, int x, int y) {
      this(image, rset, x, y, -1);
   }

   
   /**
    * Initialize TileRef. Computes Dimensions of next rset (for retrieving subtiles), and texture coordinates
    * @param image
    * @param rset
    * @param x
    * @param y
    * @param band
    */
   public TileRef(TileserverImage image, int rset, int x, int y, int band) {
	   super(image.getCollectionID(), image.getTimestamp(), rset, x, y, band);
      this.image = image;
      this.nextRsetRowCols = getRowsAndColumnsForRset(this.getRset()-1);
      textureCoordinates = this.calculateTextureCoordinates();
   }

   
   /**
    * Get tile that differs only by band
    * @param band
    * @return
    */
   public TileRef getForBand(int band) {
      return new TileRef(this.image, getRset(), getX(), getY(), band);
   }

   /**
    * @return Parent of this tile, i.e Tile at rset + 1, x / 2, y /2.
    */
   public TileRef getParentTile() {
      TileRef parentTile = null;
      if (getRset() < image.getMaxRLevel()) {
         parentTile = new TileRef(image, getRset() + 1, getX() / 2, getY() / 2, getBand());
      }
      return parentTile;
   }
   
   public TileRef getBorderTile(int xOffset, int yOffset) {
	   TileRef result = null;
	   Dimension numTiles = this.getRowsAndColumnsForRset(getRset());
	   int tileX = getX() + xOffset;
	   int tileY = getY() + yOffset;
	   
	   if (tileX >= 0 && tileX < numTiles.width && tileY >= 0 && tileY < numTiles.height) {
		   result = new TileRef(image, getRset(), tileX, tileY);
	   }
	   return result;
   }

   /**
    * Dimensions of next rset (rset - 1) in tiles
    * @return
    */
   public Dimension getNextRsetRowCols() {
      return nextRsetRowCols;
   }

   /**
    * @return <MaxRSet> - <curRset>, e.g top level tile would be level 0
    */
   public int getLevel() {
      return getImage().getMaxRLevel() - getRset();
   }

   public TileserverImage getImage() {
      return image;
   }

   public Dimension getSize() {
      return new Dimension(image.getTileWidth(), image.getTileHeight());
   }
   public int getWidth() {
      return image.getTileWidth();
   }

   public int getHeight() {
      return image.getTileHeight();
   }

   public String getCollectionID() {
      return image.getCollectionID();
   }

   public String getTimestamp() {
      return image.getTimestamp();
   }

   /**
    * Using the images camera model, Apply imageToWorld transformation on tile's corresponding R0 image space bounds 
    * to retrieve geodetic coordinates
    * @return 
    */
   public Polygon getGeodeticBounds() {
      Coordinate[] geoCoordinates =
            Arrays.stream(getImageSpaceBounds().getCoordinates())
            .map(c-> image.getCameraModel().imageToWorld(GeometryUtils.toPoint(c)))
            .toArray(Coordinate[]::new);

      return GeometryUtils.GEOMETRY_FACTORY.createPolygon(geoCoordinates);
   }

   /**
    * @return Get tile's bounds in R0 image space
    */
   public Polygon getImageSpaceBounds() {
      Rectangle r0Rect = getR0RectInImageSpace();
      
      Coordinate ll = new Coordinate(r0Rect.getMinX(), r0Rect.getMinY(), 0.0);
      Coordinate lr = new Coordinate(r0Rect.getMaxX()-1, r0Rect.getMinY(), 0.0);
      Coordinate ur = new Coordinate(r0Rect.getMaxX()-1, r0Rect.getMaxY()-1, 0.0);
      Coordinate ul = new Coordinate(r0Rect.getMinX(), r0Rect.getMaxY()-1, 0.0);

      // Other pieces of the code depend on this order.  Don't change it without checking out it's usage.
      Polygon imageBounds = GeometryUtils.GEOMETRY_FACTORY.createPolygon(new Coordinate[] {
            ll, lr, ur, ul, ll
      });
      return imageBounds;
   }

   /**
    * @return Tiles's R0 rectangle, unclipped (potentially out of bounds of image's R0 rectangle)
    */
   public Rectangle getFullR0RectInImageSpace() {
      final ImageScale tileScale = ImageScale.forRset(getRset());
      final Point2D tileDim = new Point2D.Double(getWidth(), getHeight());
      final Point2D tileDimR0 = tileScale.scaleUpToR0(tileDim);
      final int tileWidth = (int)Math.round(tileDimR0.getX());
      final int tileHeight = (int)Math.round(tileDimR0.getY());
      Rectangle fullTile = new Rectangle(getX()*tileWidth, getY()*tileHeight, tileWidth, tileHeight);
      
//      System.out.println("fullTile == " + fullTile.toString());

      return fullTile;
   }

   /**
    * @return Intersection of tile's unclipped rectangle and image's R0 rectangle
    */
   public Rectangle getR0RectInImageSpace() {
      Rectangle imageRectangle = image.getR0ImageRectangle();
      Rectangle result = imageRectangle.intersection(getFullR0RectInImageSpace());
      return result;
   }

   private double[][] calculateTextureCoordinates() {
      Rectangle fullTile = getFullR0RectInImageSpace();
      Rectangle croppedTile = getR0RectInImageSpace();

      final double textureWidth = croppedTile.getWidth() / fullTile.getWidth();
      final double textureHeight = croppedTile.getHeight() / fullTile.getHeight();
      final double[][] textureQuad =
            new double[][] {{0.0, textureHeight}, {0.0, 0.0}, {textureWidth, textureHeight}, {textureWidth, 0.0} };
            return textureQuad;
   }

   public double[][] getTextureCoordinates() {
      return textureCoordinates;
   }
   public double[] getLLTextureCoordinate() {
      return textureCoordinates[0];
   }
   public double[] getULTextureCoordinate() {
      return textureCoordinates[1];
   }
   public double[] getLRTextureCoordinate() {
      return textureCoordinates[2];
   }
   public double[] getURTextureCoordinate() {
      return textureCoordinates[3];
   }
   
   /**
    * @return Identifier for tile: collectionID/timeStamp/rset/x/y/band
    */
   public String getKey() {
      return String.format("%s/%s/%d/%d/%d/%d", this.getCollectionID(), this.getTimestamp(),
            this.getRset(), this.getX(), this.getY(), this.getBand());
   }

   /**
    * @param rset
    * @return Dimension of rows and columns in given rset
    */
   public Dimension getRowsAndColumnsForRset(int rset) {
      Dimension result = null;
      if (rset >= 0) {
         final double rsetFactor = Math.pow(2.0, rset);
         result = new Dimension((int)Math.ceil(image.getR0ImageWidth() / (rsetFactor * this.getWidth())),
               (int)Math.ceil(image.getR0ImageHeight() / (rsetFactor * this.getHeight())));
      }
      return result;
   }

   /**
    * @return Subtiles of current tile i.e corresponding tiles in getRset() - 1
    */
   public Collection<TileRef> createSubTiles() {
      return createSubTiles(new ArrayList<TileRef>());
   }
   private Collection<TileRef> createSubTiles(Collection<TileRef> subtiles) {
      if (getRset() > 0) {
         final int nextRset = getRset() - 1;
         for (int row=getY()*2; row<Math.min(getY()*2+2, nextRsetRowCols.getHeight()); ++row) {
            for (int col=getX()*2; col<Math.min(getX()*2+2, nextRsetRowCols.getWidth()); ++col) {
               subtiles.add(new TileRef(this.image, nextRset, col, row, getBand()));
            }
         }
      }
      return subtiles;
   }


   /**
    * @param forRset
    * @return Collection of Tiles in this rset that are in this current tile's band
    */
   public Collection<TileRef> getAllTilesForRset(int forRset) {
      Dimension rsetDim = getRowsAndColumnsForRset(forRset);
      List<TileRef> tiles = new ArrayList<TileRef>();
      for (int x=0;x<rsetDim.getWidth();++x) {
         for (int y=0;y<rsetDim.getHeight();++y) {
            tiles.add(new TileRef(this.image, forRset, x, y, getBand()));
         }
      }
      return tiles;
   }
   
   /**
    * @return All tiles of the image, only in current tile's band
    */
   public Collection<TileRef> getAllTiles() {
      Collection<TileRef> allTiles = IntStream.rangeClosed(0, image.getMaxRLevel()).boxed().flatMap(r-> getAllTilesForRset(r).stream()).collect(Collectors.toList());
      return allTiles;
   }

   public Coordinate getGeodeticCenter() {
	   return GeometryUtils.toCoordinate(this.getGeodeticBounds().getCentroid());   
   }
   
   public double getGSD() {
	   return image.getGSD(getGeodeticCenter(), this.getRset());
   }
   
   @Override
   public String toString() {
      return String.format("%d (%d, %d) - %d", getRset(), getX(), getY(), getBand());
   }

   
   /**
    * TileRefs are equal iff they are the same reference or share the same unique key
    */
   @Override
   public boolean equals(Object obj) {
      boolean result = (this == obj);

      if (!result && TileRef.class.isInstance(obj)) {
         TileRef tile = (TileRef) obj;
         result = this.getKey().equals(tile.getKey());
      }
      return result;
   }

   @Override
   public int hashCode() {
      return this.getKey().hashCode();
   }

   /**
    * @return size of tile reference reference 
    */
   public long getSizeInBytes() {
      // Don't account for image in the tile size, just account for the references to them
      return CacheableUtil.getDefault().getSizeInBytesForObjects(getBand(), getRset(), getX(), getY())
            + Double.BYTES;
   }

   /**
    * Calls our ImageTileFetcher to retrieve tile as a CoreImage from storage.
    * @return Tile image data from s3/cache
    * @throws IOException
    */
   public CoreImage getTileImage() throws IOException {
      return image.getTileserverTile(this);
   }
   
   public CoreImage getTileImageMinPriority() throws IOException {
	      return image.getTileserverTile(this, true);
   }

}
