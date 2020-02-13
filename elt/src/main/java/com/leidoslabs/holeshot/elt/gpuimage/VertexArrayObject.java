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

import static org.lwjgl.opengl.GL11.GL_FLOAT;
import static org.lwjgl.opengl.GL15.GL_ARRAY_BUFFER;
import static org.lwjgl.opengl.GL15.GL_STATIC_DRAW;
import static org.lwjgl.opengl.GL15.glBindBuffer;
import static org.lwjgl.opengl.GL15.glBufferData;
import static org.lwjgl.opengl.GL15.glGenBuffers;
import static org.lwjgl.opengl.GL20.glEnableVertexAttribArray;
import static org.lwjgl.opengl.GL20.glVertexAttribPointer;
import static org.lwjgl.opengl.GL30.glBindVertexArray;
import static org.lwjgl.opengl.GL30.glGenVertexArrays;
import static org.lwjgl.opengl.GL30.glDeleteVertexArrays;
import static org.lwjgl.opengl.GL40.*;

import java.awt.Dimension;
import java.io.Closeable;
import java.io.IOException;
import java.nio.FloatBuffer;

import org.lwjgl.BufferUtils;

import com.leidoslabs.holeshot.elt.imagechain.Framebuffer;

/**
 * Representation of a VAO given a float vertexBuffer
 */
public class VertexArrayObject implements Closeable {
   private final int vao;
   private final int arrayBuffer;
   private int downsamplingFactor;
   private final FloatBuffer vertexBuffer;
   private final Dimension size;

   /**
    * Construct VAO from vertexBuffer size, and attribute
    * @param vertexBuffer
    * @param size
    * @param attribute
    */
   public VertexArrayObject(FloatBuffer vertexBuffer, Dimension size, int attribute) {
      arrayBuffer = glGenBuffers();
      this.size = size;
      this.downsamplingFactor = -1;
      glBindBuffer(GL_ARRAY_BUFFER, arrayBuffer);
      this.vertexBuffer = vertexBuffer;
      glBufferData(GL_ARRAY_BUFFER, this.vertexBuffer, GL_STATIC_DRAW);

      // Allocate an Array Buffer to hold the vertices that are sent down to the vertex shader
      this.vao = glGenVertexArrays();
      glBindVertexArray(vao);

      glEnableVertexAttribArray(attribute);
      glVertexAttribPointer(attribute, 2, GL_FLOAT, false, 0, 0L);


      glBindBuffer(GL_ARRAY_BUFFER, 0);
      glBindVertexArray(0);
   }
   private VertexArrayObject(Framebuffer framebuffer, int downsamplingFactor, int attribute, Dimension size) {
      this(allocateVertexBuffer(framebuffer, size), size, attribute);
      this.downsamplingFactor = downsamplingFactor;
   }

   public VertexArrayObject(Framebuffer framebuffer, int downsamplingFactor, int attribute) {
      this(framebuffer, downsamplingFactor, attribute, getSize(framebuffer, downsamplingFactor));
   }

   public Dimension getSize() {
      return size;
   }

   public int getVao() {
      return vao;
   }

   public FloatBuffer getVertexBuffer() {
      return vertexBuffer;
   }

   public int getDownsamplingFactor() {
      return downsamplingFactor;
   }

   private static Dimension getSize(Framebuffer framebuffer, int downsamplingFactor) {
      Dimension fbSize = framebuffer.getSize();
      Dimension size = new Dimension((int)(fbSize.getWidth()/downsamplingFactor), (int)(fbSize.getHeight()/downsamplingFactor));
      return size;

   }
   private static FloatBuffer allocateVertexBuffer(Framebuffer framebuffer, Dimension size) {

      final int bufferSize = size.width * size.height * 2;

      float[] verts = new float[bufferSize];

      float texturePixelX = 1.0f / size.width;
      float texturePixelY = 1.0f / size.height;

      float textureHalfPixelX = texturePixelX / 2.0f;
      float textureHalfPixelY = texturePixelY / 2.0f;

      for (int y = 0; y < size.height; ++y) {
         for (int x = 0; x < size.width; ++x) {
            final int offset = 2 * (y * size.width + x);
            verts[offset] = texturePixelX * x + textureHalfPixelX;
            verts[offset + 1] = texturePixelY * y + textureHalfPixelY;
         }
      }
      FloatBuffer vertexBuffer = BufferUtils.createFloatBuffer(bufferSize);
      vertexBuffer.put(verts);
      vertexBuffer.position(0);
      return vertexBuffer;
   }
   @Override
   public void close() throws IOException {
      glDeleteVertexArrays(vao);
      glDeleteBuffers(arrayBuffer);
   }

}
