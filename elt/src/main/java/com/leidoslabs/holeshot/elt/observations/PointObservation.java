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
import org.locationtech.jts.geom.Point;

import com.leidoslabs.holeshot.elt.utils.GeometryUtils;

import java.util.Map;

/**
 * Observation parameterized with a Point
 */
public class PointObservation extends Observation<Point> {

    public PointObservation(Coordinate p) {
        this(p, null);
    }

    public PointObservation(Coordinate p, Map<String, String> properties) {
        super(GeometryUtils.GEOMETRY_FACTORY.createPoint(p), properties);
    }
}
