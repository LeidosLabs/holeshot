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

package com.leidoslabs.holeshot.elt.drawing;

import static org.lwjgl.opengl.GL11.GL_DOUBLE;
import static org.lwjgl.opengl.GL11.GL_FLOAT;
import static org.lwjgl.opengl.GL11.GL_TRIANGLES;
import static org.lwjgl.opengl.GL15.GL_ARRAY_BUFFER;
import static org.lwjgl.opengl.GL15.GL_STATIC_DRAW;
import static org.lwjgl.opengl.GL15.glBindBuffer;
import static org.lwjgl.opengl.GL15.glBufferData;
import static org.lwjgl.opengl.GL15.glDeleteBuffers;
import static org.lwjgl.opengl.GL15.glGenBuffers;
import static org.lwjgl.opengl.GL20.glEnableVertexAttribArray;
import static org.lwjgl.opengl.GL20.glUniformMatrix4fv;
import static org.lwjgl.opengl.GL20.glVertexAttribPointer;
import static org.lwjgl.opengl.GL30.glBindVertexArray;
import static org.lwjgl.opengl.GL30.glDeleteVertexArrays;
import static org.lwjgl.opengl.GL30.glGenVertexArrays;
import static org.lwjgl.opengl.GL31.glDrawArraysInstanced;
import static org.lwjgl.opengl.GL33.glVertexAttribDivisor;

import java.awt.Color;
import java.io.Closeable;
import java.io.IOException;
import java.nio.FloatBuffer;
import java.util.List;

import org.image.common.util.CloseableUtils;
import org.joml.Matrix4f;
import org.joml.Vector3d;
import org.joml.Vector3f;
import org.lwjgl.BufferUtils;

import com.leidoslabs.holeshot.elt.ELTDisplayContext;
import com.leidoslabs.holeshot.elt.coord.GeodeticELTCoordinate;
import com.leidoslabs.holeshot.elt.coord.ImageWorld;
import com.leidoslabs.holeshot.elt.gpuimage.ShaderProgram;
import com.leidoslabs.holeshot.elt.imagechain.Framebuffer;
import com.leidoslabs.holeshot.elt.layers.Renderer;
import com.leidoslabs.holeshot.elt.observations.Observation;
import com.leidoslabs.holeshot.imaging.coord.ImageScale;

/**
 * OpenGL Renderer Placemarks in ELT
 */
public class PlacemarkRenderer extends Renderer<List<Observation>> implements Closeable {

   private static final Color placemarkColor = new Color(255, 0, 0);
   private static final Color placemarkHighlightColor = new Color(0, 255, 0);
   private static final float[] VERTICES = new float[]{
         0f, 0f,
         -0.5f, .75f,
         0.5f, .75f};

   private ShaderProgram placemarkShader;

   private int placemarkVAO;
   private int placemarkVBO;
   private int placemarkColorVBO;
   private int placemarkInstanceVBO;

   private FloatBuffer modelBuffer;
   private FloatBuffer projectionBuffer;

   /**
    * Construct PlacemarkRenderer given imageWorld and displaycontext
    * @param imageWorld
    * @param eltDisplayContext
    */
   public PlacemarkRenderer(ImageWorld imageWorld, ELTDisplayContext eltDisplayContext) {
      super(imageWorld, eltDisplayContext);
      modelBuffer = BufferUtils.createFloatBuffer(16);
      projectionBuffer = BufferUtils.createFloatBuffer(16);
   }

   @Override
   /**
    * Render a list of observations 
    */
   public void render(List<Observation> points) throws IOException {
      final Framebuffer fb = getResultFramebuffer();
      if(fb != null) {
         try {
            fb.bind();
            initializeShaders();
            drawPlacemarks(points);
         } finally {
            glBindBuffer(GL_ARRAY_BUFFER, 0);
            glBindVertexArray(0);
            fb.unbind();
         }
      }
   }

