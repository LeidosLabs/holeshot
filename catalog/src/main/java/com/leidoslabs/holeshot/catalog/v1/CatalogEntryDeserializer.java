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

package com.leidoslabs.holeshot.catalog.v1;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.TextNode;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Polygon;

import java.io.IOException;
import java.util.stream.StreamSupport;

public class CatalogEntryDeserializer extends StdDeserializer<CatalogEntry> {
    public CatalogEntryDeserializer() {
        this(null);
    }

    public CatalogEntryDeserializer(Class<?> vc) {
        super(vc);
    }

    /**
     * Deserialize from a GeoJSON representation into CatalogEntry object
     * @param jp
     * @param ctxt
     * @return
     * @throws IOException
     * @throws JsonProcessingException
     */
    @Override
    public CatalogEntry deserialize(JsonParser jp, DeserializationContext ctxt)
            throws IOException, JsonProcessingException {
        JsonNode node = jp.getCodec().readTree(jp);

        final CatalogEntry catalogEntry = new CatalogEntry();
        final String imageLink = ((TextNode)node.get("imageLink")).asText();
        catalogEntry.setImageLink(imageLink);

        JsonNode bounds = node.get("bounds");
        ArrayNode coordinates = (ArrayNode)bounds.withArray("coordinates");
        ArrayNode coordinates2 = (ArrayNode)coordinates.get(0);

        Coordinate[] jtsCoords =
                StreamSupport.stream(coordinates2.spliterator(), false).map(c-> new Coordinate(c.get(0).asDouble(), c.get(1).asDouble(), c.get(2).asDouble())).toArray(Coordinate[]::new);

        GeometryFactory geometryFactory = new GeometryFactory();
        final Polygon jtsBounds = geometryFactory.createPolygon(jtsCoords);
        catalogEntry.setBounds(jtsBounds);

        catalogEntry.setNCOLS(node.get("NCOLS").asInt());
        catalogEntry.setNROWS(node.get("NROWS").asInt());
        catalogEntry.setMaxRLevel(node.get("maxRLevel").asInt());
        catalogEntry.setNBANDS(node.get("NBANDS").asInt());

        final String[] imageName = node.get("name").toString().replace("\"","").split(":");
        catalogEntry.setImageId(imageName[0]);
        catalogEntry.setTimestamp(imageName[1]);

        return catalogEntry;
    }
}
