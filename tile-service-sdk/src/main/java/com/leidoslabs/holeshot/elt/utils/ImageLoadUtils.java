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
package com.leidoslabs.holeshot.elt.utils;

import java.awt.color.ColorSpace;
import java.awt.color.ICC_Profile;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;

import javax.imageio.ImageIO;

public class ImageLoadUtils {
   private static ImageLoadUtils instance = new ImageLoadUtils();

   public static ImageLoadUtils getInstance() {
      return instance;
   }

   private ImageLoadUtils() {
      // Load deferred color space profiles to avoid 
      // ConcurrentModificationException due to JDK
      // Use in public static main void or prior to application initialization
      // https://github.com/haraldk/TwelveMonkeys/issues/402
      // https://bugs.openjdk.java.net/browse/JDK-6986863
      // https://stackoverflow.com/questions/26297491/imageio-thread-safety
      ICC_Profile.getInstance(ColorSpace.CS_sRGB).getData();
      ICC_Profile.getInstance(ColorSpace.CS_PYCC).getData();
      ICC_Profile.getInstance(ColorSpace.CS_GRAY).getData();
      ICC_Profile.getInstance(ColorSpace.CS_CIEXYZ).getData();
      ICC_Profile.getInstance(ColorSpace.CS_LINEAR_RGB).getData();
   }

//   private ImageInputStream createImageInputStream(InputStream inputStream) throws IOException {
//      ImageInputStream nStream = ImageIO.createImageInputStream(inputStream);
//
//      if (nStream == null) {
//         throw new IIOException("Can't create an ImageInputStream!");
//      }
//      return nStream;
//   }
//   private ImageReader getImageReaderForStream(ImageInputStream nStream) throws IIOException {
//      if (nStream == null) {
//         throw new IIOException("Can't find an ImageReader for a null stream");
//      }
//      Iterator<ImageReader> iter = ImageIO.getImageReaders(nStream);
//      return iter.hasNext() ? iter.next() : null;
//   }

//   public BufferedImage loadImage(SeekableStream inputStream) throws IOException {
//      BufferedImage image = null;
//      ParameterBlock pb = new ParameterBlock();
//      pb.add(inputStream);
//      // Create the PNG operation.
//      RenderedOp op = JAI.create("PNG", pb);
//      image = op.getAsBufferedImage();
//
//      return image;
//   }

   /**
    * @param inputStream
    * @return BufferedImage
    * @throws IOException
    */
   public BufferedImage loadImage(InputStream inputStream) throws IOException {
      BufferedImage result = ImageIO.read(inputStream);
      return result;
   }
}

