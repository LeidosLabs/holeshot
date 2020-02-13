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
import java.io.InputStream;

import org.apache.commons.lang3.tuple.Pair;
import org.codice.imaging.nitf.core.common.NitfFormatException;

import com.google.common.collect.ImmutableSet;
import com.leidoslabs.holeshot.imaging.ImageKey;
import com.leidoslabs.holeshot.imaging.ImageSource;
import com.leidoslabs.holeshot.imaging.ImageSourceFactory;
import com.leidoslabs.holeshot.imaging.io.CheckedSupplier;

/**
 * ImageSourceFactory for NITFImages 
 */
public class NITFImageSourceFactory extends  ImageSourceFactory {

   private static ImmutableSet<Pair<Long, byte[]>> MAGIC_NUMBERS =
         new ImmutableSet.Builder<Pair<Long, byte[]>> ()
         .add(Pair.of(0l, new byte[] {0x4E, 0x49, 0x54, 0x46, 0x30 }))
         .build();


   /**
    * Initializes NITFMagic numbers
    */
   public NITFImageSourceFactory() {
      super(MAGIC_NUMBERS);
   }

   @Override
   /**
    * Retrieve NITFImageSource
    */
   public ImageSource getImageSource(CheckedSupplier<InputStream, IOException> inputStreamSupplier, ImageKey fallbackImageKey) throws IOException {
      ImageSource imageSource = null;
      try {
         imageSource = new NITFImageSource(inputStreamSupplier, fallbackImageKey);
      } catch (NitfFormatException e) {
         e.printStackTrace();
      }
      return imageSource;
   }

}
