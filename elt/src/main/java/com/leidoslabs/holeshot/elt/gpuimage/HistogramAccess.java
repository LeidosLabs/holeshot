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

import java.awt.Dimension;

import org.joml.Vector2d;
import org.joml.Vector2dc;
import org.joml.Vector2i;
import org.joml.Vector2ic;

/**
 * Class for storing/retrieving histogram info, and for point conversions
 */
public class HistogramAccess {

  private final Dimension histogramSize;
  private final int buckets;
  private final int maxPixel;
  private final Vector2dc uniformPixel;
  private final Vector2dc uniformHalfPixel;
  private final Vector2dc texturePixel;
  private final Vector2dc textureHalfPixel;
  private final int pixelsPerBucket;

  public HistogramAccess(Dimension histogramSize, int buckets, int maxPixel) {
    this.histogramSize = histogramSize;
    this.buckets = buckets;
    this.maxPixel = maxPixel;
    this.uniformPixel = new Vector2d(2.0/histogramSize.getWidth(), 2.0/histogramSize.getHeight());
    this.uniformHalfPixel = new Vector2d(this.uniformPixel).mul(0.5);
    this.texturePixel = new Vector2d(1.0/histogramSize.getWidth(), 1.0/histogramSize.getHeight());
    this.textureHalfPixel = new Vector2d(this.texturePixel).mul(0.5);
    this.pixelsPerBucket = maxPixel/buckets;
  }

  public int getFullIntensity(double uniformIntensity) {
    return (int)(uniformIntensity * maxPixel);
  }

  public int getTargetBucket(double uniformIntensity) {
    return getFullIntensity(uniformIntensity) / pixelsPerBucket;
  }

  public Vector2i getRowColPositionForIntensity(double uniformIntensity) {
    int targetBucket = getTargetBucket(uniformIntensity);
    int col = targetBucket % histogramSize.width;
    int row = targetBucket / histogramSize.width;
    return new Vector2i(col, row);
  }

  public Vector2dc getUniformPositionForRowCol(Vector2i rowColPosition) {
    return new Vector2d(rowColPosition).mul(uniformPixel).sub(1.0, 1.0).add(uniformHalfPixel); 
  }

  public Vector2dc getUniformPositionForIntensity(double uniformIntensity) {
    Vector2i rowColPosition = getRowColPositionForIntensity(uniformIntensity);
    return getUniformPositionForRowCol(rowColPosition);
  }

  public Vector2dc getTexturePositionFromUniform(Vector2dc uniformPosition) {
    return new Vector2d(uniformPosition).add(1.0,1.0).mul(0.5);
  }

  public Vector2dc getUniformPositionFromTexture(Vector2dc texturePosition) {
    return new Vector2d(texturePosition).mul(2.0).sub(1.0, 1.0);
  }

  public Vector2ic getRowColFromUniformPosition(Vector2dc uniformPosition) {
    Vector2dc texPosition = getTexturePositionFromUniform(uniformPosition);
    Vector2dc result = new Vector2d(texPosition).sub(textureHalfPixel).mul(1.0 / texturePixel.x(), 1.0/texturePixel.y());
    return new Vector2i((int)result.x(), (int)result.y());
  }

  public double getIntensityFromUniformPosition(Vector2dc uniformPosition) {
    Vector2ic rowCol = getRowColFromUniformPosition(uniformPosition);
    return ((double)(rowCol.y() * histogramSize.getWidth() + rowCol.x()))/(double)maxPixel;
  }

  public Vector2dc getTexturePositionForRowCol(Vector2i rowColPosition) {
    return getTexturePositionFromUniform(getUniformPositionForRowCol(rowColPosition));
  }

  public Dimension getHistogramSize() {
    return histogramSize;
  }

  public int getBuckets() {
    return buckets;
  }

  public int getMaxPixel() {
    return maxPixel;
  }

  public Vector2dc getUniformPixel() {
    return uniformPixel;
  }

  public Vector2dc getUniformHalfPixel() {
    return uniformHalfPixel;
  }

  public Vector2dc getTexturePixel() {
    return texturePixel;
  }

  public Vector2dc getTextureHalfPixel() {
    return textureHalfPixel;
  }

  public int getPixelsPerBucket() {
    return pixelsPerBucket;
  }
}
