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

/**
 * {
 *     "type": "Polygon",
 *     "coordinates": [
 *         [[30, 10], [40, 40], [20, 40], [10, 20], [30, 10]]
 *     ]
 * }
 * -OR-
 * {
 *     "type": "Polygon",
 *     "coordinates": [
 *         [[35, 10], [45, 45], [15, 40], [10, 20], [35, 10]],
 *         [[20, 30], [35, 35], [30, 20], [20, 30]]
 *     ]
 * }
 */
public class Polygon implements Geometry {

    private String type = "Polygon";
    //First dimension represents outside ring in position zero followed by interior rings (holes)
    private Double[][][] coordinates;

    /**
     * Constructs a simple shape without holes
     * @param coordinates (Closed) Array of ordered coordinates representing vertices.
     *                    i.e. [ [30, 10], [40, 40], [20, 40], [10, 20], [30, 10] ]
     */
    public Polygon(Double[][] coordinates) {
        this.coordinates = new Double[1][coordinates.length][coordinates[0].length];
        this.coordinates[0] = coordinates;
    }

    /**
     * Constructs a shape with holes
     * @param coordinates First dimension is the exterior, higher dimensions define interior "holes"
     *                    i.e. [
     *                      [[35, 10], [45, 45], [15, 40], [10, 20], [35, 10]],
     *                      [[20, 30], [35, 35], [30, 20], [20, 30]]
     *                    ]
     */
    public Polygon(Double[][][] coordinates) {
        this.coordinates = coordinates;
    }

    public Double[][][] getCoordinates() {
        return coordinates;
    }

    @Override
    public String getType(){
        return this.type;
    }

    @Override
    public Double[] getBBox() {
        Double[] ll = Arrays.stream(coordinates[0]).reduce(new Double[]{coordinates[0][0][0], coordinates[0][0][1]},
                (a, b) -> {
                    double x = Math.min(a[0], b[0]);
                    double y = Math.min(a[1], b[1]);
                    return new Double[]{x,y};
                });

        Double[] ur = Arrays.stream(coordinates[0]).reduce(new Double[]{coordinates[0][0][0], coordinates[0][0][1]},
                (a, b) -> {
                    double x = Math.max(a[0], b[0]);
                    double y = Math.max(a[1], b[1]);
                    return new Double[]{x,y};
                });

        return new Double[]{ll[0], ll[1], ur[0], ur[1]};
    }
}
