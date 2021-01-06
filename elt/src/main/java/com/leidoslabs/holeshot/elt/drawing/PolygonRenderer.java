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
import static org.lwjgl.opengl.GL11.GL_LINE_STRIP;
import static org.lwjgl.opengl.GL11.glDrawArrays;
import static org.lwjgl.opengl.GL11.glLineWidth;
import static org.lwjgl.opengl.GL15.GL_ARRAY_BUFFER;
import static org.lwjgl.opengl.GL15.GL_STATIC_DRAW;
import static org.lwjgl.opengl.GL15.glBindBuffer;
import static org.lwjgl.opengl.GL15.glBufferData;
import static org.lwjgl.opengl.GL15.glDeleteBuffers;
import static org.lwjgl.opengl.GL15.glGenBuffers;
import static org.lwjgl.opengl.GL20.glEnableVertexAttribArray;
import static org.lwjgl.opengl.GL20.glUniform3fv;
import static org.lwjgl.opengl.GL20.glUniformMatrix4fv;
import static org.lwjgl.opengl.GL20.glVertexAttribPointer;
import static org.lwjgl.opengl.GL30.glBindVertexArray;
import static org.lwjgl.opengl.GL30.glDeleteVertexArrays;
import static org.lwjgl.opengl.GL30.glGenVertexArrays;

import java.awt.Color;
import java.io.Closeable;
import java.io.IOException;
import java.nio.FloatBuffer;
import java.util.List;

import org.image.common.util.CloseableUtils;
import org.joml.Vector3f;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.lwjgl.BufferUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.leidoslabs.holeshot.elt.ELTDisplayContext;
import com.leidoslabs.holeshot.elt.gpuimage.ShaderProgram;
import com.leidoslabs.holeshot.elt.imagechain.Framebuffer;
import com.leidoslabs.holeshot.elt.layers.Renderer;
import com.leidoslabs.holeshot.elt.observations.Observation;
import com.leidoslabs.holeshot.elt.viewport.ImageWorld;

/**
 * OpenGL renderer for polygons in ELT
 */
public class PolygonRenderer extends Renderer<List<Observation>> implements Closeable {
	private static final Logger LOGGER = LoggerFactory.getLogger(PolygonRenderer.class);
    private static final float POLY_LINE_WIDTH = 4f;
    private static final Color polygonColor = new Color(255, 0, 177);
    private static final Color polygonHighlightColor = new Color(255, 255, 66);
    private static final String SHADER_KEY = PolygonRenderer.class.getName();

    private int polygonVAO;
    private int polygonVBO;

    private FloatBuffer projectionBuffer;
    private FloatBuffer colorBuffer;
    private boolean initialized;
    
    /**
     * Initializes buffers
     * @param imageWorld
     * @param eltDisplayContext
     */
    public PolygonRenderer(ImageWorld imageWorld, ELTDisplayContext eltDisplayContext) {
        super(imageWorld, eltDisplayContext);
        initialized = false;
        projectionBuffer = BufferUtils.createFloatBuffer(16);
        colorBuffer = BufferUtils.createFloatBuffer(3);
    }

    @Override
    /**
     * Renders given a list of observations 
     */
    public void render(List<Observation> obs) throws IOException {
       final Framebuffer fb = getResultFramebuffer();
       if (fb != null) {
        try {
            fb.bind();
            initializeShaders();
            drawPolygons(obs);
        } finally {
            glBindBuffer(GL_ARRAY_BUFFER, 0);
            glBindVertexArray(0);
            fb.unbind();
        }
       }
    }

    private void drawPolygons(List<Observation> obs) throws IOException {

        if (obs.size() == 0) {
            return;
        }

        final ImageWorld imageWorld = getImageWorld();

        for (Observation observation : obs) {

            Geometry boundary = observation.getGeometry();

            int numVertices = boundary.getNumPoints();
            double[] vertices = new double[numVertices * 2];

            for (int j = 0; j < numVertices; j++) {
                Coordinate projectedCoord = imageWorld.geodeticToProjected(boundary.getCoordinates()[j]);
                vertices[j * 2] = projectedCoord.x;
                vertices[j * 2 + 1] = projectedCoord.y;
            }

            Color color = observation.isSelected() ? polygonHighlightColor : polygonColor;

            new Vector3f(color.getRed(),
                         color.getGreen(),
                         color.getBlue()).div(255f).get(colorBuffer);

            glBindVertexArray(polygonVAO);

            glBindBuffer(GL_ARRAY_BUFFER, polygonVBO);
            glBufferData(GL_ARRAY_BUFFER, vertices, GL_STATIC_DRAW);
            glVertexAttribPointer(0, 2, GL_DOUBLE, false, 2 * 8, 0L);
            glEnableVertexAttribArray(0);

        	ShaderProgram shader = getELTDisplayContext().getShader(SHADER_KEY, ShapeType.class, ShapeType.Shaders.POLYGON_VERTEX_SHADER, ShapeType.Shaders.POLYGON_FRAGMENT_SHADER);

        	shader.useProgram();
            glUniformMatrix4fv(shader.getUniformLocation("pMat"), false, projectionBuffer);
            glUniform3fv(shader.getUniformLocation("color"), colorBuffer);
            glDrawArrays(GL_LINE_STRIP, 0, numVertices);

            glBindBuffer(GL_ARRAY_BUFFER, 0);
            glBindVertexArray(0);

        }
    }

    private void initializeShaders() throws IOException {

        if (!initialized) {
        	initialized = true;
            polygonVAO = glGenVertexArrays();
            polygonVBO = glGenBuffers();
        }

        final ImageWorld imageWorld = getImageWorld();
        imageWorld.getOpenGLMVPMatrix().get(this.projectionBuffer);
        glLineWidth(POLY_LINE_WIDTH);

    }

    @Override
    public void close() throws IOException {
       super.close();
       glDeleteVertexArrays(polygonVAO);
       glDeleteBuffers(polygonVBO);
    }
}
