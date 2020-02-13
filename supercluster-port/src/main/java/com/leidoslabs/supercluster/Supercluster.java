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
package com.leidoslabs.supercluster;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import com.github.filosganga.geogson.gson.GeometryAdapterFactory;
import com.github.filosganga.geogson.model.Feature;
import com.github.filosganga.geogson.model.FeatureCollection;
import com.github.filosganga.geogson.model.Point;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.leidoslabs.util.KDBush;

import org.locationtech.jts.geom.Coordinate;

/**
 * Representation of a Supercluster: a list of KDBushes of Clusters, and a list of Features
 */
public class Supercluster {

    private static final Function<Cluster, Coordinate> GET_COORDINATE_FUNCTION = (cluster) -> new Coordinate(cluster.x, cluster.y);
    private static final Gson GSON = new GsonBuilder().registerTypeAdapterFactory(new GeometryAdapterFactory()).create();

    private int minZoom, maxZoom, radius, extent, nodeSize, reduce;
    private boolean log;
    private List<KDBush<Cluster>> trees;
    private ArrayList<Feature> points;

    // constructor
    public Supercluster() {
        this(0, 16, 40, 512, 64, false, 0); // I am not sure what rdc is
    }

    /**
     * Initialize Supercluster
     * @param minZ min zoom
     * @param maxZ max zoom 
     * @param rd radius
     * @param ext tiles spanned?
     * @param ns nodesize, unused
     * @param lg log
     * @param rdc reduce
     */
    public Supercluster(int minZ, int maxZ, int rd, int ext, int ns, boolean lg, int rdc){
        this.minZoom  = minZ;
        this.maxZoom = maxZ;
        this.radius = rd;
        this.extent = ext;
        this.nodeSize = ns;
        this.log = lg;
        this.reduce = rdc;
        // some map props => props (unimplemented yet)

        trees = new ArrayList<KDBush<Cluster>>(maxZoom + 2);

        // fill trees up so we can do .set() later
        for(int i = 0; i < maxZoom + 2; i++) {
            trees.add(null);
        }
    }

    /**
     * loads json representing a FeatureCollection into the supercluster
     * @param json
     * @return
     */
    public Supercluster load(String json) {

        FeatureCollection features;

        try {
            features = GSON.fromJson(json, FeatureCollection.class);
        } catch(JsonParseException e) {
            e.printStackTrace();
            return null;
        }
        
        return load(features.features());
    }

    /**
     * Loads in points to the supercluster
     * @param points
     * @return
     */
    public Supercluster load(List<Feature> points) {

        ArrayList<Cluster> clusters = new ArrayList<>();
        this.points = new ArrayList<>(points);

        // generate a cluster object for each point and index input points into a KD-tree
        for(int i = 0; i < points.size(); i++) {
            if(!(points.get(i).geometry() instanceof Point)) continue;
            clusters.add(createPointCluster((Point) points.get(i).geometry(), i));
        }

        trees.set(maxZoom + 1, new KDBush<Cluster>(clusters, GET_COORDINATE_FUNCTION));

        // cluster points on max zoom, then cluster the results on previous zoom, etc.;
        // results in a cluster hierarchy across zoom levels
        for(int z = maxZoom; z >= minZoom; z--) {
            clusters = cluster(clusters, z);
            trees.set(z, new KDBush<>(clusters, GET_COORDINATE_FUNCTION));
        }

        return this;
    }

    private ArrayList<Cluster> cluster(ArrayList<Cluster> clusters, int zoom) {

        ArrayList<Cluster> toReturn = new ArrayList<>();

        double r = (double) radius / (extent * Math.pow(2, zoom));
        // loop through each point

        for(int i = 0; i < clusters.size(); i++) {
        	
        	
            Cluster p = clusters.get(i);

            // if we've already visited the point at this zoom level, skip it
            if(p.zoom <= zoom) continue;
            p.zoom = zoom;

            // find all nearby points
            KDBush<Cluster> tree = trees.get(zoom + 1);
            List<Cluster> neighbors = tree.within(p.x, p.y, r);

            int numPoints = p.numPoints;
            if(numPoints == 0) {
                numPoints = 1;
            }

            double wx = p.x * numPoints;
            double wy = p.y * numPoints;

            Map<String, JsonElement> clusterProperties = reduce != 0 ? p.properties: null;

            int id = (i << 5) + (zoom + 1);            

            for(Cluster b : neighbors) {

                if(b.zoom <= zoom) continue;

                b.zoom = zoom; 

                int numPoints2 = b.numPoints;
                if(numPoints2 == 0) {
                    numPoints2 = 1;
                }

                wx += b.x * numPoints2;
                wy += b.y * numPoints2;

                numPoints += numPoints2;

                b.parentId = id;


                if(reduce != 0) {
                    // TODO
                }
            }
            if(numPoints == 1) {
                toReturn.add(p);
            } else {
                p.parentId = id;
                toReturn.add(new Cluster(wx / numPoints, wy / numPoints, id, numPoints, clusterProperties));
            }
        }

        return toReturn;
    }

