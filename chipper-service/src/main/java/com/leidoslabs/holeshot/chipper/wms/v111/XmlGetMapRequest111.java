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

package com.leidoslabs.holeshot.chipper.wms.v111;

import java.math.BigInteger;
import java.net.URL;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Response;
import javax.xml.datatype.XMLGregorianCalendar;

import org.apache.commons.lang3.Range;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.leidoslabs.holeshot.chipper.wms.GetMapRequest;
import com.leidoslabs.holeshot.chipper.wms.GetMapRequestHandler;
import com.leidoslabs.holeshot.chipper.wms.XmlWMSRequest;

import net.opengis.sld.ExceptionsType;
import net.opengis.sld.GetMapType;
import net.opengis.sld.NamedLayer;
import net.opengis.sld.NamedStyle;
import net.opengis.sld.OutputType;
import net.opengis.sld.OutputType.Size;
import net.opengis.sld.StyledLayerDescriptor;

public class XmlGetMapRequest111 extends XmlWMSRequest<GetMapType> implements GetMapRequest {
   private static final Logger LOGGER = LoggerFactory.getLogger(XmlGetMapRequest111.class);
   private final GetMapRequestHandler requestHandler;

   public XmlGetMapRequest111(GetMapType request) {
      super(request);
      requestHandler = new GetMapRequestHandler();
   }

   public List<String> getLayers() {
      final List<String> result = new ArrayList<String>();
      final StyledLayerDescriptor styledLayerDescriptor = getInfo().getStyledLayerDescriptor();
      if (styledLayerDescriptor != null) {
         final List<Object> layers = styledLayerDescriptor.getNamedLayerOrUserLayer();
         if (layers != null) {
            layers.stream()
            .filter(o-> o instanceof NamedLayer)
            .map(o->((NamedLayer)o).getName())
            .forEach(n -> result.add(n));
         }
      }
      return result;
   }
   public List<String> getStyles() {
      final List<String> result = new ArrayList<String>();
      final StyledLayerDescriptor styledLayerDescriptor = getInfo().getStyledLayerDescriptor();
      if (styledLayerDescriptor != null) {
         final List<Object> layers = styledLayerDescriptor.getNamedLayerOrUserLayer();
         if (layers != null) {
            layers.stream()
            .filter(o-> o instanceof NamedLayer)
            .map(o->((NamedLayer)o).getNamedStyleOrUserStyle())
            .filter(s-> s instanceof NamedStyle)
            .map(s->((NamedStyle)s).getName())
            .forEach(n -> result.add(n));
         }
      }
      return result;
   }
   public String getCrs() {
      return getInfo().getCRS();
   }
   public double[] getBbox() {
      double[] result = null;
      if (getInfo().getBoundingBox() != null) {
         final List<Double> lowerLeft = getInfo().getBoundingBox().getLowerCorner();
         final List<Double> upperRight = getInfo().getBoundingBox().getUpperCorner();

         if (lowerLeft != null && upperRight != null && lowerLeft.size() == 2 && upperRight.size() == 2) {
            final double minX = Math.min(lowerLeft.get(0), upperRight.get(0));
            final double maxX = Math.max(lowerLeft.get(0), upperRight.get(0));
            final double minY = Math.min(lowerLeft.get(1), upperRight.get(1));
            final double maxY = Math.max(lowerLeft.get(1), upperRight.get(1));

            result = new double[] { minX, minY, maxX, maxY };
         }
      }
      return result;
   }

   private Size getSize() {
      Size result = null;
      final OutputType output = getInfo().getOutput();
      if (output != null) {
         result = output.getSize();
      }
      if (result == null) {
         result = new Size();
         final BigInteger defaultValue = BigInteger.valueOf(-1);
         result.setWidth(defaultValue);
         result.setHeight(defaultValue);
      }
      return result;
   }
   public int getWidth() {
      return getSize().getWidth().intValue();
   }
   public int getHeight() {
      return getSize().getHeight().intValue();
   }
   public String getFormat() {
      String result = null;
      final OutputType output = getInfo().getOutput();

      if (output != null) {
         result = output.getFormat();
      }
      return result;
   }
   public boolean isTransparent() {
      boolean result = false;
      final OutputType output = getInfo().getOutput();

      if (output != null) {
         result = output.isTransparent();
      }
      return result;
   }
   public String getBgColor() {
      String result = null;
      final OutputType output = getInfo().getOutput();

      if (output != null) {
         result = output.getBGcolor();
      }
      return result;
   }
   public String getExceptions() {
      String result = null;
      final ExceptionsType exceptions = getInfo().getExceptions();

      if (exceptions != null) {
         result = exceptions.value();
      }
      return result;
   }

   public Range<ZonedDateTime> getTime() {
      Range<ZonedDateTime> result = null;
      final XMLGregorianCalendar time = getInfo().getTime();

      if (time != null) {
         ZonedDateTime instant = time.toGregorianCalendar().toZonedDateTime();

         result = Range.between(instant, instant, new Comparator<ZonedDateTime>() {
            @Override
            public int compare(ZonedDateTime o1, ZonedDateTime o2) {
               return o1.compareTo(o2);
            }
         });
      }
      return result;
   }

   public URL getSld() {
      return null;
   }
   public StyledLayerDescriptor getSldBody() {
      return getInfo().getStyledLayerDescriptor();
   }

   @Override
   public String getService() {
      return "WMS";
   }

   @Override
   public List<String> getVersion() {
      return Arrays.asList(getInfo().getVersion());
   }

   @Override
   public String getRequest() {
      return "GetMap";
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
