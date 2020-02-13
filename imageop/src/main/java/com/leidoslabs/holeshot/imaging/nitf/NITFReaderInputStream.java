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

import org.codice.imaging.nitf.core.common.NitfFormatException;
import org.codice.imaging.nitf.core.common.NitfReader;

/**
 * InputStream for NITFReaders
 */
public class NITFReaderInputStream extends InputStream {
   private NitfReader reader;

   public NITFReaderInputStream(NitfReader reader) throws NitfFormatException {
      this.reader = reader;
   }

   @Override
   public int read() throws IOException {
      try {
         return reader.readBytesAsInteger(1);
      } catch (NitfFormatException e) {
         throw new IOException(e);
      }
   }

   @Override
   public int read(byte[] b, int off, int len) throws IOException {
      int result = -1;
      try {
         byte[] bytes = reader.readBytesRaw(len);
         System.arraycopy(bytes,  0,  b,  off,  len);
         result = bytes.length;
      } catch (NitfFormatException e) {
    	  //throw new IOException(e);
      }
      return result;
   }
}