   private void drawPlacemarks(List<Observation> points) {

      if (points.size() == 0) {
         return;
      }

      double translations[] = new double[points.size() * 2];
      float colors[] = new float[points.size() * 3];

      final ImageWorld imageWorld = getImageWorld();
      for (int i = 0; i < points.size(); i++) {
         Vector3d openGLCoord = new GeodeticELTCoordinate(imageWorld, points.get(i).getGeometry().getCoordinate(), ImageScale.forRset(Math.floor(imageWorld.getImageScale().getRset()))).getOpenGLCoordinate();
         translations[i * 2] = openGLCoord.x;
         translations[i * 2 + 1] = openGLCoord.y;
         Color color = points.get(i).isSelected() ? placemarkHighlightColor : placemarkColor;
         colors[i * 3] = color.getRed() / 255f;
         colors[i * 3 + 1] = color.getGreen() / 255f;
         colors[i * 3 + 2] = color.getBlue() / 255f;
      }

      glBindVertexArray(placemarkVAO);

      //Set Vertices
      glBindBuffer(GL_ARRAY_BUFFER, placemarkVBO);
      glBufferData(GL_ARRAY_BUFFER, VERTICES, GL_STATIC_DRAW);
      glVertexAttribPointer(0, 2, GL_FLOAT, false, 2 * 4, 0L);
      glEnableVertexAttribArray(0);

      // Set colors
      glBindBuffer(GL_ARRAY_BUFFER, placemarkColorVBO);
      glBufferData(GL_ARRAY_BUFFER, colors, GL_STATIC_DRAW);
      glVertexAttribPointer(1, 3, GL_FLOAT, true, 3 * 4, 0L);
      glVertexAttribDivisor(1, 1);
      glEnableVertexAttribArray(1);

      // Set translation data for instances of marker
      glBindBuffer(GL_ARRAY_BUFFER, placemarkInstanceVBO);
      glBufferData(GL_ARRAY_BUFFER, translations, GL_STATIC_DRAW);
      glVertexAttribPointer(2, 2, GL_DOUBLE, false, 2 * 8, 0L);
      glVertexAttribDivisor(2, 1);
      glEnableVertexAttribArray(2);

      placemarkShader.useProgram();
      glUniformMatrix4fv(placemarkShader.getUniformLocation("mvMat"), false, modelBuffer);
      glUniformMatrix4fv(placemarkShader.getUniformLocation("pMat"), false, projectionBuffer);
      glDrawArraysInstanced(GL_TRIANGLES, 0, 3, translations.length / 2);

      glBindBuffer(GL_ARRAY_BUFFER, 0);
      glBindVertexArray(0);

   }

   private void initializeShaders() throws IOException {

      final ImageWorld imageWorld = getImageWorld();
      if (placemarkShader == null) {
         placemarkShader = new ShaderProgram(ShapeType.class, ShapeType.Shaders.PLACEMARK_VERTEX_SHADER, ShapeType.Shaders.PLACEMARK_FRAGMENT_SHADER);
         placemarkVAO = glGenVertexArrays();
         placemarkVBO = glGenBuffers();
         placemarkInstanceVBO = glGenBuffers();
         placemarkColorVBO = glGenBuffers();
      }

      imageWorld.getCurrentProjection().get(this.projectionBuffer);
      new Matrix4f()
      .scale((float) imageWorld.getTopTile().getImage().normalize(50) * (float) Math.pow(2.0, imageWorld.getImageScale().getRset()))
      .rotate((float) -imageWorld.getRotation(), new Vector3f(0.0f, 0.0f, 1.0f)).get(modelBuffer);

   }
   @Override
   public void close() throws IOException {
      super.close();
      CloseableUtils.close(placemarkShader);
      glDeleteVertexArrays(placemarkVAO);
      glDeleteBuffers(new int[] {placemarkVBO, placemarkColorVBO, placemarkInstanceVBO});
   }
}