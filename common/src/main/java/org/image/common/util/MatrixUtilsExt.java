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
package org.image.common.util;

import org.apache.commons.math3.linear.DefaultRealMatrixChangingVisitor;
import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.RealMatrix;

/**
 * Utility class for RealMatricies
 */
public class MatrixUtilsExt {
   private MatrixUtilsExt() {
   }
   /**
    * Adds a scalar to all entries of input matrix
    * @param mat
    * @param s value to add to all entries
    * @return matrix mat' where mat'_ij = mat_ij + s forall ij
    */
   public static RealMatrix plusEquals(RealMatrix mat, double s) {
      mat.walkInColumnOrder(new DefaultRealMatrixChangingVisitor() {
         @Override
       public double visit(int row, int column, double value) {
          return value + s;
       }
      });
      return mat;
   }
   
   /**
    * Matrix addition of mat1 and mat2
    * @param mat1
    * @param mat2
    * @return matrix sum of mat1 and mat2
    */
   public static RealMatrix plusEquals(RealMatrix mat1, RealMatrix mat2) {
      mat1.walkInColumnOrder(new DefaultRealMatrixChangingVisitor() {
         @Override
       public double visit(int row, int column, double value) {
          return value + mat2.getEntry(row, column);
       }
      });
      return mat1;
   }
   
   /**
    * Creates matrix from vals
    * @param vals 
    * @param m
    * @return RealMatrix composed of entries in vals, with m row and vals.length/m columns.
    */
   public static RealMatrix createRealMatrix (double vals[], int m) {
      final int n = (m != 0 ? vals.length/m : 0);
      if (m*n != vals.length) {
         throw new IllegalArgumentException("Array length must be a multiple of m.");
      }
      RealMatrix result = MatrixUtils.createRealMatrix(m, n);
      for (int i = 0; i < m; i++) {
         for (int j = 0; j < n; j++) {
            result.setEntry(i, j, vals[i+j*m]);
         }
      }
      return result;
   }

}
