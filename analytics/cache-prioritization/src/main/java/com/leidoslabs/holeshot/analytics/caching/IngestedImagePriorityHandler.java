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

package com.leidoslabs.holeshot.analytics.caching;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.SNSEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.leidoslabs.holeshot.analytics.common.client.AnalyticsURLBuilder;
import com.leidoslabs.holeshot.analytics.common.model.RequestSummary;
import com.leidoslabs.holeshot.analytics.common.model.ScoredTile;
import com.leidoslabs.holeshot.analytics.common.model.TileScores;
import com.leidoslabs.holeshot.analytics.common.utils.GeohashUtils;
import com.leidoslabs.holeshot.elt.tileserver.TileRef;
import com.leidoslabs.holeshot.elt.tileserver.TileserverImage;
import com.leidoslabs.holeshot.tileserver.v1.TilePyramidDescriptor;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.image.common.geojson.GeoJsonModule;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

/**
 * Determines which tiles, if any, of a newly ingested image should be immediately placed into the tileserver cache,
 * and initiates this caching activity
 */
public class IngestedImagePriorityHandler implements RequestHandler<SNSEvent, Object> {
    private static final String TILESERVER_API_KEY = System.getenv().getOrDefault("HOLESHOT_TILESERVICE_KEY", "APIKEYVALUE");
    private static final String ANALYTICS_API_KEY = System.getenv().getOrDefault("ANALYTICS_API_KEY", "APIKEYVALUE2");
    private static final String LOOKBACK_DAYS = System.getenv().getOrDefault("LOOKBACK_DAYS", "1000");
    private Context context;
    private boolean isLocalTest = true;

    public Object handleRequest(SNSEvent request, Context context) {
        // Message received contains image metadata for a just ingested image
        this.context = context;
        this.isLocalTest = false;
        String message = request.getRecords().get(0).getSNS().getMessage();
        log("Received event with message: \n" + message);
        this.handleImage(message);
        return null;
    }

