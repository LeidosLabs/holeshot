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

package com.leidoslabs.holeshot.chipper.wms;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

import javax.ws.rs.core.UriInfo;

import org.apache.commons.collections4.map.CaseInsensitiveMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Parser for URIInfo. Stores query parameters as a map. Provides parsing methods
 */
public class UriInfoParser {
   private static final Logger LOGGER = LoggerFactory.getLogger(UriInfoParser.class);
   private CaseInsensitiveMap<String, List<String>> queryParameters;

   /**
    * Constructor. Stores query parameters as map
    */
   public UriInfoParser(UriInfo info) {
      super();
      queryParameters = new CaseInsensitiveMap<String, List<String>>(info.getQueryParameters());
   }

   public boolean containsKeyMatch(String patternString) {
      Pattern pattern = Pattern.compile(patternString, Pattern.CASE_INSENSITIVE);
      return queryParameters.keySet().stream().anyMatch(k->pattern.matcher(k).matches());
   }
   /**
    * @param key
    * @return parameters with this key
    */
   public List<String> getQueryParameter(String key) {
      return queryParameters.get(key.toLowerCase());
   }
   
   /**
    * @return return first entry for service in uri params
    */
   public String getService() {
      return getString("service");
   }
   public List<String> getVersion() {
      return getStringList("version");
   }
   public String getRequest() {
      return getString("request");
   }
   public double[] getDoubleArray(String key) {
      double[] result = null;
      final List<String> stringValue = getStringList(key);
      if (stringValue != null) {
         result = stringValue.stream().mapToDouble(s->Double.parseDouble(s)).toArray();
      }
      return result;
   }
   public int getIntValue(String key, int fallback) {
      int result = fallback;
      final String stringResult = getString(key);
      if (stringResult != null) {
         result = Integer.parseInt(stringResult);
      }
      return result;
   }
   public URL getURLValue(String key) {
      URL result = null;
      final String stringResult = getString(key);
      if (stringResult != null) {
         try {
            result = new URL(stringResult);
         } catch (MalformedURLException e) {
            LOGGER.error("Couldn't convert URL", e);
         }
      }
      return result;
   }
   public boolean getBoolValue(String key, boolean fallback) {
      boolean result = fallback;
      final String stringResult = getString(key);
      if (stringResult != null) {
         result = Boolean.parseBoolean(stringResult);
      }
      return result;
   }
   public List<String> getStringList(String key) {
      List<String> result = new ArrayList<String>();
      final List<String> values = getQueryParameter(key);
      if (values != null) {
         result.addAll(values);
      }
      return result;
   }

   /**
    * Return first parameter with this key
    * @param key
    * @return
    */
   public String getString(String ...key) {
      String result = Arrays.stream(key).map(s-> {
         String first = null;
         List<String> list = getQueryParameter(s);
         if (list != null && list.size() > 0) {
            first = list.get(0);
         }
         return first;
      }).filter(s->s!=null).findFirst().orElse(null);
      return result;
   }
}
