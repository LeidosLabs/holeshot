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
package com.leidoslabs.holeshot.imaging.coord;

import java.awt.geom.Point2D;

/**
 * ImageScale keeps track of scaling state and provides conversions to and from this state and the unscaled image
 */
public class ImageScale {
   private double scaleX;
   private double scaleY;

    /**
     * Create an initial 1:1 image scale
     */
   public ImageScale() {
      this(1.0, 1.0);
   }

    /**
     * Create an image scale initialized to given rSet level
     * @param rset The initial rSet to scale to
     * @return A new ImageScale object
     */
   public static ImageScale forRset(double rset) {
      final ImageScale imageScale = new ImageScale();
      imageScale.setRset(rset);
      return imageScale;
   }

    /**
     * Create an image scale with the given x and y scaling properties
     * @param scaleX The scaling in the x dimension
     * @param scaleY The scaling in the y dimension
     */
   public ImageScale(double scaleX, double scaleY) {
      this.setScale(scaleX, scaleY);
   }

    /**
     * Create an image scale with the same properties as the given image scale
     * @param scale The image scale to be copied
     */
   public ImageScale(ImageScale scale) {
      this.setTo(scale);
   }

    /**
     * Create an image scale that is a copy of this one
     * @return A new image scale with the same scale properties as this one
     */
   public ImageScale copy() {
      return new ImageScale(this);
   }

    /**
     * Set the state to the given scaling parameters
     * @param scaleX set scaling in the X dimension
     * @param scaleY set scaling in the Y direction
     */
   public void setScale(double scaleX, double scaleY) {
      this.scaleX = scaleX;
      this.scaleY = scaleY;
   }

    /**
     * @return the current scaling in the X direction
     */
   public double getScaleX() {
      return scaleX;
   }

    /**
     * @return the current scaling in the Y direction
     */
   public double getScaleY() {
      return scaleY;
   }

    /**
     * Updates the internal scale representation
     * scaleX = this.scaleX * scaleX
     * scaleY = this.scaleY * scaleY
     * @param scaleX the X scale multiplier
     * @param scaleY the Y scale multiplier
     * @return this object, for chaining
     */
   public ImageScale mul(double scaleX, double scaleY) {
      setScale(this.scaleX * scaleX, this.scaleY * scaleY);
      return this;
   }

    /**
     * Updates the internal scale representation, updates both x and y proportionally
     * scaleX = this.scaleX * scale
     * scaleY = this.scaleY * scale
     * @param scale the multiplier
     * @return this object, for chaining
     */
   public ImageScale mul(double scale) {
      return this.mul(scale, scale);
   }

    /**
     * Get the rSet for the given scale i.e.
     * getRsetForScale(1.0) = 0.0 (full resolution)
     * getRsetForScale(0.5) = 1.0 (half resolution is the first rSet)
     * The value may be between two actual rSets
     * @param scale The scale of the rSet
     * @return The rSet, may be between rSets
     */
   private static double getRsetForScale(double scale) {
      return Math.log(scale) / Math.log(0.5);
   }

    /**
     * Get the scaling of a given rSet or fractional rSet
     * 0.5 ^ rSet
     * getScaleForRset(2.0) = 0.25
     * @param rset the rSet, may be between integer rSets or negative
     * @return The scaling of the given rSet
     */
   private static double getScaleForRset(double rset) {
      return Math.pow(0.5, rset);
   }

    /**
     * Given a point in the current scaling regime (current rSet) get its location on the
     * full resolution, 1:1 scale image
     * @param point A valid point in image coordinates in the current scale
     * @return A point in the 1:1 image scale
     */
   public Point2D scaleUpToR0(Point2D point) {
      final double rsetFactorX = Math.pow(2.0, getRsetForScaleX());
      final double rsetFactorY = Math.pow(2.0, getRsetForScaleY());
      return new Point2D.Double(rsetFactorX * point.getX(), rsetFactorY * point.getY());
   }

    /**
     * Inverse of scaleUpToR0, given a point in R0, the 1:1 scaling, get its location in the current scaling regime
     * @param point A valid point in image coordinates in R0
     * @return The location of that point in the current scaled representation of the image
     */
   public Point2D scaleDownToRset(Point2D point) {
      final double rsetFactorX = Math.pow(0.5, getRsetForScaleX());
      final double rsetFactorY = Math.pow(0.5, getRsetForScaleY());
      return new Point2D.Double(rsetFactorX * point.getX(), rsetFactorY * point.getY());
   }

    /**
     * Get the rSet of the current X dimension scaling
     * @return a (possibly fractional) rSet
     */
   public double getRsetForScaleX() {
      return getRsetForScale(this.scaleX);
   }

    /**
     * Get the rSet of the current Y dimension scaling
     * @return a (possibly fractional) rSet
     */
   public double getRsetForScaleY() {
      return getRsetForScale(this.scaleY);
   }

    /**
     * Get the rSet corresponding to the current image scaling, bounded by maxRset. Useful for determining
     * which real rSet tiles should be displayed in the current scaling state
     * @param maxRset The maximum bound to be returned, for example, the highest rSet in a tile pyramid
     * @return an integer corresponding to a real rSet, not less than 0.0 and not higher than maxRset
     */
   public int getImageRset(int maxRset) {
      final double xRset = getRsetForScaleX();
      final double yRset = getRsetForScaleY();
      final int imageRset = (int)Math.min(Math.max(0.0, Math.min(xRset, yRset)), maxRset);
      return imageRset;
   }

    /**
     * Get the representative rSet for the current scaling. If the scaling is non-uniform, return the rSet for the
     * greater resolution dimension.
     * @return The rSet matching this scales highest scaled dimension
     */
   public double getRset() {
      return Math.min(getRsetForScaleX(), getRsetForScaleY());
   }

    /**
     * Set the scale state to that of the given rSet, for example 0.0 will set the scale back to 1:1
     * @param rset the rSet to scale the state to
     */
   public void setRset(double rset) {
      final double scaleForRset = getScaleForRset(rset);
      setScale(scaleForRset, scaleForRset);
   }

    /**
     * Set this imagescale to the same scaling state as the given one
     * @param imageScale The ImageScale to copy scaling state from
     */
   public void setTo(ImageScale imageScale) {
      setScale(imageScale.scaleX, imageScale.scaleY);
   }

    /**
     * @return Text representation of the current scaling state
     */
   @Override
   public String toString() {
      return String.format("ImageScale(scaleX = %f, scaleY = %f, rsetX = %f, rsetY = %f, imageRset = %d)", scaleX, scaleY, getRsetForScaleX(), getRsetForScaleY(), getImageRset(10));
   }

    /**
     * @param minScale ImageScale representing the lower bounds of the window
     * @param maxScale ImageScale representing the upper bounds of the window
     * @return True if this image scale falls between the given image scales in both X and Y dimensional scaling
     */
   public boolean within(ImageScale minScale, ImageScale maxScale) {
      return scaleX >= minScale.scaleX && scaleY >= minScale.scaleY &&
            scaleX <= maxScale.scaleX && scaleY <= maxScale.scaleY;
   }
}
