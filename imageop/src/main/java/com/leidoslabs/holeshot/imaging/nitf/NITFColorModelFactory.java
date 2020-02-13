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

import java.awt.Transparency;
import java.awt.color.ColorSpace;
import java.awt.color.ICC_ColorSpace;
import java.awt.color.ICC_Profile;
import java.awt.image.ColorModel;
import java.awt.image.ComponentColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.IndexColorModel;
import java.awt.image.SampleModel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.leidoslabs.holeshot.imaging.BandRepresentations;

/**
 * Factory class for creating a ColorModel from a Sample model
 */
public class NITFColorModelFactory {

  private static final Logger LOGGER = LoggerFactory.getLogger(NITFColorModelFactory.class);

  private static final int[] GrayBits8 = {8};
  private static final ComponentColorModel colorModelGray8 =
      new ComponentColorModel(ColorSpace.getInstance(ColorSpace.CS_GRAY),
          GrayBits8, false, false,
          Transparency.OPAQUE,
          DataBuffer.TYPE_BYTE);
  private static final int[] GrayAlphaBits8 = {8, 8};
  private static final ComponentColorModel colorModelGrayAlpha8 =
      new ComponentColorModel(ColorSpace.getInstance(ColorSpace.CS_GRAY),
          GrayAlphaBits8, true, false,
          Transparency.TRANSLUCENT,
          DataBuffer.TYPE_BYTE);
  private static final int[] RGBBits8 = {8, 8, 8};
  private static final ComponentColorModel colorModelRGB8 =
      new ComponentColorModel(ColorSpace.getInstance(ColorSpace.CS_sRGB),
          RGBBits8, false, false,
          Transparency.OPAQUE,
          DataBuffer.TYPE_BYTE);
  public static final ComponentColorModel COLOR_MODEL_RGB8 = colorModelRGB8;
  public static final ComponentColorModel COLOR_MODEL_GRAY_ALPHA8 = colorModelGrayAlpha8;
  public static final ComponentColorModel COLOR_MODEL_GRAY8 = colorModelGray8;

  private static final int[] GrayBits32 = {32};
  private static final ComponentColorModel colorModelGray32 =
      new ComponentColorModel(ColorSpace.getInstance(ColorSpace.CS_GRAY),
          GrayBits32, false, false,
          Transparency.OPAQUE,
          DataBuffer.TYPE_INT);
  private static final int[] GrayAlphaBits32 = {32, 32};
  private static final ComponentColorModel colorModelGrayAlpha32 =
      new ComponentColorModel(ColorSpace.getInstance(ColorSpace.CS_GRAY),
          GrayAlphaBits32, true, false,
          Transparency.TRANSLUCENT,
          DataBuffer.TYPE_INT);
  private static final int[] RGBBits32 = {32, 32, 32};
  private static final ComponentColorModel colorModelRGB32 =
      new ComponentColorModel(ColorSpace.getInstance(ColorSpace.CS_sRGB),
          RGBBits32, false, false,
          Transparency.OPAQUE,
          DataBuffer.TYPE_INT);

  private static final int[] GrayBits16 = {16};
  private static final ComponentColorModel colorModelGray16 =
      new ComponentColorModel(ColorSpace.getInstance(ColorSpace.CS_GRAY),
          GrayBits16, false, false,
          Transparency.OPAQUE,
          DataBuffer.TYPE_USHORT);
  private static final int[] GrayAlphaBits16 = {16, 16};
  private static final ComponentColorModel colorModelGrayAlpha16 =
      new ComponentColorModel(ColorSpace.getInstance(ColorSpace.CS_GRAY),
          GrayAlphaBits16, true, false,
          Transparency.TRANSLUCENT,
          DataBuffer.TYPE_USHORT);
  private static final int[] RGBBits16 = {16, 16, 16};
  private static final ComponentColorModel colorModelRGB16 =
      new ComponentColorModel(ColorSpace.getInstance(ColorSpace.CS_sRGB),
          RGBBits16, false, false,
          Transparency.OPAQUE,
          DataBuffer.TYPE_USHORT);
  public static final ComponentColorModel COLOR_MODEL_RGB16 = colorModelRGB16;
  public static final ComponentColorModel COLOR_MODEL_GRAY_ALPHA16 = colorModelGrayAlpha16;
  public static final ComponentColorModel COLOR_MODEL_GRAY16 = colorModelGray16;

  /**
   * @param sampleModel
   * @param bandRepresentations
   * @return A component color model based on the SampleModel's data type, if available.
   */
  public static ColorModel createColorModel(
      final SampleModel sampleModel, final BandRepresentations bandRepresentations) {
    switch (sampleModel.getDataType()) {
      case DataBuffer.TYPE_BYTE:
        return createComponentColorModelByte(sampleModel, bandRepresentations);
      case DataBuffer.TYPE_USHORT:
      case DataBuffer.TYPE_SHORT:
        return createComponentColorModelUShort(sampleModel, bandRepresentations);
      case DataBuffer.TYPE_INT:
        return createComponentColorModelUShort(sampleModel, bandRepresentations);
      case DataBuffer.TYPE_FLOAT:
      case DataBuffer.TYPE_DOUBLE:
      case DataBuffer.TYPE_UNDEFINED:
      default:
        LOGGER.warn("Request for ColorModel using an unsupported DataBuffer TYPE: {}",
            sampleModel.getDataType());
        return null;

    }
  }

