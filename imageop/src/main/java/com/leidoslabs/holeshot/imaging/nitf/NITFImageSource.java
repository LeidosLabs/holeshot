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
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.imageio.stream.MemoryCacheImageInputStream;

import org.codice.imaging.nitf.core.common.NitfFormatException;
import org.codice.imaging.nitf.core.common.NitfReader;
import org.codice.imaging.nitf.core.common.impl.NitfInputStreamReader;
import org.codice.imaging.nitf.core.header.impl.NitfParser;
import org.codice.imaging.nitf.core.image.ImageRepresentation;
import org.codice.imaging.nitf.core.image.ImageSegment;
import org.codice.imaging.nitf.core.impl.InMemoryHeapStrategy;
import org.codice.imaging.nitf.core.impl.SlottedParseStrategy;
import org.image.common.util.CloseableUtils;

import com.google.common.collect.ImmutableSet;
import com.leidoslabs.holeshot.imaging.ImageKey;
import com.leidoslabs.holeshot.imaging.ImageSource;
import com.leidoslabs.holeshot.imaging.ImageSourceSegment;
import com.leidoslabs.holeshot.imaging.io.CheckedSupplier;

public class NITFImageSource implements ImageSource {
   private static ImmutableSet<ImageRepresentation> SUPPORTED_IMAGE_REPRESENTATIONS =
         new ImmutableSet.Builder<ImageRepresentation>()
         .add(ImageRepresentation.MONOCHROME)
         .add(ImageRepresentation.MULTIBAND)
         .add(ImageRepresentation.RGBTRUECOLOUR)
         .add(ImageRepresentation.UNKNOWN)
         .build();

   private final List<RenderedImageSegment> imageSourceSegments;
   private final InputStream inputStream;

   public NITFImageSource(CheckedSupplier<InputStream, IOException> inputStreamSupplier, ImageKey fallbackImageKey) throws NitfFormatException, IOException {
      SlottedParseStrategy parseStrategy = new SlottedParseStrategy();
      parseStrategy.setImageHeapStrategy(new LargeInMemoryHeapStrategy<>((InputStream is) -> new MemoryCacheImageInputStream(is)));

      inputStream = inputStreamSupplier.get();
      NitfReader reader = new NitfInputStreamReader(inputStream);
      NitfParser.parse(reader, parseStrategy);

      final List<ImageSegment> imageSegments = parseStrategy.getDataSource().getImageSegments();
      final Iterator<ImageSegment> segmentIter = imageSegments.iterator();
      final int numberOfImageSegments = imageSegments.size();

      this.imageSourceSegments = new ArrayList<RenderedImageSegment>();
      for (int i=0; segmentIter.hasNext(); ++i) {
         final ImageSegment currentSegment = segmentIter.next();

         if (SUPPORTED_IMAGE_REPRESENTATIONS.contains(currentSegment.getImageRepresentation())) {
            // Copy fallback image key as the key name might change in the case of multiple segments.
            final ImageKey segmentImageKey = new ImageKey(fallbackImageKey);
            final RenderedImageSegment renderedImageSegment = new RenderedImageSegment(currentSegment, segmentImageKey, i, numberOfImageSegments);
            this.imageSourceSegments.add(renderedImageSegment);
         }
      }

      //      // If there are multiple imageSegments and the later ones don't have a camera model, use the camera model from the first.
      //      final Optional<RenderedImageSegment> firstCamera = this.imageSourceSegments.stream().filter(s->s.getCameraModel() != null).findFirst();
      //      if (firstCamera.isPresent()) {
      //         this.imageSourceSegments.stream().filter(s->s.getCameraModel() == null).forEach(s->s.setCameraModel(firstCamera.get().getCameraModel()));
      //      }
      //
   }

   @Override
   public List<? extends ImageSourceSegment> getSegments() {
      return this.imageSourceSegments;
   }

   @Override
   public void close() throws Exception {
      CloseableUtils.close(inputStream);
      CloseableUtils.close(imageSourceSegments.toArray(new RenderedImageSegment[imageSourceSegments.size()]));
   }


}
