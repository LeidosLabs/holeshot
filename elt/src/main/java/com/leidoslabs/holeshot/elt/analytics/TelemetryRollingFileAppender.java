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

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.apache.log4j.RollingFileAppender;
import org.apache.log4j.spi.LoggingEvent;


/**
 * A custom RollingFileAppender used ELT.
 * Helps keep track of log rollovers for telemetry service
 */
public class TelemetryRollingFileAppender extends RollingFileAppender{
	private final ReadWriteLock readWriteLock = new ReentrantReadWriteLock();
    private final Lock readLock = readWriteLock.readLock(); 
    private final Lock writeLock = readWriteLock.writeLock();
    
	
	
	// keeps track of 2 most recent rollovers timestamps with recentRollOvers[0]
	// representing the most recent time (i.e recentRollOvers[0] > recentRollOvers[1])
	private final long[] recentRollOvers = new long[]{Long.MIN_VALUE + 1L, Long.MIN_VALUE};

	@Override
	public void rollOver() {
		this.recentRollOvers[1] = this.recentRollOvers[0];
		this.recentRollOvers[0] = System.currentTimeMillis();
		super.rollOver();
	}
	
	/**
	 * @param time Reference time
	 * @return Enum representing whether no rollovers, 1 rollover, or more than 1 rollover
	 * has occurred since a given timestamp
	 */
	public Rollover rolloversSince(long time) {
		Rollover rollovers;
		if (time < this.recentRollOvers[0]) {
			if (time < this.recentRollOvers[1]) { 
				// time came before the most second most recent rollover (i.e >= 2 rollovers have occurred since) 
				rollovers = Rollover.MULTI;
			} else rollovers = Rollover.ONE;
		} else rollovers = Rollover.NONE;
		
		return rollovers;
	}
	
    /**
     * @return Read lock from Telemetry ReadWriteLock is exposed to log readers
     */
	public Lock getReadLock() {
    	return readLock;
    }
	
	/**
	 * Acquire the write lock from our ReadWriteLock before appending
	 */
	@Override
	public void doAppend(LoggingEvent event) {
		this.writeLock.lock();
		try {
			super.doAppend(event);
		} finally {
			this.writeLock.unlock();
		}
	}
	
	/**
	 * Enum representing whether no rollovers, 1 rollover, or more than 1 rollover
	 * has occurred since a given timestamp
	 */
	public static enum Rollover {
		NONE, ONE, MULTI
	}

}
