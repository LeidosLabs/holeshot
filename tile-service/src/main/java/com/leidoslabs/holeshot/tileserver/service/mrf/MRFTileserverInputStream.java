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
package com.leidoslabs.holeshot.tileserver.service.mrf;

import java.io.IOException;
import java.io.InputStream;

import com.leidoslabs.holeshot.elt.tileserver.TileserverImage;

public class MRFTileserverInputStream extends InputStream {
   private TileserverImage image;
   private long markOffset;
   private long offset;
   
   public MRFTileserverInputStream(TileserverImage image) {
      this.image = image;
   }
   @Override
   public int available() throws IOException {
      // TODO Auto-generated method stub
      return super.available();
   }

   @Override
   public void close() throws IOException {
      // TODO Auto-generated method stub
      super.close();
   }
   
   @Override
   public synchronized void mark(int readlimit) {
      // TODO Auto-generated method stub
      super.mark(readlimit);
   }
   
   @Override
   public boolean markSupported() {
      // TODO Auto-generated method stub
      return super.markSupported();
   }
   
   @Override
   public int read() throws IOException {
      return 0;
   }
   
   @Override
   public int read(byte[] b) throws IOException {
      // TODO Auto-generated method stub
      return super.read(b);
   }
   
   @Override
   public int read(byte[] b, int off, int len) throws IOException {
      // TODO Auto-generated method stub
      return super.read(b, off, len);
   }

   @Override
   public synchronized void reset() throws IOException {
      // TODO Auto-generated method stub
      super.reset();
   }

   @Override
   public long skip(long n) throws IOException {
      // TODO Auto-generated method stub
      return super.skip(n);
   }

   
}
