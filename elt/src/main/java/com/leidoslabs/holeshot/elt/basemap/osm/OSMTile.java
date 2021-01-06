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
package com.leidoslabs.holeshot.elt.basemap.osm;

import java.awt.Dimension;
import java.awt.Transparency;
import java.awt.color.ColorSpace;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.ComponentColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.IntStream;

import org.apache.sis.measure.Range;
import org.joml.Vector2d;
import org.joml.Vector2dc;
import org.joml.Vector2i;
import org.joml.Vector2ic;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Polygon;

import com.google.common.primitives.Doubles;
import com.leidoslabs.holeshot.elt.coord.MapScale;
import com.leidoslabs.holeshot.elt.tileserver.CoreImage;
import com.leidoslabs.holeshot.elt.utils.GeometryUtils;
import com.leidoslabs.holeshot.elt.utils.ImageLoadUtils;
import com.leidoslabs.holeshot.elt.viewport.ImageWorld;
import com.leidoslabs.holeshot.elt.viewport.WebMercatorProjection;

/**
 * @author robertsrg
 *
 */
public class OSMTile {

	private static String DEFAULT_TILESERVER = "tile.example.com";
	
	public final static int MAX_ZOOM = 19;
	private static final Range<Double> WORLD_XSPAN = ImageWorld.WEB_MERCATOR_PROJECTION.getXAxisRange();
	private static final Range<Double> WORLD_YSPAN = ImageWorld.WEB_MERCATOR_PROJECTION.getYAxisRange();

	private final int xTile;
	private final int yTile;
	private final MapScale zoom;
	private final Coordinate upperLeft;
	private final String tileserver;

	private OSMTile(int xTile, int yTile, MapScale zoom) {
		this(DEFAULT_TILESERVER, xTile, yTile, zoom);
	}

	private OSMTile(String tileserver, int xTile, int yTile, MapScale zoom) {
		this.tileserver = tileserver;
		this.xTile = xTile;
		this.yTile = yTile;
		this.zoom = new MapScale(zoom);
		this.upperLeft = ImageWorld.WEB_MERCATOR_PROJECTION.transformTo(new Coordinate(tile2lon(xTile, this.zoom), tile2lat(yTile, this.zoom), 0.0));
		validate();
	}
	
	public boolean isImageAvailable() {
		return zoom.getZoom() >= 0 && zoom.getZoom() <= MAX_ZOOM;
	}
	
	public int getXTile() {
		return xTile;
	}
	public int getYTile() {
		return yTile;
	}
	public MapScale getZoom() {
		return new MapScale(zoom);
	}
	
	private void validate() {
		if (!isValid(xTile, yTile, zoom)) {
			throw new IllegalArgumentException(String.format("Invalid OSMTile specified (z=%f x=%d y=%d) validRange = %s", zoom.getZoom(), xTile, yTile, getTilesPerZoom(zoom).toString()));
		}
	}
	
	public static boolean isValid(int xTile, int yTile, MapScale zoom) {
		final Vector2ic tilesPerZoom = getTilesPerZoom(zoom);
		return xTile >= 0 && xTile < tilesPerZoom.x() && yTile >= 0 && yTile < tilesPerZoom.y(); 
		
	}
	public static OSMTile getTile(int xTile, int yTile, MapScale zoom) {
		return getTile(DEFAULT_TILESERVER, xTile, yTile, zoom);
	}
	public static OSMTile getTile(String tileserver, int xTile, int yTile, MapScale zoom) {
		OSMTile result = null;
		if (isValid(xTile, yTile, zoom)) {
			result = new OSMTile(xTile, yTile, zoom);
		}
		return result;
	}
	
	public static OSMTile getTile(Coordinate interiorPoint, MapScale zoom) {
		return getTile(DEFAULT_TILESERVER, interiorPoint, zoom);
	}
	public static OSMTile getTile(String tileserver, Coordinate interiorPoint, MapScale zoom) {
		MapScale effectiveZoom = new MapScale(Doubles.constrainToRange(zoom.getZoom(), 0.0, OSMTile.MAX_ZOOM));
		Vector2ic tilesPerZoom = getTilesPerZoom(effectiveZoom);
		return getTile(tileserver, (int)Math.floor( (interiorPoint.getX() + 180.0) / 360.0 * tilesPerZoom.x()),
				(int)Math.floor( (1.0 - Math.log(Math.tan(Math.toRadians(interiorPoint.getY())) + 1.0 / Math.cos(Math.toRadians(interiorPoint.getY()))) / Math.PI) / 2.0 * tilesPerZoom.y() ),
				effectiveZoom);
	}
	