  private static ColorModel createComponentColorModelByte(
      final SampleModel sampleModel, final BandRepresentations bandRepresentations) {

    ColorModel colorModel = null;

    if (sampleModel.getSampleSize(0) == 1) {
      int sampleSize = sampleModel.getSampleSize(0);
      byte[] r = {-1,0};
      byte[] g = {0,-1};
      byte[] b = {0,0};
      return new IndexColorModel(sampleSize,2,r,g,b);
    }

    int bands = sampleModel.getNumBands();
    switch (bands) {
      case 1:
        colorModel = colorModelGray8;
        LOGGER.trace("ByteColorModelFactory creating gray color model (1 band)");
        break;
      case 2:
        colorModel = colorModelGrayAlpha8;
        LOGGER.trace("ByteColorModelFactory creating gray color model with alpha (2 bands)");
        break;
      case 3:
        colorModel = colorModelRGB8;
        LOGGER.trace("ByteColorModelFactory creating RGB color model (3 bands)");
        break;
      case 4:   // or more..
      default:
        int transparency = Transparency.OPAQUE;
        boolean alpha = bandRepresentations.hasAlpha();
        if (alpha) {
          transparency = Transparency.TRANSLUCENT;
          LOGGER.trace("ByteColorModelFactory creating RGBA color model (" + bands + " bands)");
        } else {
          LOGGER.trace("ByteColorModelFactory creating RGB color model (" + bands + " bands)");
        }
        int[] bits = new int[bands];
        for (int i = 0; i < bands; i++) {
          bits[i] = 8;
        } // end for
        colorModel = new ComponentColorModel(ColorSpace.getInstance(ColorSpace.CS_sRGB),
            bits, alpha, false,
            transparency,
            DataBuffer.TYPE_BYTE);
    }
    return colorModel;
  }

  private static ColorModel createComponentColorModelInt(
      final SampleModel sampleModel, final BandRepresentations bandRepresentations) {
    ComponentColorModel colorModel = null;
    int bands = sampleModel.getNumBands();
    switch (bands) {
      case 1:
        colorModel = colorModelGray32;
        LOGGER.trace("IntColorModelFactory creating gray color model (1 band)");
        break;
      case 2:
        colorModel = colorModelGrayAlpha32;
        LOGGER.trace("IntColorModelFactory creating gray color model with alpha (2 bands)");
        break;
      case 3:
        colorModel = colorModelRGB32;
        LOGGER.trace("IntColorModelFactory creating RGB color model (3 bands)");
        break;
      case 4:   // or more..
      default:
        int transparency = Transparency.OPAQUE;
        boolean alpha = bandRepresentations.hasAlpha();
        if (alpha) {
          transparency = Transparency.TRANSLUCENT;
          LOGGER.trace("IntColorModelFactory creating RGBA color model (" + bands + " bands)");
        } else {
          LOGGER.trace("IntColorModelFactory creating RGB color model (" + bands + " bands)");
        }
        int[] bits = new int[bands];
        for (int i = 0; i < bands; i++) {
          bits[i] = 32;
        }
        colorModel = new ComponentColorModel(ColorSpace.getInstance(ColorSpace.CS_sRGB),
            bits, alpha, false,
            transparency,
            DataBuffer.TYPE_INT);
    }
    return colorModel;
  }

  private static ColorModel createComponentColorModelUShort(
      final SampleModel sampleModel, final BandRepresentations bandRepresentations) {
    ComponentColorModel colorModel = null;
    int bands = sampleModel.getNumBands();
    switch (bands) {
      case 1:
        colorModel = colorModelGray16;
        LOGGER.trace("UShortColorModelFactory creating gray color model (1 band)");
        break;
      case 2:
        colorModel = colorModelGrayAlpha16;
        LOGGER.trace("UShortColorModelFactory creating gray color model with alpha (2 bands)");
        break;
      case 3:
        colorModel = colorModelRGB16;
        LOGGER.trace("UShortColorModelFactory creating RGB color model (3 bands)");
        break;
      case 4:   // or more..
      default:
        int transparency = Transparency.OPAQUE;
        boolean alpha = bandRepresentations.hasAlpha();
        ColorSpace cs;
        if (bands == 4 && alpha) {
          // if we have 4 bands and alpha, assume it's RGBA
          transparency = Transparency.TRANSLUCENT;
          cs = ColorSpace.getInstance(ColorSpace.CS_sRGB);
          LOGGER.info("UShortColorModelFactory creating RGBA color model (" + bands + " bands), alpha:" + alpha);
        } else {
          if (alpha) {
            LOGGER.trace("UShortColorModelFactory creating RGBA color model (" + bands + " bands)");
            transparency = Transparency.TRANSLUCENT;
          } else {
            LOGGER.trace("UShortColorModelFactory creating RGB color model (" + bands + " bands)");
          }
          cs = new MultibandColorspace(bands);
        }
        int[] bits = new int[bands];
        for (int i = 0; i < bands; i++) {
          bits[i] = 16;
        }
        colorModel = new ComponentColorModel(cs, bits, alpha, false, transparency, DataBuffer.TYPE_USHORT);
    }
    return colorModel;
  }
}
