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
package com.leidoslabs.holeshot.imaging.cache;

import java.awt.*;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.util.Comparator;

/**
 * Provides common interface for implementing a tile cache of RenderedImages
 */
public interface TileCache {
  void add(RenderedImage owner, int tileX, int tileY, Raster data);

  void add(RenderedImage owner, int tileX, int tileY, Raster data, Object tileCacheMetric);

  void remove(RenderedImage owner, int tileX, int tileY);

  Raster getTile(RenderedImage owner, int tileX, int tileY);

  Raster[] getTiles(RenderedImage owner);

  void removeTiles(RenderedImage owner);

  void addTiles(RenderedImage owner, Point[] tileIndices, Raster[] tiles, Object tileCacheMetric);

  Raster[] getTiles(RenderedImage owner, Point[] tileIndices);

  void flush();

  void memoryControl();

  void setMemoryCapacity(long memoryCapacity);

  long getMemoryCapacity();

  void setMemoryThreshold(float memoryThreshold);

  float getMemoryThreshold();

  void setTileComparator(Comparator comparator);

  Comparator getTileComparator();
}

