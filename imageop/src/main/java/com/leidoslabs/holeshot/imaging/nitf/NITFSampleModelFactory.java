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

import java.awt.image.ComponentSampleModel;
import java.awt.image.DataBuffer;
import java.awt.image.PixelInterleavedSampleModel;
import java.awt.image.SampleModel;
import java.util.Arrays;
import java.util.stream.IntStream;

import org.codice.imaging.nitf.core.image.ImageSegment;
import org.codice.imaging.nitf.core.image.PixelValueType;

/**
 * Created by parrise on 8/24/17.
 */
public class NITFSampleModelFactory {

   /**
    * @param imageSegment
    * @return Create a SampleModel based on imageSegments's imageMode
    */
	public static SampleModel createSampleModel(final ImageSegment imageSegment) {

      int dataType = selectDataType(
            imageSegment.getNumberOfBitsPerPixelPerBand(),
            imageSegment.getPixelValueType());

      System.out.println("NITFSampleModelFactory dataType: " + dataType);

      System.out.println("NITFSampleModelFactory imageMode: " + imageSegment.getImageMode());

      System.out.println("NITFSampleModelFactory blockSize (bytes)" + imageSegment.getNumberOfBytesPerBlock());

      SampleModel result = null;

      switch (imageSegment.getImageMode()) {

      case BLOCKINTERLEVE: {
         if (imageSegment.getActualBitsPerPixelPerBand() == 1) {
            result = new NITFSingleBandUncompressedSampleModel(
                  (int) imageSegment.getNumberOfPixelsPerBlockHorizontal(),
                  (int) imageSegment.getNumberOfPixelsPerBlockVertical(),
                  imageSegment.getNumberOfBitsPerPixelPerBand(),
                  imageSegment.getActualBitsPerPixelPerBand());
         } else {
            final int offset =
                  (int)(imageSegment.getNumberOfPixelsPerBlockHorizontal() * imageSegment.getNumberOfPixelsPerBlockVertical());// *

            final int[] bandOffsets = IntStream.range(0, imageSegment.getNumBands()).map(i-> i * offset).toArray();
            final int[] bankIndices = IntStream.range(0, imageSegment.getNumBands()).map(i->0).toArray();
            result = new ComponentSampleModel(
                  dataType,
                  (int) imageSegment.getNumberOfPixelsPerBlockHorizontal(),
                  (int) imageSegment.getNumberOfPixelsPerBlockVertical(),
                  1,
                  (int) imageSegment.getNumberOfPixelsPerBlockHorizontal(),
                  bankIndices,
                  bandOffsets);
         }
      }
      break;
      case PIXELINTERLEVE: {
         int[] offsets = IntStream.range(0, imageSegment.getNumBands()).toArray();

         result = new PixelInterleavedSampleModel(
               dataType,
               (int) imageSegment.getNumberOfPixelsPerBlockHorizontal(),
               (int) imageSegment.getNumberOfPixelsPerBlockVertical(),
               imageSegment.getNumBands(),
               (int) imageSegment.getNumberOfPixelsPerBlockHorizontal() * imageSegment.getNumBands(),
               offsets);
      }
      break;
      case ROWINTERLEVE:
         break;
      case BANDSEQUENTIAL: {
         final long offset =
               (imageSegment.getNumberOfPixelsPerBlockHorizontal() * imageSegment.getNumberOfPixelsPerBlockVertical());// *

         long[] bandOffsets = IntStream.range(0, imageSegment.getNumBands()).mapToLong(i-> i * offset).toArray();
         int[] intBandOffsets = Arrays.stream(bandOffsets).mapToInt(x->(int)x).toArray();

         result = new ComponentSampleModel(dataType,
               (int) imageSegment.getNumberOfPixelsPerBlockHorizontal(),
               (int) imageSegment.getNumberOfPixelsPerBlockVertical(),
               1,
               (int) imageSegment.getNumberOfPixelsPerBlockHorizontal(),
               intBandOffsets);
      }
      break;
      case UNKNOWN:
      default:
         break;

      }
      return result;
   }

   private static int selectDataType(int bitsPerSample, final PixelValueType pixelValueType) {
      int dataType = DataBuffer.TYPE_UNDEFINED;

      if (bitsPerSample <= 8) {
         dataType = DataBuffer.TYPE_BYTE;
      } else if (bitsPerSample <= 16
            && PixelValueType.SIGNEDINTEGER.equals(pixelValueType)) {
         dataType = DataBuffer.TYPE_SHORT;
      } else if (bitsPerSample <= 16
            && PixelValueType.INTEGER.equals(pixelValueType)) {
         dataType = DataBuffer.TYPE_USHORT;
      } else if (bitsPerSample == 32
            && (PixelValueType.INTEGER.equals(pixelValueType)
                  || PixelValueType.SIGNEDINTEGER.equals(pixelValueType))) {
         dataType = DataBuffer.TYPE_INT;
      } else if (bitsPerSample == 32
            && PixelValueType.REAL.equals(pixelValueType)) {
         dataType = DataBuffer.TYPE_FLOAT;
      } else if (bitsPerSample == 64
            && PixelValueType.REAL.equals(pixelValueType)) {
         dataType = DataBuffer.TYPE_DOUBLE;
      }
      return dataType;
   }
}
