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

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.SNSEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.leidoslabs.holeshot.analytics.common.client.AnalyticsClient;
import com.leidoslabs.holeshot.analytics.common.client.AnalyticsURLBuilder;
import com.leidoslabs.holeshot.analytics.common.model.CacheList;
import com.leidoslabs.holeshot.analytics.common.model.ScoredTile;
import com.leidoslabs.holeshot.analytics.common.model.UserTileRef;
import com.leidoslabs.holeshot.analytics.common.model.aoi.AOIElement;
import com.leidoslabs.holeshot.analytics.common.model.aoi.UserAOI;
import com.leidoslabs.holeshot.catalog.v1.CatalogEntry;
import com.leidoslabs.holeshot.credentials.HoleshotCredentials;
import com.leidoslabs.holeshot.elt.tileserver.TileRef;
import com.leidoslabs.holeshot.elt.tileserver.TileserverImage;
import com.leidoslabs.holeshot.tileserver.v1.TilePyramidDescriptor;
import com.leidoslabs.holeshot.tileserver.v1.TileServerClient;
import com.leidoslabs.holeshot.tileserver.v1.TileServerClientBuilder;
import org.image.common.geojson.GeoJsonModule;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryCollection;
import org.locationtech.jts.operation.union.CascadedPolygonUnion;
import org.locationtech.spatial4j.context.jts.JtsSpatialContext;
import org.locationtech.spatial4j.shape.jts.JtsGeometry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.geom.Point2D;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class CreateCacheList implements RequestHandler<SNSEvent, Object> {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(CreateCacheList.class);
	private static final int periodHours = System.getenv("PERIOD_HOURS") != null ? Integer.parseInt(System.getenv("PERIOD_HOURS")): 24;
	private final Map<String, TileserverImage> allImages;
	private final Map<String, List<UserTileRef>> toCache;
    private final AnalyticsClient client;
	private final TileServerClient tsClient;
    private boolean writeKML = false;
	
	// aoi/image intersections, mostly for debugging purposes
	private final List<Geometry> intersections;

	public CreateCacheList(boolean writeKML) {
		this();
		this.writeKML = writeKML;
	}

	public CreateCacheList() {
		allImages = new HashMap<>();
		toCache = new HashMap<>();
		intersections = new ArrayList<Geometry>();
        ObjectMapper mapper = new ObjectMapper();
		GeoJsonModule gjsonMod = new GeoJsonModule();
		mapper.registerModules(gjsonMod);
		client = new AnalyticsClient(mapper);
		this.tsClient = TileServerClientBuilder
				.forEndpoint(AnalyticsURLBuilder.getTileserverBaseUrl(false))
				.withCredentials(HoleshotCredentials.getApplicationDefaults())
				.withDefaultMaxPerRoute(Integer.MAX_VALUE)
				.withMaxTotal(Integer.MAX_VALUE).build();
	} 
	
	
	
	@Override
	public Object handleRequest(SNSEvent event, Context context) {		
		String userID = event.getRecords().get(0).getSNS().getMessage();
		LOGGER.debug("Fetching AOIs for " + userID);
		
		try {
			long oneDayPast = System.currentTimeMillis() - (1000 * 60 * 60 * periodHours);
			List<CacheList> existingLists = client.getCacheLists(userID, oneDayPast);
			if (existingLists.size() == 0) {
				List<UserAOI> aois = client.getAOIs(oneDayPast, userID);
				for(UserAOI aoi: aois) {
					LOGGER.debug("Computing tiles for  " + userID);
					computeTiles(aoi);
					if (writeKML) {
						writeKMLs(aoi);
					}
				}
				List<CacheList> lists = getCacheLists();
				LOGGER.debug("Updating backend cache lists");
				client.postCacheList(lists);
			} else {
				LOGGER.debug("No new lists for user " + userID);
			}
		} catch (IOException e) {
			LOGGER.error("Error creating cache list for " + userID);
			e.printStackTrace();
		}

		return null;
	}
	

	/**
	 * Given a UserAOI, find the intersections between these images and the images in tileserver catalog, and for 
	 * each of these intersections, determine a 'reasonable' set of tiles from the image's tile pyramid that the user
	 * should cache. These tiles are added to toCache
	 * @param aoi
	 * @return
	 * @throws IOException
	 */
	private void computeTiles(UserAOI aoi) throws IOException {
		JtsSpatialContext ctx = JtsSpatialContext.GEO;
		
		CascadedPolygonUnion unioner = new CascadedPolygonUnion(aoi.getElements().stream().map(e -> e.getGeometry()).collect(Collectors.toList()));
		Geometry union = unioner.union();
		
		List<TileserverImage> images = getTileserverImages(union);
				
		for (AOIElement e : aoi.getElements()) {
			JtsGeometry elementGeom = new JtsGeometry(e.getGeometry(), ctx, false, false);
			for (TileserverImage image: images) {
				JtsGeometry imageGeom = new JtsGeometry(image.getTopTile().getGeodeticBounds(), ctx, false, false);
				if (imageGeom.relate(elementGeom).intersects()) {
					Geometry intersection = imageGeom.getGeom().intersection(elementGeom.getGeom());
					if (intersection instanceof GeometryCollection) {
						int numGeoms = ((GeometryCollection) intersection).getNumGeometries();
						for (int i = 0; i < numGeoms; i++) {
							Geometry intersectPart = ((GeometryCollection) intersection).getGeometryN(i);
							JtsGeometry intersect = new JtsGeometry(intersectPart, ctx, false, false);
							getTileIntersections(image, e, intersect, aoi.getUserID());
						}
					} else {
						JtsGeometry intersect = new JtsGeometry(intersection, ctx, false, false);
						getTileIntersections(image, e, intersect, aoi.getUserID());
					}
				}
			}	
		}
	}
	
	private void getTileIntersections(TileserverImage image, AOIElement element, JtsGeometry intersection, String userID){
		this.intersections.add(intersection.getGeom());
		int rset = getClosestRset(image, intersection.getArea(JtsSpatialContext.GEO), image.getMaxRLevel());
		getTileIntersections(image, element, intersection, userID, rset);
	}
	
	/**
	 * For a given rset, image, and intersection calculate the tiles in that image that span this intersection.
	 * If the related AOIElement has high importance (we decide to 
	 * @param image
	 * @param element
	 * @param intersection
	 * @param userID
	 * @param rset
	 */
	private void getTileIntersections(TileserverImage image, AOIElement element, JtsGeometry intersection, String userID, int rset){
		//rset = Math.max(0, rset - 1);
		TileRef rsetTile = new TileRef(image, rset, 0, 0);
		
		int numCols = rsetTile.getRowsAndColumnsForRset(rset).width;
		int numRows = rsetTile.getRowsAndColumnsForRset(rset).height;
		int imageWidth = image.getR0ImageWidth();
		int imageHeight = image.getR0ImageHeight();
		
		List<Point2D> imgPoints = Arrays.stream(intersection.getGeom().getCoordinates())
									   .map(cord -> image.getCameraModel().worldToImage(new Coordinate(cord.getX(), cord.getY(), 0.0)))
									   .collect(Collectors.toList());
		int minTileX = Integer.MAX_VALUE, minTileY = Integer.MAX_VALUE;
		int maxTileX = -1, maxTileY = -1;
		
		String metadataUrl = AnalyticsURLBuilder.getTileserverMetadataUrl(image.getCollectionID(), image.getTimestamp(), false);
		String key = userID + "#" + metadataUrl;
		List<UserTileRef> toAdd;
		if (toCache.containsKey(key)) {
			toAdd = toCache.get(key);
		} else {
			toAdd = new ArrayList<>();
			toCache.put(key, toAdd);
		}
		
		
		for (Point2D point : imgPoints) {
			//point.getX() / 
			int tileX = (int) ((point.getX() / imageWidth) * numCols);
			int tileY = (int) ((point.getY() / imageHeight) * numRows);
			minTileX = Math.min(tileX, minTileX);
			minTileY = Math.min(tileY, minTileY);
			maxTileX = Math.max(tileX, maxTileX);
			maxTileY = Math.max(tileY, maxTileY);
		}
		for (int y = minTileY; y <= maxTileY; y++) {
			for (int x = minTileX; x <= maxTileX; x++) {
				UserTileRef tileToCache = new UserTileRef(image, rset, x, y, userID);
				toAdd.add(tileToCache);
			}
		}
		if (element.getImportance() > 20 && rset > 0) {
			getTileIntersections(image, element, intersection, userID, rset - 1);
		}
		
	}
	
	/**
	 * Given some intersection area, find the rset of an image that has a the closest tile area
	 * @param image
	 * @param intersectionArea
	 * @param maxRset
	 * @return 
	 */
	private int getClosestRset(TileserverImage image, double intersectionArea, int maxRset) {
		JtsSpatialContext ctx = JtsSpatialContext.GEO;
		double minDist = Double.MAX_VALUE;
		int minIndex = -1;
		for (int i = 0; i <= maxRset; i++) {
			//double tileArea = new TileRef(image, i, 0, 0).getGeodeticBounds().getArea();
			double tileArea = new JtsGeometry(new TileRef(image, i, 0, 0).getGeodeticBounds(), ctx, false, false).getArea(ctx);
			double dist = Math.abs(tileArea - intersectionArea);
			if (dist < minDist) {
				minDist = dist;
				minIndex = i;
			}
		}
		return minIndex;
	}
	
	/**
	 * @param union Geometric union of AOIElement geometries 
	 * @return List of TileserverImages in our catalog that intersect union
	 */
	private List<TileserverImage> getTileserverImages(Geometry union){
		List<TileserverImage> results = new ArrayList<>();
		try{
			for (CatalogEntry entry: client.getCatalogEntries(union)) {
				if (!allImages.containsKey(entry.getMetadataURL().toString())) {
					TilePyramidDescriptor metadata = tsClient.getMetadata(entry.getImageId(), entry.getTimestamp());
					TileserverImage img = new TileserverImage(metadata);
					results.add(img);
                    String metadataUrl = AnalyticsURLBuilder.getTileserverMetadataUrl(entry.getImageId(), entry.getTimestamp(), false);
					allImages.put(metadataUrl, img);
				} else {
					results.add(allImages.get(entry.getMetadataURL().toString()));
				}
			};
		} catch (IOException e) {
			e.printStackTrace();
		}
		return results;
		
	}
	
	/**
	 * Converts toCache to a list of cache lists
	 * @return list of cache lsits
	 */
	private List<CacheList> getCacheLists(){
		List<CacheList> result = new ArrayList<>();
		long date = System.currentTimeMillis();
		for (Map.Entry<String, List<UserTileRef>> entry: toCache.entrySet()) {
			String[] keyParts = entry.getKey().split("#");
			String userID = keyParts[0];
			String image = keyParts[1];
			List<ScoredTile> tiles = entry.getValue().stream().map(utr -> new ScoredTile(utr.getRset(), utr.getX(), utr.getY(), 1)).collect(Collectors.toList());
			result.add(new CacheList(tiles, image, date, userID));
		}
		return result;
	}
	
/**
    old method when we just had a collection of UTrs
	private List<CacheList> partitionUserTileRefs(Collection<UserTileRef> cacheList) {
		List<CacheList> result = new ArrayList<>();
		long date = System.currentTimeMillis();

		Map<String, Map<TileserverImage, List<UserTileRef>>> partitioned;
		partitioned = cacheList.stream()
				.collect(Collectors.groupingBy(utr -> utr.getUserID()))
				.entrySet().stream()
				.map(e -> new SimpleEntry<String, Map<TileserverImage, List<UserTileRef>>>
				(e.getKey(), e.getValue().stream().collect(Collectors.groupingBy(utr -> utr.getImage()))))
				.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

		for (Map.Entry<String, Map<TileserverImage, List<UserTileRef>>> e1 : partitioned.entrySet()) {
			for (Map.Entry<TileserverImage, List<UserTileRef>> e2: e1.getValue().entrySet()) {
				List<ScoredTile> tiles = e2.getValue().stream().map(utr -> new ScoredTile(utr.getRset(), utr.getX(), utr.getY(), 1)).collect(Collectors.toList());
				result.add(new CacheList(tiles, e2.getKey().toString(), date, e1.getKey()));
			}
		}
		return result;
	}
*/
	
	/**
	 * Write 4KML files, one for aoi elements, one for image bounds, one for intersections, one for cache_tiles
	 * @param aoi
	 */
	private void writeKMLs(UserAOI aoi) {
		List<Geometry> imageTiles = allImages.values().stream()
										.map(image -> image.getTopTile().getGeodeticBounds())
										.collect(Collectors.toList());
		List<UserTileRef> allUtrs = new ArrayList<>();
		toCache.values().forEach(allUtrs::addAll);
		List<Geometry> cacheTiles = allUtrs.stream().map(tile -> tile.getGeodeticBounds()).collect(Collectors.toList());
		List<Geometry> aoiTiles = aoi.getElements().stream().map(element -> element.getGeometry()).collect(Collectors.toList());
		
		KMLUtils.writeKML(imageTiles, "image_tiles-new", "FF0000FF");
		KMLUtils.writeKML(cacheTiles, "cache_tiles-new", "FFFF0000");
		KMLUtils.writeKML(this.intersections, "intersections-new", "FF00FF00");
		
		KMLUtils.writeKML(aoiTiles, "aoi-new", "FF00FFFF");
		
	}
	

	/**
	 * For local testing
	 * @param args
	 */
	public static void main(String[] args) {
		SNSEvent.SNS sns = new SNSEvent.SNS();
		sns.setMessage("lobugliop");
		SNSEvent.SNSRecord record = new SNSEvent.SNSRecord();
		record.setSns(sns);
		SNSEvent event = new SNSEvent();
		event.setRecords(Arrays.asList(record));
		new CreateCacheList(true).handleRequest(event, null);
	}
	

}
