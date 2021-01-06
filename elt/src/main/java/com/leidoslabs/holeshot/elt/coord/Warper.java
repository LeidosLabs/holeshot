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

package com.leidoslabs.holeshot.elt.coord;

import org.apache.commons.lang3.tuple.Pair;
import org.joml.Matrix4d;
import org.joml.Matrix4dc;
import org.joml.Vector2d;
import org.joml.Vector2dc;

/**
 * Class for warping between two quadrilaterals, currently unused 
 * @author lobugliop
 *
 */
public class Warper {
   private Pair<Vector2dc, Vector2dc> lowerLeft;
   private Pair<Vector2dc, Vector2dc> lowerRight;
   private Pair<Vector2dc, Vector2dc> upperRight;
   private Pair<Vector2dc, Vector2dc> upperLeft;
   private Matrix4d srcMat;
   private Matrix4d destMat;
   private Matrix4d warpMat;

   private Warper() {
   }

   private void computeWarp() {
      computeQuadToSquare(lowerLeft.getLeft(),
                          lowerRight.getLeft(),
                          upperRight.getLeft(),
                          upperLeft.getLeft(),
                          srcMat);
      computeSquareToQuad(lowerLeft.getRight(),
                          lowerRight.getRight(),
                          upperRight.getRight(),
                          upperLeft.getRight(),
                          destMat);
      srcMat.mul(destMat, warpMat);
   }


   public void computeSquareToQuad(  Vector2dc point0,
                                     Vector2dc point1,
                                     Vector2dc point2,
                                     Vector2dc point3,
                                     Matrix4d mat ) {

      Vector2dc d1 = point1.sub(point2, new Vector2d());
      Vector2dc d2 = point3.sub(point2, new Vector2d());
      Vector2dc s = point0.sub(point1, new Vector2d()).add(point2).sub(point3);

      double g = (s.x() * d2.y() - d2.x() * s.y()) / (d1.x() * d2.y() - d2.x() * d1.y());
      double h = (d1.x() * s.y() - s.x() * d1.y()) / (d1.x() * d2.y() - d2.x() * d1.y());
      double a = point1.x() - point0.x() + g * point1.x();
      double b = point3.x() - point0.x() + h * point3.x();
      double c = point0.x();
      double d = point1.y() - point0.y() + g * point1.y();
      double e = point3.y() - point0.y() + h * point3.y();
      double f = point0.y();

      mat.set(a, d, 0.0, g,
              b, e, 0.0, h,
              0.0, 0.0, 1.0, 0.0,
              c, f, 0.0, 1.0);

   }

   public void computeQuadToSquare( Vector2dc point0,
         Vector2dc point1,
         Vector2dc point2,
         Vector2dc point3,
         Matrix4d mat ) {

      computeSquareToQuad(point0, point1, point2, point3, mat);

      // invert through adjoint

      double a = mat.m00(), d = mat.m10(),   /* ignore */      g = mat.m30();
      double b = mat.m01(), e = mat.m11(),   /* 3rd col*/      h = mat.m31();
      /* ignore 3rd row */
      double c = mat.m03(), f = mat.m13();

      double A =     e - f * h;
      double B = c * h - b;
      double C = b * f - c * e;
      double D = f * g - d;
      double E =     a - c * g;
      double F = c * d - a * f;
      double G = d * h - e * g;
      double H = b * g - a * h;
      double I = a * e - b * d;

      // Probably unnecessary since 'I' is also scaled by the determinant,
      //   and 'I' scales the homogeneous coordinate, which, in turn,
      //   scales the X,Y coordinates.
      // Determinant  =   a * (e - f * h) + b * (f * g - d) + c * (d * h - e * g);
      double idet = 1.0f / (a * A           + b * D           + c * G);

      mat.set(A*idet, D*idet, 0.0, G*idet,
              B*idet, E*idet, 0.0, H*idet,
              0.0, 0.0, 1.0, 0.0,
              C*idet, F*idet, 0.0, I*idet);
   }

   public Matrix4dc getWarpMatrix()
   {
      return warpMat;
   }

   public static class WarperFactory {
      private Pair<Vector2dc, Vector2dc> lowerLeft;
      private Pair<Vector2dc, Vector2dc> lowerRight;
      private Pair<Vector2dc, Vector2dc> upperRight;
      private Pair<Vector2dc, Vector2dc> upperLeft;

      public WarperFactory() {
      }

      public WarperFactory lowerLeft(Vector2dc src, Vector2dc dest) {
         this.lowerLeft = Pair.of(src, dest);
         return this;
      }
      public WarperFactory lowerRight(Vector2dc src, Vector2dc dest) {
         this.lowerRight = Pair.of(src, dest);
         return this;
      }
      public WarperFactory upperRight(Vector2dc src, Vector2dc dest) {
         this.upperRight = Pair.of(src, dest);
         return this;
      }
      public WarperFactory upperLeft(Vector2dc src, Vector2dc dest) {
         this.upperLeft = Pair.of(src, dest);
         return this;
      }
      public Warper build() {
         final Warper warper = new Warper();
         warper.lowerLeft = lowerLeft;
         warper.lowerRight = lowerRight;
         warper.upperRight = upperRight;
         warper.upperLeft = upperLeft;
         warper.computeWarp();
         return warper;
      }
   }
}
