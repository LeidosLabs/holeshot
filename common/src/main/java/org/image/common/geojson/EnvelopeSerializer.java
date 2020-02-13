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

import org.locationtech.jts.geom.Envelope;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

/**
 * Serialization for Envelope data
 */
public class EnvelopeSerializer extends JsonSerializer<Envelope> {
   private Integer precision;

   /**
    * Initialize EnvelopeSerializer with specified precision
    * @param precision precision for double serialization (# of digits to the right of decimal)
    */
   public EnvelopeSerializer(Integer precision) {
       this.precision = precision;
   }

   /**
    * Initialize EnvelopeSerializer without specifying precision
    */
   public EnvelopeSerializer() {
       this(null);
   }

   @Override
   /**
    * Serialize Envelope as [x1, y1, x2, y2]
    */
   public void serialize(Envelope envelope, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
       jsonGenerator.writeStartArray();
       writeNumber(envelope.getMinX(), jsonGenerator);
       writeNumber(envelope.getMinY(), jsonGenerator);
       writeNumber(envelope.getMaxX(), jsonGenerator);
       writeNumber(envelope.getMaxY(), jsonGenerator);
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