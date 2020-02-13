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

import java.io.StringReader;
import java.net.URL;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import org.apache.commons.lang3.Range;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.leidoslabs.holeshot.chipper.wms.GetMapRequest;
import com.leidoslabs.holeshot.chipper.wms.GetMapRequestHandler;
import com.leidoslabs.holeshot.chipper.wms.UriInfoWMSRequest;

import net.opengis.sld.StyledLayerDescriptor;


/**
 * Wrapper for GetMapRequestHandler from UriInfo. Also exposes Map request's query
 * data from the URI's info
 */
public class UriInfoGetMapRequest130 extends UriInfoWMSRequest implements GetMapRequest {
   private static final Logger LOGGER = LoggerFactory.getLogger(UriInfoGetMapRequest130.class);

   private final GetMapRequestHandler requestHandler;

   public UriInfoGetMapRequest130(UriInfo info) {
      super(info);
      requestHandler = new GetMapRequestHandler();
   }

   public List<String> getLayers() {
      return getUriInfo().getStringList("layers");
   }
   public List<String> getStyles() {
      return getUriInfo().getStringList("styles");
   }

   public String getCrs() {
      return getUriInfo().getString("srs", "crs");
   }
   public double[] getBbox() {
      double[] givenBbox = Arrays.stream(getUriInfo().getString("bbox").split(",")).mapToDouble(s->Double.parseDouble(s)).toArray();
      return givenBbox;
   }
   public int getWidth() {
      final int result = getUriInfo().getIntValue("width", -1);
      return result;
   }
   public int getHeight() {
      final int result = getUriInfo().getIntValue("height", -1);
      return result;
   }
   public String getFormat() {
      return getUriInfo().getString("format");
   }
   public boolean isTransparent() {
      return getUriInfo().getBoolValue("transparent", false);
   }
   public String getBgColor() {
      return getUriInfo().getString("bgcolor");
   }
   public String getExceptions() {
      return getUriInfo().getString("exceptions");
   }

   private static ZonedDateTime parseDefaultInstant(String src) {
      ZonedDateTime result = null;
      if (src != null) {
         final DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS").withZone(ZoneOffset.UTC);
         try {
            result = ZonedDateTime.parse(src, dateFormat);
         } catch (DateTimeParseException e) {
            LOGGER.error(e.getMessage(), e);
         }
      }
      return result;
   }
   public Range<ZonedDateTime> getTime() {
      Range<ZonedDateTime> result = null;
      List<String> stringParms = getUriInfo().getStringList("time");
      if (stringParms != null) {
         ZonedDateTime[] timeInstants = stringParms.stream().map(s->parseDefaultInstant(s)).toArray(ZonedDateTime[]::new);

         if (timeInstants.length >= 1) {
            ZonedDateTime start = timeInstants[0];
            ZonedDateTime end = start;
            if (timeInstants.length >= 2) {
               end = timeInstants[1];
            }
            result = Range.between(start, end, new Comparator<ZonedDateTime>() {
               @Override
               public int compare(ZonedDateTime o1, ZonedDateTime o2) {
                  return o1.compareTo(o2);
               }
            });
         }
      }
      return result;
   }
   public URL getSld() {
      return getUriInfo().getURLValue("sld");
   }
   public StyledLayerDescriptor getSldBody() {
      StyledLayerDescriptor result = null;
      String stringValue = getUriInfo().getString("sld_body");
      if (stringValue != null) {
         JAXBContext jaxbContext;
         try {
            jaxbContext = JAXBContext.newInstance(StyledLayerDescriptor.class);
            Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
            try (StringReader reader = new StringReader(stringValue)) {
               result = (StyledLayerDescriptor) unmarshaller.unmarshal(reader);
            }
         } catch (JAXBException e) {
            LOGGER.error("Couldn't parse sld body", e);
         }
      }
      return result;
   }

   @Override
   public Response getResponse(HttpServletRequest servletRequest) {
      Response response = null;
      try {
         response = requestHandler.getResponse(this);
      } catch (Exception e) {
         LOGGER.error(e.getMessage(), e);
         e.printStackTrace();
      }
      return response;
   }
}
