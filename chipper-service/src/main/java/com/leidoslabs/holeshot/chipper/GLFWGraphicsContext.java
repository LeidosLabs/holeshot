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

import static org.lwjgl.glfw.GLFW.GLFW_FALSE;
import static org.lwjgl.glfw.GLFW.GLFW_VISIBLE;
import static org.lwjgl.glfw.GLFW.glfwCreateWindow;
import static org.lwjgl.glfw.GLFW.glfwDestroyWindow;
import static org.lwjgl.glfw.GLFW.glfwInit;
import static org.lwjgl.glfw.GLFW.glfwMakeContextCurrent;
import static org.lwjgl.glfw.GLFW.glfwSwapBuffers;
import static org.lwjgl.glfw.GLFW.glfwWindowHint;
import static org.lwjgl.system.MemoryUtil.NULL;

import java.io.Closeable;
import java.io.IOException;
import java.io.PrintStream;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.image.common.util.CloseableUtils;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.opengl.GL;
import org.slf4j.Logger;
import org.usefultoys.slf4j.LoggerFactory;

import com.leidoslabs.holeshot.elt.ELTDisplayContext;


/**
 * GLFW Graphics context
 */
class GLFWGraphicsContext extends GraphicsContext {
	private static final Logger LOGGER = LoggerFactory.getLogger(GLFWGraphicsContext.class);
	private final long window;
	private final GLFWDisplayContext eltDisplayContext;


	/**
	 * Constructor, initializes display executor and creates window
	 * @throws ExecutionException 
	 * @throws InterruptedException 
	 */
	public GLFWGraphicsContext() throws InterruptedException, ExecutionException {
		glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE);
		this.window = glfwCreateWindow(5, 5, "glfwGC", NULL, NULL);
		this.eltDisplayContext = new GLFWDisplayContext();
	}

	@Override
	public ELTDisplayContext getELTDisplayContext() {
		return eltDisplayContext;
	}

	@Override
	public void init() {
	}

	@Override
	/**
	 * Make context current
	 */
	public void makeCurrent() {
	}

	@Override
	public void swapBuffers() {
		glfwSwapBuffers(window);
	}

	@Override
	public void close() throws IOException {
		CloseableUtils.close(eltDisplayContext);
		glfwDestroyWindow(window);
	}

	private class GLFWDisplayContext extends ELTDisplayContext implements Closeable {
		private final ExecutorService displayExecutor;
		private long displayExecutorThread;

		public GLFWDisplayContext() throws InterruptedException, ExecutionException {
			super(window);
			displayExecutor = Executors.newFixedThreadPool(1);
			displayExecutor.execute(()-> {
				displayExecutorThread = Thread.currentThread().getId();
				glfwMakeContextCurrent(window);
				GL.createCapabilities();
			});
		}

		private boolean isDisplayExecutorThread() {
			return Thread.currentThread().getId() == displayExecutorThread;
		}
		@Override
		public void asyncExec(Runnable runnable) throws InterruptedException, ExecutionException {
			displayExecutor.submit(runnable);
		}

		@Override
		public void syncExec(Runnable runnable) throws InterruptedException, ExecutionException {
			if (isDisplayExecutorThread()) {
				runnable.run();
			} else {
				displayExecutor.submit(runnable).get();
			}
		}

		@Override
		protected void setOpenGLContextCurrent() {
		}

		@Override
		public void close() throws IOException {
			try {
				syncExec(()->glfwMakeContextCurrent(NULL));
			} catch (InterruptedException | ExecutionException e) {
				LOGGER.error("error shutting down context", e);
			}
			displayExecutor.shutdown();
		}
	}

	public static class LibraryInitializer extends GraphicsContext.LibraryInitializer {
		@Override
		public void initializeGL() {
			super.initializeGL();
			GLFWErrorCallback.createPrint(new PrintStream(LoggerFactory.getErrorOutputStream(LOGGER))).set();
			glfwInit();
		}
	}
}
