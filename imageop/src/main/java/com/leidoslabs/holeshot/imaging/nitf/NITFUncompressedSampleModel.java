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
import java.awt.image.SampleModel;

/**
 * Created by parrise on 9/20/17.
 */
public abstract class NITFUncompressedSampleModel extends SampleModel {


  protected final int nbpp;
  protected final int abpp;
  protected final int numDataElems;
  protected final int[] sampleSize;

  /**
   * Constructs a SampleModel with the specified parameters.
   *
   * @param w        The width (in pixels) of the region of image data.
   * @param h        The height (in pixels) of the region of image data.
   * @param numBands The number of bands of the image data.
   * @param nbpp     The number of bits per pixel
   * @param abpp     The actual number of bits used to store each pixel
   * @throws IllegalArgumentException if <code>w</code> or <code>h</code> is not greater than 0
   * @throws IllegalArgumentException if the product of <code>w</code> and <code>h</code> is greater
   *                                  than <code>Integer.MAX_VALUE</code>
   * @throws IllegalArgumentException if <code>dataType</code> is not one of the supported data
   *                                  types
   */
  public NITFUncompressedSampleModel(int w, int h, int numBands, int nbpp, int abpp) {
    super(DataBuffer.TYPE_BYTE, w, h, numBands);
    this.nbpp = nbpp;
    this.abpp = abpp;
    this.numDataElems = (nbpp + 7) / 8;
    this.sampleSize = new int[numBands];
    for (int b=0;b<sampleSize.length;b++) {
      sampleSize[b] = abpp;
    }
  }

  @Override
  public int getNumDataElements() {
    return numDataElems;
  }

  @Override
  public int[] getSampleSize() {
    return sampleSize;
  }

  @Override
  public int getSampleSize(int band) {
    return sampleSize[band];
  }

  private static final int[] BIT_MASK = {0xFF, 0x7F, 0x3F, 0x1F, 0xF, 0x07, 0x03, 0x01};

  protected static int getBitsAsInt(byte[] bytes, int offset, int len) {
/*
    int val = 0;
    int bys = offset / 8;
    int bye = (offset + len) / 8;
    int bis = offset % 8;
    int bie = (offset + len) % 8;
    for (int b = bys; b <= bye; b++) {
      val = val << 8;
      val += bytes[b] & (b == bys ? BIT_MASK[bis] : 0xFF);
    }
    val = val >> (8 - bie);
    return val;
    */

    int bys = offset / 8;
    int bis = offset % 8;

    long val = ((((bys < 0 || bys >= bytes.length) ? 0L : bytes[bys]) << 32) |
        (((bys+1 < 0 || bys+1 >= bytes.length) ? 0L : bytes[bys+1]) << 24) |
        (((bys+2 < 0 || bys+2 >= bytes.length) ? 0L : bytes[bys+2]) << 16) |
        (((bys+3 < 0 || bys+3 >= bytes.length) ? 0L : bytes[bys+3]) << 8) |
        (((bys+4 < 0 || bys+4 >= bytes.length) ? 0L : bytes[bys+4]))) << (24+bis) >>> (64 - Math.min(len,32));
    return (int)val;
  }

  protected static void setBitsFromInt(byte[] bytes, int offset, int len, int val) {

    int bie = (offset + len) % 8;
    val = val << 8 - bie;

    int bys = offset / 8;
    int bye = (offset + len) / 8;
    for (int b = bye; b >= bys; b--) {
      bytes[b] |= (val & 0xFF);
      val = val >> 8;
    }

  }
}
