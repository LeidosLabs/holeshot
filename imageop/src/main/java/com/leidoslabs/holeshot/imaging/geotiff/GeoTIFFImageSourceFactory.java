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
package com.leidoslabs.holeshot.imaging.geotiff;

import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.lang3.tuple.Pair;

import com.google.common.collect.ImmutableSet;
import com.leidoslabs.holeshot.imaging.ImageKey;
import com.leidoslabs.holeshot.imaging.ImageSource;
import com.leidoslabs.holeshot.imaging.ImageSourceFactory;
import com.leidoslabs.holeshot.imaging.io.CheckedSupplier;


/**
 * GeoTIFF image source factory, sets TIFF magic numbers and return GeoTIFFImagesSources
 */
public class GeoTIFFImageSourceFactory extends  ImageSourceFactory {

   private static ImmutableSet<Pair<Long, byte[]>> MAGIC_NUMBERS =
         new ImmutableSet.Builder<Pair<Long, byte[]>> ()
         .add(Pair.of(0l, new byte[] {0x49, 0x49, 0x2A, 0x00 }))
         .add(Pair.of(0l, new byte[] {0x4D, 0x4D, 0x00, 0x2A }))
         .build();


   /**
    * Initialize parent's magicNumbers field with available TIFF magic numbers
    */
   public GeoTIFFImageSourceFactory() {
      super(MAGIC_NUMBERS);
   }

   @Override
   public ImageSource getImageSource(CheckedSupplier<InputStream, IOException> inputStreamSupplier, ImageKey fallbackImageKey) throws IOException {
      return new GeoTIFFImageSource(inputStreamSupplier, fallbackImageKey);
   }

}
