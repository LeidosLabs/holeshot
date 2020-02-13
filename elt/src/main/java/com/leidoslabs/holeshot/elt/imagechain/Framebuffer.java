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

import static org.lwjgl.opengl.GL11.GL_COLOR;
import static org.lwjgl.opengl.GL11.GL_FLOAT;
import static org.lwjgl.opengl.GL11.GL_NEAREST;
import static org.lwjgl.opengl.GL11.GL_RGB;
import static org.lwjgl.opengl.GL11.glReadPixels;
import static org.lwjgl.opengl.GL11.glViewport;
import static org.lwjgl.opengl.GL12.GL_CLAMP_TO_EDGE;
import static org.lwjgl.opengl.GL30.GL_COLOR_ATTACHMENT0;
import static org.lwjgl.opengl.GL30.GL_DEPTH_ATTACHMENT;
import static org.lwjgl.opengl.GL30.GL_DRAW_FRAMEBUFFER;
import static org.lwjgl.opengl.GL30.GL_FRAMEBUFFER;
import static org.lwjgl.opengl.GL30.GL_READ_FRAMEBUFFER;
import static org.lwjgl.opengl.GL30.GL_RENDERBUFFER;
import static org.lwjgl.opengl.GL30.glBindFramebuffer;
import static org.lwjgl.opengl.GL30.glBindRenderbuffer;
import static org.lwjgl.opengl.GL30.glClearBufferfv;
import static org.lwjgl.opengl.GL30.glDeleteFramebuffers;
import static org.lwjgl.opengl.GL30.glFramebufferRenderbuffer;
import static org.lwjgl.opengl.GL30.glFramebufferTexture2D;
import static org.lwjgl.opengl.GL30.glGenFramebuffers;
import static org.lwjgl.opengl.GL30.glGenRenderbuffers;
import static org.lwjgl.opengl.GL30.glRenderbufferStorage;
import static org.lwjgl.opengl.GL40.*;

import java.awt.Dimension;
import java.awt.Rectangle;
import java.io.Closeable;
import java.io.IOException;
import java.nio.FloatBuffer;

import org.image.common.util.CloseableUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL14;

import com.leidoslabs.holeshot.elt.ELTDisplayContext;
import com.leidoslabs.holeshot.elt.gpuimage.GLInternalFormat;
import com.leidoslabs.holeshot.elt.gpuimage.Texture;


/**
 * Representation of an OpenGL frame buffer. Manipulated by image chain operations 
 */
public class Framebuffer implements Closeable {
   public int getFboId() {
      return fboId;
   }

   public Texture getTexture() {
      return texture;
   }
   private final int fboId;
   private final Texture texture;
   private final int depthRenderBufferID;

   /**
    * Binds frame, attaches texture, then unbinds
    * @param size
    * @param internalFormat
    * @param eltDisplayContext
    * @throws IOException
    */
   public Framebuffer(Dimension size, GLInternalFormat internalFormat, ELTDisplayContext eltDisplayContext) throws IOException {
      this(size, internalFormat, eltDisplayContext, GL_NEAREST, GL_CLAMP_TO_EDGE);
   }

   /**
    * Binds frame, attaches texture, then unbinds
    * @param size
    * @param internalFormat
    * @param eltDisplayContext
    * @param filter
    * @param wrap
    * @throws IOException
    */
   public Framebuffer(Dimension size, GLInternalFormat internalFormat, ELTDisplayContext eltDisplayContext, int filter, int wrap) throws IOException {
      fboId = glGenFramebuffers();
      this.texture = new Texture(size, internalFormat, filter, wrap, eltDisplayContext);
      bind();
      attachTexture();

      depthRenderBufferID = glGenRenderbuffers();

      resetDepthBuffer();

      unbind();
   }

   private void attachTexture() {
      glFramebufferTexture2D( GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0,
            GL11.GL_TEXTURE_2D, texture.getId(), 0);
   }

   public void bind() {
      glBindFramebuffer(GL_DRAW_FRAMEBUFFER, fboId); // this fb, msaa or normal
      glBindFramebuffer(GL_READ_FRAMEBUFFER, fboId);  // msaa: sampling sink, normal: this fb

   }

   public void unbind() {
      glBindFramebuffer(GL_DRAW_FRAMEBUFFER, 0); // this fb, msaa or normal
      glBindFramebuffer(GL_READ_FRAMEBUFFER, 0);  // msaa: sampling sink, normal: this fb
   }

   public Rectangle getRectangle() {
      return new Rectangle(0,0,getSize().width, getSize().height);
   }
   public Dimension getSize() {
      return texture.getSize();
   }

   public void clearBuffer() {
      clearBuffer(0.0f, 0.0f, 0.0f, 1.0f);
   }
   public void clearBuffer(float clearRed, float clearGreen, float clearBlue, float clearAlpha) {
      try {
         bind();
         //glEnable(GL_ALPHA_TEST);
         //glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
//         glClearBufferfv(GL_COLOR, 0, new float[] { clearRed, clearGreen, clearBlue, clearAlpha });
         glClearBufferfv(GL_COLOR, 0, new float[] { 0.0f, 0.0f, 0.0f, 0.0f });
      } finally {
         unbind();
      }
   }


   public void reset(Dimension size) {
      this.reset(size, texture.getInternalFormat());
   }

   public void reset(Dimension size, GLInternalFormat internalFormat) {
      if (!size.equals(getSize()) || texture.getInternalFormat() != internalFormat) {
         texture.reset(size, internalFormat);
         resetDepthBuffer();
      }
   }
   private void resetDepthBuffer() {
      glBindRenderbuffer(GL_RENDERBUFFER, depthRenderBufferID);                // bind the depth renderbuffer
      glRenderbufferStorage(GL_RENDERBUFFER, GL14.GL_DEPTH_COMPONENT24, getSize().width, getSize().height); // get the data space for it
      glFramebufferRenderbuffer(GL_FRAMEBUFFER,GL_DEPTH_ATTACHMENT,GL_RENDERBUFFER, depthRenderBufferID); // bind it to the renderbuffer
   }

   /**
    * Read frame into outputArray
    * @param outputBuffer
    * @param outputArray array to be written to
    */
   public void readFramebuffer(FloatBuffer outputBuffer, float[] outputArray) {
      final Dimension size = getSize();
      final int fbWidth = size.width;
      final int fbHeight = size.height;

      glBindFramebuffer(GL_READ_FRAMEBUFFER, getFboId());
      glViewport(0, 0, fbWidth, fbHeight);

      outputBuffer.rewind();
      glReadPixels(0, 0, fbWidth, fbHeight, GL_RGB, GL_FLOAT, outputBuffer);
      outputBuffer.get(outputArray);
   }

   @Override
   public void close() throws IOException {
      CloseableUtils.close(texture);
      glDeleteFramebuffers(fboId);
      glDeleteRenderbuffers(depthRenderBufferID);
   }
}
