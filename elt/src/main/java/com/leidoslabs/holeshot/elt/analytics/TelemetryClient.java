/*
 * Licensed to The Leidos Corporation under one or more contributor license agreements.
 * See the NOTICE file distributed with this work for additional information regarding copyright ownership.
 * The Leidos Corporation licenses this file to You under the Apache License, Version 2.0
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
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import com.leidoslabs.holeshot.elt.UserConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Telemetry Client for ELT. Schedules an UploadTask every PERIOD_SEC
 * which sends new log data to a firehose delivery stream over http
 */
public class TelemetryClient {

	private static final Logger LOGGER = LoggerFactory.getLogger(TelemetryClient.class);
	private static final boolean TELEMETRY_ENABLED = UserConfiguration.isTelemetryEnabled();
	private ScheduledExecutorService exec = Executors.newSingleThreadScheduledExecutor();
    private final TelemetryUploadTask uploader;
	private Thread errorHandlerThread;
	
	private final long interval_sec;

	public TelemetryClient(long interval) {
		
		this.exec = Executors.newSingleThreadScheduledExecutor();
		this.uploader = new TelemetryUploadTask();
		this.interval_sec = interval; 
	}

	public void start() {
	    if(!TELEMETRY_ENABLED) return;

        ScheduledFuture<?> handle = exec.scheduleAtFixedRate(this.uploader, interval_sec, interval_sec, TimeUnit.SECONDS);
		this.errorHandlerThread = new Thread(new TelemetryErrorHandler(this.exec, handle));
		this.errorHandlerThread.start();
	}
	
	public void stop() {
        if(!TELEMETRY_ENABLED) return;

		this.errorHandlerThread.interrupt();
		// we safely race with with error thread to shutdown executor to guarantee its shutdown when stop() returns
		synchronized (this.exec) {
			if (!exec.isShutdown()) {
				exec.shutdownNow();
			}
		}
		
	}


	private static class TelemetryErrorHandler implements Runnable {
		private final ScheduledFuture<?> handle;
		private ScheduledExecutorService exec;

		public TelemetryErrorHandler(ScheduledExecutorService exec, ScheduledFuture<?> handle) {
			this.handle = handle;
		}
		

		@Override
		public void run() {
			try {
				this.handle.get();
			} catch (ExecutionException | InterruptedException e) {
				if (e instanceof ExecutionException) {
					LOGGER.error("An error occured during telemetry upload. Stopping telemetry service" + e.getCause());
				}
				synchronized (exec) {
					if (!exec.isShutdown()) {
						exec.shutdownNow();
					}
				}
			} 
		}
	}
}
