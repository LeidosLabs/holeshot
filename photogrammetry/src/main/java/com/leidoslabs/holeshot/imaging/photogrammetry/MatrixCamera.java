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


/*
 *
 * $RCSfile: MatrixCamera.java,v $ $Revision: 1.12 $ $Date: 2007/04/25 16:24:51 $
 *
 * (C) Copyright 2001,2005 Lockheed Martin Corporation
 * All rights reserved
 * Lockheed Martin Proprietary data
 *
 */

import java.awt.geom.Point2D;
import java.text.DecimalFormat;
import java.text.NumberFormat;

import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealMatrixFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.locationtech.jts.geom.Coordinate;


/*******************************************************************
 * MatrixCamera.java - defines a matrix transformation based camera model
 *   In general, the transformation represented by this class
 *   can be thought of as a 4x4 matrix. The forward, or world to
 *   image, mapping is stored as a simplified 3x4 matrix, and the
 *   inverse, or image to world, mapping is store in a simplified
 *   4x3 matrix.
 * This is a Projective Camera in VXL-speak.
 *
 * @version February 25, 2005
 *******************************************************************/
public class MatrixCamera extends CameraModel {

   private static final Logger LOGGER = LoggerFactory.getLogger(MatrixCamera.class);

   private static final NumberFormat NUMBER_FORMAT = new DecimalFormat("###.######");
   private static final RealMatrixFormat MATRIX_FORMAT = new RealMatrixFormat(NUMBER_FORMAT);
   RealMatrix cameraMatrix;
   RealMatrix inverseMatrix;
   private double m_defaultElevation;
   /* This caused an immediate  NullPointerException so commented out since any clients
 that might have been using it were code paths that weren't being called anyway (or they
 would have seen the immediate exception).
 public MatrixCamera() {
   setMatrix(new Matrix(3,4,0.0));
   setInverse(null);
 }
    */
   public MatrixCamera(double[][] sharedArray) {
      RealMatrix matrix;
      if (sharedArray != null) {
         matrix = MatrixUtils.createRealMatrix(sharedArray);
      } else {
         matrix = MatrixUtils.createRealMatrix(3, 4);
      }
      setMatrix(matrix);
   }

   /**************************************************************
    * Takes a Matrix object and a boolean indicating whether
    * the matrix is to be treated as the inverse (image to world)
    * mapping.
    *
    * @param matrix The transformation Matrix
    * @param isInverse A boolean which indicates whether the
    *        matrix is to be used for the forward (world to image)
    *        or backward (image to world) mapping. If true, the
    *        constructor will use the input matrix as the image
    *        to world transformation. In either case, the opposite
    *        mapping is computed and stored.
    **************************************************************/
   public MatrixCamera(RealMatrix matrix, boolean isInverse) {
      if (isInverse) {
         setInverse(matrix);
      } else {
         setMatrix(matrix);
      }
   }

   public MatrixCamera(RealMatrix sharedMatrix) {
      if (sharedMatrix != null) {
         setMatrix(sharedMatrix);
      } else {
         setMatrix(MatrixUtils.createRealMatrix(3,4));
      }
   }

   /**************************************************************
    * Get the matrix camera instance
    *
    * @return A reference to the matrix camera
    **************************************************************/
   public RealMatrix getMatrix() {
      return this.cameraMatrix;
   }


   public void setMatrix(RealMatrix sharedMatrix) {

      if (sharedMatrix == null)
         throw new IllegalArgumentException(this.getClass().getName()
               + ".setMatrix(...) argument must not be null.");

      if (sharedMatrix.getRowDimension() != 3 || sharedMatrix.getColumnDimension() != 4)
         throw new IllegalArgumentException(this.getClass().getName()
               + ".setMatrix(...) argument must be 3x4 Matrix.");

      this.cameraMatrix = sharedMatrix;
      this.inverseMatrix = inverseTransformation(this.cameraMatrix);
   }

   public void setInverse(RealMatrix matrix)
   {

      if (matrix == null)
         throw new IllegalArgumentException(this.getClass().getName()
               + ".setInverse(...) argument must not be null.");

      if (matrix.getRowDimension() != 4 ||  matrix.getColumnDimension() != 3)
         throw new IllegalArgumentException(this.getClass().getName()
               + ".setInverse(...) argument must be 4x3 Matrix.");

      this.inverseMatrix = matrix;
      this.cameraMatrix = inverseTransformation(this.inverseMatrix);
   }

