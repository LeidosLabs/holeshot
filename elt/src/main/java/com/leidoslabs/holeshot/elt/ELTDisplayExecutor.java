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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Executor that ensures that a given ELT task is performed in the appropriate context
 */
public class ELTDisplayExecutor {
   private static final Logger LOGGER = LoggerFactory.getLogger(ELTDisplayExecutor.class);

   public enum ExecMode { ASYNCHRONOUS, SYNCHRONOUS };

   private ExecutorService executor;
   private final AtomicInteger tasksOutstanding;
   private final ELTDisplayContext displayContext;

   /**
    * Construct ELTDisplayExecutor given a displayContext, and set executor to have 
    * a thread pool with 1 thread
    * @param displayContext
    */
   public ELTDisplayExecutor(ELTDisplayContext displayContext) {
      this.displayContext = displayContext;
      this.tasksOutstanding = new AtomicInteger(0);
      executor = Executors.newFixedThreadPool(1);
   }

   public void shutdown() {
      executor.shutdown();
   }

   private void runNow(Runnable runnable) throws InterruptedException, ExecutionException {
      try {
         if (displayContext.setContextThread()) {
            displayContext.syncExec(runnable);
         } else {
            runnable.run();
         }
      } finally {
         tasksOutstanding.decrementAndGet();
      }
   }

   /**
    * Submit task, either await for completion or don't. 
    * @param execMode Wait for completion (ExecMode.SYNCHRONOUS) or don't (ExecMode.ASYNCHRONOUS)
    * @param runnable
    * @throws InterruptedException
    * @throws ExecutionException
    */
   public void submit(ExecMode execMode, Runnable runnable) throws InterruptedException, ExecutionException {
      if (executor.isShutdown()) {
         LOGGER.warn("Task submitted after shutdown. Discarding task", new Throwable());
      } else {
         tasksOutstanding.incrementAndGet();
         Future<?> future = executor.submit(() -> {
            try {
               runNow(runnable);
            } catch (InterruptedException | ExecutionException e) {
               e.printStackTrace();
            }
         });

         if (execMode == ExecMode.SYNCHRONOUS) {
            future.get();
         }
      }
   }



}
