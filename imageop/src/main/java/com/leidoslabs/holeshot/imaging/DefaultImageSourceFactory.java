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

import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.stream.Collectors;

import com.google.common.collect.ImmutableSet;
import com.leidoslabs.holeshot.imaging.geotiff.GeoTIFFImageSourceFactory;
import com.leidoslabs.holeshot.imaging.io.CheckedSupplier;
import com.leidoslabs.holeshot.imaging.nitf.NITFImageSourceFactory;

/**
 * An ImageSourceFactory that is agnostic to an image's file type
 */
public class DefaultImageSourceFactory extends ImageSourceFactory {
   private static final ImmutableSet<ImageSourceFactory> FACTORIES =
         new ImmutableSet.Builder<ImageSourceFactory>()
         .add(new GeoTIFFImageSourceFactory())
         .add(new NITFImageSourceFactory())
         .build();

   public DefaultImageSourceFactory() {
      super(FACTORIES.stream().flatMap(f->f.getMagicNumbers().stream()).collect(Collectors.toSet()));
   }

   /**
    * Return the appropriate ImageSource (either GeoTIFF or NITF based on InputStream's magic number
    */
   @Override
   public ImageSource getImageSource(CheckedSupplier<InputStream, IOException> inputStreamSupplier, ImageKey fallbackImageKey) throws IOException {
      ImageSource result = null;
      ImageSourceFactory factory = getFactory(inputStreamSupplier);
      if (factory != null) {
         result = factory.getImageSource(inputStreamSupplier, fallbackImageKey);
      }
      return result;
   }

   private ImageSourceFactory getFactory(CheckedSupplier<InputStream, IOException> inputStreamSupplier) throws IOException {
      ImageSourceFactory result = null;
      try (InputStream inputStream = inputStreamSupplier.get()) {
         Iterator<ImageSourceFactory> iter = FACTORIES.iterator();
         while (result == null && iter.hasNext()) {
            ImageSourceFactory currentFactory = iter.next();
            if (currentFactory.supports(inputStream)) {
               result = currentFactory;
            }
         }
      }
      return result;
   }


}
