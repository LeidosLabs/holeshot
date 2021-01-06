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

import java.io.Closeable;
import java.io.IOException;
import java.util.HashMap;
import java.util.concurrent.ExecutionException;

import org.image.common.util.CloseableUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.leidoslabs.holeshot.elt.gpuimage.ShaderProgram;
import com.leidoslabs.holeshot.elt.gpuimage.TextureTracker;

/**
 * The DisplayContext for the ELT.
 * Ensures that actions executed are syncd with SWT and OpenGL resources
 */
public abstract class ELTDisplayContext implements Closeable {
	private static final Logger LOGGER = LoggerFactory.getLogger(ELTDisplayContext.class);
	private Thread contextThread;
	private long contextHandle;
	private TextureTracker textureTracker;

	private HashMap<String, ShaderProgram> shaders;

	protected ELTDisplayContext() throws InterruptedException, ExecutionException {
		this(-1L);
	}
	protected ELTDisplayContext(long contextHandle) throws InterruptedException, ExecutionException {
		setContextHandle(contextHandle);
		shaders = new HashMap<>();
	}

	public synchronized ShaderProgram getShader(String shaderKey, Class<?> clazz, String vectorShader, String fragmentShader) throws IOException {
		ShaderProgram shader = shaders.get(shaderKey);
		if (shader == null) {
			shader = new ShaderProgram(clazz, vectorShader, fragmentShader);
			shaders.put(shaderKey, shader);
		}
		return shader;
	}

	public void setContextHandle(long handle) {
		this.contextHandle = handle;
	}

	public synchronized TextureTracker getTextureTracker() throws InterruptedException, ExecutionException {
		if (textureTracker == null) {
			syncExec(()-> this.textureTracker = new TextureTracker());
		}
		return textureTracker;
	}
	public long getContextHandle() {
		return this.contextHandle;
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

	@Override
	public void close() throws IOException {
		shaders.values().stream().forEach(s-> {
			try {
				CloseableUtils.close(s);
			} catch (IOException e) {
				LOGGER.error("Couldn't close shader", e);
			}
		});
	}

}
