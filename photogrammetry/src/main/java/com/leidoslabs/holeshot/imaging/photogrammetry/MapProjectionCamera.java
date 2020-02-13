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
package com.leidoslabs.holeshot.imaging.photogrammetry;

import java.awt.geom.Point2D;

import org.apache.sis.geometry.DirectPosition2D;
import org.apache.sis.geometry.GeneralDirectPosition;
import org.apache.sis.referencing.CRS;
import org.apache.sis.referencing.operation.transform.DefaultMathTransformFactory;
import org.locationtech.jts.geom.Coordinate;
import org.opengis.geometry.MismatchedDimensionException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.NoninvertibleTransformException;
import org.opengis.referencing.operation.TransformException;
import org.opengis.util.FactoryException;

@SuppressWarnings("serial")
public class MapProjectionCamera extends CameraModel {
   private double defaultElevation;
   private final CoordinateReferenceSystem imageCRS;
   private final CoordinateReferenceSystem targetCRS;
   private final MathTransform imageToTarget;
   private final MathTransform targetToImage;
   private final MathTransform rasterToModel;
   private final MathTransform modelToRaster;

   public MapProjectionCamera(String rasterToModelWKT, String imageCRSWKT, String targetCRSWKT) throws FactoryException, NoninvertibleTransformException {
      this(new DefaultMathTransformFactory().createFromWKT(rasterToModelWKT), CRS.fromWKT(imageCRSWKT), CRS.fromWKT(targetCRSWKT));
   }

   public MapProjectionCamera(MathTransform rasterToModel, CoordinateReferenceSystem imageCRS, CoordinateReferenceSystem targetCRS) throws FactoryException, NoninvertibleTransformException {
      this.rasterToModel = rasterToModel;
      this.modelToRaster = rasterToModel.inverse();
      this.imageCRS = imageCRS;
      this.targetCRS = targetCRS;

      this.imageToTarget = CRS.findOperation(imageCRS, targetCRS, null).getMathTransform();
      this.targetToImage = CRS.findOperation(targetCRS, imageCRS, null).getMathTransform();
      this.defaultElevation = 0.0;
   }
   private static GeneralDirectPosition toPosition(CoordinateReferenceSystem crs, Coordinate coord) {
      final GeneralDirectPosition result = new GeneralDirectPosition(crs);
      result.setCoordinate(coord.x, coord.y, coord.z);
      return result;
   }
   private static DirectPosition2D toPosition(CoordinateReferenceSystem crs, Point2D point) {
      return new DirectPosition2D(crs, point.getX(), point.getY());
   }

   @Override
   public Point2D worldToImage(Coordinate worldPoint, Point2D imagePoint) {
      Point2D result = null;
      try {
         GeneralDirectPosition src = toPosition(targetCRS, worldPoint);
         DirectPosition2D modelDest = new DirectPosition2D(imageCRS);
         DirectPosition2D dest = new DirectPosition2D(imageCRS);

         targetToImage.transform(src, modelDest);
         modelToRaster.transform(modelDest, dest);

         imagePoint.setLocation(dest.getX(), dest.getY());
         result = imagePoint;
      } catch (MismatchedDimensionException | TransformException e) {
         e.printStackTrace();
      }
      return result;

   }

   @Override
   public Coordinate imageToWorld(Point2D imagePoint, Surface surf, Coordinate worldPoint) {
      Coordinate result = null;
      try {
         DirectPosition2D src = toPosition(imageCRS, imagePoint);
         DirectPosition2D modelSrc = new DirectPosition2D(imageCRS);
         DirectPosition2D dest = new DirectPosition2D(targetCRS);

         rasterToModel.transform(src, modelSrc);
         imageToTarget.transform(modelSrc, dest);

         worldPoint.x = dest.x;
         worldPoint.y = dest.y;
         worldPoint.z = (surf == null) ? worldPoint.z : surf.getElevation(imagePoint.getX(), imagePoint.getY());

         result = worldPoint;
      } catch (MismatchedDimensionException | TransformException e) {
         e.printStackTrace();
      }
      return result;
   }

   @Override
   public double getDefaultElevation() {
      return defaultElevation;
   }

   @Override
   public void setDefaultElevation(double elevation) {
      defaultElevation = elevation;
   }

   @Override
   public Coordinate getReferencePoint() {
      // assume 0,0 on the image will be in the image
      Point2D imagePoint = new Point2D.Double(0.0, 0.0);
      return imageToWorld(imagePoint);
   }

}
