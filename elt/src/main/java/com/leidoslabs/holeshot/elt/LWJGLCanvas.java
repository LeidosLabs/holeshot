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

package com.leidoslabs.holeshot.elt;

import static org.lwjgl.opengl.GL11.glGetError;
import static org.lwjgl.opengl.WGL.wglCreateContext;
import static org.lwjgl.opengl.WGL.wglDeleteContext;
import static org.lwjgl.opengl.WGL.wglMakeCurrent;
import static org.lwjgl.system.jawt.JAWTFunctions.JAWT_DrawingSurface_FreeDrawingSurfaceInfo;
import static org.lwjgl.system.jawt.JAWTFunctions.JAWT_DrawingSurface_GetDrawingSurfaceInfo;
import static org.lwjgl.system.jawt.JAWTFunctions.JAWT_DrawingSurface_Lock;
import static org.lwjgl.system.jawt.JAWTFunctions.JAWT_DrawingSurface_Unlock;
import static org.lwjgl.system.jawt.JAWTFunctions.JAWT_FreeDrawingSurface;
import static org.lwjgl.system.jawt.JAWTFunctions.JAWT_GetAWT;
import static org.lwjgl.system.jawt.JAWTFunctions.JAWT_GetDrawingSurface;
import static org.lwjgl.system.jawt.JAWTFunctions.JAWT_LOCK_ERROR;
import static org.lwjgl.system.jawt.JAWTFunctions.JAWT_VERSION_1_4;
import static org.lwjgl.system.windows.GDI32.ChoosePixelFormat;
import static org.lwjgl.system.windows.GDI32.DescribePixelFormat;
import static org.lwjgl.system.windows.GDI32.GetPixelFormat;
import static org.lwjgl.system.windows.GDI32.PFD_DOUBLEBUFFER;
import static org.lwjgl.system.windows.GDI32.PFD_DRAW_TO_WINDOW;
import static org.lwjgl.system.windows.GDI32.PFD_MAIN_PLANE;
import static org.lwjgl.system.windows.GDI32.PFD_SUPPORT_OPENGL;
import static org.lwjgl.system.windows.GDI32.PFD_TYPE_RGBA;
import static org.lwjgl.system.windows.GDI32.SetPixelFormat;
import static org.lwjgl.system.windows.GDI32.SwapBuffers;

import java.awt.Canvas;
import java.awt.Graphics;

import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GLCapabilities;
import org.lwjgl.system.jawt.JAWT;
import org.lwjgl.system.jawt.JAWTDrawingSurface;
import org.lwjgl.system.jawt.JAWTDrawingSurfaceInfo;
import org.lwjgl.system.jawt.JAWTWin32DrawingSurfaceInfo;
import org.lwjgl.system.windows.PIXELFORMATDESCRIPTOR;
import org.lwjgl.system.windows.WinBase;

/**
 * A Canvas component that uses OpenGL for rendering.
 *
 * <p>This implementation supports Windows only and is no way complete or robust enough for production use.</p>
 * Currently unused
 */
@SuppressWarnings("serial")
public abstract class LWJGLCanvas extends Canvas {
   private static final long NULL = 0L;

   private final JAWT awt;

   //    private final AbstractGears gears;

   private long hglrc;

   private GLCapabilities caps;
   private long lastTime;

   protected long getHglrc() {
      return hglrc;
   }

   public LWJGLCanvas() {
      lastTime = 0;
      hglrc = NULL;
      awt = JAWT.calloc();
      awt.version(JAWT_VERSION_1_4);
      if (!JAWT_GetAWT(awt)) {
         throw new IllegalStateException("GetAWT failed");
      }
      //        gears = new AbstractGears();
   }

   @Override
   public void update(Graphics g) {
      paint(g);
   }

