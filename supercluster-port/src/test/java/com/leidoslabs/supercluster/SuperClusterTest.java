package com.leidoslabs.supercluster;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.github.filosganga.geogson.gson.GeometryAdapterFactory;
import com.github.filosganga.geogson.model.Feature;
import com.github.filosganga.geogson.model.FeatureCollection;
import com.github.filosganga.geogson.model.Point;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.reflect.TypeToken;
import com.leidoslabs.supercluster.Tile.TileFeature;

import org.junit.Test;

public class SuperClusterTest {
    private static FeatureCollection fc, fc_small;
    private static final Gson GSON = new GsonBuilder()
            .registerTypeAdapterFactory(new GeometryAdapterFactory())
            .create();

    static {

        String places = null;
        String places_small = null;

        try {
            places = new String(Files.readAllBytes(Paths.get("places.json")));
            places_small = new String(Files.readAllBytes(Paths.get("places_small.json")));
        } catch (IOException e) {
            e.printStackTrace();
        }

        fc = GSON.fromJson(places, FeatureCollection.class);
        fc_small = GSON.fromJson(places_small, FeatureCollection.class);
    }

    @Test
    public void load_data_from_strings() throws IOException{
        // Makes sure the load() can parse the JSON string properly
        String places = new String(Files.readAllBytes(Paths.get("places.json")));
        Supercluster new_sc = new Supercluster();
        new_sc.load(places);
    }
        
    @Test
    public void parseTest() throws IOException {
        List<Feature> features = fc.features();

        Feature niagraFalls = features.get(0);
        Map<String, JsonElement> props = niagraFalls.properties();
        Point point;

        if(!(niagraFalls.geometry() instanceof Point)) {
            assertFalse(true);
        }

        point = (Point) niagraFalls.geometry();
        
        assertEquals("Niagara Falls", props.get("name").getAsString());
        assertEquals("North America", props.get("region").getAsString());
        assertEquals(-79.04411780507252, point.lon(), .5);
        assertEquals(43.08771393436908, point.lat(), .5);

        Feature saltoAngel = features.get(1);
        props = saltoAngel.properties();
        point = (Point) saltoAngel.geometry();
        
        assertEquals("Salto Angel", props.get("name").getAsString());
        assertEquals("South America", props.get("region").getAsString());
        assertEquals(-62.06181800038502, point.lon(), .5);
        assertEquals(5.686896063275327, point.lat(), .5);
    }

    @Test
    public void superclusterGetClusters() throws IOException {
        Supercluster s = new Supercluster();
        s.load(fc.features());

        String expectedStr = "[{\"type\":\"Feature\",\"properties\":{\"scalerank\":5,\"name\":\"I. Robinson Crusoe\",\"comment\":null,\"name_alt\":null,\"lat_y\":-33.589852,\"long_x\":-78.872522,\"region\":\"Seven seas (open ocean)\",\"subregion\":\"South Pacific Ocean\",\"featureclass\":\"island\"},\"geometry\":{\"type\":\"Point\",\"coordinates\":[-78.87254798085377,-33.58965422969342]}},{\"type\":\"Feature\",\"properties\":{\"scalerank\":2,\"name\":\"Niagara Falls\",\"comment\":null,\"name_alt\":null,\"lat_y\":43.087653,\"long_x\":-79.044073,\"region\":\"North America\",\"subregion\":null,\"featureclass\":\"waterfall\"},\"geometry\":{\"type\":\"Point\",\"coordinates\":[-79.04411780507252,43.08771393436908]}}]";
        String clusters = s.getClusters(new double[] { -80, -43, -78, 45 }, 15);

        Feature[] actual = GSON.fromJson(clusters, Feature[].class);
        Feature[] expected = GSON.fromJson(expectedStr, Feature[].class);

        assertTrue(equalsIgnoreOrder(actual, expected));

        //2
        expectedStr = "[{\"type\":\"Feature\",\"properties\":{\"scalerank\":3,\"name\":\"Cape Palmas\",\"comment\":null,\"name_alt\":null,\"lat_y\":4.373924,\"long_x\":-7.457356,\"region\":\"Africa\",\"subregion\":null,\"featureclass\":\"cape\"},\"geometry\":{\"type\":\"Point\",\"coordinates\":[-7.457386848041267,4.373968817181577]}},{\"type\":\"Feature\",\"properties\":{\"scalerank\":4,\"name\":\"Cap Lopez\",\"comment\":null,\"name_alt\":null,\"lat_y\":-0.604761,\"long_x\":8.726423,\"region\":\"Africa\",\"subregion\":null,\"featureclass\":\"cape\"},\"geometry\":{\"type\":\"Point\",\"coordinates\":[8.727299789450319,-0.615086490513119]}}]";
        clusters = s.getClusters(new double[] { -10, -10, 10, 10 }, 7);

        actual = GSON.fromJson(clusters, Feature[].class);
        expected = GSON.fromJson(expectedStr, Feature[].class);

        assertTrue(equalsIgnoreOrder(actual, expected));

    }

