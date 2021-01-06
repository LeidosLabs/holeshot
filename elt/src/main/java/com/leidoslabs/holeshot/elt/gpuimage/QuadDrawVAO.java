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

import static org.lwjgl.opengl.GL11.GL_DOUBLE;
import static org.lwjgl.opengl.GL11.GL_TRIANGLE_STRIP;
import static org.lwjgl.opengl.GL11.glDrawArrays;
import static org.lwjgl.opengl.GL15.GL_ARRAY_BUFFER;
import static org.lwjgl.opengl.GL15.GL_STATIC_DRAW;
import static org.lwjgl.opengl.GL15.GL_WRITE_ONLY;
import static org.lwjgl.opengl.GL15.glBindBuffer;
import static org.lwjgl.opengl.GL15.glBufferData;
import static org.lwjgl.opengl.GL15.glGenBuffers;
import static org.lwjgl.opengl.GL15.glMapBuffer;
import static org.lwjgl.opengl.GL15.glUnmapBuffer;
import static org.lwjgl.opengl.GL20.glEnableVertexAttribArray;
import static org.lwjgl.opengl.GL20.glVertexAttribPointer;
import static org.lwjgl.opengl.GL30.glBindVertexArray;
import static org.lwjgl.opengl.GL30.glDeleteVertexArrays;
import static org.lwjgl.opengl.GL30.glGenVertexArrays;
import static org.lwjgl.opengl.GL40.*;

import java.io.Closeable;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.DoubleBuffer;
import java.util.Arrays;
import java.util.stream.IntStream;

import org.apache.commons.lang3.ArrayUtils;

import com.google.common.collect.Streams;

/**
 * Class VAO representation for loading and drawing Quadrilaterals into openGL 
 */
public class QuadDrawVAO implements Closeable {

  public static final double[][] FULL_UNIFORM_QUAD =
      new double[][] {{-1.0, 1.0, 0.0}, {-1.0, -1.0, 0.0}, {1.0, 1.0, 0.0}, {1.0, -1.0, 0.0}};
  public static final double[][] FULL_TEXTURE_QUAD =
      new double[][] {{0.0, 0.0}, {0.0, 1.0}, {1.0, 0.0}, {1.0, 1.0}};


  private final int vao;
  private final int vboIndice;

  private static final int SIZEOF_DOUBLE;
  private static final int VERTEX_ELEMENT_SIZE;
  private static final int TEXTURE_ELEMENT_SIZE;
  private static final int NORMAL_ELEMENT_SIZE;
  private static final int QUAD_ELEMENTS;
  private static final int QUAD_ELEMENT_SIZE_WITHOUT_TEXTURE;
  private static final int QUAD_ELEMENT_SIZE_WITH_TEXTURE;
  private static final int VERTEX_STRIDE;
  private static final int TEXTURE_STRIDE;
  private static final int NORMAL_STRIDE;
  private static final int STRIDE_WITHOUT_TEXTURE;
  private static final int STRIDE_WITH_TEXTURE;

  static {
     SIZEOF_DOUBLE = 8;
    VERTEX_ELEMENT_SIZE = 3;
    TEXTURE_ELEMENT_SIZE = 2;
    NORMAL_ELEMENT_SIZE = 3;
    VERTEX_STRIDE = SIZEOF_DOUBLE * VERTEX_ELEMENT_SIZE;
    TEXTURE_STRIDE =SIZEOF_DOUBLE * TEXTURE_ELEMENT_SIZE;

    QUAD_ELEMENTS = 4;
    QUAD_ELEMENT_SIZE_WITHOUT_TEXTURE = QUAD_ELEMENTS * VERTEX_ELEMENT_SIZE * SIZEOF_DOUBLE;
    QUAD_ELEMENT_SIZE_WITH_TEXTURE = QUAD_ELEMENT_SIZE_WITHOUT_TEXTURE
        + QUAD_ELEMENTS * (TEXTURE_ELEMENT_SIZE + NORMAL_ELEMENT_SIZE) * SIZEOF_DOUBLE;

    NORMAL_STRIDE = SIZEOF_DOUBLE * NORMAL_ELEMENT_SIZE;
    STRIDE_WITHOUT_TEXTURE = VERTEX_STRIDE;
    STRIDE_WITH_TEXTURE = STRIDE_WITHOUT_TEXTURE + TEXTURE_STRIDE + NORMAL_STRIDE;
  }

  /**
   * Construct VAO without texture data
   * @param vertices
   * @param vertexAttribute
   */
  public QuadDrawVAO(double[][] vertices, int vertexAttribute) {
    this(vertices, vertexAttribute, null, -1);
  }

  /**
   * Construct VAO, load into opengl buffers
   * @param vertices
   * @param vertexAttribute
   * @param textureCoords
   * @param textureAttribute
   */
  public QuadDrawVAO(double[][] vertices, int vertexAttribute, double[][] textureCoords, int textureAttribute) {
    vao = glGenVertexArrays();
    glBindVertexArray(vao);

    final boolean textureCoordsGiven = !ArrayUtils.isEmpty(textureCoords);
    vboIndice = glGenBuffers();

    // Create vertex buffer data store
    glBindBuffer(GL_ARRAY_BUFFER, vboIndice);


    final int bufferSize =
        textureCoordsGiven ? QUAD_ELEMENT_SIZE_WITH_TEXTURE : QUAD_ELEMENT_SIZE_WITHOUT_TEXTURE;
    glBufferData(GL_ARRAY_BUFFER, bufferSize, GL_STATIC_DRAW);


    // Map the buffer and write vertex and texture data directly into it.
    ByteBuffer bytebuffer = glMapBuffer(GL_ARRAY_BUFFER, GL_WRITE_ONLY);
    DoubleBuffer doublebuffer = bytebuffer.order(ByteOrder.nativeOrder()).asDoubleBuffer();

    IntStream.range(0, vertices.length).forEach(i -> 
    {
      (textureCoordsGiven ? 
    		  Streams.concat(Arrays.stream(vertices[i]),
                             Arrays.stream(textureCoords[i]), 
                             Arrays.stream(new double[] {0, 0, 1}))
          : 
        	  Arrays.stream(vertices[i])).forEach(v -> doublebuffer.put(v));
    });

    glUnmapBuffer(GL_ARRAY_BUFFER);


    final int fullStride = textureCoordsGiven ? STRIDE_WITH_TEXTURE : STRIDE_WITHOUT_TEXTURE;

    glVertexAttribPointer(vertexAttribute, VERTEX_ELEMENT_SIZE, GL_DOUBLE, false, fullStride, 0);

    glEnableVertexAttribArray(vertexAttribute);


    if (textureCoordsGiven) {
      glVertexAttribPointer(textureAttribute, TEXTURE_ELEMENT_SIZE, GL_DOUBLE, false, fullStride, VERTEX_STRIDE);

      glEnableVertexAttribArray(textureAttribute);


      // TODO: Not sure what to do with the normal in the shaders
//      glNormalPointer(GL4.GL_DOUBLE, fullStride, VERTEX_STRIDE + TEXTURE_STRIDE);
    }

    glBindBuffer(GL_ARRAY_BUFFER, 0);

    glBindVertexArray(0);


  }

  /**
   * Draw VAO 
   */
  public void draw() {
    glBindVertexArray(vao);

    glDrawArrays(GL_TRIANGLE_STRIP, 0, 4);

    glBindVertexArray(0);

  }

  @Override
  public void close() throws IOException {
     glDeleteVertexArrays(vao);
     glDeleteBuffers(vboIndice);
  }
}
