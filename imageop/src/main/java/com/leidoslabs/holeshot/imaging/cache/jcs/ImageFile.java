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

/**
 * used to store image's filename (which is a String, used for part of the key in the cache) 
 * and width/height (helps with conversion between byte[] and Raster back)
 * maybe store the Rendered/BufferedImage, Raster here too?
 * maybe store image info (i.e. the int[] bandoffsets and other fields for converting between byte[] and Raster?
 */

public class ImageFile {

	private String filename;
	private int width;
	private int height;

	public ImageFile(String filename, int width, int height) {
		this.filename = filename;
		this.width = width;
		this.height = height;
	}

	public String getFilename() {
		return filename;
	}

	public void setFilename(String filename) {
		this.filename = filename;
	}

	public int getWidth() {
		return width;
	}

	public void setWidth(int width) {
		this.width = width;
	}

	public int getHeight() {
		return height;
	}

	public void setHeight(int height) {
		this.height = height;
	}

	public boolean equals(ImageFile image) {
		if(image.getFilename().equals(this.filename) && image.getWidth() == this.width && image.getHeight() == this.height) {
			return true;
		}
		else {
			return false;
		}
	}

}
