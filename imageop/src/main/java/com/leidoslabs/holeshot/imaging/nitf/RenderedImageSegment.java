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
package com.leidoslabs.holeshot.imaging.nitf;

import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.awt.image.ColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.awt.image.SampleModel;
import java.awt.image.WritableRaster;
import java.io.IOException;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;
import java.util.function.Supplier;

import javax.media.jai.JAI;
import javax.media.jai.PlanarImage;
import javax.media.jai.remote.SerializableRenderedImage;

import org.apache.commons.lang3.NotImplementedException;
import org.apache.commons.lang3.StringUtils;
import org.codice.imaging.nitf.core.common.impl.DateTimeImpl;
import org.codice.imaging.nitf.core.image.ImageCoordinatePair;
import org.codice.imaging.nitf.core.image.ImageCoordinates;
import org.codice.imaging.nitf.core.image.ImageSegment;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;

import com.leidoslabs.holeshot.imaging.ImageKey;
import com.leidoslabs.holeshot.imaging.ImageSourceSegment;
import com.leidoslabs.holeshot.imaging.TileReader;
import com.leidoslabs.holeshot.imaging.photogrammetry.CameraModel;
import com.leidoslabs.holeshot.imaging.photogrammetry.FourCornerBasedCameraModel;
import com.leidoslabs.holeshot.imaging.photogrammetry.rpc.RPCCameraModelFactory;

/**
 * Created by parrise on 9/22/17.
 */
public class RenderedImageSegment implements RenderedImage, ImageSourceSegment {

   protected SampleModel sampleModel;
   protected ColorModel colorModel;
   private Rectangle bounds;
   protected final Map<String, Object> properties;


   private final ImageSegment imageSegment;
   private final TileReader tileReader;
   private javax.media.jai.TileCache tileCache;
   private CameraModel cameraModel;
   private final ImageKey imageKey;

   /**
    * Initializes RenderedImageSegment with a Cache, TIleReader, sample and color model, and camera model
    * @param imageSegment
    * @param fallbackImageKey
    * @param segmentNumber
    * @param numberOfSegments
    */
   public RenderedImageSegment(ImageSegment imageSegment, ImageKey fallbackImageKey, int segmentNumber, int numberOfSegments) {
      // Copy the image key as this key might be modified.
      this.imageKey = new ImageKey(fallbackImageKey);
      this.imageSegment = imageSegment;
      this.tileReader = NITFTileReaderFactory.createTileReader(imageSegment);
      this.sampleModel = tileReader.getSampleModel();
      this.colorModel = tileReader.getColorModel();
      this.bounds = new Rectangle(0, 0, (int) imageSegment.getNumberOfColumns(), (int) imageSegment.getNumberOfRows());
      this.properties = new HashMap<String, Object>();
      this.tileCache =  JAI.getDefaultInstance().getTileCache();


      Map<String, ? extends Object> tres = imageSegment.getTREsFlat();
      final Envelope imageBounds = getImageBounds();
      final Coordinate[] imageGeoBounds = getImageGeoBounds();
      final CameraModel fourCornerBasedCameraModel = getFourCornerBasedCameraModel(imageBounds, imageGeoBounds);
      this.cameraModel = RPCCameraModelFactory.buildRPCCameraFromMetadata((Map<String, Object>)tres, fourCornerBasedCameraModel, imageBounds);

      final String segmentIdentifier = getSegmentIdentifier(imageSegment, segmentNumber, numberOfSegments);
      imageSegment.setIdentifier(segmentIdentifier);
      this.imageKey.setCollectionID(segmentIdentifier);

      if (imageSegment.getImageDateTime() == null) {
         final DateTimeImpl collectTime = new DateTimeImpl();
         collectTime.set(imageKey.getCollectTime());
         imageSegment.setImageDateTime(collectTime);
      } else {
         this.imageKey.setCollectTime(imageSegment.getImageDateTime().getZonedDateTime());
      }
   }

   private CameraModel getFourCornerBasedCameraModel(Envelope imageBounds, Coordinate[] imageGeoBounds) {
      return new FourCornerBasedCameraModel((int)imageBounds.getWidth(),
            (int)imageBounds.getHeight(),
            imageGeoBounds,
            new Point2D.Double(imageBounds.getMinX(), imageBounds.getMinY()));
   }

   private Envelope getImageBounds() {
      return new Envelope(0.0, imageSegment.getNumberOfColumns(), 0.0, imageSegment.getNumberOfRows());
   }

