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

import java.util.Map;

/**
 * GeoJSON Feature, i.e a Geometry + metadata. Used with websocket events
 * @author lobugliop
 *
 */
public class Feature {

    private String type = "Feature";
    private Geometry geometry;
    private Double[] bbox;
    private Map<String, String> properties;

    public Feature(Geometry geometry) {
        this.geometry = geometry;
        this.bbox = geometry.getBBox();
    }

    public Feature(Geometry geometry, Map<String, String> properties) {
        this.geometry = geometry;
        this.bbox = geometry.getBBox();
        this.properties = properties;
    }

    public Feature(Map<String, String> properties) { this.properties = properties; }

    public Geometry getGeometry() {
        return geometry;
    }

    public Double[] getBBox() {
        return bbox;
    }

    public Map<String, String> getProperties(){
        return properties;
    }
}
