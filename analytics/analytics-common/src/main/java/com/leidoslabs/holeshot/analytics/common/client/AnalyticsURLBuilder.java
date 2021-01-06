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

package com.leidoslabs.holeshot.analytics.common.client;

import com.leidoslabs.holeshot.tileserver.v1.TilePyramidDescriptor;
import org.apache.http.client.utils.URIBuilder;

import java.net.URISyntaxException;
import java.security.InvalidParameterException;
import java.util.HashMap;
import java.util.Map;

public class AnalyticsURLBuilder {

    // URL For the image catalog api, i.e. catalog.leidoslabs.com/imagecatalog
    private static final String IMAGE_CATALOG_URL = "https://" + System.getenv().getOrDefault("IMAGE_CATALOG_URL", "catalog.leidoslabs.com/imagecatalog");
    // URL for analytics warehouse api, i.e. analytics-warehouse.leidoslabs.com
    private static String ANALYTICS_API_URL = "https://" +  System.getenv().getOrDefault("ANALYTICS_API_URL", "analytics-warehouse.leidoslabs.com");
    // URL for elasticsearch cluster, for building elasticsearch client
    private static final String ELASTICSEARCH_URL = System.getenv("ELASTICSEARCH_URL");
    // base URL for tileserver, i.e. tileserver.leidoslabs.com/tileserver
    private static final String TILESERVER_URL = System.getenv().getOrDefault("TILESERVER_URL", "tileserver.leidoslabs.com/tileserver");

    // analytics-warehouse endpoint paths
    private static final String SUMMARIES_SEARCH_PATH = "/summaries/search";
    private static final String SUMMARIES_PATH = "/summaries";
    private static final String TILESCORES_PATH = "/tilescores";
    private static final String AOI_PATH = "/aoi";
    private static final String USERS_PATH = "/users";
    private static final String UPDATE_LIST_PATH = "/updatelist";
    private static final String CACHE_LISTS_PATH = "/cachelist";

    /**
     * This is so callers don't have to make an inline map every time. In Java 9+ this is unnecessary
     * because there are prettier ways to build maps inline
     * @param params An even-length array of params
     * @return a hashmap with the K,V pairs built from params
     */
    private static Map<String, String> getMapFromParams(String[] params) {
        if(params.length % 2 != 0) throw new InvalidParameterException("Number of arguments for parameters must be even.");
        Map<String, String> paramMap = new HashMap<>();
        for(int i = 0; i < params.length - 1; i+=2) {
            paramMap.put(params[i], params[i+1]);
        }
        return paramMap;
    }

    private static String buildParameterizedUrl(String path, String[] params) throws URISyntaxException{
        URIBuilder ub = new URIBuilder(path);
        getMapFromParams(params).forEach(ub::addParameter);
        return ub.toString();
    }

    /**
     * Create a URL given a list of query parameters
     * @param params "Key1","Value1","Key2","Value2"
     * @return the complete URL with query parameters
     * @throws URISyntaxException
     */
    public static String getSummarySearchUrl(String... params) throws URISyntaxException {
        return buildParameterizedUrl(ANALYTICS_API_URL + SUMMARIES_SEARCH_PATH, params);
    }

    public static String getSummariesUrl(String... params) throws URISyntaxException {
        return buildParameterizedUrl(ANALYTICS_API_URL + SUMMARIES_PATH, params);
    }

    public static String getAoiUrl(String... params) throws URISyntaxException {
        return buildParameterizedUrl(ANALYTICS_API_URL + AOI_PATH, params);
    }

    public static String getUsersUrl(String... params) throws URISyntaxException {
        return buildParameterizedUrl(ANALYTICS_API_URL + USERS_PATH, params);
    }

    public static String getUpdateListUrl(String... params) throws URISyntaxException {
        return buildParameterizedUrl(ANALYTICS_API_URL + UPDATE_LIST_PATH, params);
    }

    public static String getCacheListsUrl(String... params) throws URISyntaxException {
        return buildParameterizedUrl(ANALYTICS_API_URL + CACHE_LISTS_PATH, params);
    }

    public static String getTilescoresUrl() {
        return ANALYTICS_API_URL + TILESCORES_PATH;
    }

    /**
     * The image catalog has its own dedicated client library, so this just returns the URL to use to build that client
     * @return The url to the catalog search api endpoint
     */
    public static String getImageCatalogUrl() {
        return IMAGE_CATALOG_URL;
    }

    public static String getElasticsearchUrl() {
        return ELASTICSEARCH_URL;
    }

    public static String getTileserverBaseUrl(boolean vpcInternal) {
        String tile_url_pattern = "%s://%s";
        String protocol = vpcInternal ? "http" : "https";
        return String.format(tile_url_pattern, protocol, TILESERVER_URL);
    }

    public static String getTileserverMetadataUrl(String collectionId, String timestamp, boolean vpcInternal) {
        String metadata_url_pattern = "%s://%s/%s/%s/metadata.json";
        String protocol = vpcInternal ? "http" : "https";
        return String.format(metadata_url_pattern, protocol, TILESERVER_URL, collectionId, timestamp);
    }

    public static String getTileserverTileUrl(String collectionId, String timestamp, int rSet, int x, int y, int band, boolean vpcInternal) {
        String tile_url_pattern = "%s://%s/%s/%s/%d/%d/%d/%d.png";
        String protocol = vpcInternal ? "http" : "https";
        return String.format(tile_url_pattern, protocol, TILESERVER_URL, collectionId, timestamp, rSet, x, y, band);
    }

    public static String getTileserverCacheUrl(String collectionId, String timestamp, int rSet, int x, int y, int band, boolean vpcInternal) {
        String tile_url_pattern = "%s://%s/cache/%s/%s/%d/%d/%d/%d.png";
        String protocol = vpcInternal ? "http" : "https";
        return String.format(tile_url_pattern, protocol, TILESERVER_URL, collectionId, timestamp, rSet, x, y, band);
    }

    public static void setAnalyticsApiUrl(String url) {
        ANALYTICS_API_URL = url;
    }
}
