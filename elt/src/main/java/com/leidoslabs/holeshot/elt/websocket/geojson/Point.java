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

/**
 * {
 *     "type": "Point",
 *     "coordinates": [30, 10]
 * }
 */
public class Point implements Geometry {
    private String type = "Point";
    private Double[] coordinates;

    public Point(Double[] coordinates) {
        this.coordinates = coordinates;
    }

    public Point(Double lon, Double lat) {
        this.coordinates = new Double[]{lon, lat};
    }

    public Double[] getCoordinates() {
        return coordinates;
    }

    public Double getLon() {
        return coordinates[0];
    }

    public Double getLat() {
        return coordinates[1];
    }

    @Override
    public String getType(){
        return this.type;
    }

    @Override
    public Double[] getBBox() {
        return new Double[]{this.getLon(), this.getLat(),
                            this.getLon(), this.getLat()};
    }
}