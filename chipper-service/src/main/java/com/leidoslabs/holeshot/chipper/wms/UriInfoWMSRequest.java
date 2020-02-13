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

import java.util.List;

import javax.ws.rs.core.UriInfo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * WMS request for URIInfo
 */
public abstract class UriInfoWMSRequest extends DefaultWMSRequest {
   private static final Logger LOGGER = LoggerFactory.getLogger(UriInfoWMSRequest.class);

   private final UriInfoParser uriInfoParser;

   protected UriInfoWMSRequest(UriInfo info) {
      super();
      uriInfoParser = new UriInfoParser(info);
   }

   protected UriInfoParser getUriInfo() {
      return uriInfoParser;
   }

   public String getService() {
      return getUriInfo().getService();
   }
   public List<String> getVersion() {
      return getUriInfo().getVersion();
   }
   public String getRequest() {
      return getUriInfo().getRequest();
   }
}