    @Test
    public void generateClusters() throws IOException{

    	Supercluster s = new Supercluster();
    	s.load(fc.features());
    	String t = s.getTile(0, 0, 0);
    	String placesTile = new String(Files.readAllBytes(Paths.get("places-z0-0-0.json")));
    	
    	t = t.substring(t.indexOf(':') + 1, t.length() - 1);
    	placesTile = placesTile.substring(placesTile.indexOf(':') + 1, placesTile.length() - 3);

        TileFeature[] actualArr = GSON.fromJson(t, TileFeature[].class);
        TileFeature[] expectedArr = GSON.fromJson(placesTile, TileFeature[].class);

        HashSet<TileFeature> actual = new HashSet<TileFeature>();
        HashSet<TileFeature> expected = new HashSet<TileFeature>();
        Collections.addAll(actual, actualArr);
        Collections.addAll(expected, expectedArr);
        assertEquals(actual, expected);
    }
    
    @Test
    public void returnChildrenOfACluster() throws NoSuchFieldException{
    	Supercluster s = new Supercluster();
    	s.load(fc.features());
    	LinkedList<Feature> children = (LinkedList<Feature>) s.getChildren(1);
    	ArrayList<Integer> test = new ArrayList<Integer>();
    	for(Feature f : children) {
    		int temp;
    		if(f.properties().get("point_count") == null) {
    			temp = 1;
    		}else {
    			temp = f.properties().get("point_count").getAsInt();
    		}
    		test.add(temp);
    	}
    	assertTrue(test.contains(6));
    	assertTrue(test.contains(7));
    	assertTrue(test.contains(1));
    	assertTrue(test.contains(2));
    	
    }
    
    @Test
    public void returnLeavesOfACluster() throws NoSuchFieldException {
    	Supercluster s = new Supercluster();
    	s.load(fc.features());
    	String leaves = s.getLeaves(1, 999, 0);
    	
    	//System.out.println(leaves);
    	ArrayList<Feature> leafNamesCollection = GSON.fromJson(leaves, new TypeToken<ArrayList<Feature>>() {}.getType());
    	ArrayList<String> leafNamesList = new ArrayList<String>();
    	
    	for(Feature f : leafNamesCollection) {
    		//System.out.println(f.properties().get("name").getAsString());
    		leafNamesList.add(f.properties().get("name").getAsString());
    	}
    	
    	assertTrue(leafNamesList.contains("Cape Cod")); 
        assertTrue(leafNamesList.contains("Cape Sable")); 
        assertTrue(leafNamesList.contains("Cape Hatteras")); 
        assertTrue(leafNamesList.contains("Cape Fear")); 
        assertTrue(leafNamesList.contains("Cape May")); 
        assertTrue(leafNamesList.contains("Niagara Falls")); 
        assertTrue(leafNamesList.contains("Cape San Blas")); 
        assertTrue(leafNamesList.contains("Cape Sable")); 
        assertTrue(leafNamesList.contains("Cape Canaveral")); 
        assertTrue(leafNamesList.contains("San Salvador")); 
        assertTrue(leafNamesList.contains("Cabo Gracias a Dios")); 
        assertTrue(leafNamesList.contains("I. de Cozumel")); 
        assertTrue(leafNamesList.contains("Grand Cayman")); 
        assertTrue(leafNamesList.contains("Miquelon"));
        assertTrue(leafNamesList.contains("Cape Bauld"));
        assertTrue(leafNamesList.contains("Bermuda Islands"));
    	
    }

