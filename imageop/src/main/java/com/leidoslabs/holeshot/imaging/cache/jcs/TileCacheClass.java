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

import java.awt.Point;
import java.awt.Transparency;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.ComponentColorModel;
import java.awt.image.DataBuffer;

import java.awt.image.Raster;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.regex.Pattern;

import javax.imageio.ImageIO;

import org.apache.commons.jcs.JCS;
import org.apache.commons.jcs.access.CacheAccess;

/**
 * TileCache implementation, using CacheAccess to store Rasters in memory,
 * byte arrays on disk.
 */
public class TileCacheClass implements TileCache {

	private static CacheAccess<String, Raster> memCache;		// memCache holds Rasters
	private static CacheAccess<String, byte[]> diskCache;		// diskCache holds byte arrays (convert between these and Rasters)

	
	/**
	 *  TileCache constructor - get memory and disk cache instances
	 */
	public TileCacheClass() {
		memCache = JCS.getInstance("memRegion");		
		diskCache = JCS.getInstance("diskRegion");		 
	}

	
	/**
	 *  Given owner, tileX, tileY, data --> makes key, stores data
	 *  Depending on if there's room in memCache or not, add the Raster with/without converting to byte array
	 */
	public void add(ImageFile owner, int tileX, int tileY, Raster data) {
		TileKey key = new TileKey(owner, tileX, tileY);
		// Check that data is not in memCache and not in diskCache
		if (memCache.get(key.toString()) == null && diskCache.get(key.toString()) == null) {
			// Check if memCache has reached capacity --> if it hasn't, add Raster to memCache
			if(!this.isMemFull()) {
				memCache.put(key.toString(), data);
			}
			// Otherwise (because memCache is full), add byte array (converted from Raster) to diskCache
			else {
				// Encode the tile (Raster data) as a PNG image
				ColorModel colorModel = new ComponentColorModel(ColorModel.getRGBdefault().getColorSpace(), false, true,
						Transparency.OPAQUE, DataBuffer.TYPE_BYTE);
				ByteArrayOutputStream encodedImage = new ByteArrayOutputStream();
				BufferedImage image = new BufferedImage(colorModel,Raster.createWritableRaster(data.getSampleModel(), data.getDataBuffer(), new Point(0,0)),colorModel.isAlphaPremultiplied(),null);
				try {
					ImageIO.write(image,"png", encodedImage);
				} catch (IOException e) {
					e.printStackTrace();
				}
				byte[] bytes = encodedImage.toByteArray();
				
				diskCache.put(key.toString(), bytes);
			}
		}
	}

	
	@Override
	/**
	 *  Remove a tile given owner, tileX, tileY (forms key)
	 */
	public void remove(ImageFile owner, int tileX, int tileY) {
		TileKey key = new TileKey(owner, tileX, tileY);
		if (memCache.get(key.toString()) != null) {
			memCache.remove(key.toString());
		}
		if(diskCache.get(key.toString()) != null) {
			diskCache.remove(key.toString());
		}
	}

	
	@Override
	/**
	 *  Gets byte[] or Raster from cache, converts into Raster if needed
	 */
	public Raster getTile(ImageFile owner, int tileX, int tileY) {
		TileKey key = new TileKey(owner, tileX, tileY);
		// First, check if it's in memory --> if so, retrieve from memCache
		if (memCache.get(key.toString()) != null) {
			Raster rast = memCache.get(key.toString());
			return rast;
		} 
		// Otherwise, check if it's on disk --> if so, retrieve from diskCache
		else if (diskCache.get(key.toString()) != null){
			byte[] arr = diskCache.get(key.toString());
			Raster rast = convertByteArr(owner, arr);
			return rast;
		}
		else {
			return null;
		}
	}

	
	@Override
	/**
	 *  Gets byte arrays/Rasters from cache, converts as needed and stores in Raster array
	 */
	public Raster[] getTiles(ImageFile owner) {
		// This pattern allows any String that starts w/ the owner's filename
		String pat = "^" + Pattern.quote(owner.getFilename()) + ".*";

		// c and c1 hold the overall matching objects for the given owner
		Collection<Raster> c = memCache.getMatching(pat).values();
		Collection<byte[]> c1 = diskCache.getMatching(pat).values();
		
		// Combine c and c1 contents into Raster array to return
		Raster[] result = new Raster[c.size() + c1.size()];
		int count = 0;
		for (Raster rast : c) {
			result[count] = rast;
			count++;
		}
		
		for(byte[] byteArr : c1) {
			result[count] = convertByteArr(owner, byteArr);
			count++;
		}
		return result;
	}

	
	@Override
	/**
	 *  Remove multiple tiles
	 */
	public void removeTiles(ImageFile owner) {
		memCache.remove(owner.getFilename() + ":");
		diskCache.remove(owner.getFilename() + ":");
	}
	
	
	/**
	 * @return  If memCache has reached the max # of objects, return true
	 */
	public boolean isMemFull() {
		int memCacheSize = memCache.getCacheControl().getMemoryCache().getSize();
		int memMaxObj = memCache.getCacheAttributes().getMaxObjects();
		
		return (memCacheSize >= memMaxObj);
	}
	
	
	/**
	 * Convert byte[] into Raster - for when we return Raster for getTile(s)
	 * @param owner
	 * @param arr
	 * @return
	 */
	public Raster convertByteArr(ImageFile owner, byte[] arr) {
		ByteArrayInputStream bais = new ByteArrayInputStream(arr);
		BufferedImage image = null;
		Raster rast = null;
		try {
			image = ImageIO.read(bais);
			rast = image.getData();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return rast;
	}

}