   /**************************************************************
    * {@inheritDoc}
    **************************************************************/
   public Point2D worldToImage(Coordinate worldPoint, Point2D imagePoint) {
      RealMatrix homoWP = MatrixUtils.createRealMatrix(4,1);
      homoWP.setEntry(0,0,worldPoint.x);
      homoWP.setEntry(1,0,worldPoint.y);
      homoWP.setEntry(2,0,worldPoint.z);
      homoWP.setEntry(3,0,1.0);

      RealMatrix homoIP = this.cameraMatrix.multiply(homoWP);
      double x = homoIP.getEntry(0,0);
      double y = homoIP.getEntry(1,0);
      double h = homoIP.getEntry(2,0);

      if (h == 0.0)
         imagePoint.setLocation(Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY);
      else
         imagePoint.setLocation(x/h,y/h);

      return imagePoint;
   }

   /**************************************************************
    * {@inheritDoc}
    **************************************************************/
   public Coordinate imageToWorld(Point2D imagePoint, Surface surf, Coordinate worldPoint) {
      // first, do the default transformation
      worldPoint.setCoordinate(new Coordinate(0.0, 0.0, 0.0));
      if(inverseMatrix != null)
      {
         RealMatrix homoIP = 
               MatrixUtils.createRealMatrix(
                     new double[][] {
                        { imagePoint.getX() },
                        { imagePoint.getY() },
                        { 1.0 }
                     });


         RealMatrix homoWP = this.inverseMatrix.multiply(homoIP);
         double x = homoWP.getEntry(0,0);
         double y = homoWP.getEntry(1,0);
         double z = homoWP.getEntry(2,0); // should be 0.0
         double w = homoWP.getEntry(3,0);

         if (w == 0.0) {
            worldPoint.setCoordinate(new Coordinate(
                  Double.POSITIVE_INFINITY,
                  Double.POSITIVE_INFINITY,
                  Double.POSITIVE_INFINITY));
         } else {
            worldPoint.setCoordinate(new Coordinate(x/w, y/w, z/w));
         }
         
         // TODO: Ok...this makes no sense as it invalidates the previous few lines.
         // but it was in the XIIO code ..so I'm leaving it here until I can investigate.
         worldPoint.setCoordinate(new Coordinate(x, y, z));
      }

      // now get the corresponding z value from the surface
      double zValue = 0.0;
      if (surf != null) {
         zValue = surf.getElevation(imagePoint.getX(), imagePoint.getY());
      } else {
         // this is not an error; the contract of this method in the parent abstract class
         // allows for a null surface
         LOGGER.warn("In MatrixCamera.imageToWorld: SURFACE IS NULL");
         // TODO:  from the comment 20 lines above it appears the z value will probably
         // always be zero, so it is not clear here whether we should use what is returned to
         // us from the camera, or whether we should use the output of getDefaultElevation()?
         // As of 4/23/07, it seems there isn't really anything that is setting the default
         // elevation in this MatrixCamera class anyway (no real 
         // users of the setDefaultElevation() method),
         // so perhaps it is OK to just return the output from the matrix for now?
         // Anyway, this is still better than the NullPointer that we were getting before
         // when the surface param was assumed to be not null (which isn't always the case).
         zValue = worldPoint.z;
      }
      worldPoint.setCoordinate(new Coordinate(worldPoint.x, worldPoint.y, zValue));
      return worldPoint;
   }



