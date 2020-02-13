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
package com.leidoslabs.holeshot.tileserver.mrf;

import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.commons.io.IOUtils;

import com.leidoslabs.holeshot.elt.tileserver.TileRef;
import com.leidoslabs.holeshot.tileserver.utils.ResourceUtils;
import com.leidoslabs.holeshot.tileserver.v1.TileServerClient;

/**
 * InputStream used by MRFService
 *
 */
public class MRFTiledInputStream extends InputStream {
   private final SortedSet<MRFTileRef> tileRefs;
   private long offset;
   private final long size; 
   private final AtomicLong firstOffset;
   private final Iterator<MRFTileRef> tileIterator;
   private final byte[] readBuffer;
   private InputStream tileInputStream;
   private final TileServerClient tileserverClient;

   /**
    * Initialize input stream Given a tileserver client and MRFIndex file. Gets MRFTileRefs for specified offset and size
    * @param tileServerClient
    * @param indexFile
    * @param offset
    * @param size
    */
   public MRFTiledInputStream(TileServerClient tileServerClient, MRFIndexFile indexFile, long offset, long size) {
      this.readBuffer = new byte[1];
      this.firstOffset = new AtomicLong();
      this.tileRefs = getTilesToWrite(indexFile, offset, size, firstOffset);
      this.tileIterator = tileRefs.iterator();
      this.offset = offset - firstOffset.longValue();
      this.size = size;
      this.tileInputStream = null;
      this.tileserverClient = tileServerClient;
   }

   /**
    * Return MRFTileRefs from index for specified offset and size
    * @param indexFile
    * @param offset
    * @param size
    * @param firstOffset
    * @return
    */
   private SortedSet<MRFTileRef> getTilesToWrite(MRFIndexFile indexFile, long offset, long size, AtomicLong firstOffset) {
      long currentOffset = 0;
      Iterator<MRFTileRef> iter = indexFile.getOutputTiles().iterator();
      SortedSet<MRFTileRef> tilesToWrite = new TreeSet<MRFTileRef>();
      while (iter.hasNext() && currentOffset < (offset + size)) {
         final MRFTileRef currentTileRef = iter.next();
         if (currentOffset >= offset) {
            if (tilesToWrite.isEmpty()) {
               firstOffset.set(currentOffset);
            }
            tilesToWrite.add(currentTileRef);
         }
         currentOffset += currentTileRef.getObjectSize();
      }
      return tilesToWrite;
   }
   
   @Override
   public int read(byte[] b) throws IOException {
      return read(b, 0, b.length);
   }

   
   
   /**
    * Fetch current tile from client, read into b, then repeat until we have read over len bytes
    */
   @Override
   public int read(byte[] b, int off, int len) throws IOException {
      int bytesRead = 0;
      do {
         if (tileInputStream != null) {
            final int bytesToRead = len - bytesRead;
            final int currentBytesRead = tileInputStream.read(b, off, bytesToRead);
            if (currentBytesRead < bytesToRead) {
               if (currentBytesRead > 0) {
                  bytesRead += currentBytesRead;
               }
               ResourceUtils.closeQuietly(tileInputStream);
               tileInputStream = null;
            }
         }
         if (tileInputStream == null && tileIterator.hasNext()) {
            final MRFTileRef tileref = tileIterator.next();
            tileInputStream = tileserverClient.getTile(tileref.getCollectionID(), tileref.getTimestamp(), tileref.getRset(), tileref.getX(), tileref.getY(), tileref.getBand());
            if (offset > 0) {
               IOUtils.skip(tileInputStream, offset);
               offset = 0;
            }
         }
      } while (bytesRead < len && tileInputStream != null);
      
      return (bytesRead == 0) ? -1 : bytesRead;
   }
   
   @Override
   public int read() throws IOException {
      final int result = read(readBuffer, 0, 1);
      return result < 0 ? result : readBuffer[0];
   }

   @Override
   public void close() throws IOException {
      while (tileIterator.hasNext()) {
         tileIterator.next();
      }
      ResourceUtils.closeQuietly(tileInputStream);
   }

}
