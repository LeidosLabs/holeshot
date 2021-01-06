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

package com.leidoslabs.holeshot.analytics.caching.user;

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.*;
import java.util.function.Consumer;

import org.image.common.geojson.GeoJsonModule;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryCollection;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LinearRing;
import org.locationtech.jts.geom.MultiLineString;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.io.kml.KMLWriter;
import org.locationtech.jts.operation.union.CascadedPolygonUnion;
import org.locationtech.spatial4j.context.jts.JtsSpatialContext;
import org.locationtech.spatial4j.io.GeohashUtils;
import org.locationtech.spatial4j.shape.Rectangle;
import org.locationtech.spatial4j.shape.jts.JtsGeometry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Maps;

import de.micromata.opengis.kml.v_2_2_0.AltitudeMode;
import de.micromata.opengis.kml.v_2_2_0.ColorMode;
import de.micromata.opengis.kml.v_2_2_0.Document;
import de.micromata.opengis.kml.v_2_2_0.Kml;
import de.micromata.opengis.kml.v_2_2_0.KmlFactory;
import de.micromata.opengis.kml.v_2_2_0.MultiGeometry;
import de.micromata.opengis.kml.v_2_2_0.Placemark;
import de.micromata.opengis.kml.v_2_2_0.Style;
import de.micromata.opengis.kml.v_2_2_0.StyleSelector;

public class Spatial4jTest {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(Spatial4jTest.class);

	public static void main(String[] args) {

		buildAOI();
		//writeKML(polys);
		//postAOI();

	}
	
	public static Geometry readGeom(String fname) {
		ObjectMapper mapper = new ObjectMapper();
		GeoJsonModule gjsonMod = new GeoJsonModule();
		mapper.registerModules(gjsonMod);
		try {
			return mapper.readValue(new File(fname), Polygon.class);
		} catch (IOException e) {
			System.out.println("Error reading geojson file");
			e.printStackTrace();
		}
		return null;
	}
	
	public static void buildAOI() {
		JtsSpatialContext sc = JtsSpatialContext.GEO;
		GeometryFactory gFactory = sc.getShapeFactory().getGeometryFactory();
		List<Geometry> polys = new ArrayList<Geometry>();
		
		String[] geohashes = {"sk", "ss", "su", "se", "skv", "sky", "skz", "ts", "tt"};
		for (String geohash: geohashes) {
			
			Rectangle rect = GeohashUtils.decodeBoundary(geohash, sc);
	        Coordinate ll = new Coordinate(rect.getMinX(), rect.getMinY());
	        Coordinate lr = new Coordinate(rect.getMaxX(), rect.getMinY());
	        Coordinate ur = new Coordinate(rect.getMaxX(), rect.getMaxY());
	        Coordinate ul = new Coordinate(rect.getMinX(), rect.getMaxY());
			Polygon poly = gFactory.createPolygon(new Coordinate[] {ll, lr, ur, ul, ll});
			JtsGeometry geom = new JtsGeometry(poly, sc, false, false);
			polys.add(geom.getGeom());
		}
		Geometry sahara = readGeom("sahara_rough.json");
		polys.add(sahara);
		CascadedPolygonUnion unioner = new CascadedPolygonUnion(polys);
		Geometry unionRaw = unioner.union();
		Geometry union = new JtsGeometry(unionRaw, sc, false, true).getGeom();
		polys.add(union);
	}
	
	public static void writeKML(List<Geometry> geoms) {
		Kml kml = KmlFactory.createKml();
		
		Document kmlDoc = kml.createAndSetDocument().withName("spatial4jTest");
		geoms.sort(Comparator.comparingDouble(Geometry::getArea));
		double baseAltitude = 100.0;
		for (int i = 0; i < geoms.size(); i++) {
			Placemark newPM = kmlDoc.createAndAddPlacemark();
			Geometry geo = geoms.get(i);
			double altitude = baseAltitude + i * 10;
			if (geo instanceof Polygon) {
				LinearRing ring = (LinearRing) ((Polygon) geo).getBoundary();
				newPM.createAndSetPolygon().createAndSetOuterBoundaryIs().createAndSetLinearRing().withCoordinates(getOuterCoordinates(ring, altitude));
			} else if (geo instanceof GeometryCollection) {
				GeometryCollection gCol = (GeometryCollection) geo;
				MultiGeometry mGeom = newPM.createAndSetMultiGeometry();
				for (int j = 0; j < gCol.getNumGeometries(); j++) {
					Polygon poly = (Polygon) geo.getGeometryN(j);
					LinearRing ring = (LinearRing) poly.getBoundary();
					mGeom.createAndAddPolygon().createAndSetOuterBoundaryIs().createAndSetLinearRing().withCoordinates(getOuterCoordinates(ring, altitude));
				}
			}
			
			
			newPM.setName(Integer.toString(i));
			
			setPolyStyleAndLineStyle(newPM, "FFFFFFFF", ColorMode.RANDOM, 1, null, ColorMode.NORMAL);
		}
		try(OutputStream os = new FileOutputStream("spatial4jTest.kml")){
			kml.marshal(os);
		} catch (IOException e) {
			LOGGER.error("error marshalling kml");
			e.printStackTrace();
		}
	}
	
	public static void setPolyStyleAndLineStyle(Placemark placemark, String color, ColorMode polyMode, double width, String lineColor,
		    ColorMode lineMode) {
			if (color == null || color.length() != 8) {
				color = "FFFFFFFF";
				polyMode = ColorMode.NORMAL;
			}

			if (width <= 0) {
				width = 0;
			}
			if (lineColor == null || lineColor.length() != 8) {
				lineColor = "FF000000";
				lineMode = ColorMode.NORMAL;
			}
			List<StyleSelector> styleSelector = placemark.getStyleSelector();
			if (styleSelector.isEmpty()) {
				Style style = new Style();
				style.createAndSetPolyStyle();
				styleSelector.add(style);
			}
			Iterator<StyleSelector> iterator = styleSelector.iterator();
			Style style = null;
			while (iterator.hasNext()) {
				StyleSelector tmp = iterator.next();
				if (tmp instanceof Style) {
					style = (Style) tmp;
					style.getPolyStyle().withColor(color).withColorMode(polyMode);
					style.createAndSetLineStyle().withWidth(width).withColor(lineColor).withColorMode(lineMode);
				}
			}
		}
	
	
	public static List<de.micromata.opengis.kml.v_2_2_0.Coordinate> getOuterCoordinates(LinearRing ring, double altitude){
		Coordinate[] inCords = ring.getCoordinates();
		List<de.micromata.opengis.kml.v_2_2_0.Coordinate> outCords = new ArrayList<>();
        for (Coordinate inCord : inCords) {
            outCords.add(new de.micromata.opengis.kml.v_2_2_0.Coordinate(inCord.x, inCord.y, altitude));
        }
		return outCords;
	}

}
