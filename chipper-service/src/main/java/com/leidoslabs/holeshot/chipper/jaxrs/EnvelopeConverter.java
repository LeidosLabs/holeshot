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

import org.locationtech.jts.geom.Envelope;

/**
 * JAX RS Converter for Envelopes.
 * Strings should be of form x1,x2,y1,y2 as doubles
 */
public class EnvelopeConverter implements ParamConverter<Envelope> {
   @Override
   public Envelope fromString(String value) {
      Envelope result = null;
      if (value != null) {
         double[] dims = Arrays.stream(value.split(","))
               .mapToDouble(Double::parseDouble)
               .toArray();
         if (dims.length == 4) {
            result = new Envelope(dims[0], dims[1], dims[2], dims[3]);
         }
      }
      return result;
   }

   @Override
   public String toString(Envelope value) {
      String result = null;
      if (value != null) {
         result = String.format("%d,%d,%d,%d", value.getMinX(), value.getMaxX(), value.getMinY(), value.getMaxY());
      }
      return result;
   }

}
