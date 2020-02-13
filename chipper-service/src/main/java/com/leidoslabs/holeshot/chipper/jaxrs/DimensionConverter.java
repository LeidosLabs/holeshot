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

package com.leidoslabs.holeshot.chipper.jaxrs;

import java.awt.Dimension;
import java.util.Arrays;

import javax.ws.rs.ext.ParamConverter;

/**
 * JAX RS Converter for Dimensions.
 * Strings should be of form x,y where x and y are integers
 */
public class DimensionConverter implements ParamConverter<Dimension> {
   @Override
   public Dimension fromString(String value) {
      Dimension result = null;
      if (value != null) {
         int[] dims = Arrays.stream(value.split(","))
               .mapToInt(Integer::parseInt)
               .toArray();
         if (dims.length == 2) {
            result = new Dimension(dims[0], dims[1]);
         }
      }
      return result;
   }

   @Override
   public String toString(Dimension value) {
      String result = null;
      if (value != null) {
         result = String.format("%d,%d", value.width, value.height);
      }
      return result;
   }

}
