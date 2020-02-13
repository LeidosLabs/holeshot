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

import com.leidoslabs.holeshot.credentials.HoleshotCredentials;

/**
 * This class provides a fluent API used to create a tile server client with configuration options
 * appropriate for the calling application.
 */
public class TileServerClientBuilder {
   
   private static String localDomainName = null;
   
   public static synchronized void setLocalDomainName(String name) {
      localDomainName = name;
   }
   
  private TileServerClient instance = new TileServerClient();

  /**
   * All clients must be given the URL of the tile server service as a start.
   *
   * @param endpoint the URL of the tile server
   * @return a new client builder
   */
  public static TileServerClientBuilder forEndpoint(String endpoint) {
    TileServerClientBuilder result = new TileServerClientBuilder();
    
    if (localDomainName != null && endpoint.contains(localDomainName)) {
       endpoint = endpoint.replaceFirst("^https", "http");
    }
    
    result.instance.setEndpoint(endpoint);
    return result;
  }

  /**
   * Most tile servers will require some sort of authentication and authorizations before use; this
   * method can be used to provide credentials used on each call.
   *
   * @param credentials the user credentials
   * @return this client builder
   */
  public TileServerClientBuilder withCredentials(HoleshotCredentials credentials) {
    instance.setCredentials(credentials);
    return this;
  }
  
  /**
   * Sets the default for the max connections per route
   *
   * @param max The max number of connections per route
   * @return this client builder
   */
  public TileServerClientBuilder withDefaultMaxPerRoute(int max) {
    instance.setDefaultMaxPerRoute(max);
    return this;
  }

  /**
   * Sets the max number of total connections allowed
   *
   * @param max The max number of total Connections
   * @return this client builder
   */
  public TileServerClientBuilder withMaxTotal(int max) {
    instance.setMaxTotal(max);
    return this;
  }
  
  
  /**
   * Once all the configuration options have been set this method will provide the client.
   * @return the configured client
   */
  public TileServerClient build() {
    return instance.setup();
  }

  private TileServerClientBuilder() {}

}