    /**
     * Returns a geojson-vt compatible tile JSON string, given a zoom, x and y
     * @param z
     * @param x
     * @param y
     * @return
     */
    public String getTile(int z, int x, int y) {

        KDBush<Cluster> tree = trees.get(limitZoom(z));
        
    	int z2 = (int)Math.pow(2, z);
        double p = (double) radius / extent;
        double top = (y - p) / z2;
        double bottom = (y + 1 + p) / z2;

        Tile tile = new Tile();

        addTileFeatures(tree.range((x - p) / z2, top, (x + 1 + p) / z2, bottom), x, y, z2, tile);

        if(x == 0) {
            addTileFeatures(tree.range(1 - p / z2, top, 1, bottom), z2, y, z2, tile);
        }

        if(x == z2 - 1) {
            addTileFeatures(tree.range(0, top, p / z2, bottom), -1, y, z2, tile);
        }
        
        return GSON.toJson(tile);
    }
    
    private void addTileFeatures(List<Cluster> ids, int x, int y, int z2, Tile t) {

		for(Cluster c : ids) {

            double modifiedX = Math.round(extent * (c.x * z2 - x));
            double modifiedY = Math.round(extent * (c.y * z2 - y));
            Map<String, JsonElement> props = c.numPoints > 0 ? getClusterProperties(c) : points.get(c.index).properties();
            
            t.features.add(new Tile.TileFeature(modifiedX, modifiedY, props));
		}
	}

    public String getLeaves(int clusterId) throws NoSuchFieldException {
        return getLeaves(clusterId, 10, 0);
    }

    public String getLeaves(int clusterId, int limit) throws NoSuchFieldException {
        return getLeaves(clusterId, limit, 0);
    }

    /*
        Returns a JSON array of clusters
    */
    public String getLeaves(int clusterId, int limit, int offset) throws NoSuchFieldException {

        List<Feature> leaves = new LinkedList<Feature>();

        appendLeaves(leaves, clusterId, limit, offset, 0);

        return GSON.toJson(leaves);
    }

    private int appendLeaves(List<Feature> result, int clusterId, int limit, int offset, int skipped) throws NoSuchFieldException {
        List<Feature> children = getChildren(clusterId);

        for (Feature child : children) {

            Map<String, JsonElement> props = child.properties();
            if(props != null && props.get("cluster") != null) {

                int pointCount = props.get("point_count").getAsInt();

                if(skipped + pointCount <= offset) {
                    skipped += pointCount;
                } else {
                    skipped = appendLeaves(result, props.get("cluster_id").getAsInt(), limit, offset, skipped);
                }
            } else if (skipped < offset) {
                skipped++;
            } else {
                result.add(child);
            }

            if (result.size() == limit) break;
        }

        return skipped;
    }

    /**
     * Returns a list of children of the specified cluster
     * @param clusterId
     * @return
     * @throws NoSuchFieldException
     */
    public List<Feature> getChildren(int clusterId) throws NoSuchFieldException {

        int originId = clusterId >> 5;
        int originZoom = clusterId % 32;
        double r = (double) radius / (extent * Math.pow(2, originZoom - 1));

        KDBush<Cluster> index = trees.get(originZoom);
        Cluster origin = index.getPoints().get(originId);
        
        List<Cluster> ids = index.within(origin.x, origin.y, r);

        LinkedList<Feature> children = new LinkedList<Feature>();

        for(Cluster c : ids) {

            if(c.parentId == clusterId) {
                children.add(c.numPoints > 0 ? getFeatureFromCluster(c) : points.get(c.index));
            }
        }

        // throws an error in the original code
        if(children.size() == 0) {
            throw new NoSuchFieldException("No cluster with the specified id");
        }

        return children;
    }

