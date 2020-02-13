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

import java.io.IOException;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.swt.GLCanvas;
import org.lwjgl.opengl.swt.GLData;

import com.leidoslabs.holeshot.elt.ELTDisplayContext;
import com.leidoslabs.holeshot.elt.ELTDisplayExecutor;

/**
 * Graphics context for windows platforms
 */
class WindowsGraphicsContext extends GraphicsContext {
   private GLCanvas context;
   private Shell shell;
   private Display display;
   private ELTDisplayExecutor eltDisplayExecutor;

   /**
    * Starts context thread, sets up canvas, window, etc.
    */
   public WindowsGraphicsContext() {
      Thread eltThread = new Thread(() -> {
         display = new Display();
         shell = new Shell(display, SWT.NO_TRIM);
         context = new GLCanvas(shell, SWT.DOUBLE_BUFFERED | SWT.NO_BACKGROUND, getGLData());
         context.setCurrent();
         GL.createCapabilities();
         context.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));
         context.setLayout(new GridLayout(1, false));
         shell.open();
         synchronized (WindowsGraphicsContext.this) {
            eltDisplayExecutor = new ELTDisplayExecutor(new DisplayContext());
            WindowsGraphicsContext.this.notify();
         }
         while (!display.isDisposed()) {
            if (!display.readAndDispatch()) {
               display.sleep();
            }
         }
      });
      eltThread.start();

      synchronized(this) {
         try {
            while (eltDisplayExecutor == null) {
               this.wait();
            }
         } catch (InterruptedException e) {
         }
      }
   }
   @Override
   public ELTDisplayExecutor getDisplayExecutor() {
      return eltDisplayExecutor;
   }
   private class DisplayContext extends ELTDisplayContext {
      @Override
      public void asyncExec(Runnable runnable) {
         display.asyncExec(runnable);
      }
      @Override
      protected void setOpenGLContextCurrent() {
      }
      @Override
      public void syncExec(Runnable runnable) {
         display.syncExec(runnable);
      }
      @Override
      public synchronized boolean setContextThread() {
         return super.setContextThread() || (Thread.currentThread() != display.getThread());
      }

   }

   private static GLData getGLData() {
      GLData data = new GLData();
      data.profile = GLData.Profile.CORE;
      data.majorVersion = 4;
      data.minorVersion = 0;
      data.samples = 0; // 4; 4x multisampling
      data.swapInterval = null; // for enabling v-sync (swapbuffers sync'ed to monitor refresh)
      data.doubleBuffer = true;
      data.depthSize = 32;

      return data;
   }

   @Override
   public void init() {
   }

   @Override
   public void makeCurrent() {
      context.setCurrent();
   }

   @Override
   public void swapBuffers() {
      context.swapBuffers();
   }
   @Override
   public void close() throws IOException {
      shell.close();
   }

}
