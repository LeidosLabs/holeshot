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

import static org.lwjgl.opengl.GL11.GL_TEXTURE_2D;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_MAG_FILTER;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_MIN_FILTER;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_WRAP_S;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_WRAP_T;
import static org.lwjgl.opengl.GL11.GL_UNPACK_ALIGNMENT;
import static org.lwjgl.opengl.GL11.GL_UNPACK_ROW_LENGTH;
import static org.lwjgl.opengl.GL11.GL_UNPACK_SKIP_PIXELS;
import static org.lwjgl.opengl.GL11.GL_UNPACK_SKIP_ROWS;
import static org.lwjgl.opengl.GL11.glBindTexture;
import static org.lwjgl.opengl.GL11.glDeleteTextures;
import static org.lwjgl.opengl.GL11.glGenTextures;
import static org.lwjgl.opengl.GL11.glPixelStorei;
import static org.lwjgl.opengl.GL11.glTexImage2D;
import static org.lwjgl.opengl.GL11.glTexParameteri;
import static org.lwjgl.opengl.GL12.GL_TEXTURE_BASE_LEVEL;
import static org.lwjgl.opengl.GL12.GL_TEXTURE_MAX_LEVEL;

import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.DataBufferInt;
import java.awt.image.DataBufferShort;
import java.awt.image.DataBufferUShort;
import java.io.Closeable;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;
import java.util.concurrent.ExecutionException;

import org.lwjgl.BufferUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.leidoslabs.holeshot.elt.ELTDisplayContext;

/**
 * Representation of an OpenGL texture
 */
public class Texture implements Closeable {
   private static final Logger LOGGER = LoggerFactory.getLogger(Texture.class);

   private final int id;
   private final Dimension size;

   private GLInternalFormat internalFormat;
   private final int filter;
   private final int wrap;
   private BufferedImage image;
   private final ELTDisplayContext eltDisplayContext;

   /**
    * @return area texture
    */
   public long getArea() {
      return (long) (size.getWidth() * size.getHeight());
   }

   public Texture(Dimension size, GLInternalFormat internalFormat, int filter, int wrap, ELTDisplayContext eltDisplayContext) throws IOException {
      this(size, internalFormat, filter, wrap, eltDisplayContext, null);
   }
   
   /**
    * Construct texture, setup opengl patameters
    * @param size texture size
    * @param internalFormat internal format
    * @param filter
    * @param wrap
    * @param eltDisplayContext
    * @param image
    * @throws IOException
    */
   public Texture(Dimension size, GLInternalFormat internalFormat, int filter, int wrap, ELTDisplayContext eltDisplayContext, BufferedImage image) throws IOException {
      this.size = new Dimension(size);
      this.filter = filter;
      this.wrap  = wrap;
      this.image = image;
      this.eltDisplayContext = eltDisplayContext;

      //enable textures and generate an ID
      id = glGenTextures();

      //bind texture
      bind();

      //setup unpack mode
      glPixelStorei(GL_UNPACK_ALIGNMENT, 1);

      //setup parameters
      glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, this.filter);
      glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, this.filter);
      glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, wrap);
      glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, wrap);
      glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_BASE_LEVEL, 0);
      glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAX_LEVEL, 0);

      if (image != null) {
         final int alignment = 1;
         glPixelStorei(GL_UNPACK_ALIGNMENT, alignment);
         glPixelStorei(GL_UNPACK_ROW_LENGTH, image.getWidth());
         glPixelStorei(GL_UNPACK_SKIP_ROWS, 0);
         glPixelStorei(GL_UNPACK_SKIP_PIXELS, 0);
      }
      reset(size, internalFormat, true);

   }

   public int getId() {
      return id;
   }
   /**
    * Bind texture in OpenGL
    */
   public void bind() {
      glBindTexture(GL_TEXTURE_2D, id);
   }
   public Dimension getSize() {
      return size;
   }

   /**
    * Setup opengl texture to be of new size
    * @param newSize
    * @param format
    */
   public void reset(Dimension newSize, GLInternalFormat format) {
      reset(newSize, format, false);
   }

   private void reset(Dimension newSize, GLInternalFormat format, boolean force) {
      if (newSize == null || newSize.width < 1 || newSize.height < 1) {
         throw new IllegalArgumentException("Texture must be at least 1x1");
      }
      if (force || !newSize.equals(size) || format != getInternalFormat()) {
         size.setSize(newSize);
         this.internalFormat = format;
         bind();

         if (image == null) {
            glTexImage2D(GL_TEXTURE_2D, 0, getInternalFormat().getInternalFormat(), size.width, size.height, 0, getInternalFormat().getFormat(), getInternalFormat().getType(), (ByteBuffer)null);
         } else {
            DataBuffer dataBuffer = image.getRaster().getDataBuffer();

            if (dataBuffer instanceof DataBufferByte) {
                byte[] pixelData = ((DataBufferByte) dataBuffer).getData();
//                ByteBuffer byteBuffer = ByteBuffer.wrap(pixelData);
                ByteBuffer byteBuffer = BufferUtils.createByteBuffer(pixelData.length);
                byteBuffer.put(pixelData);
                byteBuffer.flip();
                glTexImage2D(GL_TEXTURE_2D, 0, getInternalFormat().getInternalFormat(), size.width, size.height, 0, getInternalFormat().getFormat(), getInternalFormat().getType(), byteBuffer);
            }
            else if (dataBuffer instanceof DataBufferUShort) {
                short[] pixelData = ((DataBufferUShort) dataBuffer).getData();
                ShortBuffer shortBuffer = BufferUtils.createShortBuffer(pixelData.length);
                shortBuffer.put(ShortBuffer.wrap(pixelData));
                shortBuffer.flip();
                glTexImage2D(GL_TEXTURE_2D, 0, getInternalFormat().getInternalFormat(), size.width, size.height, 0, getInternalFormat().getFormat(), getInternalFormat().getType(), shortBuffer);
            }
            else if (dataBuffer instanceof DataBufferShort) {
               short[] pixelData = ((DataBufferShort) dataBuffer).getData();
               ShortBuffer shortBuffer = BufferUtils.createShortBuffer(pixelData.length);
               shortBuffer.put(ShortBuffer.wrap(pixelData));
               shortBuffer.flip();
               glTexImage2D(GL_TEXTURE_2D, 0, getInternalFormat().getInternalFormat(), size.width, size.height, 0, getInternalFormat().getFormat(), getInternalFormat().getType(), shortBuffer);
            }
            else if (dataBuffer instanceof DataBufferInt) {
               int[] pixelData = ((DataBufferInt) dataBuffer).getData();
               IntBuffer intBuffer = BufferUtils.createIntBuffer(pixelData.length);
               intBuffer.put(pixelData);
               intBuffer.flip();
               glTexImage2D(GL_TEXTURE_2D, 0, getInternalFormat().getInternalFormat(), size.width, size.height, 0, getInternalFormat().getFormat(), getInternalFormat().getType(), intBuffer);
            }
            else {
                throw new IllegalArgumentException("Not implemented for data buffer type: " + dataBuffer.getClass());
            }
         }
      }
   }
   protected ELTDisplayContext getELTDisplayContext() {
      return eltDisplayContext;
   }

   @Override
   public void close() throws IOException {
      try {
         eltDisplayContext.syncExec(() -> {
            glDeleteTextures(id);
         });
      } catch (InterruptedException | ExecutionException e) {
         LOGGER.error(e.getMessage(), e);
      }
   }

   public GLInternalFormat getInternalFormat() {
      return internalFormat;
   }

}