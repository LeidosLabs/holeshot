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
package org.image.common.geojson;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;

import org.locationtech.jts.geom.Coordinate;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

/**
 * Serializer for of Coordinate data
 */
public class CoordinateSerializer extends JsonSerializer<Coordinate> {

   private Integer precision;

   /**
    * Initialize CoordinateSerializer with specified precision
    * @param precision precision for double serialization (# of digits to the right of decimal)
    */
   public CoordinateSerializer(Integer precision) {
       this.precision = precision;
   }

   /**
    * Initialize CoordinateSerializer with default precision
    */
   public CoordinateSerializer() {
       this(null);
   }

   @Override
   /**
    * Serialize coordinate data as an array of 2 numeric values.
    */
   public void serialize(Coordinate coordinate, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
       jsonGenerator.writeStartArray();
       writeNumber(coordinate.x, jsonGenerator);
       writeNumber(coordinate.y, jsonGenerator);
       jsonGenerator.writeEndArray();
   }

   void writeNumber(double number, JsonGenerator gen) throws IOException {
       if (precision != null) {
           gen.writeNumber(new BigDecimal(number).setScale(precision, RoundingMode.HALF_UP));
       } else {
           gen.writeNumber(number);
       }
   }

}