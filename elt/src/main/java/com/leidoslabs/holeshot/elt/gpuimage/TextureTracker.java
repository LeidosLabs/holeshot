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

package com.leidoslabs.holeshot.elt.gpuimage;

import static org.lwjgl.opengl.GL11.glGetIntegerv;
import static org.lwjgl.opengl.GL13.GL_TEXTURE0;
import static org.lwjgl.opengl.GL20.GL_MAX_COMBINED_TEXTURE_IMAGE_UNITS;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Class for tracking least recently used textures
 */
public class TextureTracker {
   private static class TextureRef {
      private EvictionCallback evictionCB;

      public TextureRef(int id) {
         this.evictionCB = null;
      }

      public EvictionCallback getEvictionCB() {
         return evictionCB;
      }

      public void setEvictionCB(EvictionCallback evictionCB) {
         this.evictionCB = evictionCB;
      }
   }

   public interface EvictionCallback {
      public void evict(int textureID);
   }


   private static int RESERVED_TEXTURES = 10;
   private static TextureTracker instance = null;
   private Map<Integer, TextureRef> lruTextures;

   public static synchronized TextureTracker getInstance() {
      if (instance == null) {
         instance = new TextureTracker();
      }
      return instance;
   }

   /**
    * Returns a texture given a specified EvictionCalback
    * @param evictionCB
    * @return
    */
   public synchronized int getTexture(EvictionCallback evictionCB) {

      final Entry<Integer, TextureRef> first = lruTextures.entrySet().iterator().next();
      final int firstKey = first.getKey();
      final TextureRef firstValue = first.getValue();

      EvictionCallback oldEvictionCB = firstValue.getEvictionCB();
      if (oldEvictionCB != null) {
         oldEvictionCB.evict(firstKey);
      }
      firstValue.setEvictionCB(evictionCB);

      // Access the Entry to move it to back of LRU queue
      lruTextures.get(firstKey);

      return firstKey;
   }

   private static int getMaxTextures() {
      int[] buf = new int[1];
      glGetIntegerv(GL_MAX_COMBINED_TEXTURE_IMAGE_UNITS, buf);
      return buf[0];
   }

   private TextureTracker() {
      final int maxTextures = getMaxTextures();
      final int textureCount = maxTextures - RESERVED_TEXTURES;

      lruTextures = Collections.synchronizedMap(new LinkedHashMap<Integer, TextureRef>(textureCount, 1.0f, true));
      for (int i=GL_TEXTURE0 + RESERVED_TEXTURES;i<GL_TEXTURE0 + maxTextures;++i) {
         lruTextures.put(i, new TextureRef(i));
      }
   }
}
