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
import java.util.function.Function;

import org.codice.imaging.nitf.core.HeapStrategy;
import org.codice.imaging.nitf.core.common.NitfFormatException;
import org.codice.imaging.nitf.core.common.NitfReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * HeapStrategy for storing large segments of NITFdata. HeapStrategy comes from the codice NITFlibrary
 * @param <R>
 */
public class LargeInMemoryHeapStrategy<R> implements HeapStrategy<R> {
   private static final Logger LOGGER = LoggerFactory.getLogger(LargeInMemoryHeapStrategy.class);

   private final Function<InputStream, R> resultConversionFunction;

   public LargeInMemoryHeapStrategy(final Function<InputStream, R> resultConverter) {
      this.resultConversionFunction = resultConverter;
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public R handleSegment(NitfReader reader, long length) throws NitfFormatException {
      R result =  null;
      LOGGER.info(String.format("Storing %s bytes in heap space.", length));

      try (InputStream inputStream = new NITFReaderInputStream(reader)) {
         result = resultConversionFunction.apply(inputStream);
      } catch (IOException ioe) {
         throw new NitfFormatException(ioe);
      }

      return result;
   }

   @Override
   public void cleanUp() {
      // Nothing to do

   }

}
