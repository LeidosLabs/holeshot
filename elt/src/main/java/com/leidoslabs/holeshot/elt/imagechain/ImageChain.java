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

import java.io.Closeable;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.image.common.util.CloseableUtils;

import com.leidoslabs.holeshot.elt.ELTDisplayContext;
import com.leidoslabs.holeshot.elt.coord.ImageWorld;
import com.leidoslabs.holeshot.elt.gpuimage.HistogramType;
import com.leidoslabs.holeshot.elt.imageop.ImageOp;
import com.leidoslabs.holeshot.elt.imageop.ImageOpFactory;
import com.leidoslabs.holeshot.elt.layers.Renderer;
import com.leidoslabs.holeshot.elt.tileserver.TileserverImage;
import com.leidoslabs.holeshot.elt.utils.ReverseListSpliterator;

/**
 * The ImageChain is an ordered sequence of events to manipulate the framebuffer prior to rendering. Each subsequent step
 * can access the results of previous steps in order to build upon the output.
 */
public class ImageChain extends Renderer<TileserverImage> implements Closeable {
   private final LinkedList<ImageOp> imageOps;
   private TileserverImage image;

   private ImageChain(LinkedList<ImageOp> imageOps, ImageWorld imageWorld, ELTDisplayContext eltDisplayContext) {
      super(imageWorld, eltDisplayContext);
      this.imageOps = imageOps;
      this.imageOps.stream().forEach(ic->ic.setImageChain(this));
   }

   public List<ImageOp> getImageOps() {
      return imageOps;
   }

   public Stream<ImageOp> getPreviousImageOpsAsStream(ImageOp currentImageOp) {
      return StreamSupport.stream(new ReverseListSpliterator<ImageOp>(imageOps, currentImageOp), false);
   }

   public Stream<ImageOp> getPreviousImageOpsAsStream() {
      return StreamSupport.stream(new ReverseListSpliterator<ImageOp>(imageOps), false);
   }

   @SuppressWarnings("unchecked")
   public <T extends ImageOp> Stream<T> getPreviousImageOpsOfTypeAsStream(ImageOp currentImageOp, Class<T> previousImageOpClass) {
      return getPreviousImageOpsAsStream(currentImageOp).filter(i->previousImageOpClass.isInstance(i)).map(i->(T)i);
   }

   @SuppressWarnings("unchecked")
   public <T extends ImageOp> Stream<T> getPreviousImageOpsOfTypeAsStream(Class<T> previousImageOpClass) {
      return getPreviousImageOpsAsStream().filter(i->previousImageOpClass.isInstance(i)).map(i->(T)i);
   }

   public <T extends ImageOp> List<T> getPreviousImageOps(ImageOp currentImageOp, Class<T> previousImageOpClass) {
      return getPreviousImageOpsOfTypeAsStream(currentImageOp, previousImageOpClass).collect(Collectors.toList());
   }

   /**
    * Returns the ImageOp instance of previousImageOpClass type that occurred most recently in the image chain prior to
    * current imageop.
    * @param currentImageOp The place in the chain to start searching (Typically the current step, i.e. 'this')
    * @param previousImageOpClass The imageop type you are looking for
    * @param <T> The type of the returned class, will be equal to the type of the previous ImageOp
    * @return The first found instance, else null if the operation isn't found earlier in the chain
    */
   public <T extends ImageOp> T getPreviousImageOp(ImageOp currentImageOp, Class<T> previousImageOpClass) {
      return getPreviousImageOpsOfTypeAsStream(currentImageOp, previousImageOpClass).findFirst().orElse(null);
   }

   public <T extends ImageOp> T getLastImageOp(Class<T> previousImageOpClass) {
      return getPreviousImageOpsOfTypeAsStream(previousImageOpClass).findFirst().orElse(null);
   }

   public TileserverImage getImage() {
      return image;
   }

   /**
    * Evaluate the image chain by calling the render() function on each ImageOp in the chain in sequence.
    * @throws IOException
    */
   @Override
   public void render(TileserverImage image) throws IOException {
      this.image = image;
      for (ImageOp imageOp: imageOps) {
         imageOp.render();
      }
   }

   /**
    * Return the last non-null result framebuffer from the image chain, if one exists. Null if none exists.
    * @return Final framebuffer output of image chain
    */
   public Framebuffer getResultFramebuffer() {
      return StreamSupport.stream(new ReverseListSpliterator<ImageOp>(imageOps), false)
            .map(i->i.getResultFramebuffer())
            .filter(b->b != null)
            .findFirst().orElse(null);
   }

   /**
    * Builder pattern for performing a sequence of ImageChain operations
    */
   public static class ImageChainBuilder {
      private final LinkedList<ImageOp> imageOps;
      private final ImageOpFactory imageOpFactory;
      private final ImageWorld imageWorld;
      private final ELTDisplayContext eltDisplayContext;

      public ImageChainBuilder(ImageOpFactory imageOpFactory, ImageWorld imageWorld, ELTDisplayContext eltDisplayContext) {
         this.imageOpFactory = imageOpFactory;
         this.imageOps = new LinkedList<ImageOp>();
         this.imageWorld = imageWorld;
         this.eltDisplayContext = eltDisplayContext;
      }

      public ImageChain build() {
         return new ImageChain(this.imageOps, this.imageWorld, this.eltDisplayContext);
      }

      public ImageChainBuilder rawImage(boolean progressiveRender) {
         this.imageOps.add(imageOpFactory.rawImage(progressiveRender));
         return this;
      }
      public ImageChainBuilder cumulativeHistogram() {
         this.imageOps.add(imageOpFactory.cumulativeHistogram());
         return this;
      }
      public ImageChainBuilder dynamicRangeAdjust() {
         this.imageOps.add(imageOpFactory.dynamicRangeAdjust());
         return this;
      }
      public ImageChainBuilder histogram(HistogramType histogramType) {
         this.imageOps.add(imageOpFactory.histogram(histogramType));
         return this;
      }
      public ImageChainBuilder summedArea() {
         this.imageOps.add(imageOpFactory.summedArea());
         return this;
      }
      public ImageChainBuilder draParameters(boolean phasedDRA) {
         this.imageOps.add(imageOpFactory.draParameters(phasedDRA));
         return this;
      }
      public ImageChainBuilder toneTransferCurve() throws IOException {
         this.imageOps.add(imageOpFactory.toneTransferCurve());
         return this;
      }
   }

   @Override
   public boolean isFullyRendered() {
      return !(imageOps.stream().filter(imageOp-> !imageOp.isFullyRendered()).findFirst().isPresent());
   }

   @Override
   public void close() throws IOException {
      super.close();
      CloseableUtils.close(imageOps.stream().toArray(ImageOp[]::new));
   }

   public void reset() {
       this.imageOps.forEach(i->i.reset());
   }
}
