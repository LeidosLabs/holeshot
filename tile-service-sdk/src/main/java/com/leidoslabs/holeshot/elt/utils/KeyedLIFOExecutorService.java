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
package com.leidoslabs.holeshot.elt.utils;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.RunnableFuture;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * An executor service that executes tasks on a LIFO basis, typically based on system time
 */
public class KeyedLIFOExecutorService extends ThreadPoolExecutor {
	private ConcurrentHashMap<String, KeyedFutureTask<?>> tasksInProgress;

	/**
	 * A Comparable future task with priority
	 * @param <T> Return type of runnable task
	 */
	private static class KeyedFutureTask<T> extends FutureTask<T> implements Comparable<KeyedFutureTask<T>> {
		private long priority = 0;
		private String key;
		private Future<?> future;


		public KeyedFutureTask(Runnable runnable, T result, long priority, String key) {
			super(runnable, result);
			this.priority = priority;
			this.key = key;
		}
		public KeyedFutureTask(Callable<T> callable, long priority, String key) {
			super(callable);
			this.priority = priority;
			this.key = key;
		}
		public void setPriority(long priority) {
			this.priority = priority;
		}
		public String getKey() {
			return key;
		}

		@Override
		public synchronized void run() {
			super.run();
		}

		@Override
		public int compareTo(KeyedFutureTask<T> o) {
			return Long.valueOf(o.priority).compareTo(priority);
		}
		public Future<?> getFuture() {
			return future;
		}
		public void setFuture(Future<?> future) {
			this.future = future;
		}
	}

	@Override
	protected void beforeExecute(Thread t, Runnable r) {
		// TODO Auto-generated method stub
		super.beforeExecute(t, r);
		KeyedFutureTask newTask = (KeyedFutureTask)r;
	}
	@Override
	protected void afterExecute(Runnable r, Throwable t) {
		super.afterExecute(r, t);
		KeyedFutureTask newTask = (KeyedFutureTask)r;
		tasksInProgress.remove(newTask.key);
	}

	private KeyedLIFOExecutorService(int corePoolSize, int maximumPoolSize,
			long keepAliveTime, TimeUnit unit, BlockingQueue<Runnable> workQueue) {
		super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue);

		this.tasksInProgress = new ConcurrentHashMap<String, KeyedFutureTask<?>>();
	}
	/**
	 * Utility method to create thread pool easily
	 * @param nThreads
	 * @return
	 */
	public static KeyedLIFOExecutorService newFixedThreadPool(int nThreads) {
		return new KeyedLIFOExecutorService(nThreads, nThreads, Long.MAX_VALUE,
				TimeUnit.MILLISECONDS, new PriorityBlockingQueue<Runnable>());
	}
	@Override
	protected <T> RunnableFuture<T> newTaskFor(Callable<T> callable) {
		return (RunnableFuture<T>) callable;
	}

	@Override
	protected <T> RunnableFuture<T> newTaskFor(Runnable runnable, T value) {
		return (RunnableFuture<T>) runnable;
	}

	public boolean isInProcess(String key) {
		return tasksInProgress.containsKey(key); 
	}

	/**
	 * Submit with New comparable task
	 */
	public synchronized Future<?> submit(Runnable task) {
		if (!(task instanceof KeyedFutureTask)) {
			throw new IllegalArgumentException(String.format("%s can only receive tasks of type %s, not %s", KeyedLIFOExecutorService.class.getName(), KeyedFutureTask.class.getName(), task.getClass().getName()));
		}
		KeyedFutureTask newTask = (KeyedFutureTask)task;
		final String key = newTask.getKey();
		KeyedFutureTask existingTask = tasksInProgress.get(key);
		Future<?> result = null;
		if (existingTask == null) {
			tasksInProgress.put(key, newTask);
			result = super.submit(newTask);
			newTask.setFuture(result);
		} else {
			result = existingTask.getFuture();
			if (result != null && !result.isCancelled() && !result.isDone()) {
				if (newTask.priority <= existingTask.priority) {
					remove(existingTask);
					existingTask.setPriority(newTask.priority);
					result = super.submit(existingTask);
					existingTask.setFuture(result);
				}
			} 
		}
		return result;
	}
	/**
	 * execute with New comparable task
	 */
	public void execute(Runnable command) {
		super.execute(command);
	}

	/**
	 * Submit with New comparable task
	 * @param key
	 * @param task
	 * @return
	 */
	public Future<?> submit(String key, Runnable task) {
		return this.submit(new KeyedFutureTask(task, null, System.currentTimeMillis(), key));
	}


	public Future<?> submit(String key, Runnable task, long priority){
		return this.submit(new KeyedFutureTask(task, null, priority, key));
	}

	/**
	 * execute with New comparable task
	 * @param key
	 * @param command
	 */
	public void execute(String key, Runnable command) {
		this.execute(new KeyedFutureTask(command, null, System.currentTimeMillis(), key));
	}
}
