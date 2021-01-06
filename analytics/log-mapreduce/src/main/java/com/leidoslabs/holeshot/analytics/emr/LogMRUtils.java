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

package com.leidoslabs.holeshot.analytics.emr;

import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;

import com.leidoslabs.holeshot.analytics.common.client.AnalyticsURLBuilder;
import org.apache.http.entity.StringEntity;
import org.apache.spark.api.java.function.FlatMapFunction;
import org.apache.spark.api.java.function.PairFlatMapFunction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.leidoslabs.holeshot.analytics.common.model.UserTileRef;
import com.leidoslabs.holeshot.analytics.common.utils.GeohashUtils;
import com.leidoslabs.holeshot.elt.tileserver.TileRef;
import com.leidoslabs.holeshot.elt.tileserver.TileserverImage;

import scala.Tuple2;

/**
 * Collection of constants and utilities for our log mapreduce job
 */
public class LogMRUtils {
    private static final Logger LOGGER = LoggerFactory.getLogger(LogMRUtils.class);

    public static final String ANALYTICS_API_KEY = System.getenv("ANALYTICS_API_KEY"); // Used to post summaries
    public static final int MAX_PARTITION_SIZE = (int) 64e6;
    public static final String INPUT_LOG_LOCATION = System.getenv("INPUT_LOG_LOCATION");
    public static final boolean LOCAL_MODE = System.getenv("LOCAL_MODE") != null ? Boolean.parseBoolean(System.getenv("LOCAL_MODE")) : !insideVPC();
    
    public static final String OUTPUT_LOCATION = LOCAL_MODE ? System.getProperty("user.home") 
    		: Strings.nullToEmpty(System.getenv("OUTPUT_LOCATION")).trim();

    /**
     * @return a Functional interface that parses a logline and georeferences
     * the tile, finds the appropiate geohash, and returns that geohash along with a default count as 1.
     * Returns 2 of these tuples if user information is included (one global Key value, one user KV).
     */
    public static PairFlatMapFunction<String, String, Integer> parseAndGeoRefFunctional(){
    	return new PairFlatMapFunction<String, String, Integer>(){
			private static final long serialVersionUID = -803989533568345219L;
			@Override
        	public Iterator<Tuple2<String, Integer>> call(String s) {
        		return LogMRUtils.extractInfo(s).stream().flatMap(t -> GeohashUtils.locatePointsMult(t).stream()).map(g -> new Tuple2<>(g, 1)).iterator();
        	}
        };
    }
    
    /**
     * @param t1 start time of log data
     * @param t2 end time of log data
     * @return A functional interface that takes our {geohash/geohash<username: count} Key value pair, and for each entry 
     * constructs a payload and POSTs to our analytics endpoint. We POST in bulk, but entries are subpartitioned so we don't exceed 
     * API Gateway's 10MB payload limit
     */
    public static FlatMapFunction<Iterator<Tuple2<String,Integer>>, String> uploadFunctional(Long t1, Long t2){
    	return new FlatMapFunction<Iterator<Tuple2<String,Integer>>, String>() {
			private static final long serialVersionUID = -4170200207057850986L;
			@Override
        	public Iterator<String> call(Iterator<Tuple2<String, Integer>> parts) throws Exception {
        		List<StringEntity> payloads = AnalyticsAPIClient.getPayloads(parts, t1, t2);
        		
				List<String> results = new ArrayList<>();
        		for (StringEntity payload: payloads) {
        			results.add(AnalyticsAPIClient.post(payload));
        		}
        		return results.iterator();
        	}
		};
    }
    
    /**
     * Takes an individual line from the log file and parses it in order to get
     * the TileRef for the referenced point
     *
     * @param logline individual line from the log file
     * @return new TileRef object
     */
    public static List<TileRef> extractInfo(String logline) {
    	
    	List<TileRef> results = new ArrayList<>();
    	LOGGER.debug("Entered extract info");
        try {
            JsonObject log = new JsonParser().parse(logline).getAsJsonObject();
            String userID = log.get("userID") != null ? log.get("userID").getAsString() : null; // can be null
            String[] imageID = log.get("imageID").getAsString().split(":");
           
            String urlString = AnalyticsURLBuilder.getTileserverMetadataUrl(imageID[0], imageID[1], true);
            URL metadataURL = new URL(urlString);
            LOGGER.debug("Metadata URL: {}", metadataURL.toString());
            TileserverImage image = new TileserverImage(metadataURL);
            results.add(new TileRef(image, log.get("rSet").getAsInt(), log.get("x").getAsInt(), log.get("y").getAsInt()));
            if (userID != null) {
            	results.add(new UserTileRef(image, log.get("rSet").getAsInt(), log.get("x").getAsInt(), log.get("y").getAsInt(), userID));
            }
            return results;
        } catch (IOException e) {
            LOGGER.error(e.getMessage(), e);
            e.printStackTrace();

            return null;
        }
    }
    
    /**
     * 	dumb way to determine if we are running in the VPC (do we have a network interface with a 10.0... IP?)
     *  TODO: find a better way
     * @return True if determined to be in private network. False if not or unable to tell
     */
    private static boolean insideVPC() {
		boolean inside = false;
	
		try {
			Enumeration<NetworkInterface> cards = NetworkInterface.getNetworkInterfaces();
			while (cards.hasMoreElements()) {
				NetworkInterface card = cards.nextElement();
				Enumeration<InetAddress> ips = card.getInetAddresses();
				while (ips.hasMoreElements()) {
					InetAddress ip = ips.nextElement();
					if (ip.getHostAddress().startsWith("10.0.")) {
						return true;
					}
				}
			}
		} catch (IOException e) {
			LOGGER.error("Couldn't determine whether client is in VPC", e);
			return false;
		}
		return inside;
	}
}