    /**
     * Returns the level at which a given cluster expands
     * @param clusterId
     * @return
     * @throws NoSuchFieldException
     */
    public int getClusterExpansionZoom(int clusterId) throws NoSuchFieldException {

        int clusterZoom = (clusterId % 32) - 1;

        while(clusterZoom <= this.maxZoom) {

            List<Feature> children = getChildren(clusterId);
            clusterZoom++;
            if(children.size() != 1) break;
            clusterId = children.get(0).properties().get("cluster_id").getAsInt();
        }

        return clusterZoom;
    }

    /**
     * Returns a list of clusters within a given bounding box
     * @param bbox
     * @param zoom
     * @return
     */
    public String getClusters(double[] bbox, int zoom) {
        ArrayList<Feature> jsons = getClustersHelper(bbox, zoom);
        return GSON.toJson(jsons);
    }

    private ArrayList<Feature> getClustersHelper(double[] bbox, int zoom){

        // this.points -> Feature
        ///
        double minLng = ((bbox[0] + 180) % 360 + 360) % 360 - 180;
        double minLat = Math.max(-90, Math.min(90, bbox[1]));
        double maxLng = bbox[2] == 180 ? 180 : ((bbox[2] + 180) % 360 + 360) % 360 - 180;
        double maxLat = Math.max(-90, Math.min(90, bbox[3]));

        if(bbox[2] - bbox[0] >= 360) {
            minLng = -180;
            maxLng = 180;
        }
        else if(minLng > maxLng) {
            double[] easternBbox = {minLng, minLat, 180, maxLat};
            double[] westernBbox = {-180, minLat, maxLng, maxLat};
            ArrayList<Feature> easternHem = getClustersHelper(easternBbox, zoom);
            ArrayList<Feature> westernHem = getClustersHelper(westernBbox, zoom);
            easternHem.addAll(westernHem);
            return easternHem;
        }

        KDBush<Cluster> tree = this.trees.get(limitZoom(zoom));

        // Fix KDBush and the range. Im not sure which
        List<Cluster> list_o_clusters = tree.range(lngX(minLng), latY(maxLat), lngX(maxLng), latY(minLat));

        ArrayList<Feature> clusters = new ArrayList<>();

        for(Cluster clstr : list_o_clusters) {
            clusters.add((clstr.numPoints != 0) ? getFeatureFromCluster(clstr) : this.points.get(clstr.index));
        }

        return clusters;
    }

    private Cluster createPointCluster(Point p, int id) {
        double x = p.lon(), y = p.lat();
        return new Cluster(lngX(x), latY(y), id, -1);
    }

    /*
        Equivalent of getClusterJSON
    */
    private Feature getFeatureFromCluster(Cluster c) {

        return Feature.builder()
            .withGeometry(Point.from(xLng(c.x), yLat(c.y)))
            .withProperties(getClusterProperties(c))
            .withId(c.index + "")
            .build();
    }

    /*
        Returns additional properties of a cluster
    */
    private Map<String, JsonElement> getClusterProperties(Cluster c) {
        
        int count = c.numPoints;
        String abbrev =
            count >= 10000 ? Math.round(count / 1000) + "k" :
            count >= 1000 ? Math.round(count / 100) / 10 + "k" : count + "";

        Map<String, JsonElement> props = c.properties;

        if(props == null) {
            props = new HashMap<String, JsonElement>();
        }
        
        props.put("cluster", new JsonPrimitive(true));
        props.put("cluster_id", new JsonPrimitive(c.index));
        props.put("point_count", new JsonPrimitive(count));
        props.put("point_count_abbreviated", new JsonPrimitive(abbrev));
        
        return props;
    }

    private int limitZoom(int z) {
    	return Math.max(this.minZoom, Math.min(z, this.maxZoom + 1));
    }

    private double lngX(double lng) {
        return lng / 360.00 + .5;
    }

    private double latY(double lat) {

        double sin = Math.sin(lat * Math.PI / 180.0);
        double y = (0.5 - 0.25 * Math.log((1 + sin) / (1 - sin)) / Math.PI);
        return y < 0 ? 0 : y > 1 ? 1 : y;

    }

    private double xLng(double x) {
        return (x - 0.5) * 360.0;
    }

    private double yLat(double y) {
        double y2 = (180.0 - y * 360.0) * Math.PI / 180.0;
        return 360 * Math.atan(Math.exp(y2)) / Math.PI - 90.0;
    }
}
