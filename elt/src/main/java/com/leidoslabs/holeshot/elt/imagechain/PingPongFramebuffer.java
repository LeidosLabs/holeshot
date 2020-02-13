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

package com.leidoslabs.holeshot.elt.imagechain;

import java.awt.Dimension;
import java.io.Closeable;
import java.io.IOException;
import java.util.Arrays;

import org.image.common.util.CloseableUtils;

import com.leidoslabs.holeshot.elt.ELTDisplayContext;
import com.leidoslabs.holeshot.elt.gpuimage.GLInternalFormat;


/**
 * Two FrameBuffers that can be easily swapped
 */
public class PingPongFramebuffer  implements Closeable {
   private Framebuffer[] buffers;
   private int sourceIndex;

   public PingPongFramebuffer(Dimension size, GLInternalFormat internalFormat, ELTDisplayContext eltDisplayContext) throws IOException {
      sourceIndex = 0;
      buffers = new Framebuffer[] {
            new Framebuffer(size, internalFormat, eltDisplayContext),
            new Framebuffer(size, internalFormat, eltDisplayContext)
      };
   }

   public PingPongFramebuffer(Dimension size, GLInternalFormat internalFormat, ELTDisplayContext eltDisplayContext, int filter, int wrap) throws IOException {
      sourceIndex = 0;
      buffers = new Framebuffer[] {
            new Framebuffer(size, internalFormat, eltDisplayContext, filter, wrap),
            new Framebuffer(size, internalFormat, eltDisplayContext, filter, wrap)
      };
   }

   private int getSourceIndex() {
      return sourceIndex;
   }
   private int getDestinationIndex() {
      return (sourceIndex + 1) % 2;
   }

   public Framebuffer getSource() {
      return buffers[getSourceIndex()];
   }
   public Framebuffer getDestination() {
      return buffers[getDestinationIndex()];
   }
   public void swap() {
      sourceIndex = getDestinationIndex();
   }
   public void reset(Dimension size, GLInternalFormat internalFormat) {
      Arrays.stream(buffers).forEach(b->b.reset(size, internalFormat));
   }
   @Override
   public void close() throws IOException {
      CloseableUtils.close(buffers);
   }

}
