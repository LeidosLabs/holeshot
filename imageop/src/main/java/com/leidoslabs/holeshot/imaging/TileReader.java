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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.image.ColorModel;
import java.awt.image.Raster;
import java.awt.image.SampleModel;
import java.io.IOException;

/**
 * This abstraction encapsulates common attributes and code for classes capable of reading
 * individual tiles for an image.
 */
public abstract class TileReader {

  private static final Logger LOGGER = LoggerFactory.getLogger(TileReader.class);

  private SampleModel mySampleModel = null;
  private ColorModel myColorModel = null;

  // TODO: Implement Band Selection in TileReader

  /**
   * Returns the SampleModel
   */
  public SampleModel getSampleModel() {
    if (mySampleModel == null) {
      mySampleModel = createSampleModel();
    }
    return mySampleModel;
  }

  /**
   * Returns the ColorModel
   */
  public ColorModel getColorModel() {
    if (myColorModel == null) {
      myColorModel = createColorModel();
    }
    return myColorModel;
  }

  /**
   * Reads a tile from the input stream.
   *
   * @param tileX The tile column for the desired tile.
   * @param tileY The tile row for the desired tile.
   */
  public Raster readTile(int tileX, int tileY)
      throws IOException {
    LOGGER.debug("readTile({},{})", tileX, tileY);
    return readRaster(tileX, tileY);
  }

  /**
   * Creates a sample model.
   */
  protected abstract SampleModel createSampleModel();

  /**
   * Creates a color model.
   */
  protected abstract ColorModel createColorModel();

  /**
   * Reads a raster from the input stream.
   *
   * @param tileX The tile column for the desired tile.
   * @param tileY The tile row for the desired tile.
   */
  protected abstract Raster readRaster(int tileX, int tileY)
      throws IOException;

}
