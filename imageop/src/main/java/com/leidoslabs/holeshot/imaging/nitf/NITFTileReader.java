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

import java.awt.Point;
import java.awt.image.ColorModel;
import java.awt.image.Raster;
import java.awt.image.SampleModel;
import java.io.IOException;

import javax.imageio.stream.ImageInputStream;

import org.codice.imaging.nitf.core.image.ImageCompression;
import org.codice.imaging.nitf.core.image.ImageSegment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.leidoslabs.holeshot.imaging.BandRepresentations;
import com.leidoslabs.holeshot.imaging.TileReader;

/**
 * Tile Reader for NITFImages
 */
public abstract class NITFTileReader extends TileReader {

   private static final Logger LOGGER = LoggerFactory.getLogger(NITFTileReader.class);

   private static final long MAX_BUF_SIZE_IN_BYTES = 128L * 1024L * 1024L; // 128 MB.  Must be an even number
   private final ImageSegment imageSegment;
   private final ImageMask imageMask;
   private final SampleModel sampleModel;
   private final ColorModel colorModel;
   private static final int TILE_SIZE = 512;

   /**
    * Initializes NI
    * @param imageSegment
    * @throws IOException
    */
   public NITFTileReader(final ImageSegment imageSegment) throws IOException {

      this.imageSegment = imageSegment;
      this.sampleModel = NITFSampleModelFactory.createSampleModel(this.imageSegment);
      this.imageMask = getImageMask(imageSegment);

      // TODO: Get rid of this BandRepresentations usage here.
      // All we really need to know is if there is an alpha band for this image. We should be able to
      // get that directly from the image segment and then use it as a flag for the color model
      // creation.
      String[] bandReps = new String[this.imageSegment.getNumBands()];
      for (int i=0;i<bandReps.length;i++) {
         bandReps[i] = this.imageSegment.getImageBandZeroBase(i).getImageRepresentation();
      }
      this.colorModel = NITFColorModelFactory.createColorModel(this.sampleModel,new BandRepresentations(bandReps));
   }

   public ImageSegment getImageSegment() {
      return imageSegment;
   }

   public ImageMask getImageMask() { return imageMask; }

   @Override
   protected SampleModel createSampleModel() {
      return sampleModel;
   }

   @Override
   public ColorModel createColorModel() {
      return colorModel;
   }

   protected Raster createEmptyRaster(int tileX, int tileY) {
      Point origin = new Point(tileXToX(tileX), tileYToY(tileY));

      final SampleModel sm = getSampleModel();
      return Raster.createWritableRaster(sm, origin);
   }

   protected int tileXToX(int tx) {
      // TODO: There was a + tileGridOffsetX in this calc; figure out if it is needed
      return tx * (int) imageSegment.getNumberOfPixelsPerBlockHorizontal();
   }

   protected int tileYToY(int ty) {
      // TODO: There was a + tileGridOffsetY in this calc; figure out if it is needed
      return ty * (int) imageSegment.getNumberOfPixelsPerBlockVertical();
   }

   protected ImageMask getImageMask(final ImageSegment imageSegment) throws IOException {
      if (ImageCompression.NOTCOMPRESSEDMASK.equals(imageSegment.getImageCompression())) {
         return new ImageMask(imageSegment, imageSegment.getData());
      } else {
         return new ImageMask(imageSegment);
      }
   }

   /**
    * Reads the specified number of bytes from the input stream.
    *
    * @param input        The stream from which the bytes are to be read.
    * @param buffer       The buffer into which the bytes are to be read.
    * @param bufferOffset The buffer location into which the bytes are read.
    * @param count        The number of bytes to read.
    */
   public static void readBytes(ImageInputStream input,
         byte[] buffer,
         long bufferOffset,
         long count) throws IOException {
      long offset = 0;
      long byteCount = count;
      long eod = byteCount;

      while (offset < eod) {
         int bytesRead = input.read(buffer, (int)offset, (int)(eod - offset));
         if (bytesRead < 1) {
            if (bytesRead < 0) {
               throw new IOException("Attempt to read past end of file");
            } else {
               throw new IOException("Zero bytes read");
            }
         } else {
            offset += bytesRead;
         }
      }
   }

