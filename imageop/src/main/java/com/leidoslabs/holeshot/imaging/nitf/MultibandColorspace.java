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

import java.awt.color.ColorSpace;
import java.util.Map;

import org.apache.commons.jcs.access.exception.InvalidArgumentException;
import org.apache.commons.lang3.NotImplementedException;

import com.google.common.collect.ImmutableMap;

/**
 * 
 * Colorspace for Multiband images, provides conversions to RGB, CIEXYZ, etc.
 */
public class MultibandColorspace extends ColorSpace {
   private static Map<Integer, ColorSpaceDefaults> TYPE_MAP = 
         new ImmutableMap.Builder<Integer, ColorSpaceDefaults>()
         .put(4, new ColorSpaceDefaults(TYPE_4CLR))
         .put(5, new ColorSpaceDefaults(TYPE_5CLR))
         .put(6, new ColorSpaceDefaults(TYPE_6CLR))
         .put(7, new ColorSpaceDefaults(TYPE_7CLR))
         .put(8, new ColorSpaceDefaults(TYPE_8CLR))
         .put(9, new ColorSpaceDefaults(TYPE_9CLR))
         .put(10, new ColorSpaceDefaults(TYPE_ACLR))
         .put(11, new ColorSpaceDefaults(TYPE_BCLR))
         .put(12, new ColorSpaceDefaults(TYPE_CCLR))
         .put(13, new ColorSpaceDefaults(TYPE_DCLR))
         .put(14, new ColorSpaceDefaults(TYPE_ECLR))
         .put(15, new ColorSpaceDefaults(TYPE_FCLR))
         .build();
         
   private final int[] rgbIndices;
   private final int[] ciexyzIndices;
   
   public MultibandColorspace(int numBands) {
      this(numBands, getDefaults(numBands));
   }
   public MultibandColorspace(int numBands, int[] rgbIndices, int[] ciexyzIndices) {
      this(numBands, getDefaults(numBands).getType(), rgbIndices, ciexyzIndices);
   }
   
   private MultibandColorspace(int numBands, ColorSpaceDefaults defaults) {
      this(numBands, defaults.getRgbIndices(), defaults.getCiexyzIndices());
   }
   private MultibandColorspace(int numBands, int type, int[] rgbIndices, int[] ciexyzIndices) {
      super(type, numBands);
      if (rgbIndices == null || ciexyzIndices == null || rgbIndices.length != 3 || ciexyzIndices.length != 3) {
         throw new InvalidArgumentException("rgbIndices and ciexyzIndices must have 3 elements each");
      }
      this.rgbIndices = rgbIndices;
      this.ciexyzIndices = ciexyzIndices;
   }

   @Override
   public float[] toRGB(float[] colorvalue) {
      final float[] result = new float[3];
      for (int i=0;i<3;++i) {
         result[i] = colorvalue[rgbIndices[i]];
      }
      return result;
   }

   @Override
   public float[] fromRGB(float[] rgbvalue) {
      final float[] result = new float[getNumComponents()];
      for (int i=0;i<3;++i) {
         result[rgbIndices[i]] = rgbvalue[i];
      }
      return result;
   }

   @Override
   public float[] toCIEXYZ(float[] colorvalue) {
      final float[] result = new float[3];
      for (int i=0;i<3;++i) {
         result[i] = colorvalue[ciexyzIndices[i]];
      }
      return result;
   }

   @Override
   public float[] fromCIEXYZ(float[] colorvalue) {
      final float[] result = new float[getNumComponents()];
      for (int i=0;i<3;++i) {
         result[ciexyzIndices[i]] = colorvalue[i];
      }
      return result;
   }

   private static ColorSpaceDefaults getDefaults(int numBands) {
      ColorSpaceDefaults defaults = TYPE_MAP.get(numBands);
      if (defaults == null) {
         throw new NotImplementedException("This class intended for 4 to 15 more bands of data");
      }
      return defaults;
   }
   
   private static class ColorSpaceDefaults {
      private final int type;
      private final int[] rgbIndices;
      private final int[] ciexyzIndices;
      
      public ColorSpaceDefaults(int type) {
         this(type, new int[] { 0, 1, 2 }, new int[] { 0, 1, 2 });
      }
      public ColorSpaceDefaults(int type, int[] rgbIndices, int[] ciexyzIndices) {
         this.type = type;
         this.rgbIndices = rgbIndices;
         this.ciexyzIndices = ciexyzIndices;
      }

      public int getType() {
         return type;
      }

      public int[] getRgbIndices() {
         return rgbIndices;
      }

      public int[] getCiexyzIndices() {
         return ciexyzIndices;
      }
   }
   

}
