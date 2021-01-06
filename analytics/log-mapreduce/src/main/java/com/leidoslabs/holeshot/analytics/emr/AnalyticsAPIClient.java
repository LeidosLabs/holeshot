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
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import com.leidoslabs.holeshot.analytics.common.client.AnalyticsURLBuilder;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.Iterators;

import scala.Tuple2;
import shaded.parquet.org.codehaus.jackson.map.ObjectMapper;


/**
 * Collection of functions for interacting with our analytics API (our proxy to elastic search)
 * functions are static so they can be used easily in spark closures
 */
public class AnalyticsAPIClient {
	
    private static final Logger LOGGER = LoggerFactory.getLogger(AnalyticsAPIClient.class);
	
	public static String post(StringEntity payload) {
    	CloseableHttpClient client = HttpClientBuilder.create().build();
    	String result = "";
    	String id = String.valueOf(new Date().getTime());
    	try {
    		URI recourseURL = new URI(AnalyticsURLBuilder.getSummariesUrl());
    		
    		HttpPost post = new HttpPost(recourseURL);
    		post.addHeader("x-api-key", LogMRUtils.ANALYTICS_API_KEY);
    	    post.addHeader("Content-Type", "application/json");

    		post.setEntity(payload);
    		String postCode = backoffRetry(client, post);
    		result = String.format("%s:%s-%s", postCode, recourseURL.toString(), payload.toString());
    		client.close();

    	} catch (URISyntaxException | IOException e) {
    		e.printStackTrace();
    	}
    	
    	return result;
    }
    
    public static List<StringEntity> getPayloads(Iterator<Tuple2<String, Integer>> records, Long t1, Long t2) throws IOException{
    	
    	List<StringEntity> results = new ArrayList<>();
    	// API Gateway has a limit of 10MB for a payload, so we partition records into parts (we assume a single serialized ESEntry should be no greater
    	// than 256 bytes, so the max # of records per partition is 10e6 / 256. TODO: 256 is arbitrary
    	int maxRecords = (int) 10e6 / 256;
        Iterator<List<Tuple2<String, Integer>>> partitions = Iterators.partition(records, maxRecords);
        while (partitions.hasNext()) {
            List<Tuple2<String, Integer>> part = partitions.next();
            results.add(getPayload(part, t1, t2));
        }

        return results;
    }
    
    
    
    private static StringEntity getPayload(List<Tuple2<String, Integer>> records, Long t1, Long t2) throws IOException{
    	ObjectMapper mapper = new ObjectMapper();
    
    	List<ESEntry> entries = new ArrayList<>();
    	for (Tuple2<String, Integer> record : records) {
    		
    		ESEntry entry = new ESEntry();
    		String key = record._1;
    		String[] keyParts = key.split("#");
    		
    		String hash = keyParts[0];
        	int pLevel = hash.length();
        	int count = record._2;
        
        	if (keyParts.length < 2 || keyParts[1].equals("admin")){
        		entry.setUserID("global");
        	} else {
        		LOGGER.debug("--------------------------" + keyParts[1]);
        		entry.setUserID(keyParts[1]);
        	}
    		
    		entry.setLocation(hash);
    		entry.setStartTime(t1);
    		entry.setEndTime(t2);
    		entry.setPrecisionLevel(pLevel);
    		entry.setRequestCount(count);
    		
    		entries.add(entry);
    	}
    	APIPayload payload = new APIPayload();
    	payload.setSummaries(entries);
    	String content = null;
    	try {
			content = mapper.writeValueAsString(payload);
		} catch (IOException e) {
			LOGGER.error("Error serializing payload body", e);
			throw e;
		}
    	
    	return new StringEntity(content);
    }
    
    
    
    /**
     * Really simple backoffRetry loop for post requests, waits 1.2^n seconds capping at 60 seconds
     * TODO: come up with a better growth function
     * @param client
     * @param req
     * @return
     */
    private static String backoffRetry(CloseableHttpClient client, HttpPost req) {
    	int code = -1;
    	Long sleepTime = 1000L;
    	while (code != 200) {
    		try {
    			CloseableHttpResponse response = client.execute(req);
    			code = response.getStatusLine().getStatusCode();
    			response.close();
    			if (code == 503 || code == 504){
    				Thread.sleep(sleepTime);
    				sleepTime = (long) (sleepTime * 1.2);
    			}
    			else if (code != 200 || sleepTime > 6e7) { 
    				LOGGER.error("ES endpoint timeout");
    				return "ERORR";
    			}
    		} catch (IOException | InterruptedException e) {
    			e.printStackTrace();
    			return "ERROR";
    		}
    	}
    	return Integer.toString(code);
    }
	
	//TODO: duplicate POJO, just add analytics-common as a dependency
	private static class APIPayload {
		@JsonProperty("summaries")
		private List<ESEntry> summaries;

		public List<ESEntry> getSummaries() {
			return summaries;
		}
		public void setSummaries(List<ESEntry> summaries) {
			this.summaries = summaries;
		}
	}

	@JsonInclude(JsonInclude.Include.NON_DEFAULT)
	private static class ESEntry {
		@JsonProperty("location")
		private String location;
		@JsonProperty("startTime")
		private long startTime;
		@JsonProperty("endTime")
		private long endTime;
		@JsonProperty("precisionLevel")
		private int precisionLevel;
		@JsonProperty("requestCount")
		private int requestCount;
		@JsonProperty("userID")
		private String userID;

		public String getUserID() {
			return userID;
		}
		public void setUserID(String userID) {
			this.userID = userID;
		}
		public String getLocation() {
			return location;
		}
		public void setLocation(String location) {
			this.location = location;
		}
		public long getStartTime() {
			return startTime;
		}
		public void setStartTime(long startTime) {
			this.startTime = startTime;
		}
		public long getEndTime() {
			return endTime;
		}
		public void setEndTime(long endTime) {
			this.endTime = endTime;
		}
		public int getPrecisionLevel() {
			return precisionLevel;
		}
		public void setPrecisionLevel(int precisionLevel) {
			this.precisionLevel = precisionLevel;
		}
		public int getRequestCount() {
			return requestCount;
		}
		public void setRequestCount(int requestCount) {
			this.requestCount = requestCount;
		}

	}

}