	public OSMTile getParentTile() {
		OSMTile parentTile = null;
		final int currentZoom = (int)Math.floor(getZoom().getZoom() + 0.5);
		
		if (currentZoom > 0) {
			parentTile = getTile(getXTile()/2, getYTile()/2, new MapScale(currentZoom -1));
		}
		return parentTile;
	}
	public List<OSMTile> getSubtiles() {
		List<OSMTile> subtiles = new ArrayList<>();
		final MapScale nextZoom = new MapScale(getZoom().getZoom() + 1);
		final OSMTile ul = getTile(getXTile()*2, getYTile()*2, nextZoom);
		final OSMTile ur = getTile(ul.getXTile() + 1, ul.getYTile(), nextZoom);
		final OSMTile ll = getTile(ul.getXTile(), ul.getYTile() + 1, nextZoom);
		final OSMTile lr = getTile(ur.getXTile(), ll.getYTile(), nextZoom);

		Arrays.stream(new OSMTile[] { ll, lr, ur, ul}).filter(t->t!=null).forEach(t->subtiles.add(t));
		return subtiles;
	}

	private String getRelativeURL() {
		return String.format("%d/%d/%d.png", (int)zoom.getZoom(), xTile, yTile);
	}
	public String getURLString() {
		return String.format("https://%s/%s", tileserver, getRelativeURL());
	}
	public URL getURL() throws MalformedURLException {
		return new URL(getURLString());
	}

	public static Set<OSMTile> getOverlappingTiles(ImageWorld imageWorld) {
		final Set<OSMTile> overlappingTiles = new HashSet<>();
		
		Polygon geodeticViewport = imageWorld.getGeodeticViewport();
		final MapScale zoom = imageWorld.getMapScale();

		// Get the WebMercator projected viewport (We do it this 
		WebMercatorProjection proj = ImageWorld.WEB_MERCATOR_PROJECTION;
		Polygon projectedViewport = GeometryUtils.GEOMETRY_FACTORY.createPolygon(
				Arrays.stream(geodeticViewport.getCoordinates()).map(c->proj.transformTo(c)).toArray(Coordinate[]::new));
		final Coordinate point = GeometryUtils.toCoordinate(geodeticViewport.getInteriorPoint());
		final OSMTile firstTile = getTile(point, zoom);
		
		if (firstTile != null) {
			firstTile.getOverlappingTiles(projectedViewport, overlappingTiles);
		}
		return overlappingTiles;

	}
	
	private void getOverlappingTiles(Polygon projectedViewport, Set<OSMTile> overlappingTiles) {
		if (!overlappingTiles.contains(this) && projectedViewport.intersects(getBoundingBoxProjected())) {
			overlappingTiles.add(this);
			
			IntStream.rangeClosed(-1,1)
			.mapToObj(x->IntStream.rangeClosed(-1, 1).mapToObj(y->getRelativeTile(x,y)))
			.flatMap(s->s)
			.filter(t->t!=null && !t.equals(OSMTile.this))
			.forEach(t->t.getOverlappingTiles(projectedViewport, overlappingTiles));
		}
	}

	public Polygon getBoundingBoxProjected() {
		OSMTile southeastTile = getRelativeTile(1,1);
		OSMTile southTile = getRelativeTile(0,1);
		OSMTile eastTile = getRelativeTile(1,0);
		
		Coordinate northwest = getUpperLeftProjected();
		Coordinate southwest = (southTile != null) ? southTile.getUpperLeftProjected() : new Coordinate(northwest.getX(), WORLD_YSPAN.getMinValue(),0.0);
		Coordinate northeast = (eastTile != null) ? eastTile.getUpperLeftProjected() : new Coordinate(WORLD_XSPAN.getMaxValue(), northwest.getY(), 0.0);
		Coordinate southeast = (southeastTile != null) ? southeastTile.getUpperLeftProjected() : new Coordinate(northeast.getX(), southwest.getY(),0.0);

		final Polygon bbox = GeometryUtils.GEOMETRY_FACTORY.createPolygon(
				new Coordinate[] { northwest, northeast, southeast, southwest, northwest });
		
		return bbox;
	}
	
