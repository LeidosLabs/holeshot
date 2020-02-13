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

import java.util.Arrays;

import javax.ws.rs.ext.ParamConverter;

import org.locationtech.jts.geom.Coordinate;

/**
 * JAX RS Converter for Coordiantes.
 * Strings should be of form x,y[,z] where x,y,z are doubles
 */
public class CoordinateConverter implements ParamConverter<Coordinate> {
   @Override
   public Coordinate fromString(String value) {
      Coordinate result = null;
      if (value != null) {
         double[] dims = Arrays.stream(value.split(","))
               .mapToDouble(Double::parseDouble)
               .toArray();
         if (dims.length == 2) {
            result = new Coordinate(dims[0], dims[1]);
         } else if (dims.length == 3) {
            result = new Coordinate(dims[0], dims[1], dims[2]);
         }
      }
      return result;
   }

   @Override
   public String toString(Coordinate value) {
      String result = null;
      if (value != null) {
         if (Double.isNaN(value.z)) {
            result = String.format("%d,%d", value.x, value.y);
         } else {
            result = String.format("%d,%d,%d", value.x, value.y, value.z);
         }
      }
      return result;
   }

}
