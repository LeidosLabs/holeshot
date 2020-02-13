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

import java.io.IOException;

import org.codice.imaging.nitf.core.image.ImageSegment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.leidoslabs.holeshot.imaging.TileReader;

/**
 * Factory class for NIFTTileReader
 */
public class NITFTileReaderFactory {
   private static final Logger LOGGER = LoggerFactory.getLogger(TileReader.class);

   /**
    * @param imageSegment
    * @return TileReader based on imageSegment's compression.
    */
   public static TileReader createTileReader(final ImageSegment imageSegment) {
      TileReader result = null;
      LOGGER.debug("Compression: {}", imageSegment.getImageCompression());
      LOGGER.debug("ImageMode: {}", imageSegment.getImageMode());
      LOGGER.debug("ImageRepresentation: {}", imageSegment.getImageRepresentation());
      LOGGER.debug("NBBP-PerBand: {}", imageSegment.getNumberOfBitsPerPixelPerBand());

      try {
         switch (imageSegment.getImageCompression()) {
         case NOTCOMPRESSED:
         case NOTCOMPRESSEDMASK:
            result = new NITFUncompressedTileReader(imageSegment);
            break;
         case LOSSLESSJPEG:
         case DOWNSAMPLEDJPEG:
         case JPEG:
            result = new NITFJPEGCompressedTileReader(imageSegment);
            break;
         case JPEG2000:
        	 result = new NITFJP2KCompressedTileReader(imageSegment);
        	 break;
         default:
            LOGGER.error(String.format("We don't currently support %s compression", imageSegment.getImageCompression().toString()));
            break;
         }
      } catch (IOException ioe) {
         return null;
      }
      return result;
   }

}