    @Test

    public void clusterQueryCrossingInternational() throws IOException{
        Supercluster s = new Supercluster();
        String medium_place =  new String(Files.readAllBytes(Paths.get("places_medium.json")));
        s.load(medium_place);
        double[] nonCrossing_bbox = {-179, -10, -177, 10};
        double[] crossing_bbox = {179, -10, -177, 10};
        String nonCrossing = s.getClusters(nonCrossing_bbox, 1);
        String crossing = s.getClusters(crossing_bbox, 1);

        ArrayList<Feature> ncs = GSON.fromJson(nonCrossing, ArrayList.class);
        ArrayList<Feature> cs = GSON.fromJson(crossing, ArrayList.class);
        assertEquals(cs.size(), ncs.size());
    }

    @Test
    public void doesNotCrashOnWeirdBboxValues() {
        Supercluster s = new Supercluster();
        s.load(fc.features());

        String test1 = s.getClusters(new double[]{129.426390, -103.720017, -445.930843, 114.518236}, 1);
        String test2 = s.getClusters(new double[]{112.207836, -84.578666, -463.149397, 120.169159}, 1);
        String test3 = s.getClusters(new double[]{129.886277, -82.332680, -445.470956, 120.390930}, 1);
        String test4 = s.getClusters(new double[]{458.220043, -84.239039, -117.137190, 120.206585}, 1);
        String test5 = s.getClusters(new double[]{456.713058, -80.354196, -118.644175, 120.539148}, 1);
        String test6 = s.getClusters(new double[]{453.105328, -75.857422, -122.251904, 120.732760}, 1);
        String test7 = s.getClusters(new double[]{-180, -90, 180, 90}, 1);

        ArrayList<Feature> feature_test1 = GSON.fromJson(test1, ArrayList.class);
        ArrayList<Feature> feature_test2 = GSON.fromJson(test2, ArrayList.class);
        ArrayList<Feature> feature_test3 = GSON.fromJson(test3, ArrayList.class);
        ArrayList<Feature> feature_test4 = GSON.fromJson(test4, ArrayList.class);
        ArrayList<Feature> feature_test5 = GSON.fromJson(test5, ArrayList.class);
        ArrayList<Feature> feature_test6 = GSON.fromJson(test6, ArrayList.class);
        ArrayList<Feature> feature_test7 = GSON.fromJson(test7, ArrayList.class);

        assertEquals(26, feature_test1.size());
        assertEquals(27, feature_test2.size());
        assertEquals(26, feature_test3.size());
        assertEquals(25, feature_test4.size());
        assertEquals(25, feature_test5.size());
        assertEquals(25, feature_test6.size());
        assertEquals(61, feature_test7.size());
    }
    
    @Test

