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
package com.leidoslabs.holeshot.elt.tileserver;

import java.net.MalformedURLException;
import java.net.URL;
import java.security.InvalidParameterException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.image.common.cache.CacheableUtil;


/**
 * Parses Metadata URLS and provides some utilities
 */
public class TileserverUrlBuilder {
  private static final Pattern METADATA_URL_PATTERN = Pattern.compile("^(.*)/([^/]*)/([^/]*)/metadata.json$");
  
  private String imageMetadataURL;
  private String imageBaseURL;
  private String collectionID;
  private String timestamp;

  public TileserverUrlBuilder(URL imageMetadataURL) {
    this.imageMetadataURL = String.valueOf(imageMetadataURL);
    validateMetadataURL();
  }
  
  public static URL getImageMetadataURL(URL baseURL) throws MalformedURLException {
	  return new URL(getImageMetadataURLString(baseURL.toString()));
  }
  public static URL getImageMetadataURL(String baseURLString) throws MalformedURLException {
	  return new URL(getImageMetadataURLString(baseURLString));
  }
  public static String getImageMetadataURLString(URL baseURL) {
	  return getImageMetadataURLString(baseURL.toString());
  }
  public static String getImageMetadataURLString(String baseURL) {
	  return String.format("%s/metadata.json", baseURL);
  }

  public String getImageMetadataURL() {
    return imageMetadataURL;
  }

  public String getImageBaseURL() {
    return imageBaseURL;
  }

  public String getCollectionID() {
    return collectionID;
  }

  public String getTimestamp() {
    return timestamp;
  }

  private void validateMetadataURL() {
    Matcher matcher = METADATA_URL_PATTERN.matcher(imageMetadataURL);

    if (!matcher.matches()) {
      throw new InvalidParameterException(
          String.format("Invalid imageMetadata URL '%s'.  Must match pattern '%s'",
              imageMetadataURL, METADATA_URL_PATTERN.pattern()));
    }

    imageBaseURL = matcher.group(1);
    collectionID = matcher.group(2);
    timestamp = matcher.group(3);
  }

  private String getTileURL(int rset, int col, int row, int band) {
    return String.format("%s/%s/%s/%d/%d/%d/%d.png", imageBaseURL, collectionID, timestamp, rset,
        col, row, band);
  }

  public URL getURL(int rset, int column, int row, int band) throws MalformedURLException {
    String tileURL = getTileURL(rset, column, row, band);
    return new URL(tileURL);
  }

  public long getSizeInBytes() {
    return CacheableUtil.getDefault().getSizeInBytesForObjects(imageMetadataURL, imageBaseURL, collectionID, timestamp);
  }
}
