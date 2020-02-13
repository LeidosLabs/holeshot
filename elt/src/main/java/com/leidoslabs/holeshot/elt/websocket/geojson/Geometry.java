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

package com.leidoslabs.holeshot.elt.websocket.geojson;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import java.lang.reflect.Type;

public interface Geometry {
    String getType();
    Double[] getBBox();

    /**
     * Type Adapter for GSON to determine which Geometry a GeoJSON feature is by its "type" field
     * @param <T>
     */
    class TypeAdapter<T extends Geometry> implements JsonSerializer<T>, JsonDeserializer<T> {

        private String pkgName = this.getClass().getPackage().getName();

        /**
         * Pass through serializer, doesn't change default gson behavior
         */
        @Override
        public JsonElement serialize(T object, Type interfaceType, JsonSerializationContext context) {
            return context.serialize(object);
        }

        /**
         * Determine which class of Geometry we have by reading the "type" field of the object
         */
        @Override
        public T deserialize(JsonElement element, Type interfaceType, JsonDeserializationContext context) throws JsonParseException {
            final JsonObject json = (JsonObject) element;
            try {
                final Type type = Class.forName(pkgName + "." + json.get("type").getAsString());
                return context.deserialize(json, type);
            } catch (ClassNotFoundException e) {
                throw new JsonParseException(e);
            }
        }
    }
}