   /**
    * Reads the specified number of short integers from the input stream.
    *
    * @param input        The stream from which the integers are to be read.
    * @param buffer       The buffer into which the integers are to be read.
    * @param bufferOffset The buffer location into which the bytes are read.
    * @param count        The number of short integers to read.
    */
    public static void readShorts(ImageInputStream input,
         short[] buffer,
         long bufferOffset,
         long count) throws IOException {

      final long byteCount = shortsToBytes(count);
      final long bufSize = Math.min(MAX_BUF_SIZE_IN_BYTES, byteCount);
      final byte[] internalBuffer = new byte[(int)bufSize];

      for (long offset = 0; offset<byteCount; offset+=bufSize) {
         final int readSize = (int)Math.min(byteCount-offset, bufSize);

         int bytesRead = input.read(internalBuffer, 0, readSize);
         if (bytesRead < 1) {
            if (bytesRead < 0) {
               throw new IOException("Attempt to read past end of file " + input.length());
            } else {
               throw new IOException("Zero bytes read");
            }  // end if
         } else {
            final int readSizeShorts = (int)bytesToShorts(readSize);
            final int offsetShorts = (int)bytesToShorts(offset);

            if (LOGGER.isDebugEnabled()) {
               LOGGER.debug(String.format("Setting [%d -> %d] from [%d -> %d]",
                     offsetShorts+bufferOffset, offsetShorts+bufferOffset + readSizeShorts - 1,
                     shortsToBytes(0), shortsToBytes(readSizeShorts)));
            }
            for (int j=0;j<readSizeShorts; ++j) {
               final int srcOffset = (int)shortsToBytes(j);
               buffer[(int) (j+offsetShorts+bufferOffset)] = (short) (((internalBuffer[srcOffset] & 0xff) << 8)
                     | (internalBuffer[srcOffset + 1] & 0xff));
            }
          }
      }
    }


   /**
    * Reads the specified number of integers from the input stream.
    *
    * @param input The stream from which the integers are to be read.
    * @param buffer The buffer into which the integers are to be read.
    * @param bufferOffset The buffer location into which the integers are read.
    * @param count The number of integers to read.
    */
    public static void readInts(ImageInputStream input,
          int[] buffer,
          long bufferOffset,
          long count) throws IOException {

       final long byteCount = intsToBytes(count);
       final long bufSize = Math.min(MAX_BUF_SIZE_IN_BYTES, byteCount);
       final byte[] internalBuffer = new byte[(int)bufSize];

       for (long offset = 0; offset<byteCount; offset+=bufSize) {
          final int readSize = (int)Math.min(byteCount-offset, bufSize);

          int bytesRead = input.read(internalBuffer, 0, readSize);
          if (bytesRead < 1) {
             if (bytesRead < 0) {
                throw new IOException("Attempt to read past end of file " + input.length());
             } else {
                throw new IOException("Zero bytes read");
             }  // end if
          } else {
             final int readSizeInts = (int)bytesToInts(readSize);
             final int offsetInts = (int)bytesToInts(offset);
             for (int j=0;j<readSizeInts;++j) {
                final int srcOffset = (int)intsToBytes(j);
                buffer[(int) (j + offsetInts + bufferOffset)] = (int) ((((internalBuffer[srcOffset] & 0xff) << 24)
                      | (internalBuffer[srcOffset + 1] & 0xff) << 16)
                      | (internalBuffer[srcOffset + 2] & 0xff) << 8)
                      | (internalBuffer[srcOffset + 3] & 0xff);
             }
           }
       }
     }

    private static long shortsToBytes(long count) {
       return count * Short.BYTES;
    }
    private static long bytesToShorts(long count) {
       return count / Short.BYTES;
    }
    private static long intsToBytes(long count) {
       return count * Integer.BYTES;
    }
    private static long bytesToInts(long count) {
       return count / Integer.BYTES;
    }


}
