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

import static org.lwjgl.egl.EGL10.EGL_BLUE_SIZE;
import static org.lwjgl.egl.EGL10.EGL_DEPTH_SIZE;
import static org.lwjgl.egl.EGL10.EGL_GREEN_SIZE;
import static org.lwjgl.egl.EGL10.EGL_HEIGHT;
import static org.lwjgl.egl.EGL10.EGL_NONE;
import static org.lwjgl.egl.EGL10.EGL_NO_CONTEXT;
import static org.lwjgl.egl.EGL10.EGL_NO_DISPLAY;
import static org.lwjgl.egl.EGL10.EGL_PBUFFER_BIT;
import static org.lwjgl.egl.EGL10.EGL_RED_SIZE;
import static org.lwjgl.egl.EGL10.EGL_SURFACE_TYPE;
import static org.lwjgl.egl.EGL10.EGL_WIDTH;
import static org.lwjgl.egl.EGL10.*;
import static org.lwjgl.egl.EGL10.eglChooseConfig;
import static org.lwjgl.egl.EGL10.eglCreateContext;
import static org.lwjgl.egl.EGL10.eglCreatePbufferSurface;
import static org.lwjgl.egl.EGL10.eglGetDisplay;
import static org.lwjgl.egl.EGL10.eglGetError;
import static org.lwjgl.egl.EGL10.eglInitialize;
import static org.lwjgl.egl.EGL10.eglMakeCurrent;
import static org.lwjgl.egl.EGL10.eglSwapBuffers;
import static org.lwjgl.egl.EGL12.EGL_RENDERABLE_TYPE;
import static org.lwjgl.egl.EGL12.eglBindAPI;
import static org.lwjgl.egl.EGL14.EGL_DEFAULT_DISPLAY;
import static org.lwjgl.egl.EGL14.EGL_OPENGL_API;
import static org.lwjgl.egl.EGL14.EGL_OPENGL_BIT;
import static org.lwjgl.egl.EGL15.EGL_CONTEXT_MAJOR_VERSION;
import static org.lwjgl.egl.EGL15.EGL_CONTEXT_MINOR_VERSION;
import static org.lwjgl.egl.EGL15.EGL_CONTEXT_OPENGL_CORE_PROFILE_BIT;
import static org.lwjgl.egl.EGL15.EGL_CONTEXT_OPENGL_PROFILE_MASK;
import static org.lwjgl.system.MemoryStack.stackPush;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.IntBuffer;
import java.util.concurrent.ExecutionException;

import org.lwjgl.PointerBuffer;
import org.lwjgl.egl.EGL;
import org.lwjgl.egl.EGLCapabilities;
import org.lwjgl.opengl.GL;
import org.lwjgl.system.MemoryStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.leidoslabs.holeshot.elt.ELTDisplayContext;
import com.leidoslabs.holeshot.elt.ELTDisplayExecutor;


/**
 * EGL Graphics Context, context for linux platforms
 */
class EGLGraphicsContext extends GraphicsContext {
   private static final Logger LOGGER = LoggerFactory.getLogger(EGLGraphicsContext.class);

   private boolean eglInitialized;
   private static long display;

   private static PointerBuffer configs;

   private static int[] numConfigs;
   private long context;
   private long eglSurface;
   private ELTDisplayExecutor displayExecutor;

   /**
    * Constructor, initializes DisplayExecutor for context
    */
   public EGLGraphicsContext() {
      eglInitialized = false;
      displayExecutor = new ELTDisplayExecutor(new EGLDisplayContext());
   }

   public ELTDisplayExecutor getDisplayExecutor() {
      return displayExecutor;
   }

   private class EGLDisplayContext extends ELTDisplayContext {
      @Override
      public void asyncExec(Runnable runnable) throws InterruptedException, ExecutionException {
         runnable.run();
      }
      @Override
      protected void setOpenGLContextCurrent() {
         makeCurrent();
      }
      @Override
      public void syncExec(Runnable runnable) throws InterruptedException, ExecutionException {
         runnable.run();
      }
   }


