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
package com.leidoslabs.holeshot.imaging.cache.jcs;

import java.io.Serializable;
//import java.awt.image.RenderedImage;

/**
 * holds coordinates (tileX, tileY) as a key to store image data in cache
 */
public class TileKey implements Serializable{
	/**
	 * 
	 */
	private static final long serialVersionUID = -3747944233052389936L;
	private ImageFile owner;
	public int tileX;
	public int tileY;

	public TileKey(ImageFile owner, int tileX, int tileY) {
		this.owner = owner;
		this.tileX = tileX;
		this.tileY = tileY;
	}
	
	public ImageFile getOwner() {
		return owner;
	}

	public void setOwner(ImageFile owner) {
		this.owner = owner;
	}

	
	public int getTileX() {
		return tileX;
	}

	public void setTileX(int tileX) {
		this.tileX = tileX;
	}

	public int getTileY() {
		return tileY;
	}

	public void setTileY(int tileY) {
		this.tileY = tileY;
	}

	/**
	 * @param key
	 * @return True if they belong to same ImageFIle, and tile x and y are equal
	 */
	public boolean equals(TileKey key) {
		if(key.getOwner().equals(this.owner) && key.getTileX() == this.tileX && key.getTileY() == this.tileY) {
			return true;
		}
		else {
			return false;
		}
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + tileX;
		result = prime * result + tileY;
		return result;
	}

	@Override
	public String toString() {
		return owner.getFilename() + ": (" + tileX + ", " + tileY + ")";
	}
	
}
