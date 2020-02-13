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

import java.awt.Rectangle;
import java.util.Arrays;

import javax.ws.rs.ext.ParamConverter;

/**
 * JAX RS Converter for Envelopes.
 * Strings should be of form x,y,width,height as integers
 */
public class RectangleConverter implements ParamConverter<Rectangle> {
   @Override
   public Rectangle fromString(String value) {
      Rectangle result = null;
      if (value != null) {
         int[] dims = Arrays.stream(value.split(","))
               .mapToInt(Integer::parseInt)
               .toArray();
         if (dims.length == 4) {
            result = new Rectangle(dims[0], dims[1], dims[2], dims[3]);
         }
      }
      return result;
   }

   @Override
   public String toString(Rectangle value) {
      String result = null;
      if (value != null) {
         result = String.format("%d,%d,%d,%d", value.x, value.y, value.width, value.height);
      }
      return result;
   }

}
