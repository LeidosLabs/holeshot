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
import java.util.ArrayList;
import java.util.List;

import org.locationtech.jts.geom.Envelope;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

/**
 * JSON Deserialization for Envelopes
 */
public class EnvelopeDeserializer extends JsonDeserializer<Envelope> {
   @Override
   /**
    * Deserializes JSON array of form [x1, y1, x2, y2] as Envelope(x1, x2, y1, y2)
    */
   public Envelope deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException {
       // current token is "["
       List<Double> values = new ArrayList<Double>();
       while (jsonParser.nextToken() != JsonToken.END_ARRAY) {
           Double value = jsonParser.getDoubleValue();
           values.add(value);
       }
       Envelope result = null;
       if (values.size() == 4) {
           result = new Envelope(values.get(0), values.get(2), values.get(1), values.get(3));
       }
       return result;

   }
}