	public Polygon getProjectedBoundingBox(ImageWorld imageWorld) {
		return GeometryUtils.GEOMETRY_FACTORY.createPolygon(
				Arrays.stream(getBoundingBoxProjected().getCoordinates())
				.map(c-> imageWorld.geodeticToProjected(c))
				.toArray(Coordinate[]::new));
	}

	public boolean isVisible(ImageWorld imageWorld, Polygon projectedViewport) {
		return getProjectedBoundingBox(imageWorld).intersects(projectedViewport) && imageWorld.getMapScale().equals(getZoom());
	}


	public static Vector2ic getTilesPerZoom(MapScale zoom) {
		final int tilesPerRow = (int)Math.pow(2.0, Math.floor(zoom.getZoom() + 0.5));
		Vector2ic result = new Vector2i(tilesPerRow);
		return result;
	}

	private Coordinate getUpperLeftProjected() {
		return upperLeft;
	}
	public OSMTile getRelativeTile(int xOffset, int yOffset) {
		OSMTile result = null;
		final Vector2ic tilesForZoom = getTilesPerZoom(zoom);
		
		final int newX = xTile + xOffset;
		final int newY = yTile + yOffset;
		
		if (newX >= 0 && newX < tilesForZoom.x() && newY >= 0 && newY < tilesForZoom.y()) { 
		   result = getTile(tileserver, newX, newY, zoom);
		}
		return result;
	}

	private static double tile2lon(int x, MapScale z) {
		return x / Math.pow(2.0, z.getZoom()) * 360.0 - 180;
	}

	private static double tile2lat(int y, MapScale z) {
		double n = Math.PI - (2.0 * Math.PI * y) / Math.pow(2.0, z.getZoom());
		return Math.toDegrees(Math.atan(Math.sinh(n)));
	}	 
	
	@Override
	public String toString() {
		return getRelativeURL();
	}
	
	@Override
	public int hashCode() {
		return toString().hashCode();
	}
	
	@Override
	public boolean equals(Object obj) {
		return (obj instanceof OSMTile) && obj.toString().equals(toString());
	}
	
	public static Dimension getSize() {
		return new Dimension(256, 256);
	}
	
	/**
	 * @return
	 */
	public CoreImage getTileImage() throws IOException {
		CoreImage result = null;
		URL url = getURL();
		try (InputStream tileInputStream = url.openStream()) {
			System.out.println("Fetching tile from " + url.toString());
			result = getTileImage(tileInputStream);
		}
		return result;
	}
	public static CoreImage getTileImage(InputStream tileInputStream) throws IOException {
		BufferedImage image = ImageLoadUtils.getInstance().loadImage(tileInputStream);

		// Deal with color indexed images by converting them to RGB.
		//BufferedImage rgbImage = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_3BYTE_RGB);
		BufferedImage rgbImage = create3ByteImage(image.getWidth(), image.getHeight(), new int[] { 8,8,8}, new int[] {0,1,2});
		rgbImage.getGraphics().drawImage(image,  0,  0,  null);

		return new CoreImage(rgbImage);
	}
    private static BufferedImage create3ByteImage(int width, int height, int[] nBits, int[] bOffs) {
        ColorSpace cs = ColorSpace.getInstance(ColorSpace.CS_sRGB);
        ColorModel colorModel =
            new ComponentColorModel(cs, nBits,
                                    false, false,
                                    Transparency.OPAQUE,
                                    DataBuffer.TYPE_BYTE);
        WritableRaster raster =
            Raster.createInterleavedRaster(DataBuffer.TYPE_BYTE,
                                           width, height,
                                           width*3, 3,
                                           bOffs, null);
        return new BufferedImage(colorModel, raster, false, null);
    }	
	
	/**
	 * @return
	 */
	public String getKey() {
		return getURLString();
	}
}
