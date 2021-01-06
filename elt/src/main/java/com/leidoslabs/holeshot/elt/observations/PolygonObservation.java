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

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Stream;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.LinearRing;

import com.leidoslabs.holeshot.elt.utils.GeometryUtils;

/**
 * Observation parameterized with a LinearRing (Polygon)
 */
public class PolygonObservation extends Observation<LinearRing> {

    /**
     * Takes an unclosed ring for geometry (if you send a closed ring the last point will be duplicated)
     * @param coords UNCLOSED polygon vertices
     */
    public PolygonObservation(Coordinate[] coords) {
        this(coords, null);
    }

    public PolygonObservation(Coordinate[] coords, Map<String, String> properties) {
        super(GeometryUtils.GEOMETRY_FACTORY.createLinearRing(
                Stream.concat(Arrays.stream(coords), Stream.of(coords[0])).toArray(Coordinate[]::new)), properties);
    }
}
