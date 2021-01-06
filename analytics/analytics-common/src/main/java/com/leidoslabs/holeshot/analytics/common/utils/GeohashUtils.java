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

package com.leidoslabs.holeshot.analytics.common.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.github.davidmoten.geo.Coverage;
import com.github.davidmoten.geo.GeoHash;
import com.leidoslabs.holeshot.analytics.common.model.UserTileRef;
import com.leidoslabs.holeshot.elt.tileserver.TileRef;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Polygon;

/**
 * Functions that should be used for geohashing within analytics packages to ensure that results
 * are the same across components
 */
public class GeohashUtils {

    private static final double AVERAGE_RADIUS_OF_EARTH_KM = 6371;

    /**
     * Compute a geohash that roughly approximates the tile shape and location
     *
     * @param tile the tile to geohash
     * @return A List of Strings, with the first element being the raw geohash. If tile is a UserTile, the list contains
     * a second element which is the geohash postfixed by '#<username>#
     */
    public static List<String> locatePointsMult(TileRef tile) {

        if (tile != null) {

            Polygon bbox = tile.getGeodeticBounds();
            Geometry geo = bbox.getEnvelope();
            Coordinate[] coords = geo.getCoordinates();

            double widthKm = calculateDistanceInKilometer(coords[0].getY(), coords[0].getX(), coords[3].getY(), coords[3].getX());
            int precision = getPrecision(widthKm);

            Coverage geoHash = GeoHash.coverBoundingBox(coords[0].getY(), coords[0].getX(), coords[2].getY(), coords[2].getX(), precision);

            Set<String> hashes = geoHash.getHashes();
            String s = hashes.iterator().next();
            List<String> result = new ArrayList<String>();
            result.add(s);
            if (tile instanceof UserTileRef) {
            	result.add(String.format("%s#%s", s, ((UserTileRef) tile).getUserID()));
            }
            return result;

        } else {

            return null;
        }
    }
    
    public static String locatePoints(TileRef tile) {
    	List<String> results = locatePointsMult(tile);
    	if (results != null) {
    		return results.get(0);
    	}
    	return null;
    }

    public static int getPrecision(double widthKm) {
        if (widthKm <= .00477)
            return 9;
        else if (widthKm <= .0382)
            return 8;
        else if (widthKm <= .2) // Traditionally .153, range of 7 increased to distinguish between r0 and r1 in many imgs
            return 7;
        else if (widthKm <= 1.22)
            return 6;
        else if (widthKm <= 4.89)
            return 5;
        else if (widthKm <= 39.1)
            return 4;
        else if (widthKm <= 156)
            return 3;
        else
            return 2;
    }

    // Java implementation of Haversine formula, stackoverflow.com/questions/27928
    public static double calculateDistanceInKilometer(double lat1, double lng1,
                                                      double lat2, double lng2) {

        double latDistance = Math.toRadians(lat1 - lat2);
        double lngDistance = Math.toRadians(lng1 - lng2);

        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lngDistance / 2) * Math.sin(lngDistance / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return (AVERAGE_RADIUS_OF_EARTH_KM * c);
    }
}