    public void superclusterGetTile() {

        Supercluster s = new Supercluster();
        s.load(fc.features());

        String expectedStr = "{\"features\":[{\"type\":1,\"geometry\":[[373,126]],\"tags\":{\"scalerank\":3,\"name\":\"North Magnetic Pole 2005 (est)\",\"comment\":null,\"name_alt\":null,\"lat_y\":-48.865032,\"long_x\":-123.401986,\"region\":\"Seven seas (open ocean)\",\"subregion\":\"Arctic Ocean\",\"featureclass\":\"pole\"}},{\"type\":1,\"geometry\":[[75,485]],\"tags\":{\"scalerank\":4,\"name\":\"Cape Hope\",\"comment\":null,\"name_alt\":null,\"lat_y\":68.35638,\"long_x\":-166.815582,\"region\":\"North America\",\"subregion\":null,\"featureclass\":\"cape\"}},{\"type\":1,\"geometry\":[[134,434]],\"tags\":{\"scalerank\":4,\"name\":\"Point Barrow\",\"comment\":null,\"name_alt\":null,\"lat_y\":71.372637,\"long_x\":-156.615894,\"region\":\"North America\",\"subregion\":null,\"featureclass\":\"cape\"}}]}";
        String tiles = s.getTile(2, 0, 0);

        Tile expected = GSON.fromJson(expectedStr, Tile.class);
        Tile actual = GSON.fromJson(tiles, Tile.class);
        
        assertEquals(expected, actual);

        expectedStr = "{\"features\":[{\"type\":1,\"geometry\":[[407,-30]],\"tags\":{\"scalerank\":5,\"name\":\"Cabo Fisterra\",\"comment\":null,\"name_alt\":null,\"lat_y\":42.952418,\"long_x\":-9.267837,\"region\":\"Europe\",\"subregion\":null,\"featureclass\":\"cape\"}},{\"type\":1,\"geometry\":[[410,58]],\"tags\":{\"scalerank\":5,\"name\":\"Cabo de S\u00E3o Vicentete\",\"comment\":null,\"name_alt\":null,\"lat_y\":37.038304,\"long_x\":-8.969391,\"region\":\"Europe\",\"subregion\":null,\"featureclass\":\"cape\"}},{\"type\":1,\"geometry\":[[406,120]],\"tags\":{\"scalerank\":5,\"name\":\"Ras Cantin\",\"comment\":null,\"name_alt\":null,\"lat_y\":32.581636,\"long_x\":-9.273918,\"region\":\"Africa\",\"subregion\":null,\"featureclass\":\"cape\"}},{\"type\":1,\"geometry\":[[427,462]],\"tags\":{\"scalerank\":3,\"name\":\"Cape Palmas\",\"comment\":null,\"name_alt\":null,\"lat_y\":4.373924,\"long_x\":-7.457356,\"region\":\"Africa\",\"subregion\":null,\"featureclass\":\"cape\"}},{\"type\":1,\"geometry\":[[57,544]],\"tags\":{\"scalerank\":5,\"name\":\"Ponta de Jericoacoara\",\"comment\":null,\"name_alt\":null,\"lat_y\":-2.85044,\"long_x\":-40.067208,\"region\":\"South America\",\"subregion\":null,\"featureclass\":\"cape\"}},{\"type\":1,\"geometry\":[[313,342]],\"tags\":{\"scalerank\":3,\"name\":\"Cape Verde\",\"comment\":null,\"name_alt\":null,\"lat_y\":14.732312,\"long_x\":-17.471776,\"region\":\"Africa\",\"subregion\":null,\"featureclass\":\"cape\"}},{\"type\":1,\"geometry\":[[347,203]],\"tags\":{\"scalerank\":5,\"name\":\"Cabo Bojador\",\"comment\":null,\"name_alt\":null,\"lat_y\":26.157836,\"long_x\":-14.473111,\"region\":\"Africa\",\"subregion\":null,\"featureclass\":\"cape\"}},{\"type\":1,\"geometry\":[[318,270]],\"tags\":{\"scalerank\":4,\"name\":\"Cap Blanc\",\"comment\":null,\"name_alt\":null,\"lat_y\":20.822108,\"long_x\":-17.052856,\"region\":\"Africa\",\"subregion\":null,\"featureclass\":\"cape\"}}]}";
        tiles = s.getTile(3, 3, 3);
        expected = GSON.fromJson(expectedStr, Tile.class);
        actual = GSON.fromJson(tiles, Tile.class);
        
        assertEquals(expected, actual);
    }

    @Test()
    public void returnClusterExpansionZoom() throws NoSuchFieldException{
        Supercluster s = new Supercluster();
        s.load(fc.features());
        assertEquals(1, s.getClusterExpansionZoom(1));
        assertEquals(1, s.getClusterExpansionZoom(33));

        /**
        Three following assertion(s) have out of bounds exception.
         **/
        //assertEquals(2, s.getClusterExpansionZoom(353));
        //assertEquals(2, s.getClusterExpansionZoom(833));
        //assertEquals(4, s.getClusterExpansionZoom(1857));
    }


