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

import static org.lwjgl.opengl.GL20.GL_COMPILE_STATUS;
import static org.lwjgl.opengl.GL20.GL_FRAGMENT_SHADER;
import static org.lwjgl.opengl.GL20.GL_LINK_STATUS;
import static org.lwjgl.opengl.GL20.GL_VERTEX_SHADER;
import static org.lwjgl.opengl.GL20.glAttachShader;
import static org.lwjgl.opengl.GL20.glCompileShader;
import static org.lwjgl.opengl.GL20.glCreateProgram;
import static org.lwjgl.opengl.GL20.glCreateShader;
import static org.lwjgl.opengl.GL20.glDeleteProgram;
import static org.lwjgl.opengl.GL20.glGetProgramInfoLog;
import static org.lwjgl.opengl.GL20.glGetProgrami;
import static org.lwjgl.opengl.GL20.glGetShaderInfoLog;
import static org.lwjgl.opengl.GL20.glGetShaderi;
import static org.lwjgl.opengl.GL20.glGetUniformLocation;
import static org.lwjgl.opengl.GL20.glLinkProgram;
import static org.lwjgl.opengl.GL20.glShaderSource;
import static org.lwjgl.opengl.GL20.glUseProgram;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Representation of a ShaderProgram. 
 */
public class ShaderProgram implements Closeable {
   private static final Logger LOGGER = LoggerFactory.getLogger(ShaderProgram.class);

   private final int programId;
   private final int vertexShader;
   private final int fragmentShader;

   /**
    * Compile and attach vertex/fragment shaders and link as into a program identifiable
    * by programId
    * @param clazz Shader class (e.g HistogramType, ShapeType)
    * @param vertexShader
    * @param fragmentShader
    * @throws IOException
    */
   public ShaderProgram(Class<?> clazz, String vertexShader, String fragmentShader) throws IOException {
      programId = glCreateProgram();

      this.vertexShader = glCreateShader(GL_VERTEX_SHADER);
      glShaderSource(this.vertexShader, readShaderCode(clazz, vertexShader));
      glCompileShader(this.vertexShader);
      checkCompileStatus(this.vertexShader, vertexShader);

      glAttachShader(this.programId, this.vertexShader);

      this.fragmentShader = glCreateShader(GL_FRAGMENT_SHADER);
      glShaderSource(this.fragmentShader, readShaderCode(clazz, fragmentShader));
      glCompileShader(this.fragmentShader);
      checkCompileStatus(this.fragmentShader, fragmentShader);
      glAttachShader(this.programId, this.fragmentShader);

      glLinkProgram(this.programId);
      checkLinkStatus(vertexShader, fragmentShader);
   }

   private void checkCompileStatus(int shader, String shaderSource) {
      int compileStatus = glGetShaderi(shader, GL_COMPILE_STATUS);
//      LOGGER.debug(shaderSource + " compileStatus == " + compileStatus);

      if (compileStatus == 0) {
         LOGGER.error(glGetShaderInfoLog(shader));
      }

   }
   private void checkLinkStatus(String vShaderSource, String fShaderSource) {
      int linkStatus = glGetProgrami(this.programId, GL_LINK_STATUS);
//      LOGGER.debug(String.format("%s/%s linkStatus == %d", vShaderSource, fShaderSource, linkStatus));
      if (linkStatus == 0) {
         LOGGER.error(glGetProgramInfoLog(programId));
      }
   }

   /**
    * Use Shader program
    */
   public void useProgram() {
      glUseProgram(programId);
   }

   public int getUniformLocation(String uniformName) {
      final int location = glGetUniformLocation(programId, uniformName);
      return location;
   }

   public int getProgramId() {
      return programId;
   }


   /**
    * @param clazz
    * @param shaderFilename
    * @return shader code as string
    * @throws IOException
    */
   private static String readShaderCode(Class<?> clazz, String shaderFilename) throws IOException {
      String result;

      // scoping the path within the class's package
      final String packageName = clazz.getName().replace(".","/").replaceFirst("/[^/]*$", "");
      final String qualifiedName = String.format("/%s/shader/%s", packageName, shaderFilename);

      try (InputStream is = clazz.getResourceAsStream(qualifiedName)) {
         if (is == null) {
            throw new IllegalArgumentException(String.format("Can't find shader '%s' from '%s'", qualifiedName, clazz.toString()));
         }
         final Pattern includePattern = Pattern.compile("^#include\\s+([^\\s]+)\\s*$");
         String line;

         try (StringWriter sw = new StringWriter()) {
            try (PrintWriter pw = new PrintWriter(sw)) {
               try (BufferedReader br = new BufferedReader(new InputStreamReader(is))) {
                  while ((line = br.readLine()) != null) {
                     Matcher matcher = includePattern.matcher(line);
                     if (matcher.matches()) {
                        final String includeFile = matcher.group(1);
                        pw.println(readShaderCode(clazz, includeFile));
                     } else {
                        pw.println(line);
                     }
                  }
               }
            }
            result = sw.toString();
         }
      }
      return result;
   }

   @Override
   public void close() throws IOException {
      glDeleteProgram(programId);
   }

}
