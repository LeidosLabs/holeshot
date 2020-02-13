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
import java.util.Arrays;
import java.util.Iterator;
import java.util.Set;

import org.apache.commons.lang3.tuple.Pair;

import com.leidoslabs.holeshot.imaging.io.CheckedSupplier;

/**
 * Abstract class, provides an API for retrieving ImageSource from a CheckedSupplier.
 * Check if this factory is compatible with an inputStream using Supports. 
 */
public abstract class ImageSourceFactory {
   private Set<Pair<Long, byte[]>> magicNumbers;

   public abstract ImageSource getImageSource(CheckedSupplier<InputStream, IOException> inputStreamSupplier, ImageKey fallbackImageKey) throws IOException;

   /**
    * @return Compatible magic numbers for this image source format
    */
   protected Set<Pair<Long, byte[]>> getMagicNumbers() {
      return this.magicNumbers;
   }

   protected ImageSourceFactory(Set<Pair<Long, byte[]>> magicNumbers) {
      this.magicNumbers = magicNumbers;
   }

   /**
    * Reads first few bytes of InputStream and checks to see if there if the streams magic numb er
    * is supported
    * @param inputStream
    * @return is this input stream supported?
    * @throws IOException
    */
   public boolean supports(InputStream inputStream) throws IOException {
      if (!inputStream.markSupported()) {
         throw new IllegalArgumentException("Given InputStream must support mark() (e.g. BufferedInputStream)");
      }
      boolean isSupported = false;
      Iterator<Pair<Long, byte[]>> iter = magicNumbers.iterator();

      while (!isSupported && iter.hasNext()) {
         Pair<Long, byte[]> magicNumber = iter.next();
         isSupported = supports(inputStream, magicNumber.getLeft(), magicNumber.getRight());
      }
      return isSupported;

   }
   private boolean supports(InputStream inputStream, long offset, byte[] magicNumber) throws IOException {
      boolean isSupported = false;
      try {
         inputStream.mark((int)offset+magicNumber.length);

         inputStream.skip(offset);

         byte[] buf = new byte[magicNumber.length];
         inputStream.read(buf);

         isSupported = Arrays.equals(magicNumber, buf);
      } finally {
         inputStream.reset();
      }
      return isSupported;

   }

}
