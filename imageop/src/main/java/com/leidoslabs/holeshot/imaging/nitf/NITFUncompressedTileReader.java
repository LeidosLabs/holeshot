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

import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.DataBufferInt;
import java.awt.image.DataBufferUShort;
import java.awt.image.Raster;
import java.io.IOException;
import java.util.stream.IntStream;

import javax.imageio.stream.ImageInputStream;

import org.codice.imaging.nitf.core.image.ImageMode;
import org.codice.imaging.nitf.core.image.ImageSegment;

/**
 * TileReader for UncompressedNITF
 */
public class NITFUncompressedTileReader extends NITFTileReader {
   public NITFUncompressedTileReader(final ImageSegment imageSegment) throws IOException {
      super(imageSegment);
   }


   @Override
   protected Raster readRaster(int tileX, int tileY) throws IOException {
      final int blockNumber = tileX + tileY * getImageSegment().getNumberOfBlocksPerRow();
      Raster result = this.createEmptyRaster(tileX, tileY);
      final ImageMask imageMask = getImageMask();

      if (!imageMask.isMaskedBlock(blockNumber, 0)) {
         final long pixelsPerBlock = getImageSegment().getNumberOfPixelsPerBlockHorizontal() * getImageSegment().getNumberOfPixelsPerBlockVertical();
         final long bitsPerPixel = getImageSegment().getActualBitsPerPixelPerBand();
         final long bytesPerPixel = (bitsPerPixel + 7) / 8;

         // BPP of 1 is a special case whereby a single byte can hold multiple pixels.
         final long imageDataOffset = getImageMask().getImdatoff();

         final int numberOfBands = getImageSegment().getNumBands();
         final ImageMode imageMode = getImageSegment().getImageMode();

         TileSampleModel.Factory tsFactory = new TileSampleModel.Factory();
         long blockSize;

         switch (imageMode) {
         case BANDSEQUENTIAL:
            blockSize = (bitsPerPixel == 1) ? ((pixelsPerBlock + 7) / 8) : pixelsPerBlock * bytesPerPixel;
            tsFactory.setReadOffset(blockSize * (getImageSegment().getNumberOfBlocksPerColumn() * getImageSegment().getNumberOfBlocksPerRow()));
            tsFactory.setNumElementsToRead(pixelsPerBlock);
            tsFactory.setNumberOfReads(numberOfBands);
            break;
         case PIXELINTERLEVE:
            blockSize = (bitsPerPixel == 1) ? ((pixelsPerBlock + 7) / 8) : pixelsPerBlock * bytesPerPixel * numberOfBands;
            tsFactory.setReadOffset(0);
            tsFactory.setNumElementsToRead(blockSize / bytesPerPixel);
            tsFactory.setNumberOfReads(1);
            break;
         case BLOCKINTERLEVE:
            blockSize = (bitsPerPixel == 1) ? ((pixelsPerBlock + 7) / 8) : pixelsPerBlock * bytesPerPixel;
            tsFactory.setReadOffset(blockSize);
            tsFactory.setNumElementsToRead(blockSize / bytesPerPixel);
            tsFactory.setNumberOfReads(numberOfBands);
            break;
         default:
            blockSize = (bitsPerPixel == 1) ? ((pixelsPerBlock + 7) / 8) : pixelsPerBlock * bytesPerPixel;
            tsFactory.setReadOffset(0);
            tsFactory.setNumElementsToRead(blockSize / bytesPerPixel * numberOfBands);
            tsFactory.setNumberOfReads(1);
            break;
         }

         final long firstBlockOffset = imageDataOffset + IntStream.range(0,blockNumber).filter(i-> !imageMask.isMaskedBlock(i, 0)).mapToLong(i->blockSize).sum();
         final TileSampleModel tileSampleModel = tsFactory.build();
         final ImageInputStream dataStream = getImageSegment().getData();
         final DataBuffer dataBuffer = result.getDataBuffer();

         synchronized(dataStream) {
            for (int i=0;i<tileSampleModel.getNumberOfReads();++i) {

               final int numElementsToRead = (int)tileSampleModel.getNumElementsToRead();
               final int destinationOffset = i * numElementsToRead;

               final long seekTo = firstBlockOffset + i*tileSampleModel.getReadOffset();
               dataStream.seek(seekTo);

               switch (dataBuffer.getDataType()) {
               case DataBuffer.TYPE_BYTE: {
                  byte[] sampleBuffer = ((DataBufferByte) dataBuffer).getData();
                  readBytes(dataStream, sampleBuffer, destinationOffset, numElementsToRead);
                  break;
               }
               case DataBuffer.TYPE_USHORT: {
                  short[] sampleBuffer = ((DataBufferUShort) dataBuffer).getData();
                  readShorts(dataStream, sampleBuffer, destinationOffset, numElementsToRead);
                  break;
               }
               case DataBuffer.TYPE_SHORT: {
                  short[] sampleBuffer = ((DataBufferUShort) dataBuffer).getData();
                  readShorts(dataStream, sampleBuffer, destinationOffset, numElementsToRead);
                  break;
               }
               case DataBuffer.TYPE_INT: {
                  int[] sampleBuffer = ((DataBufferInt) dataBuffer).getData();
                  readInts(dataStream, sampleBuffer, destinationOffset, numElementsToRead);
                  break;
               }
               }
            }
         }
      }
      return result;
   }
}
