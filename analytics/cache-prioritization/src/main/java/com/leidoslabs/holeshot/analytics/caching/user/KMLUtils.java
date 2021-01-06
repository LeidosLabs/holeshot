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

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryCollection;
import org.locationtech.jts.geom.LinearRing;
import org.locationtech.jts.geom.Polygon;

import de.micromata.opengis.kml.v_2_2_0.ColorMode;
import de.micromata.opengis.kml.v_2_2_0.Document;
import de.micromata.opengis.kml.v_2_2_0.Kml;
import de.micromata.opengis.kml.v_2_2_0.KmlFactory;
import de.micromata.opengis.kml.v_2_2_0.MultiGeometry;
import de.micromata.opengis.kml.v_2_2_0.Placemark;
import de.micromata.opengis.kml.v_2_2_0.Style;
import de.micromata.opengis.kml.v_2_2_0.StyleSelector;

public class KMLUtils {
	
	
	public static void writeKML(List<Geometry> geoms, String name, String color) {
		Kml kml = KmlFactory.createKml();
		
		Document kmlDoc = kml.createAndSetDocument().withName(name);
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
			
			String col;
			ColorMode mode;
			if (color == null) {
				col = "FFFFFFFF";
				mode = ColorMode.RANDOM;
			} else {
				col = color;
				mode = ColorMode.NORMAL;
			}
			setPolyStyleAndLineStyle(newPM, col, mode, 1, null, ColorMode.NORMAL);
		}
		try(OutputStream os = new FileOutputStream(name + ".kml")){
			kml.marshal(os);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private static void setPolyStyleAndLineStyle(Placemark placemark, String color, ColorMode polyMode, double width, String lineColor,
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
	
	private static List<de.micromata.opengis.kml.v_2_2_0.Coordinate> getOuterCoordinates(LinearRing ring, double altitude){
		Coordinate[] inCords = ring.getCoordinates();
		List<de.micromata.opengis.kml.v_2_2_0.Coordinate> outCords = new ArrayList<>();
        for (Coordinate inCord : inCords) {
            outCords.add(new de.micromata.opengis.kml.v_2_2_0.Coordinate(inCord.x, inCord.y, altitude));
        }
		return outCords;
	}
}
