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

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;

import com.leidoslabs.holeshot.imaging.photogrammetry.CameraModel;

/**
 * A representation of a 2-dimensional point and its relation to a digital image
 * @param <C> The source coordinate containing type
 */
public abstract class GeointCoordinate<C> {
  private C sourceCoordinate;
  private ImageScale imageScale;
  private CameraModel cameraModel;

    /**
     * Create a GeointCoordinate, an extensible wrapper for providing conversions between coordinate systems
     * @param cameraModel The camera model of the image this coordinate is in
     * @param sourceCoordinate The raw coordinate in the implementing classes base coordinate system
     * @param imageScale The ImageScale object to track image scale and convert between different rSets
     */
  protected GeointCoordinate(CameraModel cameraModel, C sourceCoordinate, ImageScale imageScale) {
    this.sourceCoordinate = sourceCoordinate;
    this.imageScale = imageScale.copy();
    this.cameraModel = cameraModel;
  }

    /**
     * Get this coordinates equivalent Longitude - Latitude point on the earth surface
     * @return A Longitude - Latitude coordinate
     */
  public abstract Coordinate getGeodeticCoordinate();

    /**
     * Get this coordinates equivalent point in image coordinates of the original resolution image
     * @return A cartesian point representing this point in image space
     */
  public abstract Point2D getR0ImageCoordinate();

    /**
     * The camera model of the image in which this point resides
     * @return The image camera model
     */
  public CameraModel getCameraModel() {
     return cameraModel;
  }

    /**
     * The image coordinate of this point in the images current rSet
     * @return An image coordinate in the current scale of the image
     */
  public Point2D getRsetImageCoordinate() {
    return scaleDownToRset(getR0ImageCoordinate());
  }

    /**
     * Get the image scale
     * @return
     */
  public ImageScale getImageScale() {
     return imageScale;
  }

    /**
     * Find the given image coordinate on the full resolution image
     * @param source The source coordinate as a image coordinate at the current scale
     * @return The point on the full resolution image
     */
  protected Point2D scaleUpToR0(Point2D source) {
    return imageScale.scaleUpToR0(source);
  }

    /**
     * Find the given coordinate in the current image scale
     * @param source The source coordinate as an image coordinate
     * @return The image coordinate on the current rSet
     */
  protected Point2D scaleDownToRset(Point2D source) {
    return imageScale.scaleDownToRset(source);
  }

    /**
     * Get the source coordinate
     * @return This coordinate in its original coordinate object
     */
  public C getSourceCoordinate() {
    return this.sourceCoordinate;
  }

    /**
     * Get the envelope dimensions as an int array
     * @param envelope The envelope to convert to int array
     * @return [ minX, minY, width, height ]
     */
  protected static int[] getData(Envelope envelope) {
    return new int[] {(int) envelope.getMinX(), (int) envelope.getMinY(), (int) envelope.getWidth(),
        (int) envelope.getHeight()};
  }

    /**
     * Source Coordinate as a string
     * @return sourceCoordinate.toString
     */
  @Override
   public String toString() {
      return sourceCoordinate.toString();
   }
}
