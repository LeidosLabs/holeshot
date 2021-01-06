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

package com.leidoslabs.holeshot.elt.analytics;

import java.lang.Runnable;
import java.lang.Thread;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class CacheListClient {
	private static final Logger LOGGER = LoggerFactory.getLogger(CacheListClient.class);
	private ScheduledExecutorService exec = Executors.newSingleThreadScheduledExecutor();
    private final CacheListTask uploader;
	private Thread errorHandlerThread;
	
	private final long intervalSec;

	public CacheListClient(long intervalSec) {
		this.exec = Executors.newSingleThreadScheduledExecutor();
		this.uploader = new CacheListTask(intervalSec);
		this.intervalSec = intervalSec; 
	}

	public void start() {
        ScheduledFuture<?> handle = exec.scheduleAtFixedRate(this.uploader, intervalSec, intervalSec, TimeUnit.SECONDS);
		this.errorHandlerThread = new Thread(new CacheListErrorHandler(this.exec, handle));
		this.errorHandlerThread.start();
	}
	
	public void stop() {
		this.errorHandlerThread.interrupt();
		// we safely race with with error thread to shutdown executor to guarantee its shutdown when stop() returns
		synchronized (this.exec) {
			if (!exec.isShutdown()) {
				exec.shutdownNow();
			}
		}
		
	}


	private static class CacheListErrorHandler implements Runnable {
		private ScheduledExecutorService exec;
		private final ScheduledFuture<?> handle;

		public CacheListErrorHandler(ScheduledExecutorService exec, ScheduledFuture<?> handle) {
			this.handle = handle;
			this.exec = exec;
		}

		@Override
		public void run() {
			try {
				this.handle.get();
			} catch (ExecutionException | InterruptedException e) {
				LOGGER.error("An error occured during telemetry upload. Stopping telemetry service");
				e.printStackTrace();
				synchronized (this.exec) {
					if (!exec.isShutdown()) {
						exec.shutdownNow();
					}
				}
			} 
		}
	}
}