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
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.image.common.util.CloseableUtils;

import com.leidoslabs.holeshot.elt.imagechain.Framebuffer;
import com.leidoslabs.holeshot.elt.observations.Observation;

/**
 * Representation of an ELTCanvas layer for annotations ontop of the image
 */
public class DataLayer implements Closeable {

   private ArrayList<Observation> data;
   private Renderer<List<Observation>> renderer;
   private boolean readOnly;

   /*
    * Constructor
    * @param renderer Observation Renderer (e.g. PolygonRenderer, PlacemarkRenderer)
    */
   public DataLayer(Renderer<List<Observation>> renderer) {
      this(renderer, false);
   }

   /**
    * Observation Renderer (e.g. PolygonRenderer, PlacemarkRenderer)
    * @param renderer
    * @param readOnly if true, data cannot be removed from layer
    */
   public DataLayer(Renderer<List<Observation>> renderer, boolean readOnly) {
      super();
      this.renderer = renderer;
      this.data = new ArrayList<>();
      this.readOnly = readOnly;
   }

   /**
    * Add observations to renderer
    * @param data Collection of observations
    */
   public void setData(Collection<? extends Observation> data) {
      this.data.clear();
      this.data.addAll(data);
   }

   public void setRenderer(Renderer<List<Observation>> renderer) {
      this.renderer = renderer;
   }

   public void addData(Collection<? extends Observation> data) {
      this.data.addAll(data);
   }

   /**
    * Add observations to layer
    * @param obs
    */
   public void addData(Observation obs) {
      this.data.add(obs);
   }

   /**
    * @return List of observations in layer
    */
   public ArrayList<Observation> getData() {
      return this.data;
   }

   /**
    * Remove observation from data
    * @param obs
    * @return True iff this data layer is readable and layer contained obs
    */
   public boolean remove(Observation obs) {
      if(!readOnly) {
         if (this.data.contains(obs)) {
            this.data.remove(obs);
            return true;
         }
      }
      return false;
   }

   /**
    * Draws rendered data layer on top of given FrameBuffer
    * @param fb
    * @return resultant frame buffer, after drawing on top of supplied FrameBuffer
    */
   public synchronized Framebuffer draw(Framebuffer fb) {
      Framebuffer result = fb;

      if(data!=null) {
         renderer.setFramebuffer(fb);
         try {
            renderer.render(data);
            result = renderer.getResultFramebuffer();
         } catch (Exception e) {
            e.printStackTrace();
         }
      }
      return result;
   }

   /**
    * @return is this layer fully rendered?
    */
   public synchronized boolean isFullyRendered() {
      return renderer.isFullyRendered();
   }
   @Override
   public void close() throws IOException {
      CloseableUtils.close(renderer);
   }
}
