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

package com.leidoslabs.holeshot.analytics.warehouse.api.model;

public class LowerLeftUpperRightBBox {

        Double[] coords;

        public LowerLeftUpperRightBBox(Double[] coords) throws InstantiationException {
            if(coords.length != 4) {
                throw new InstantiationException("BBOX must have exactly 4 coordinates, received " + coords.length);
            }
            this.coords = coords;
        }

        public double getLeft() {
            return coords[0];
        }

        public double getBottom() {
            return coords[1];
        }

        public double getRight() {
            return coords[2];
        }

        public double getTop() {
            return coords[3];
        }


}