/*
 * Licensed to Leidos, Inc. under one or more contributor license agreements.  
 * See the NOTICE file distributed with this work for additional information regarding copyright ownership.
 * Leidos, Inc. licenses this file to You under the Apache License, Version 2.0
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

package com.leidoslabs.holeshot.elt.gpuimage;

import static org.lwjgl.opengl.GL40.*;

/**
 * Enum for openGL formats for different colorspaces, bands, images, and depth types
 */
public enum GLInternalFormat {
   // 1 - Band
   GlInternalFormatR8(1, 8, GL_R8, GL_RED, GL_UNSIGNED_BYTE),
   GlInternalFormatR16UI(1, 16, GL_R16UI, GL_RED, GL_UNSIGNED_SHORT),
   GlInternalFormatR32UI(1, 32, GL_R32UI, GL_RED, GL_UNSIGNED_INT),
   GlInternalFormatR32F(1, 32, GL_R32F, GL_RED, GL_FLOAT),

   GlInternalFormatR32FUS(1, 32, GL_R32F, GL_RED, GL_UNSIGNED_SHORT),

   // 3 - Bands
//   GlInternalFormatRGB8UI(3, 8, GL_RGB8UI, GL_RGB_INTEGER, GL_UNSIGNED_INT),
   GlInternalFormatRGB8UI(3, 32, GL_RGB32F, GL_RGB, GL_UNSIGNED_BYTE),

   GlInternalFormatRGB8(3, 8, GL_RGB32F, GL_RGB, GL_UNSIGNED_BYTE),
   GlInternalFormatRGB16UI(3, 16, GL_RGB32F, GL_RGB, GL_UNSIGNED_SHORT),
   GlInternalFormatRGB32UI(3, 32, GL_RGB32F, GL_RGB, GL_UNSIGNED_INT),
   GlInternalFormatRGB32F(3, 32, GL_RGB32F, GL_RGB, GL_FLOAT),

   // 4 - Bands
   GlInternalFormatRGBA8(4, 8, GL_RGBA32F, GL_RGBA, GL_UNSIGNED_BYTE),
   GlInternalFormatRGBA16UI(4, 16, GL_RGBA32F, GL_RGBA, GL_UNSIGNED_SHORT),
   GlInternalFormatRGBA32UI(4, 32, GL_RGBA32F, GL_RGBA, GL_UNSIGNED_INT),
   GlInternalFormatRGBA32F(4, 32, GL_RGBA32F, GL_RGBA, GL_FLOAT);


//   public int getNumBands() {
//      return numBands;
//   }
//
//   public int getBpp() {
//      return bpp;
//   }

   public int getInternalFormat() {
      return internalFormat;
   }

   public int getFormat() {
      return format;
   }

   public int getType() {
      return type;
   }

   private int numBands;
   private int bpp;
   private int internalFormat;
   private int format;
   private int type;

   private GLInternalFormat(int numBands, int bpp, int internalFormat, int format, int type) {
      this.numBands = numBands;
      this.bpp = bpp;
      this.internalFormat = internalFormat;
      this.format = format;
      this.type = type;
   }
}
