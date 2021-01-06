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

package com.leidoslabs.holeshot.analytics.simulateddata;

import com.google.gson.JsonObject;
import com.leidoslabs.holeshot.elt.tileserver.TileRef;
import com.leidoslabs.holeshot.elt.tileserver.TileserverImage;
import org.locationtech.jts.geom.Coordinate;

import java.awt.*;
import java.awt.geom.Point2D;
import java.io.*;
import java.net.URL;
import java.util.List;
import java.util.Queue;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class GenFakeLogs {
	
	private static final boolean FIXED_TSIS = false; //Determine images of interest in s3 or use fixed for testing.
	private static final String OUTPUT_DIR = System.getProperty("user.home") + File.separator + "dev" 
											+ File.separator + "emr-gen-logs";
	private static final Long seed = 3093854875328L;
	private final String endpoint = "https://tileserver.leidoslabs.com/tileserver";
	private Coordinate nw;
	private Coordinate se;
	private List<Coordinate> randCords;
	
	public GenFakeLogs(Coordinate nw, Coordinate se) {
		this.nw = nw;
		this.se = se;
		this.randCords = kRandom(nw, se, 5);
	}
	
	/*public void generateLog() {
		List<TileserverImage> validTsis = new ArrayList<TileserverImage>(); 
		if (!FIXED_TSIS) {
			ListObjectsIterator<String> itemsIt = ListObjectsIterator.listPrefixes("advanced-analytics-geo-tile-images", "");
			Stream<String> items = itemsIt.stream().filter(s -> !s.contains("XVIEWCHALLENGE"));
			Stream<String> imgAndTimestamp = items
					.flatMap(s -> ListObjectsIterator.listPrefixes("advanced-analytics-geo-tile-images", s).stream());
			List<TileserverImage> tsis = imgAndTimestamp
					.map(s -> {
						try {
							return new URL(String.join("/", endpoint, s + "metadata.json"));
						} catch (MalformedURLException e) {
							e.printStackTrace();
						}
						return null;
					}).map(s -> {
						try {
							return new TileserverImage(s);
						} catch (IOException | TypeNotPresentException e) {
							e.printStackTrace();
						}
						return null;
					})
					.collect(Collectors.toList());
			validTsis = tsis.stream().filter(i -> i != null && containedPoints(i).size() != 0).collect(Collectors.toList());
		}
		else {
			validTsis = getFixedTSIS();
		}
		validTsis.forEach(tsi -> browseImageAndSave(tsi));
	}*/
	
	public List<TileserverImage> getFixedTSIS(){
		List<TileserverImage> tsis = new ArrayList<TileserverImage>();
		try (InputStream is = getClass().getClassLoader().getResourceAsStream("tsis_shayrat_fixed.txt")){
			try (BufferedReader reader = new BufferedReader(new InputStreamReader(is))){
				String line = "";
				while ((line = reader.readLine()) != null){
					tsis.add(new TileserverImage(new URL(line)));
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return tsis;
	}
	
	
	public void browseImageAndSave(TileserverImage tsi) {
		String fName = String.format("fakelog-%s-%s-%d", tsi.getCollectionID(), tsi.getTimestamp(), new Date().getTime());
		String browseText = browseImage(tsi).stream().map(tile -> tileToJSONString(tile))
				.collect(Collectors.joining("\n"));
		try (PrintWriter out = new PrintWriter(String.format("%s%s%s", OUTPUT_DIR, File.separator, fName))){
			out.print(browseText);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public List<TileRef> browseImage(TileserverImage tsi) {
		List<TileRef> browsedTiles = new ArrayList<TileRef>();
		List<Coordinate> validPoints = containedPoints(tsi);
		
		for (Coordinate point: validPoints) {
			Point2D imagePoint = tsi.getCameraModel().worldToImage(new Coordinate(point.getX(), point.getY(), 0));
			int tileX = (int) (imagePoint.getX() / tsi.getTileWidth()),
				tileY = (int) (imagePoint.getY() / tsi.getTileHeight());
			TileRef poiTile = new TileRef(tsi, 0, tileX, tileY);
			
			Queue<TileRef> browsing = new LinkedList<TileRef>();
			browsing.add(poiTile);
			browsing.addAll(adjacentTiles(tsi, poiTile));
			Set<TileRef> visited = new HashSet<TileRef>(browsing);
			
			while(!browsing.isEmpty()) {
				TileRef curTile = browsing.poll();
				browsedTiles.add(curTile);
				TileRef parent = curTile.getParentTile();
				if (parent != null && !visited.contains(parent)) {
					browsing.add(parent);
					visited.add(parent);
				}
			}
		}
		return browsedTiles;
	}
	
	public List<TileRef> adjacentTiles(TileserverImage tsi, TileRef center) {
		int rset = center.getRset();
		Dimension dim = center.getRowsAndColumnsForRset(rset);
		int tx = center.getX(), ty = center.getY();
		List<TileRef> adjs = new ArrayList<TileRef>(
				Arrays.asList(new TileRef(tsi, rset, tx - 1, ty),
							  new TileRef(tsi, rset, tx - 1, ty - 1),
							  new TileRef(tsi, rset, tx, ty - 1),
							  new TileRef(tsi, rset, tx + 1, ty - 1),
							  new TileRef(tsi, rset, tx + 1, ty),
							  new TileRef(tsi, rset, tx + 1, ty + 1),
							  new TileRef(tsi, rset, tx, ty + 1),
							  new TileRef(tsi, rset, tx - 1, ty + 1)));
		Predicate<TileRef> inBounds = tf -> tf.getX() < dim.getWidth() 
											&& tf.getX() > 0
											&& tf.getY() < dim.getHeight()
											&& tf.getY() > 0;
		return adjs.stream().filter(inBounds).collect(Collectors.toList());
	}

	
	public List<Coordinate> kRandom(Coordinate tl, Coordinate br, int k) {
		ArrayList<Coordinate> result = new ArrayList<Coordinate>();
		Random rand1 = new Random(seed), rand2 = new Random(seed / 81 * 21);  
		Iterator<Double> d1 = rand1.doubles(k).iterator(), d2 = rand2.doubles(k).iterator();
		while (d1.hasNext() && d2.hasNext()) {
			double lon = (d1.next() * (this.se.getX() - this.nw.getX())) + this.nw.getX();
			double lat = (d2.next() * (this.se.getY() - this.nw.getY())) + this.nw.getY();
			result.add(new Coordinate(lon, lat));
		}
		return result;
	}
	
	
	public List<Coordinate> containedPoints(TileserverImage tsi) {
		double[] bounds = tsi.getTilePyramidDescriptor().getBoundingBox();
		Predicate<Coordinate> inBounds = c -> c.getX() >= bounds[0] 
									&& c.getX() <= bounds[2] 
									&& c.getY() >= bounds[1]
									&& c.getY() <= bounds[3];
		return this.randCords.stream().filter(inBounds).collect(Collectors.toList());
	}
	
	
	public static String tileToJSONString(TileRef tile) {
		JsonObject jo = new JsonObject(); 
		jo.addProperty("x", tile.getX());
		jo.addProperty("y", tile.getX());
		jo.addProperty("imageID", tile.getX());
		jo.addProperty("rSet", tile.getX());
		return jo.toString();
	}
	
	
	public static void main(String[] args) {
		// Rough Shayrat bounding box
		//new GenFakeLogs(new Coordinate(36.939168, 34.498387), new Coordinate(36.954446, 34.488855)).generateLog();

	}

}
