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

package com.leidoslabs.holeshot.chipper.wms.v130;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.leidoslabs.holeshot.chipper.wms.GetCapabilitiesRequest;
import com.leidoslabs.holeshot.chipper.wms.UriInfoWMSRequest;

/**
 * Wrapper for GetCapabilitiesRequestHandler130
 */
public class UriInfoGetCapabilitiesRequest130 extends UriInfoWMSRequest implements GetCapabilitiesRequest {
   private static final Logger LOGGER = LoggerFactory.getLogger(UriInfoGetCapabilitiesRequest130.class);

   private final GetCapabilitiesRequestHandler130 requestHandler;

   public UriInfoGetCapabilitiesRequest130(UriInfo info) {
      super(info);
      requestHandler = new GetCapabilitiesRequestHandler130();
   }


   @Override
   public Response getResponse(HttpServletRequest servletRequest) throws IOException {
      return requestHandler.getResponse(servletRequest);
   }
}
