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

package com.leidoslabs.holeshot.chipper;

import static org.lwjgl.glfw.GLFW.GLFW_FALSE;
import static org.lwjgl.glfw.GLFW.GLFW_VISIBLE;
import static org.lwjgl.glfw.GLFW.glfwCreateWindow;
import static org.lwjgl.glfw.GLFW.glfwDestroyWindow;
import static org.lwjgl.glfw.GLFW.glfwInit;
import static org.lwjgl.glfw.GLFW.glfwMakeContextCurrent;
import static org.lwjgl.glfw.GLFW.glfwSwapBuffers;
import static org.lwjgl.glfw.GLFW.glfwWindowHint;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.system.MemoryUtil.NULL;

import java.io.IOException;
import java.io.PrintStream;
import java.util.concurrent.ExecutionException;

import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.opengl.GL;
import org.slf4j.Logger;
import org.usefultoys.slf4j.LoggerFactory;

import com.leidoslabs.holeshot.elt.ELTDisplayContext;
import com.leidoslabs.holeshot.elt.ELTDisplayExecutor;


/**
 * GLFW Graphics context
 */
class GLFWGraphicsContext extends GraphicsContext {
   private static final Logger LOGGER = LoggerFactory.getLogger(GLFWGraphicsContext.class);

   private final ELTDisplayExecutor eltDisplayExecutor;
   private final long window;

   /**
    * Constructor, initializes display executor and creates window
    */
   public GLFWGraphicsContext() {
      glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE);
//      glfwWindowHint(GLFW_OPENGL_FORWARD_COMPAT, GLFW_TRUE);
//      glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);

      this.window = glfwCreateWindow(5, 5, "glfwGC", NULL, NULL);
      this.eltDisplayExecutor = new ELTDisplayExecutor(new GLFWDisplayContext());
   }

   @Override
   public void init() {
   }

   @Override
   /**
    * Make context current
    */
   public void makeCurrent() {
      glfwMakeContextCurrent(window);
      GL.createCapabilities();
   }

   @Override
   public void swapBuffers() {
      glfwSwapBuffers(window);
   }

   @Override
   public ELTDisplayExecutor getDisplayExecutor() {
      return eltDisplayExecutor;
   }
   @Override
   public void close() throws IOException {
      glfwDestroyWindow(window);
   }

   private class GLFWDisplayContext extends ELTDisplayContext {
      public GLFWDisplayContext() {
      }

      @Override
      public void asyncExec(Runnable runnable) throws InterruptedException, ExecutionException {
         new Thread(runnable).start();
      }

      @Override
      public void syncExec(Runnable runnable) throws InterruptedException, ExecutionException {
         runnable.run();
      }

      @Override
      protected void setOpenGLContextCurrent() {
         makeCurrent();
      }
   }

   public static class LibraryInitializer extends GraphicsContext.LibraryInitializer {
      @Override
      public void initializeGL() {
         super.initializeGL();
         GLFWErrorCallback.createPrint(new PrintStream(LoggerFactory.getErrorOutputStream(LOGGER))).set();
         glfwInit();
      }
   }

}
