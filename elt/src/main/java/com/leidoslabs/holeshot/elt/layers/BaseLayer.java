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

package com.leidoslabs.holeshot.elt.layers;

import java.io.Closeable;
import java.io.IOException;

import org.image.common.util.CloseableUtils;

import com.leidoslabs.holeshot.elt.imagechain.Framebuffer;
import com.leidoslabs.holeshot.elt.tileserver.TileserverImage;

/**
 * Base (Image) Layer of ELTCanvas. Uses a Renderer (typically ImageChain)
 * to render a TileServerImage
 */
public class BaseLayer implements Closeable {

    private TileserverImage image;
    private Renderer<TileserverImage> renderer;

    /**
     * Constructor
     * @param renderer TileServerImage Renderer, typically ImageChain
     */
    public BaseLayer(Renderer<TileserverImage> renderer) {
       this(renderer, null);
   }

    /**
     * Constructor
     * @param renderer TileServerImage Renderer, typically ImageChain
     * @param image
     */
    public BaseLayer(Renderer<TileserverImage> renderer, TileserverImage image) {
        this.renderer = renderer;
        this.image = image;
    }

    public void setImage(TileserverImage image) {
        this.image = image;
    }

    /**
     * Render tile server image, and return output from buffer
     * @return FrameBuffer from rendered TileServerImage
     */
    public Framebuffer draw() {
        try {
            renderer.render(image);
        } catch (
                IOException e) {
            e.printStackTrace();
        }
        return renderer.getResultFramebuffer();
    }

   public boolean isFullyRendered() {
      return this.renderer.isFullyRendered();
   }

   public Renderer<TileserverImage> getRenderer() {
      return renderer;
   }

   @Override
   public void close() throws IOException {
      CloseableUtils.close(renderer);
   }
}
