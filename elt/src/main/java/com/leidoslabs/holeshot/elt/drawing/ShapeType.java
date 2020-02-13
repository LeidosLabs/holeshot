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

package com.leidoslabs.holeshot.elt.drawing;

import java.util.Arrays;
import java.util.List;

/**
 * Enum for shape types that stores compatible shader names
 *
 */
public enum ShapeType {

    MARKER(Arrays.asList(Shaders.PLACEMARK_VERTEX_SHADER)),
    POLYGON(Arrays.asList(Shaders.POLYGON_VERTEX_SHADER));

    private List<String> vertexShaders;

    ShapeType(List<String> vertexShaders) {
        this.vertexShaders = vertexShaders;
    }

    public List<String> getVertexShaders() {
        return vertexShaders;
    }

    public static class Shaders {
        public static final String PLACEMARK_VERTEX_SHADER = "Marker.vp";
        public static final String PLACEMARK_FRAGMENT_SHADER = "Marker.fp";
        public static final String POLYGON_VERTEX_SHADER = "Polygon.vp";
        public static final String POLYGON_FRAGMENT_SHADER = "Polygon.fp";
    }
}