   @Override
   /**
    * Set to current context
    */
   public void makeCurrent() {
      if (eglInitialized) {
         eglMakeCurrent(display, eglSurface, eglSurface, context);
      }
   }

   @Override
   /**
    * Initialize EGL
    */
   public void init() {
      final int pbufferWidth = 1;
      final int pbufferHeight = 1;
      IntBuffer pbufferAttribs = IntBuffer.wrap(new int[] { EGL_WIDTH, pbufferWidth,
            EGL_HEIGHT, pbufferHeight,
            EGL_NONE });

      eglSurface = eglCreatePbufferSurface(display,  configs.get(0),  pbufferAttribs);

      // bind OpenGL api
      eglBindAPI(EGL_OPENGL_API);

      if (numConfigs[0] < 1)
         throw new RuntimeException("Could not find a suitable EGL configuration");

      // create a context and make it current
      IntBuffer ai32ContextAttribs = IntBuffer.wrap(new int[] { EGL_CONTEXT_MAJOR_VERSION, 1,
            EGL_CONTEXT_MINOR_VERSION, 5,
            EGL_CONTEXT_OPENGL_PROFILE_MASK, EGL_CONTEXT_OPENGL_CORE_PROFILE_BIT,
            EGL_NONE });
      context = eglCreateContext(display, configs.get(0), EGL_NO_CONTEXT, ai32ContextAttribs);

      eglMakeCurrent(display, eglSurface, eglSurface, context);

      GL.createCapabilities();

      eglInitialized = true;
   }

   @Override
   public void swapBuffers() {
      eglSwapBuffers(display, eglSurface);
   }

   private static void createCapabilities() {
      try (MemoryStack stack = stackPush()) {
         IntBuffer major = stack.mallocInt(1);
         IntBuffer minor = stack.mallocInt(1);

         if (!eglInitialize(display, major, minor)) {
            throw new IllegalStateException(String.format("Failed to initialize EGL [0x%X]", eglGetError()));
         }

         LOGGER.info(String.format("EGL %d.%d", major.get(0), minor.get(0)));

         final EGLCapabilities egl =
               EGL.createDisplayCapabilities(display, major.get(0), minor.get(0));

         if (LOGGER.isDebugEnabled()) {
            try {
               StringBuilder sb = new StringBuilder("EGL Capabilities:");
               for (Field f : EGLCapabilities.class.getFields()) {
                  if (f.getType() == boolean.class) {
                     if (f.get(egl).equals(Boolean.TRUE)) {
                        sb.append(String.format("\n\t%s",f.getName()));
                     }
                  }
               }
               LOGGER.debug(sb.toString());
            } catch (IllegalAccessException e) {
               LOGGER.error(e.getMessage(), e);
            }
         }
      }
   }

   @Override
   /**
    * Destroy context and surface
    */
   public void close() throws IOException {
      eglDestroyContext(display, context);
      eglDestroySurface(display, eglSurface);
   }

   private static void initEGL() {
      display = eglGetDisplay(EGL_DEFAULT_DISPLAY);
      if ( display == EGL_NO_DISPLAY ) {
         LOGGER.error("EGL_NO_DISPLAY");
      }
      createCapabilities();

      try (MemoryStack stack = stackPush()) {
         int[] configAttribs = new int[] {
               EGL_SURFACE_TYPE, EGL_PBUFFER_BIT,
               EGL_BLUE_SIZE, 8,
               EGL_GREEN_SIZE, 8,
               EGL_RED_SIZE, 8,
               EGL_DEPTH_SIZE, 8,
               EGL_RENDERABLE_TYPE, EGL_OPENGL_BIT,
               EGL_NONE};
         configs = stack.mallocPointer(1);
         numConfigs = new int[1];
         eglChooseConfig(display, configAttribs, configs, numConfigs);

      }
   }

   public static class LibraryInitializer extends GraphicsContext.LibraryInitializer {
      @Override
      public void initializeGL() {
         super.initializeGL();
         EGL.create();
         initEGL();
      }
   }
}