   /******************************************************************
    * Inverts a matrix for use as the opposite transformation mapping.
    * If the input matrix is a 3x4 matrix (world to image mapping),
    * the output will be the inverse image to world mapping in a
    * 4x3 matrix. If the input is a 4x3 (image to world) matrix,
    * the result will be a 3x4 matrix which represents the inverse
    * (world to image) mapping.
    *
    * <p>Since the world to image mapping drops the Z coordinate,
    * and the image to world mapping must make up a Z coordinate
    * out of thin air, some matrix operations are simplified by
    * storing the mappings as 3x4 and 4x3 matrices. To convert
    * between the forward (3x4) and inverse (4x3) mapping, the
    * following steps are performed:
    *
    * <ol>
    * <li>The row [0, 0, 1, 0] is inserted as the new third row
    * of the matrix.</li>
    * <li>The inverse of the resulting matrix is computed.</li>
    * <li>The inverted matrix is converted to a 4x3 by dropping
    * the third column. The effect of this is to map a 3D homogeneous
    * point to a 2D homogeneous point where the Z value is dropped</li>
    * </ol>
    *
    * <p>In the inverse mapping, the following steps are performed:
    *
    * <ol>
    * <li>The column [0, 0, 1, 0] is inserted as the new third
    * column of the matrix.</li>
    * <li>The inverse of the resulting matrix is computed.</li>
    * <li>The inverted matrix is converted to a 3x4 by dropping
    * the third row. The effect of this is to map a 2D homogeneous
    * point to a 3D homogeneous point where the Z value is set
    * to 0.0</li>
    * </ol>
    *
    * <p>This scheme should be compatible with the capability to
    * support perspective transformations in the matrix.
    *
    * @param cameraMatrix2 The Matrix to be inverted
    * @return The inverted matrix
    *****************************************************************/
   private RealMatrix inverseTransformation(RealMatrix cameraMatrix2)
   {
      double[][] array = cameraMatrix2.copy().getData();
      double[][] extendedMatrix = new double[4][4];
      int[] rows = {0,1,2,3};
      int[] cols = {0,1,2,3};

      if (LOGGER.isDebugEnabled()) {
         // this goes to INFO so it goes to stdout by default
         // the reason is because the m.print() method below prints to stdout
         // and there doesn't seem to be a way to get m.print() to go
         // to the logger (logger doesn't seem to have a way to get a PrintWriter)
         LOGGER.info(String.format("matrix before calculate inverse: %s", 
               MATRIX_FORMAT.format(cameraMatrix2)));
      }

      if(cameraMatrix2.getRowDimension() == 3) // world-to-image matrix
      {
         LOGGER.debug("m.getRowDimension() == 3 " );  
         int sourceRow = 0;
         for(int i=0; i<4; i++)
         {
            // insert a new row in the third position
            if(i==2)
            {
               extendedMatrix[i][0] = 0.0;
               extendedMatrix[i][1] = 0.0;
               extendedMatrix[i][2] = 1.0;
               extendedMatrix[i][3] = 0.0;
            }
            else
            {
               for(int j=0; j<4; j++)
               {
                  extendedMatrix[i][j] = cameraMatrix2.getEntry(sourceRow,j);
               }
               sourceRow++;
            }
         }
         // force the result to drop the third column.
         cols = new int[]{0,1,3};
      }
      else if(cameraMatrix2.getColumnDimension() == 3) // image-to-world matrix
      {
         LOGGER.debug("m.getColumnDimension() == 3 " );  
         for(int i=0; i<4; i++)
         {
            int sourceCol = 0;
            for(int j=0; j<4; j++)
            {
               // insert a new column in the third position
               if(j==2)
               {
                  if(i==2)
                  {
                     extendedMatrix[i][j] = 1.0;
                  }
                  else
                  {
                     extendedMatrix[i][j] = 0.0;
                  }
               }
               else
               {
                  extendedMatrix[i][j] = cameraMatrix2.getEntry(i,sourceCol);
                  sourceCol++;
               }
            }
         }
         // force the result to drop the third row.
         rows = new int[]{0,1,3};
      }

      RealMatrix inv = MatrixUtils.inverse(MatrixUtils.createRealMatrix(extendedMatrix));
      return inv.getSubMatrix(rows, cols);
   }

   /**************************************************************
    * {@inheritDoc}
    **************************************************************/
   public double getDefaultElevation() {
      return m_defaultElevation;
   }

   /**************************************************************
    * {@inheritDoc}
    **************************************************************/
   public void setDefaultElevation(double elevation) {
      m_defaultElevation = elevation;
   }

   /**************************************************************
    * {@inheritDoc}
    **************************************************************/
   public Coordinate getReferencePoint() {
      // assume 0,0 on the image will be in the image
      Point2D imagePoint = new Point2D.Double(0.0, 0.0);
      return imageToWorld(imagePoint);
   }
} // end class MatrixCamera
