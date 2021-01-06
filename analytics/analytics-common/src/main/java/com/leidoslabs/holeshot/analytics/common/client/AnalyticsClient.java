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

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.locationtech.jts.geom.Geometry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.leidoslabs.holeshot.analytics.common.model.CacheList;
import com.leidoslabs.holeshot.analytics.common.model.RequestSummary;
import com.leidoslabs.holeshot.analytics.common.model.User;
import com.leidoslabs.holeshot.analytics.common.model.aoi.UserAOI;
import com.leidoslabs.holeshot.catalog.v1.CatalogClient;
import com.leidoslabs.holeshot.catalog.v1.CatalogCredentials;
import com.leidoslabs.holeshot.catalog.v1.CatalogEntry;

public class AnalyticsClient {

	private static final Logger LOGGER = LoggerFactory.getLogger(AnalyticsClient.class);

	public static String ANALYTICS_API_KEY = System.getenv("ANALYTICS_API_KEY");
	private final ObjectMapper mapper;
	
	public AnalyticsClient(ObjectMapper mapper) {
		this.mapper = mapper;
	}

	public AnalyticsClient() {
		this(new ObjectMapper());
	}

    public AnalyticsClient(String analyticsWarehouseUrl, String analyticsWarehouseApiKey) {
	    this.mapper = new ObjectMapper();
	    ANALYTICS_API_KEY = analyticsWarehouseApiKey;
	    AnalyticsURLBuilder.setAnalyticsApiUrl(analyticsWarehouseUrl);
    }


	public List<RequestSummary> getSummaries(long startTime, String userID) throws IOException {
		try {

		    String url = AnalyticsURLBuilder.getSummarySearchUrl("startTime", Long.toString(startTime), "userID", userID);

			HttpGet request = new HttpGet(url);
			request.addHeader("x-api-key", ANALYTICS_API_KEY);
			try (CloseableHttpClient client = HttpClients.createDefault();
					CloseableHttpResponse response = client.execute(request)) {

				HttpEntity entity = response.getEntity();
				if (entity != null) {
					try {
						return mapper.readValue(EntityUtils.toString(entity), new TypeReference<List<RequestSummary>>(){});
					} catch (IOException e) {
						LOGGER.error("Unable to process response from 'summaries' server");
						throw e;
					}
				}
			} catch (IOException e) {
				LOGGER.error("Summary request http GET failed");
				throw e;
			}
		} catch (URISyntaxException e) {
			LOGGER.error("Summary Endpoint URI building caused exception");
			throw new IOException(e);
		}
		return null;
	}

	public List<UserAOI> getAOIs(long date, String userID) throws IOException {
		try {
		    String url = AnalyticsURLBuilder.getAoiUrl("userID", userID, "startTime", Long.toString(date));
			HttpGet request = new HttpGet(url);
			request.addHeader("x-api-key", ANALYTICS_API_KEY);
			try (CloseableHttpClient client = HttpClients.createDefault();
					CloseableHttpResponse response = client.execute(request)) {
				HttpEntity entity = response.getEntity();
				if (entity != null) {
					try {
						return mapper.readValue(EntityUtils.toString(entity), new TypeReference<List<UserAOI>>(){});
					} catch (IOException e) {
						LOGGER.error("Unable to process response from 'aoi' server");
						throw e;
					}
				}
			} catch (IOException e) {
				LOGGER.error("AOI request http GET failed");
				throw e;
			}
		} catch (URISyntaxException e) {
			LOGGER.error("AOI Endpoint URI building caused exception");
			throw new IOException(e);
		}
		return null;
	}

	public Set<String> getUsers() throws IOException {
		try {

		    String url = AnalyticsURLBuilder.getUsersUrl();
			HttpGet request = new HttpGet(url);
			request.addHeader("x-api-key", ANALYTICS_API_KEY);

			try (CloseableHttpClient client = HttpClients.createDefault();
					CloseableHttpResponse response = client.execute(request)) {

				HttpEntity entity = response.getEntity();
				if (entity != null) {
					try {                        
						return mapper.readValue(EntityUtils.toString(entity), new TypeReference<List<User>>(){})
								.stream().map(u -> u.getUserID()).collect(Collectors.toSet());
					} catch (IOException e) {
						LOGGER.error("Unable to process response from request 'Users' server");
						throw e;
					}
				}
			} catch (IOException e) {
				LOGGER.error("Users http GET failed");
				throw e;
			}
		} catch (URISyntaxException e) {
			LOGGER.error("Could not build getUsers HTTP request");
			throw new IOException(e);
		}
		return null;
	}
	
