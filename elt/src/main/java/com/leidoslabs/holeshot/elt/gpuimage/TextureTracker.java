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

package com.leidoslabs.holeshot.elt.gpuimage;

import static org.lwjgl.opengl.GL11.glGetIntegerv;
import static org.lwjgl.opengl.GL13.GL_TEXTURE0;
import static org.lwjgl.opengl.GL20.GL_MAX_COMBINED_TEXTURE_IMAGE_UNITS;

import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.commons.collections4.map.LRUMap;
import org.slf4j.Logger;
import org.usefultoys.slf4j.LoggerFactory;

/**
 * Class for tracking least recently used textures
 */
public class TextureTracker {
	private static final Logger LOGGER = LoggerFactory.getLogger(TextureTracker.class);
	public static final int MAX_TEXTURES = getMaxTextures();
	private static int RESERVED_TEXTURES = 10;

	private final LinkedList<TextureIDRef> idPool;
	
	public class TextureID {
		private ReentrantLock inUse;
		private boolean isDirty;
		private TextureIDRef id;
		
		private TextureID() {
			this.inUse = new ReentrantLock();
			this.isDirty = true;
		}
		
		public int getID() throws Exception {
			if (!inUse.isHeldByCurrentThread()) {
				throw new IllegalStateException("Can't get ID unless you have a lock on it");
			}
			if (isDirty) {
				id =  idPool.getLast();
				id.setDirty();
				id.setDirtyCallback(()->setDirty());
				isDirty = false;
			}
			use(id);
			return id.getID();
		}
		
		public void lock() {
			inUse.lock();
		}
		public void unlock() {
			inUse.unlock();
		}
		
		private void setDirty() {
			lock();
			try {
				isDirty = true;
			} finally {
				unlock();
			}
			
		}
		public boolean isDirty() {
			if (!inUse.isHeldByCurrentThread()) {
				throw new IllegalStateException("Can't check isDirty unless locked");
			}
			return isDirty;
		}
	}
	
	private class TextureIDRef {
		private int id;
		private Runnable dirtyCallback;
		
		public TextureIDRef(int id) { 
			this.id = id;
			this.dirtyCallback = null;
		}
		public int getID() {
			return id;
		}
		public void setDirtyCallback(Runnable dirtyCallback) {
			this.dirtyCallback = dirtyCallback;
		}
		public void setDirty() {
			if (dirtyCallback != null) {
				dirtyCallback.run();
				dirtyCallback = null;
			}
		}
	}
	
	public TextureTracker() {
		idPool = new LinkedList<>();
		for (int i=GL_TEXTURE0 + RESERVED_TEXTURES;i<GL_TEXTURE0 + MAX_TEXTURES;++i) {
			idPool.add(new TextureIDRef(i));
		}
	}
	
	public TextureID createTextureID() {
		return new TextureID();
	}
	
	private void use(TextureIDRef id) {
		idPool.remove(id);
		idPool.addFirst(id);
	}

	private static int getMaxTextures() {
		int[] buf = new int[1];
		glGetIntegerv(GL_MAX_COMBINED_TEXTURE_IMAGE_UNITS, buf);
		return buf[0];
	}
}
