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

import java.awt.image.BufferedImage;
import java.awt.image.Raster;
import java.io.IOException;
import java.util.Iterator;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;

import org.codice.imaging.nitf.core.image.ImageSegment;

/**
 * NIFTTileReader capable of handling JPEG NITFs. Overrides readRaster
 */
public class NITFJPEGCompressedTileReader extends NITFTileReader {

  private static final int BYTE_MASK = 0xFF;
  private static final int START_OF_IMAGE = (short)0xFFD8;

  public NITFJPEGCompressedTileReader(final ImageSegment imageSegment) throws IOException {
    super(imageSegment);
  }

  @Override
  /**
   * Using a JPEG compatible ImageReader, returns a Raster of the tile location
   */
  protected Raster readRaster(int tileX, int tileY) throws IOException {
    ImageReader reader = getImageReader("image/jpeg");
    ImageInputStream imageInputStream = getImageSegment().getData();
    // TODO: Check to see if this is correct. Maybe seeking to the mask offset will avoid the skip to marker
    imageInputStream.seek(0);
    skipToMarker(imageInputStream, START_OF_IMAGE);
    reader.setInput(imageInputStream);

    // TODO: Subtract any masked blocks that would have been before this block.
    BufferedImage image = reader.read(tileX + tileY * getImageSegment().getNumberOfBlocksPerRow());

    return image.getData();
  }

  private void skipToMarker(final ImageInputStream imageInputStream, final int markerCode) throws IOException {
    imageInputStream.mark();
    byte fillByte = (byte) ((markerCode >> Byte.SIZE) & BYTE_MASK);
    byte markerByte = (byte) (markerCode & BYTE_MASK);

    int i = 0;
    byte a = imageInputStream.readByte();

    while (a == fillByte) {
      i++;
      a = imageInputStream.readByte();
    }

    imageInputStream.reset();

    if (a == markerByte) {
      imageInputStream.skipBytes(i - 1);
    }
  }

  private ImageReader getImageReader(final String mediaType) {
    Iterator<ImageReader> imageReaders = ImageIO.getImageReadersByMIMEType(mediaType);

    if (imageReaders == null || !imageReaders.hasNext()) {
      throw new UnsupportedOperationException(
          String.format("NitfRenderer.render(): no ImageReader found for media type '%s'.", mediaType));
    }

    return imageReaders.next();
  }
}
