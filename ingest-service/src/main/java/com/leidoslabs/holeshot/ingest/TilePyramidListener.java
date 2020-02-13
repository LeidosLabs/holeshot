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
package com.leidoslabs.holeshot.ingest;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;

import com.leidoslabs.holeshot.elt.tileserver.TileserverImage;
import com.leidoslabs.holeshot.tileserver.mrf.MRFIndexFile;
import com.leidoslabs.holeshot.tileserver.mrf.MRFTileRef;
import com.leidoslabs.holeshot.imaging.ImageKey;
import com.leidoslabs.holeshot.imaging.metadata.TilePyramidDescriptor;

public abstract class TilePyramidListener {
   
   private SortedSet<MRFTileRef> outputTiles;

    /**
     * Implementation specific publication of metadata, such as publishing it to an external topic or log (like SNS)
     * @param imageKey The imagekey for the associated metadata
     * @param metadata Metadata to be formatted and published
     * @throws Exception
     */
   protected abstract void handleMetadataInternal(ImageKey imageKey, TilePyramidDescriptor metadata) throws Exception;

    /**
     * Implementation specific publication of tile data
     * @param imageKey Unique reference to the image this tile is part of
     * @param band tile image band
     * @param rlevel tile level in reduced resolution pyramid
     * @param c tile column
     * @param r tile row
     * @param tile tile image data
     * @return the number of bytes in the published image tile
     * @throws Exception
     */
   protected abstract long handleTileInternal(ImageKey imageKey, int band, int rlevel, int c, int r, BufferedImage tile) throws Exception;

    /**
     * Implementation specific publication of MRF index data file
     * @param imageKey Unique reference to the image this index refers to
     * @param indexFile THe MRF index file data to be serialized and published
     * @throws Exception
     */
   protected abstract void handleMRFInternal(ImageKey imageKey, MRFIndexFile indexFile) throws Exception;

    /**
     * Create a new TilePyramidListener that tracks the tiles it publishes
     */
   public TilePyramidListener() {
      outputTiles = new TreeSet<MRFTileRef>();
   }

    /**
     * Publish metadata as defined by implementation specific handleMetadataInternal
     * @param imageKey The imagekey for the associated metadata
     * @param metadata Metadata to be formatted and published
     * @throws Exception on failure of internal metadata handler implementation
     */
   public final void handleMetadata(ImageKey imageKey, TilePyramidDescriptor metadata) throws Exception {
      handleMetadataInternal(imageKey, metadata);
   }

    /**
     * Publish a completed tile as defined by implementation specific handleTileInternal
     * @param imageKey Unique reference to the image this tile is part of
     * @param band tile image band
     * @param rlevel tile level in reduced resolution pyramid
     * @param c tile column
     * @param r tile row
     * @param tile tile image data
     * @throws Exception on failure of handleTileInternal implementation
     */
   public final void handleTile(ImageKey imageKey, int band, int rlevel, int c, int r, BufferedImage tile) throws Exception {
      final long tileSize = handleTileInternal(imageKey, band, rlevel, c, r, tile);
      outputTiles.add(new MRFTileRef(imageKey.getCollectionID(), imageKey.getCollectTimeString(), rlevel, band, c, r, tileSize));
   }

    /**
     * Clear the list of tiles published since initialization or last flush
     */
   public final void flushOutputTiles() {
      outputTiles.clear();
   }

    /**
     * Consolidate published tile data into an index file representation and then publish that data as an
     * MRF Index file using implementation specific handler
     * @param imageKey Unique reference to the image the index file will refer to
     * @throws Exception
     */
   public final void handleMRF(ImageKey imageKey) throws Exception {
      MRFIndexFile indexFile = new MRFIndexFile(imageKey, getOutputTiles());
      handleMRFInternal(imageKey, indexFile);
   }

    /**
     * Get the set of tiles published since initialization or last flushOutputTiles call
     * @return a set of tile references
     */
   protected SortedSet<MRFTileRef> getOutputTiles() {
      return outputTiles;
   }

    /**
     * Write the BufferedImage image to an OutputStream with png formatting
     * @param out The outputstream to write the image data to
     * @param image the image data to write
     * @return the number of bytes in the image/outputstream
     * @throws IOException Failure to create an ImageOutputStream, write to it, or write data to final OutputStream
     */
   protected long writeImage(OutputStream out, BufferedImage image) throws IOException {
      long imageSize = -1; 
      ImageWriter writer =  ImageIO.getImageWritersByFormatName("png").next();
      try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
         try (ImageOutputStream ios = ImageIO.createImageOutputStream(bos)) {
            writer.setOutput(ios);
            ImageWriteParam param = writer.getDefaultWriteParam();
            if (param.canWriteCompressed()){
               param.setCompressionMode(ImageWriteParam.MODE_DISABLED);
            }
            writer.write(null, new IIOImage(image, null, null), param);
         }
         final byte[] imageBytes = bos.toByteArray();
         imageSize = imageBytes.length;
         out.write(imageBytes);
      } finally {
         writer.dispose();
      }
      return imageSize;
   }
}
