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
package com.leidoslabs.holeshot.tileserver.service.wmts;

import com.leidoslabs.holeshot.tileserver.service.wmts.handlers.GetServiceMetadataRequestHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.NotFoundException;
import javax.ws.rs.core.Response;
import java.io.IOException;
/**
 * WMTSRequest for metadata requests, creates a GetServiceMetadataRequestHandler
 */
public class GetServiceMetadataRequest implements WMTSRequest {
   private static final Logger LOGGER = LoggerFactory.getLogger(GetServiceMetadataRequest.class);

   private final GetServiceMetadataRequestHandler requestHandler;

   public GetServiceMetadataRequest(String tileServerUrl, String imageId, String timestamp) throws IOException, NotFoundException {
      LOGGER.debug("Creating metadata handler for " + imageId + ":" + timestamp);
      requestHandler = new GetServiceMetadataRequestHandler(tileServerUrl, imageId, timestamp);
      LOGGER.debug("Metadata handler created succesfully");
   }

   @Override
   public Response getResponse() {
      return requestHandler.getResponse();
   }
}