   private Coordinate[] getImageGeoBounds() {
      return toCoordinates(imageSegment.getImageCoordinates());
   }
   private static Coordinate[] toCoordinates(ImageCoordinates imageCoords)
   {
      return new Coordinate[] {
            toCoordinate(imageCoords.getCoordinate00()),
            toCoordinate(imageCoords.getCoordinate0MaxCol()),
            toCoordinate(imageCoords.getCoordinateMaxRowMaxCol()),
            toCoordinate(imageCoords.getCoordinateMaxRow0())
      };

   }
   private static Coordinate toCoordinate(ImageCoordinatePair imageCoord)  {
      return new Coordinate(imageCoord.getLongitude(), imageCoord.getLatitude(), 0.0);
   }
   private String getSegmentIdentifier(ImageSegment imageSegment, int segmentNumber, int numberOfSegments) {
      return getValidIdentifier(() -> imageSegment.getImageIdentifier2(),
            () -> imageSegment.getIdentifier(),
            () -> (numberOfSegments == 1) ? imageKey.getCollectionID() : String.format("%s-%d", imageKey.getCollectionID(), segmentNumber));
   }

   @SafeVarargs
   private final String getValidIdentifier(Supplier<String>...alternatives) {
      return Arrays.stream(alternatives).map(a->a.get()).filter(a->isValidIdentifier(a)).findFirst().orElse(null);
   }
   private static boolean isValidIdentifier(String identifier) {
      return !StringUtils.isEmpty(identifier) && !identifier.equalsIgnoreCase("Missing");
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
    * Returns the ColorModel of the image.
    */
   @Override
   public ColorModel getColorModel() {
      return colorModel;
   }

   /**
    * Returns the SampleModel of the image.
    */
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

   /**
    * Returns the number of tiles in the x direction.
    *
    * @return the number of tiles in the x direction.
    */
   @Override
   public int getNumXTiles() {
	   return imageSegment.getNumberOfBlocksPerRow();
   }

   /**
    * Returns the number of tiles in the y direction.
    *
    * @return the number of tiles in the y direction.
    */
   @Override
   public int getNumYTiles() {
	   return imageSegment.getNumberOfBlocksPerColumn();
   }

   /**
    * Returns the minimum tile index in the x direction.
    *
    * @return the minimum tile index in the x direction.
    */
   @Override
   public int getMinTileX() {
      return 0;
   }

   /**
    * Returns the minimum tile index in the y direction.
    *
    * @return the minimum tile index in the y direction.
    */
   @Override
   public int getMinTileY() {
      return 0;
   }

   /**
    * Returns the tile width in pixels.
    *
    * @return the tile width in pixels.
    */
   @Override
   public int getTileWidth() {
      return (int) imageSegment.getNumberOfPixelsPerBlockHorizontal();
   }

   /**
    * Returns the tile height in pixels.
    *
    * @return the tile height in pixels.
    */
   @Override
   public int getTileHeight() {
      return (int) imageSegment.getNumberOfPixelsPerBlockVertical();
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
      return 0;
   }

   @Override
   public int getTileGridYOffset() {
      return 0;
   }
   @Override
   public int hashCode() {
      return this.imageKey.hashCode();
   }

   private boolean validTile(int tileX, int tileY) {
      return tileX >= getMinTileX() && tileX < (getMinTileX() + getNumXTiles()) &&
            tileY >= getMinTileY() && tileY < (getMinTileY() + getNumYTiles());
   }

   @Override
   public synchronized Raster getTile(int tileX, int tileY) {
      Raster result = null;
      if (validTile(tileX, tileY)) {
         result = (tileCache == null ? null : tileCache.getTile(this, tileX, tileY));
         if (result == null) {
            try {
               result = tileReader.readTile(tileX, tileY);
               if (result != null) {
                  if (tileCache != null) {
                     tileCache.add(this, tileX, tileY, result);
                  }
               }
            } catch (IOException ioe) {
               ioe.printStackTrace();
               result = null;
            }
         }
      }
      return result;
   }

   @Override
   public Raster getData() {
      return this.getData(new Rectangle(this.getMinX(), this.getMinY(), this.getWidth(), this.getHeight()));
   }

   @Override
   public Raster getData(Rectangle region) {
      if (region == null) {
         region = bounds;
      } else if (!region.intersects(bounds)) {
         throw new IllegalArgumentException("Empty region intersection");
      }

      Rectangle xsect = region == bounds ? region : region.intersection(bounds);
      int startTileX = Math.max(this.XToTileX(xsect.x), getMinTileX());
      int startTileY = Math.max(this.YToTileY(xsect.y), getMinTileY());
      int endTileX = Math.min(this.XToTileX(xsect.x + xsect.width - 1), getMinTileX() + getNumXTiles() - 1);
      int endTileY = Math.min(this.YToTileY(xsect.y + xsect.height - 1), getMinTileY() + getNumYTiles() - 1);
      int nbands = sampleModel.getNumBands();

      SampleModel resultSampleModel = this.sampleModel;
      if (resultSampleModel.getWidth() != region.width || resultSampleModel.getHeight() != region.height) {
         resultSampleModel = resultSampleModel.createCompatibleSampleModel(region.width, region.height);
      }
      WritableRaster resultRaster = WritableRaster.createWritableRaster(resultSampleModel, region.getLocation());

      for (int y = startTileY; y <= endTileY; ++y) {
         for (int x = startTileX; x <= endTileX; ++x) {
            Raster tile = this.getTile(x, y);
            Rectangle subRegion = region.intersection(tile.getBounds());
            if (!subRegion.isEmpty()) {
               Raster subRaster = tile.createChild(subRegion.x, subRegion.y, subRegion.width, subRegion.height, subRegion.x, subRegion.y, null);
               switch (resultSampleModel.getDataType()) {
               case DataBuffer.TYPE_FLOAT:
                  resultRaster.setPixels(subRegion.x, subRegion.y, subRegion.width, subRegion.height,
                        subRaster.getPixels(subRegion.x, subRegion.y, subRegion.width, subRegion.height,
                              new float[nbands * subRegion.width * subRegion.height]));
                  break;
               case DataBuffer.TYPE_DOUBLE:
                  resultRaster.setPixels(subRegion.x, subRegion.y, subRegion.width, subRegion.height,
                        subRaster.getPixels(subRegion.x, subRegion.y, subRegion.width, subRegion.height,
                              new double[nbands * subRegion.width * subRegion.height]));
                  break;
               default:
                  resultRaster.setPixels(subRegion.x, subRegion.y, subRegion.width, subRegion.height,
                        subRaster.getPixels(subRegion.x, subRegion.y, subRegion.width, subRegion.height,
                              new int[nbands * subRegion.width * subRegion.height]));
               }
            }
         }
      }

      return resultRaster;
   }

   @Override
   public WritableRaster copyData(WritableRaster raster) {
      Rectangle xsect;
      if(raster == null) {
         xsect = bounds;
         SampleModel sampleModel = this.getSampleModel();
         if(sampleModel.getWidth() != xsect.width || sampleModel.getHeight() != xsect.height) {
            sampleModel = sampleModel.createCompatibleSampleModel(xsect.width, xsect.height);
         }

         raster = WritableRaster.createWritableRaster(sampleModel, xsect.getLocation());
      } else {
         xsect = raster.getBounds().intersection(this.bounds);
         if(xsect.isEmpty()) {
            return raster;
         }
      }

      int startTileX = Math.max(this.XToTileX(xsect.x), getMinTileX());
      int startTileY = Math.max(this.YToTileY(xsect.y), getMinTileY());
      int endTileX = Math.min(this.XToTileX(xsect.x + xsect.width - 1), getMinTileX() + getNumXTiles() - 1);
      int endTileY = Math.min(this.YToTileY(xsect.y + xsect.height - 1), getMinTileY() + getNumYTiles() - 1);
      int nbands = raster.getSampleModel().getNumBands();

      for (int y = startTileY; y <= endTileY; ++y) {
         for (int x = startTileX; x <= endTileX; ++x) {
            Raster tile = this.getTile(x, y);
            Rectangle subRegion = xsect.intersection(tile.getBounds());
            if (!subRegion.isEmpty()) {
               Raster subRaster = tile.createChild(subRegion.x, subRegion.y, subRegion.width, subRegion.height, subRegion.x, subRegion.y, null);
               switch (sampleModel.getDataType()) {
               case DataBuffer.TYPE_FLOAT:
                  raster.setPixels(subRegion.x, subRegion.y, subRegion.width, subRegion.height,
                        subRaster.getPixels(subRegion.x, subRegion.y, subRegion.width, subRegion.height,
                              new float[nbands * subRegion.width * subRegion.height]));
                  break;
               case DataBuffer.TYPE_DOUBLE:
                  raster.setPixels(subRegion.x, subRegion.y, subRegion.width, subRegion.height,
                        subRaster.getPixels(subRegion.x, subRegion.y, subRegion.width, subRegion.height,
                              new double[nbands * subRegion.width * subRegion.height]));
                  break;
               default:
                  raster.setPixels(subRegion.x, subRegion.y, subRegion.width, subRegion.height,
                        subRaster.getPixels(subRegion.x, subRegion.y, subRegion.width, subRegion.height,
                              new int[nbands * subRegion.width * subRegion.height]));
               }
            }
         }
      }

      return raster;
   }


   public static int XToTileX(int x, int tileGridXOffset, int tileWidth) {
      x -= tileGridXOffset;
      if (x < 0) {
         x += tileWidth - 1;
      }
      return x / tileWidth;
   }

   public static int YToTileY(int y, int tileGridYOffset, int tileHeight) {
      y -= tileGridYOffset;
      if (y < 0) {
         y += tileHeight - 1;
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


   @Override
   public RenderedImage getRenderedImage() {
      return this;
   }


   @Override
   public Map<String, Object> getMetadata() {
      throw new NotImplementedException("getMetadata not implemented for NITF at this time");
   }


   @Override
   public CameraModel getCameraModel() {
      return this.cameraModel;
   }

   @Override
   public ImageKey getImageKey() {
      return this.imageKey;
   }

   @Override
   public void close() throws Exception {
      // TODO Auto-generated method stub

   }


   @Override
   public Object getSegmentMetadataObject() {
      return this.imageSegment;
   }


   public void setCameraModel(CameraModel cameraModel) {
      this.cameraModel = cameraModel;
   }
}
