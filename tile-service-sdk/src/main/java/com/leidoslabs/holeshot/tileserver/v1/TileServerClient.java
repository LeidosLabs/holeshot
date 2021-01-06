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
package com.leidoslabs.holeshot.tileserver.v1;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.concurrent.atomic.AtomicInteger;

import javax.imageio.ImageIO;

import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.util.EntityUtils;
import org.image.common.geojson.GeoJsonModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.net.HttpHeaders;
import com.leidoslabs.holeshot.credentials.HoleshotCredentials;

/**
 * This class provides SDK access to an imagery tile server.
 */
public class TileServerClient {

  private static final Logger LOGGER = LoggerFactory.getLogger(TileServerClient.class);

  private static final String GET_METADATA_URL_PATTERN = "%s/%s/metadata.json";
  private static final String GET_TILE_URL_PATTERN = "%s/%s/%d/%d/%d/%d.png";
  private static final String GET_TILE_USER_URL_PATTERN = "%s/%s/%s/%d/%d/%d/%d.png";

  private static final String OFFLINE_CACHE_LOCATION = "offline_cache";
  private static final String AUTH_HEADER = "x-api-key";
  private static AtomicInteger numConcurrentThreads = new AtomicInteger(0);
  private Integer maxTotal;
  private Integer defaultMaxPerRoute;
  private ObjectMapper objectMapper;
  private String endpoint;
  private HoleshotCredentials credentials;
  private final OfflineCache offlineCache;
  private String username;


  private HttpClient httpClient;
  private ThreadLocal<HttpClientContext> httpClientContext;

  TileServerClient() {
     offlineCache = new OfflineCache(new File(OFFLINE_CACHE_LOCATION));
  }
  
  
  /**
   * Method to get the metadata associated with a specific collection.
   *
   * @param collectionID the unique ID of the collection at the service
   * @param timestamp the timestamp of the collection
   * @return the parsed metadata or null if no metadata is available that matches the selection
   * @throws IOException if an error occurs reading the metadata
   */
  public TilePyramidDescriptor getMetadata(String collectionID, String timestamp)
      throws IOException {
    TilePyramidDescriptor metadata = null;
    try {

      RequestConfig requestConfig = RequestConfig.custom().setRedirectsEnabled(false).build();
      HttpGet metadataRequest = new HttpGet(
          this.endpoint + "/" + String.format(GET_METADATA_URL_PATTERN, collectionID, timestamp));
      LOGGER.debug("Metadata Request: {}", metadataRequest.getURI().toString());
      metadataRequest.setConfig(requestConfig);
      setAuthHeader(metadataRequest);

      try (InputStream inputStream = fetch(metadataRequest)) {
         if (inputStream != null) {
            metadata =
                  objectMapper.readValue(inputStream, TilePyramidDescriptor.class);
         }
      }
    } catch (MalformedURLException murle) {
      LOGGER.error(murle.getMessage(), murle);
      metadata = null;
    }
    return metadata;
  }

  /**
   * Method to request an image tile from the server as an InputStream.
   *
   * @param collectionID the unique ID of the collection at the service
   * @param timestamp the timestamp of the collection
   * @param rLevel the level in the image pyramid (0 = full resolution, 1 = 1/4, 2 = 1/16, ...)
   * @param column the column of the tile requested
   * @param row the row of the tile requested
   * @param band the band
   * @return an image stream for the tile (note the tile server currently only provides PNGs), null
   *         if not found
   * @throws IOException if an error occurs reading the metadata
   */
  public InputStream getTile(String collectionID, String timestamp, int rLevel, int column, int row,
      int band) throws IOException {
    InputStream imageStream = null;
    int threads = numConcurrentThreads.incrementAndGet();
    try {
      RequestConfig requestConfig = RequestConfig.custom().setRedirectsEnabled(false)
          .setSocketTimeout(60 * 1000).setConnectTimeout(60 * 1000).build();
      HttpGet tileRequest;
      if (username != null && !username.equalsIgnoreCase("anonymous")) {
    	  tileRequest = new HttpGet(this.endpoint + "/" + String.format(GET_TILE_USER_URL_PATTERN,
                  username, collectionID, timestamp, rLevel, column, row, band));
      } else {
          tileRequest = new HttpGet(this.endpoint + "/" + String.format(GET_TILE_URL_PATTERN,
                  collectionID, timestamp, rLevel, column, row, band));
      }

      LOGGER.debug("Tile Request: {}", tileRequest.getURI().toString());
      tileRequest.setConfig(requestConfig);
      setAuthHeader(tileRequest);

      imageStream = fetch(tileRequest);

    } catch (MalformedURLException murle) {
      LOGGER.error("Malformed URL", murle);
      imageStream = null;
    } finally {
      numConcurrentThreads.decrementAndGet();
    }
    return imageStream;
  }