   @Override
   public void paint(Graphics g) {
      // Limit to 60 FPS
      if (sync(60)) {
         // Get the drawing surface
         JAWTDrawingSurface ds = JAWT_GetDrawingSurface(this, awt.GetDrawingSurface());
         if (ds == null) {
            throw new IllegalStateException("awt->GetDrawingSurface() failed");
         }

         try {
            // Lock the drawing surface
            int lock = JAWT_DrawingSurface_Lock(ds, ds.Lock());
            if ((lock & JAWT_LOCK_ERROR) != 0) {
               throw new IllegalStateException("ds->Lock() failed");
            }

            try {
               // Get the drawing surface info
               JAWTDrawingSurfaceInfo dsi = JAWT_DrawingSurface_GetDrawingSurfaceInfo(ds, ds.GetDrawingSurfaceInfo());
               if (dsi == null) {
                  throw new IllegalStateException("ds->GetDrawingSurfaceInfo() failed");
               }

               try {
                  // Get the platform-specific drawing info
                  JAWTWin32DrawingSurfaceInfo dsi_win = JAWTWin32DrawingSurfaceInfo.create(dsi.platformInfo());
                  long                        hdc     = dsi_win.hdc();
                  if (hdc != NULL) {
                     if (hglrc == NULL) {
                        createContext(dsi_win);
                        //                            gears.initGLState();
                        initGLState();
                     } else {
                        if (!wglMakeCurrent(hdc, hglrc)) {
                           throw new IllegalStateException("wglMakeCurrent() failed");
                        }
                        GL.setCapabilities(caps);
                     }

                     sync(60);
                     renderLoop();
                     SwapBuffers(hdc);

                     wglMakeCurrent(NULL, NULL);
                     GL.setCapabilities(null);
                  }
               } finally {
                  // Free the drawing surface info
                  JAWT_DrawingSurface_FreeDrawingSurfaceInfo(dsi, ds.FreeDrawingSurfaceInfo());
               }
            } finally {
               // Unlock the drawing surface
               JAWT_DrawingSurface_Unlock(ds, ds.Unlock());
            }
         } finally {
            // Free the drawing surface
            JAWT_FreeDrawingSurface(ds, awt.FreeDrawingSurface());
         }

         repaint();
      }
   }

   protected abstract void renderLoop();

   protected abstract void initGLState();

   protected abstract int getBitsPerPixel();
   protected abstract int getNumBands();

   // Simplest possible context creation.
   private void createContext(JAWTWin32DrawingSurfaceInfo dsi_win) {
      long hdc = dsi_win.hdc();

      try (
            PIXELFORMATDESCRIPTOR pfd = PIXELFORMATDESCRIPTOR.calloc()
            .nSize((byte)PIXELFORMATDESCRIPTOR.SIZEOF)
            .nVersion((short)1)
            .dwFlags(PFD_SUPPORT_OPENGL | PFD_DRAW_TO_WINDOW | PFD_DOUBLEBUFFER)
            .iPixelType(PFD_TYPE_RGBA)
            .cAccumBits((byte)getBitsPerPixel())
            .cColorBits((byte)(getBitsPerPixel() * getNumBands()))
            .cAlphaBits((byte)getBitsPerPixel())
            .cDepthBits((byte)24)
            .iLayerType(PFD_MAIN_PLANE)) {
         int pixelFormat = GetPixelFormat(hdc);
         if (pixelFormat != 0) {
            if (DescribePixelFormat(hdc, pixelFormat, pfd) == 0) {
               throw new IllegalStateException("DescribePixelFormat() failed: " + WinBase.getLastError());
            }
         } else {
            pixelFormat = ChoosePixelFormat(hdc, pfd);
            if (pixelFormat < 1) {
               throw new IllegalStateException("ChoosePixelFormat() failed: " + WinBase.getLastError());
            }

            if (!SetPixelFormat(hdc, pixelFormat, null)) {
               throw new IllegalStateException("SetPixelFormat() failed: " + WinBase.getLastError());
            }
         }
      }

      hglrc = wglCreateContext(hdc);

      if (hglrc == NULL) {
         throw new IllegalStateException("wglCreateContext() failed");
      }

      if (!wglMakeCurrent(hdc, hglrc)) {
         throw new IllegalStateException("wglMakeCurrent() failed");
      }

      caps = GL.createCapabilities();

   }

   public void destroy() {
      awt.free();

      if (hglrc != NULL) {
         wglDeleteContext(hglrc);
      }
   }

   /**
    * An accurate sync method that adapts automatically
    * to the system it runs on to provide reliable results.
    *
    * @param fps The desired frame rate, in frames per second
    * @author kappa (On the LWJGL Forums)
    */
   private boolean sync(int fps) {
      boolean result = false;

      long sleepTime = 1000000000 / fps; // nanoseconds to sleep this frame
      long now = System.nanoTime();
      
      result = ((lastTime + now) >= sleepTime);
      
      if (result) {
         lastTime = now;
      }
      
      return result;
   }



}