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

package com.leidoslabs.holeshot.elt.observations;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Representation of an annotation placed by a user atop of the ELT
 * @param <G>
 */
public class Observation<G extends Geometry> {
    protected static GeometryFactory factory = new GeometryFactory();

    private boolean selected;
    private G geometry;
    private HashMap<String, String> properties = new HashMap<>();

    /**
     * Constructor, initialize observations with a map of properties
     * @param geometry
     * @param properties Observation of properties. If key 'id' is not present,
     * One will be randomly generated
     */
    public Observation(G geometry, Map<String, String> properties) {
        this.geometry = geometry;
        this.selected = false;
        if(properties != null) {
            this.properties.putAll(properties);
        }
        if(!this.properties.containsKey("id")) {
            this.properties.put("id", UUID.randomUUID().toString());
        }
    }

    public String getId() {
        return properties.get("id");
    }
    
    /**
     * selected flag set to true
     */
    public void select() {
        selected = true;
    }
    
    /**
     * selected flag set to false
     */
    public void unselect() {
        selected = false;
    }
    public boolean isSelected() {
        return this.selected;
    }
    public G getGeometry() {
        return geometry;
    }
    
    /**
     * @return Coordinates of geometry object
     */
    public Coordinate[]  getCoordinates() {
        return geometry.getCoordinates();
    }

    public HashMap<String, String> getProperties() {
        return properties;
    }

}