  private InputStream fetch(HttpGet get) throws ClientProtocolException, IOException {
     final URI getURI = get.getURI();
     InputStream result = offlineCache.getInputStream(getURI);
     String cacheResult;
     if (result == null) {
        cacheResult = "MISS";
        HttpResponse response = httpClient.execute(get, httpClientContext.get());
        LOGGER.debug("HTTP Response from server. {}", response.getStatusLine().getStatusCode());

        if (isSuccessfulRequest(response)) {
          result = response.getEntity().getContent();
        } else {
          EntityUtils.consume(response.getEntity());
        }
     } else {
        cacheResult = "HIT";
     }
     LOGGER.debug(String.format("OFFLINE CACHE %s on %s",  cacheResult, getURI.toString()));
     return result;
  }

  /**
   * Method to request an imge tile from the server as a BufferedImage.
   *
   * @param collectionID the unique ID of the collection at the service
   * @param timestamp the timestamp of the collection
   * @param rLevel the level in the image pyramid (0 = full resolution, 1 = 1/4, 2 = 1/16, ...)
   * @param column the column of the tile requested
   * @param row the row of the tile requested
   * @param band the band
   * @return a buffered image decoded from the image stream
   * @throws IOException if an error occurs reading the metadata
   */
  public BufferedImage getTileAsImage(String collectionID, String timestamp, int rLevel, int column,
      int row, int band) throws IOException {
    try (InputStream imageStream = getTile(collectionID, timestamp, rLevel, column, row, band)) {
      if (imageStream != null) {
        return ImageIO.read(imageStream);
      }
    }
    return null;
  }

  void setEndpoint(String endpoint) {
    this.endpoint = endpoint;
  }
  
  public String getEndpoint() {
	    return endpoint;
  }

  void setCredentials(HoleshotCredentials credentials) {
    this.credentials = credentials;
  }

  /**
   * Method to check and see if the Http request was successful. This logic assumes that the basic
   * authentication mechanism is always used and that any redirect from the server is really a
   * failed authentication. While that may be true for now that assumption will certainly not hold
   * in the future so this method needs to be reworked as the authentication mechanisms for the tile
   * server are improved.
   *
   * TODO: Revisit TileServerClient handling of response codes.
   *
   * @param response the connection
   * @return true if the response indicated a successful exchange, false otherwise
   * @throws IOException if an error occurred reading from the connection
   */
  private boolean isSuccessfulRequest(HttpResponse response) throws IOException {

    boolean result = true;
    int statusCode = response.getStatusLine().getStatusCode();
    if (statusCode == HttpStatus.SC_MOVED_PERMANENTLY
        || statusCode == HttpStatus.SC_MOVED_TEMPORARILY || statusCode == HttpStatus.SC_SEE_OTHER
        || statusCode == HttpStatus.SC_TEMPORARY_REDIRECT) {
      LOGGER.error(
          "Received unexpected redirect response code from server! Cannot proceed with request.");
      LOGGER.error("Redirect to: {}", response.getHeaders(HttpHeaders.LOCATION));
      LOGGER.error(
          "If this is a redirect to an authentication service it typically means that the authentication of this SDK failed.");
      result = false;
    } else if (statusCode < HttpStatus.SC_OK || statusCode >= HttpStatus.SC_MULTIPLE_CHOICES) {
      LOGGER.error("GET metadata request failed. Error response from server!");
      LOGGER.error("Response Code: {}", statusCode);
      LOGGER.error("Response Message: {}", response.getStatusLine().getReasonPhrase());
      result = false;
    }
    return result;
  }

  private void setAuthHeader(HttpRequestBase request) {
    request.setHeader(AUTH_HEADER, credentials.getSecretAccessKey());
  }

  TileServerClient setup() {
    objectMapper = new ObjectMapper();
    objectMapper.registerModule(new GeoJsonModule());

    try {
      URL url = new URL(endpoint);
      HttpHost targetHost = new HttpHost(url.getHost(), url.getPort());

      PoolingHttpClientConnectionManager poolingConnManager =
          new PoolingHttpClientConnectionManager();

      if (maxTotal != null) {
        poolingConnManager.setMaxTotal(maxTotal);
      }
      if (defaultMaxPerRoute != null) {
        poolingConnManager.setDefaultMaxPerRoute(defaultMaxPerRoute);
      }

      httpClient = HttpClientBuilder.create().setConnectionManager(poolingConnManager).build();

      httpClientContext = new ThreadLocal<HttpClientContext>() {
        protected HttpClientContext initialValue() {
          HttpClientContext ctx = HttpClientContext.create();
          LOGGER.debug("creating ThreadLocal");
          ctx.setCookieStore(new BasicCookieStore());
          return ctx;
        };
      };
    } catch (MalformedURLException e) {
      // TODO: Do something ehere!!!!
    }
    return this;
  }

  protected Integer getMaxTotal() {
    return maxTotal;
  }

  protected void setMaxTotal(Integer maxTotal) {
    this.maxTotal = maxTotal;
  }

  protected Integer getDefaultMaxPerRoute() {
    return defaultMaxPerRoute;
  }

  protected void setDefaultMaxPerRoute(Integer defaultMaxPerRoute) {
    this.defaultMaxPerRoute = defaultMaxPerRoute;
  }

  protected String getUsername() {
      return username;
  }

  protected void setUsername(String username) {
      this.username = username;
  }
}