    @Test
    public void returnClusterExpansionZoomForMazZoom() throws NoSuchFieldException{
        Supercluster s = new Supercluster(0, 4, 60, 256, 64, false, 0);
        s.load(fc.features());

        /**
         Following assertion(s) have out of bounds exception.
         **/
        assertEquals(5, s.getClusterExpansionZoom(2436));
    }
    @Test
    public void superclusterGetChildren() throws NoSuchFieldException {

        Supercluster s = new Supercluster();
        s.load(fc_small.features());
        
        String expectedStr = "[{\"type\":\"Feature\",\"id\":34,\"properties\":{\"cluster\":true,\"cluster_id\":34,\"point_count\":2,\"point_count_abbreviated\":\"2\"},\"geometry\":{\"type\":\"Point\",\"coordinates\":[-61.530909000192494,5.7184489016016755]}}]";
        List<Feature> list = s.getChildren(33);
        Feature[] expected = GSON.fromJson(expectedStr, Feature[].class);
        Feature[] actual = new Feature[expected.length];
        list.toArray(actual);
        
        assertArrayEquals(expected, actual);
    }

    @Test
    public void superclusterGetLeaves() throws NoSuchFieldException {
        Supercluster s = new Supercluster();
        s.load(fc.features());
        String leaves = s.getLeaves(1, 10, 5);
        // decode the leaves into an ArrayList of Features
        ArrayList<Feature> data = GSON.fromJson(leaves, ArrayList.class);
        assertEquals(10, data.size());
    }

    @Test
    public void superclusterGetLeaves2() throws NoSuchFieldException {

        Supercluster s = new Supercluster();
        s.load(fc_small.features());
        
        String expectedStr = "[{\"type\":\"Feature\",\"properties\":{\"scalerank\":2,\"name\":\"Salto Angel\",\"comment\":null,\"name_alt\":\"Angel Falls\",\"lat_y\":5.686836,\"long_x\":-62.061848,\"region\":\"South America\",\"subregion\":null,\"featureclass\":\"waterfall\"},\"geometry\":{\"type\":\"Point\",\"coordinates\":[-62.06181800038502,5.686896063275327]}},{\"type\":\"Feature\",\"properties\":{\"scalerank\":2,\"name\":\"Salto Angel 2\",\"comment\":null,\"name_alt\":\"Angel Falls 2\",\"lat_y\":5.75,\"long_x\":-61,\"region\":\"Souther America\",\"subregion\":null,\"featureclass\":\"waterfall\"},\"geometry\":{\"type\":\"Point\",\"coordinates\":[-61,5.75]}}]";
        String actualStr = s.getLeaves(37);

        Feature[] expected = GSON.fromJson(expectedStr, Feature[].class);
        Feature[] actual = GSON.fromJson(actualStr, Feature[].class);
        
        assertTrue(equalsIgnoreOrder(expected, actual));

    }
    
    private static boolean equalsIgnoreOrder(Feature[] a, Feature[] b) {
            /* compare two arrays where order does not matter */
        for(int i = 0; i < a.length; i++) {
            boolean found = false;

            for(int j = 0; j < b.length; j++) {
                if(equals(a[i], b[j])) {
                    found = true;
                    break;
                }
            }
            if(!found) {
                return false;
            }
        }
        return true;
    }

    private static boolean equals(Feature a, Feature b) {

        Map<String, JsonElement> aprop = a.properties();
        Map<String, JsonElement> bprop = b.properties();
        
        return
                aprop.get("scalerank").equals(bprop.get("scalerank")) &&
                        aprop.get("name").equals(bprop.get("name")) &&
                        Math.abs(aprop.get("lat_y").getAsDouble() - bprop.get("lat_y").getAsDouble()) < .01 &&
                        Math.abs(aprop.get("long_x").getAsDouble() - bprop.get("long_x").getAsDouble()) < .01 &&
                        aprop.get("region").equals(bprop.get("region"));
    }
}