    public void handleImage(String metadata) {
        int n_DAYS = Integer.parseInt(LOOKBACK_DAYS);
        // Deserialize metadata message
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new GeoJsonModule());
        TilePyramidDescriptor descriptor;
        try {
            descriptor = mapper.readValue(metadata, TilePyramidDescriptor.class);
        } catch (JsonProcessingException e) {
            log("Unable to deserialize metadata content from event");
            e.printStackTrace();
            return;
        }
        List<RequestSummary> summaries = getSummaries(descriptor.getBoundingBox(), n_DAYS);
        if(summaries == null || summaries.size() == 0) {
            return;
        }
        HashMap<String, Integer> scoreMap = new HashMap<>();
        summaries.forEach(s -> {
            String loc = s.getLocation();
            if(scoreMap.containsKey(loc)) {
                Integer oldScore = scoreMap.get(loc);
                scoreMap.replace(loc, oldScore + s.getRequestCount());
            } else {
                scoreMap.put(loc, s.getRequestCount());
            }
        });
        // scoreMap.forEach((key, value) -> log(key + " : " + value));
        TileserverImage image = new TileserverImage(descriptor);
        int scoreThreshold = 1;
        List<ScoredTile> scoredTiles = new ArrayList<>();
        // for each "virtual" tile, check if its geohash is in the map and
        for(int rSet=0; rSet<=image.getMaxRLevel();rSet++) {
            int scaleFactor = (int) Math.pow(2, rSet);
            int pixelWidth = (int) (image.getR0ImageWidth() / scaleFactor);
            int pixelHeight = (int) (image.getR0ImageHeight() / scaleFactor);
            // May be incorrect with images that are not exactly divisible by tile width but are after pixel rounding
            int widthInTiles = pixelWidth / image.getTileWidth() + (pixelWidth % image.getTileWidth() == 0 ? 0 : 1);
            int heightInTiles = pixelHeight / image.getTileHeight() + (pixelHeight % image.getTileHeight() == 0 ? 0 : 1);
            for(int x=0;x<widthInTiles; x++) {
                for(int y=0;y<heightInTiles;y++) {
                    String loc = GeohashUtils.locatePoints(new TileRef(image, rSet, x, y));
                    // log(loc);
                    if(scoreMap.containsKey(loc)) {
                        int score = scoreMap.get(loc);
                        if(score >= scoreThreshold) {
                            scoredTiles.add(new ScoredTile(rSet, x, y, score));
                        }
                    }
                }
            }
        }
        // scoredTiles.forEach(t -> log(String.format("%d:(%d,%d) - %d", t.getrSet(), t.getX(), t.getY(), t.getScore())));
        postScores(image.getCollectionID(), image.getTimestamp(), scoredTiles);
        primeCache(image.getCollectionID(), image.getTimestamp(), image.getNumBands(), scoredTiles);
    }
    private void primeCache(String collectionId, String timestamp, int nBands, List<ScoredTile> tiles) {
        List<HttpGet> requests = new ArrayList<>();
        HttpGet metadataRequest = new HttpGet(AnalyticsURLBuilder.getTileserverMetadataUrl(collectionId, timestamp, !isLocalTest));
        metadataRequest.addHeader("x-api-key", TILESERVER_API_KEY);
        requests.add(metadataRequest);
        tiles.forEach(tile -> {
            for(int band=0; band<nBands;band++ ) {
                HttpGet request = new HttpGet(
                        AnalyticsURLBuilder.getTileserverCacheUrl(collectionId, timestamp, tile.getrSet(), tile.getX(), tile.getY(), band, !isLocalTest));
                request.addHeader("x-api-key", TILESERVER_API_KEY);
                requests.add(request);
            }
        });
        log(requests.get(0).getURI().toString());
        requests.parallelStream().forEach(request -> {
            try (CloseableHttpClient client = HttpClients.createDefault();
                 CloseableHttpResponse response = client.execute(request)) {
                // log(request.toString());
                // log(response.getStatusLine().getStatusCode() + ": " + response.getStatusLine().getReasonPhrase());
            } catch (IOException e) {
                log("Failure during prime cache request");
                log(e.getMessage());
            }
        });
    }
    private void postScores(String collectionId, String timestamp, List<ScoredTile> tiles) {
        try {
            String url = AnalyticsURLBuilder.getTilescoresUrl();
            HttpPost request = new HttpPost(url);
            request.addHeader("x-api-key", ANALYTICS_API_KEY);
            request.addHeader("content-type", "application/json");
            TileScores scores = new TileScores(tiles, collectionId + ":" + timestamp, System.currentTimeMillis());
            ObjectMapper mapper = new ObjectMapper();
            String scoreString = "{\"scores\": " + mapper.writeValueAsString(Collections.singletonList(scores)) + "}";
            log("Scores: \n" + scoreString);
            request.setEntity(new StringEntity(scoreString));
            try (CloseableHttpClient client = HttpClients.createDefault();
                 CloseableHttpResponse response = client.execute(request)) {
                HttpEntity entity = response.getEntity();
                log(EntityUtils.toString(entity));
            } catch (IOException e) {
                log("Summary request http GET failed");
                log(e.getMessage());
            }
        } catch (JsonProcessingException | UnsupportedEncodingException e) {
            log("Exception while building tile score post request");
            log(e.getMessage());
        }
    }
    private List<RequestSummary> getSummaries(double[] bBox, int lookbackDays) {
        String bbox = String.format("%f,%f,%f,%f", bBox[0], bBox[1], bBox[2], bBox[3]);
        long currentMillis = LocalDateTime.now().atZone(ZoneId.of("UTC")).toInstant().toEpochMilli();
        long diff = (long) lookbackDays * 24 * 60 * 60 * 1000;
        long startTime = currentMillis - diff;
        try {
            String url = AnalyticsURLBuilder.getSummarySearchUrl("bbox", bbox,
                    "startTime", Long.toString(startTime));
            log(url);
            HttpGet request = new HttpGet(url);
            request.addHeader("x-api-key", ANALYTICS_API_KEY);
            try (CloseableHttpClient client = HttpClients.createDefault();
                 CloseableHttpResponse response = client.execute(request)) {
                HttpEntity entity = response.getEntity();
                if (entity != null) {
                    try {
                        ObjectMapper mapper = new ObjectMapper();
                        return mapper.readValue(EntityUtils.toString(entity), new TypeReference<ArrayList<RequestSummary>>(){});
                    } catch (IOException e) {
                        log("Unable to process response from request summary server");
                        log(e.getMessage());
                        e.printStackTrace();
                    }
                }
            } catch (IOException e) {
                log("Summary request http GET failed");
                log(e.getMessage());
            }
        } catch (URISyntaxException e) {
            log("Summary Endpoint URI building caused exception");
            log(e.getMessage());
        }
        return null;
    }
    private void log(String message) {
        if(isLocalTest) {
            System.out.println(message);
        } else {
            this.context.getLogger().log(message);
        }
    }
} 