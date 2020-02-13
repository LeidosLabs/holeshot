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

import java.util.Arrays;
import java.util.function.BinaryOperator;

/**
 * {
 *     "type": "LineString",
 *     "coordinates": [
 *         [30, 10], [10, 30], [40, 40]
 *     ]
 * }
 */
public class LineString implements Geometry {

    private String type = "LineString";
    private Double[][] coordinates;

    public LineString(Double[][] coordinates) {
        this.coordinates = coordinates;
    }

    public Double[][] getCoordinates() {
        return coordinates;
    }

    @Override
    public String getType(){
        return this.type;
    }

    @Override
    public Double[] getBBox() {
        Double[] ll = Arrays.stream(coordinates).reduce(new Double[]{coordinates[0][0], coordinates[0][1]},
                (a, b) -> {
                    double x = Math.min(a[0], b[0]);
                    double y = Math.min(a[1], b[1]);
                    return new Double[]{x,y};
        });

        Double[] ur = Arrays.stream(coordinates).reduce(new Double[]{coordinates[0][0], coordinates[0][1]},
                (a, b) -> {
                    double x = Math.max(a[0], b[0]);
                    double y = Math.max(a[1], b[1]);
                    return new Double[]{x,y};
                });

        return new Double[]{ll[0], ll[1], ur[0], ur[1]};
    }
}
