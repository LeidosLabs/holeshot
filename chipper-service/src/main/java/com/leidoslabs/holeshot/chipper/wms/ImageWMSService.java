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

import java.io.IOException;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.leidoslabs.holeshot.chipper.wms.v111.UriInfoGetCapabilitiesRequest111;
import com.leidoslabs.holeshot.chipper.wms.v111.UriInfoGetMapRequest111;
import com.leidoslabs.holeshot.chipper.wms.v130.UriInfoGetCapabilitiesRequest130;
import com.leidoslabs.holeshot.chipper.wms.v130.UriInfoGetMapRequest130;
import com.leidoslabs.holeshot.chipper.wms.v130.XmlGetCapabilitiesRequest130;
import com.leidoslabs.holeshot.chipper.wms.v130.XmlGetMapRequest130;

import net.opengis.ows.GetCapabilitiesType;
import net.opengis.sld.GetMapType;

@Path("/wms")
/**
 * REST API for WMS service 
 */
public class ImageWMSService {
   private static final Logger LOGGER = LoggerFactory.getLogger(ImageWMSService.class);
   private static final String GET_CAPABILITIES_REQUEST = "GetCapabilities";
   private static final String GET_MAP_REQUEST = "GetMap";

   @Context
   private UriInfo info;

   @Context private HttpServletRequest servletRequest;

   @GET
   @Produces({"application/xml", "image/png"})
   /**
    * Specifies servlet request based on type and version, then either responds
    * with the map chip or capabilities info depending on request
    * @return Either map chip or capabilities depending on request
    * @throws IOException
    */
   public Response get() throws IOException {
      final UriInfoParser uriInfoParser = new UriInfoParser(info);
      final String request = uriInfoParser.getRequest();
      Response response = null;
      if (request != null) {
         if (request.equalsIgnoreCase(GET_CAPABILITIES_REQUEST)) {
            GetCapabilitiesRequest getCapabilities;
            if (isVersion111(uriInfoParser)) {
               getCapabilities = new UriInfoGetCapabilitiesRequest111(info);
            } else {
               getCapabilities = new UriInfoGetCapabilitiesRequest130(info);
            }
            response = getCapabilities.getResponse(servletRequest);
         } else if (request.equalsIgnoreCase(GET_MAP_REQUEST)) {
            GetMapRequest getMap;
            if (isVersion111(uriInfoParser)) {
               getMap = new UriInfoGetMapRequest111(info);
            } else {
               getMap = new UriInfoGetMapRequest130(info);
            }
            response = getMap.getResponse(servletRequest);
         }
      }
      return response;
   }

   @POST
   @Produces({"image/png"})
   @Consumes({"application/xml"})
   /**
    * Chips out map from a get map request
    * @param request GetMapType request
    * @return map chip
    * @throws IOException
    */
   public Response getMap(GetMapType request) throws IOException {
      final GetMapRequest getMapRequest = new XmlGetMapRequest130(request);
      return getMapRequest.getResponse(servletRequest);
   }

   @POST
   @Produces({"application/xml"})
   @Consumes({"application/xml"})
   /**
    * Gets capabilities from capabilities request
    * @param request GetCapabilitiesType request
    * @return capabilities data
    * @throws IOException
    */
   public Response getCapabilities(GetCapabilitiesType request) throws IOException {
      return new XmlGetCapabilitiesRequest130(request).getResponse(servletRequest);
   }

   private boolean isVersion130(UriInfoParser uriInfoParser) {
      Pattern pattern = Pattern.compile("^1\\.3(\\.[0-9.]+)?$");
      return uriInfoParser.getVersion().stream().anyMatch(v->pattern.matcher(v).matches());
   }
   private boolean isVersion111(UriInfoParser uriInfoParser) {
      Pattern pattern = Pattern.compile("^1\\.1\\.1(\\.[0-9.]+)?$");
      return uriInfoParser.getVersion().stream().anyMatch(v->pattern.matcher(v).matches());
   }

}
