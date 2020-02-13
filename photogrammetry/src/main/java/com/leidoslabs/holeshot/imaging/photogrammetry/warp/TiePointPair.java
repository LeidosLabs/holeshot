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

package com.leidoslabs.holeshot.imaging.photogrammetry.warp;

import java.awt.geom.Point2D;

/**
 * This class encapsulates the data that form a tie point pair.
 * This is the source image coordinate (x1, y1) in one image and
 * the source image coordinate (x2, y2) of the same point on the ground
 * in a second image.
 */
public class TiePointPair
{
  private Point2D myPointInFirstImage;
  private Point2D myPointInSecondImage;


    /**
     * Create a tie point pair between points representing the same ground coordinate in two different images
     * @param pointInFirstImage The image coordinate of the point in the first image
     * @param pointInSecondImage The image coordinate of the point in the second image
     */
  public TiePointPair(Point2D pointInFirstImage,
                      Point2D pointInSecondImage)
  {
    myPointInFirstImage  = pointInFirstImage;
    myPointInSecondImage = pointInSecondImage;
  }

    /**
     * Get the image coordinate of the point in the first image
     * @return The point as an image coordinate
     */
  public Point2D getPointInFirstImage()
  {
    return myPointInFirstImage;
  }

    /**
     * Get the image coordinate of the point in the second image
     * @return The point as an image coordinate
     */
  public Point2D getPointInSecondImage()
  {
    return myPointInSecondImage;
  }

    /**
     * @return text description of this tie point pair
     */
  public String toString()
  {
    return this.getClass().getName()+":\n  p1 = "+myPointInFirstImage+"\n  p2 = "+myPointInSecondImage;
  }

}
