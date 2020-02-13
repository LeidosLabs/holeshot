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

package com.leidoslabs.holeshot.elt.websocket.events;

import com.leidoslabs.holeshot.elt.websocket.geojson.*;
import org.locationtech.jts.geom.Coordinate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A WebSocketEvent is a wrapper for GeoJson data that can be extended to hold additional metadata about the event
 */
public class WebSocketEvent {

    public static final String OVERLAY_EVENT = "overlay";
    public static final String CENTER_EVENT = "center";
    public static final String LOAD_EVENT = "load";
    public static final String HIGHLIGHT_EVENT = "highlight";
    public static final String DELETE_EVENT = "delete";
    public static final String TIME_FILTER_EVENT = "timespan";
    public static final String SPATIAL_FILTER_EVENT = "bounds";

    private String event;
    private FeatureCollection geoJSON;

    public WebSocketEvent(String event, FeatureCollection collection) {
        this.event = event;
        this.geoJSON = collection;
     }

     public WebSocketEvent(String event, Feature[] features) {
        this.event = event;
        this.geoJSON = new FeatureCollection(features);
     }

    public String getEvent() {
        return event;
    }

    public FeatureCollection getGeoJSON() {
        return geoJSON;
    }

    public static class Builder {

        private String eventType;
        private List<Feature> features = new ArrayList<>();

        public Builder ofType(String type) {
            this.eventType = type;
            return this;
        }

        public Builder withString(Coordinate[] coordinates, Map<String, String> properties) {
            features.add(new Feature(new Polygon(coordinatesToArray(coordinates)), properties));
            return this;
        }

        public Builder withPoint(Coordinate coordinate) {
            features.add(new Feature(new Point(coordinate.x, coordinate.y)));
            return this;
        }

        public Builder withLineString(Coordinate[] coordinates) {
            features.add(new Feature(new LineString(coordinatesToArray(coordinates))));
            return this;
        }

        public Builder withPolygon(Coordinate[] coordinates) {
            features.add(new Feature(new Polygon(coordinatesToArray(coordinates))));
            return this;
        }

        public Builder withCoordinateArray(Double[][] coordinates) {
            features.add(new Feature(new Polygon(coordinates)));
            return this;
        }

        public Builder withPolygon(Coordinate[] coordinates, Map<String, String> properties) {
            features.add(new Feature(new Polygon(coordinatesToArray(coordinates)), properties));
            return this;
        }

        public Builder withProperties(Map<String, String> properties) {
            features.add(new Feature(properties));
            return this;
        }

        private Double[][] coordinatesToArray(Coordinate[] coordinates) {
            Double[][] coordArray = new Double[coordinates.length][2];

            for(int i=0; i< coordinates.length; i++) {
                coordArray[i][0] = coordinates[i].x;
                coordArray[i][1] = coordinates[i].y;
            }

            return(coordArray);
        }

        public Builder withPolyFromBBox(Coordinate lowerLeft, Coordinate upperRight) {
            this.features.add(new Feature(
                    new Polygon(new Double[][]{
                        {lowerLeft.x, lowerLeft.y},
                        {upperRight.x, lowerLeft.y},
                        {upperRight.x, upperRight.y},
                        {lowerLeft.x, upperRight.y},
                        {lowerLeft.x, lowerLeft.y}})));

            return this;
        }

        public WebSocketEvent build() {
            return new WebSocketEvent(this.eventType, features.toArray(new Feature[0]));
        }
    }
}
