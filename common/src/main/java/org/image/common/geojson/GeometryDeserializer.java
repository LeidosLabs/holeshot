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

import org.locationtech.jts.geom.Geometry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

/**
 * Deserialization class for Geometry, uses org.locationtech.jts.geom.Geometry.spatial4JDeserializer
 * @param <T>
 */
public class GeometryDeserializer<T extends Geometry> extends JsonDeserializer<T> {
   private static final Logger LOGGER = LoggerFactory.getLogger(GeometryDeserializer.class);
   private static final org.locationtech.spatial4j.io.jackson.GeometryDeserializer spatial4JDeserializer = new org.locationtech.spatial4j.io.jackson.GeometryDeserializer();

   @SuppressWarnings("unchecked")
   @Override
   public T deserialize(JsonParser jsonParser, DeserializationContext ctxt) throws IOException, JsonProcessingException {
      Geometry geometry = spatial4JDeserializer.deserialize(jsonParser, ctxt);
      return (T)geometry;
   }
}
