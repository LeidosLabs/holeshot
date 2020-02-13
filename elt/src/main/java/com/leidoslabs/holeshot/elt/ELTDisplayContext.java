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

import java.util.concurrent.ExecutionException;

/**
 * The DisplayContext for the ELT.
 * Ensures that actions executed are syncd with SWT and OpenGL resources
 */
public abstract class ELTDisplayContext {
   private Thread contextThread;

   protected ELTDisplayContext() {
   }

   public abstract void asyncExec(Runnable runnable) throws InterruptedException, ExecutionException;
   public abstract void syncExec(Runnable runnable) throws InterruptedException, ExecutionException;

   protected abstract void setOpenGLContextCurrent();

   /**
    * Switches context to current thread if not already identical
    * @return did a context switch occur?
    */
   public synchronized boolean setContextThread() {
      final Thread currentThread = Thread.currentThread();
      final boolean switchContext = (contextThread != currentThread);
      this.contextThread = currentThread;
      setOpenGLContextCurrent();
      return switchContext;
   }

}
