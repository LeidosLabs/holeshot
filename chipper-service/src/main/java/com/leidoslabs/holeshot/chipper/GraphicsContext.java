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

import java.io.Closeable;
import java.io.IOException;

import org.lwjgl.opengl.GL;
import org.lwjgl.system.Platform;

import com.leidoslabs.holeshot.elt.ELTDisplayContext;

/**
 * Abstract class for OGL graphics contexts. Provides static methods for
 * Retrieving contexts and library initializer
 */
public abstract class GraphicsContext implements Closeable {
   public static GraphicsContext createGraphicsContext() throws Exception {
      GraphicsContext gc;
      if (Platform.get() == Platform.LINUX) {
          gc = new EGLGraphicsContext();
      } else {
          gc = new GLFWGraphicsContext();
      }
      return gc;
   }

   /**
    * We do this instead of a more traditional static block because the
    * ChipperPool was loading multiple instances of this class via a separate
   	* classloader.  This was causing GL to get initialized multiple times.
    * @return platform dependent library initializer
    * @throws Exception
    */
   public static LibraryInitializer createGCLibraryInitializer() throws Exception {
      LibraryInitializer result;
      if (Platform.get() == Platform.LINUX) {
          result = new EGLGraphicsContext.LibraryInitializer();
      } else {
          result = new GLFWGraphicsContext.LibraryInitializer();
      }
      return result;
   }

   public abstract void init();

   public abstract void makeCurrent();

   public abstract void swapBuffers();

   @Override
   public abstract void close() throws IOException;

   public static abstract class LibraryInitializer {
      public void initializeGL() {
         GL.create();
      }
   }
   public abstract ELTDisplayContext getELTDisplayContext();

}
