/*
 * Licensed to The Leidos Corporation under one or more contributor license agreements.  
 * See the NOTICE file distributed with this work for additional information regarding copyright ownership.
 * The Leidos Corporation licenses this file to You under the Apache License, Version 2.0
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
package org.image.common.geojson;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryCollection;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.LinearRing;
import org.locationtech.jts.geom.MultiLineString;
import org.locationtech.jts.geom.MultiPoint;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.spatial4j.io.jackson.GeometryAsGeoJSONSerializer;
import org.locationtech.spatial4j.io.jackson.ShapesAsGeoJSONModule;

/**
 * ShapesAsGeoJSONModule that adds some more Serializers and Deserializers for 
 * working with geoJsons
 *
 */
public class GeoJsonModule extends ShapesAsGeoJSONModule {
   public GeoJsonModule() {
      this(null);
   }
   public GeoJsonModule(Integer precision) {
      super();
      addDeserializer(Geometry.class, new GeometryDeserializer<Geometry>());
      addDeserializer(GeometryCollection.class, new GeometryDeserializer<GeometryCollection>());
      addDeserializer(Point.class, new GeometryDeserializer<Point>());
      addDeserializer(LinearRing.class, new GeometryDeserializer<LinearRing>());
      addDeserializer(LineString.class, new GeometryDeserializer<LineString>());
      addDeserializer(MultiLineString.class, new GeometryDeserializer<MultiLineString>());
      addDeserializer(MultiPoint.class, new GeometryDeserializer<MultiPoint>());
      addDeserializer(MultiPolygon.class, new GeometryDeserializer<MultiPolygon>());
      addDeserializer(Polygon.class, new GeometryDeserializer<Polygon>());
      addDeserializer(Envelope.class, new EnvelopeDeserializer());
      addDeserializer(Coordinate.class, new CoordinateDeserializer());

      addSerializer(Geometry.class, new GeometryAsGeoJSONSerializer());
      addSerializer(Envelope.class, new EnvelopeSerializer(precision));
      addSerializer(Coordinate.class, new CoordinateSerializer(precision));
   }

}