	public void updateList(String userID) throws IOException {
		try {
		    String url = AnalyticsURLBuilder.getUpdateListUrl("userID", userID);
			HttpGet request = new HttpGet(url);
			request.addHeader("x-api-key", ANALYTICS_API_KEY);

			try (CloseableHttpClient client = HttpClients.createDefault();
				CloseableHttpResponse response = client.execute(request)) {
				if (response.getStatusLine().getStatusCode() >= 400) {
					LOGGER.error("Error from updateList endpoint");
					throw new IOException();
				}
			} catch (IOException e) {
				LOGGER.error("updateLists http GET failed");
				throw e;
			}
		} catch (URISyntaxException e) {
			LOGGER.error("Could not build updateLists HTTP request");
			throw new IOException(e);
		}
	}
	
	public List<CacheList> getCacheLists(String userID, long date) throws IOException {
		try {
		    String url = AnalyticsURLBuilder.getCacheListsUrl("date", Long.toString(date), "userID", userID);
			HttpGet request = new HttpGet(url);
			request.addHeader("x-api-key", ANALYTICS_API_KEY);
			try (CloseableHttpClient client = HttpClients.createDefault();
					CloseableHttpResponse response = client.execute(request)) {

				HttpEntity entity = response.getEntity();
				if (entity != null) {
					try {                        
						return mapper.readValue(EntityUtils.toString(entity), new TypeReference<List<CacheList>>(){});
					} catch (IOException e) {
						LOGGER.error("Unable to process response from request 'cacheList' server");
						throw e;
					}
				}
			} catch (IOException e) {
				LOGGER.error("CacheList http GET failed");
				throw e;
			}
		} catch (URISyntaxException e) {
			LOGGER.error("Could not build getCacheLists HTTP request");
			throw new IOException(e);
		}
		return null;
	}

	public List<CatalogEntry> getCatalogEntries(Geometry geom) throws IOException {
		CatalogClient client = new CatalogClient(CatalogCredentials.getApplicationDefaults());
		client.setCatalogURL(AnalyticsURLBuilder.getImageCatalogUrl());
		return client.getCatalogEntries(geom);
	}

	public void postAOIs(List<UserAOI> aois) throws IOException {
		try {
			String url = AnalyticsURLBuilder.getAoiUrl();
			HttpPost request = new HttpPost(url);
			request.addHeader("x-api-key", ANALYTICS_API_KEY);
			request.addHeader("Content-Type", "application/json");
			String payload = "{\n \"aois\": \n" + mapper.writeValueAsString(aois) + "\n}\n";
			request.setEntity(new StringEntity(payload));

			try (CloseableHttpClient client = HttpClients.createDefault();
					CloseableHttpResponse response = client.execute(request)) {
				if (response.getStatusLine().getStatusCode() >= 400) {
					LOGGER.error(response.getStatusLine().toString());
					throw new IOException("Error posting AOIs");
				}


			} catch (IOException e) {
				LOGGER.error("AOIS post failed");
				throw e;
			}

		} catch (URISyntaxException | IOException e) {
			LOGGER.error("Unable to create URL or serialize payload");
			throw new IOException(e);
		}
	}

	public void postCacheList(List<CacheList> cacheLists) throws IOException {
		try {
			String payload = "{\n \"lists\": \n" + mapper.writeValueAsString(cacheLists) + "\n}\n";
			String url = AnalyticsURLBuilder.getCacheListsUrl();
			HttpPost request = new HttpPost(url);
			request.addHeader("x-api-key", ANALYTICS_API_KEY);
			request.setEntity(new StringEntity(payload));
			try (CloseableHttpClient client = HttpClients.createDefault();
					CloseableHttpResponse response = client.execute(request)) {
				if (response.getStatusLine().getStatusCode() >= 400) {
					LOGGER.error(response.getStatusLine().toString());
					throw new IOException("Error cache lists");
				}   
			} catch (IOException e) {
				LOGGER.error("CacheList post failed");
				throw e;
			}
		} catch (URISyntaxException | IOException e) {
			LOGGER.error("Unable to create URL or serialize payload");
			throw new IOException(e);
		}


	}

}
