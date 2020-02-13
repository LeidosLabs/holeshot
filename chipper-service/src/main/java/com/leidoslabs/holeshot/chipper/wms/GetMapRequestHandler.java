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

import java.awt.Dimension;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.concurrent.ExecutionException;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import javax.ws.rs.core.Response.Status;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.leidoslabs.holeshot.chipper.ImageChipper;
import org.locationtech.jts.geom.Envelope;

public class GetMapRequestHandler {
   private static final Logger LOGGER = LoggerFactory.getLogger(GetMapRequestHandler.class);

   public GetMapRequestHandler() {
   }
   /**
    * User ImageChipper to respond to a WMS getMapRequest. 
    * @param getMapRequest WMS get map request, providing bounding box and output dimension
    * @return Chipped Image as a JAX RS response
    * @throws Exception
    */
   public Response getResponse(GetMapRequest getMapRequest) throws Exception {
      Response response = null;
      final List<String> layers = getMapRequest.getLayers();

      if (layers != null && layers.size() > 0) {
         URL firstLayer = new URL(layers.get(0));
         final double[] bbox = getMapRequest.getBbox();

         if (bbox != null && bbox.length == 4) {
            final Envelope bboxEnvelope = new Envelope(bbox[0],bbox[2],bbox[1],bbox[3]);
            final Dimension outputDim = new Dimension(getMapRequest.getWidth(), getMapRequest.getHeight());

            StreamingOutput stream = new StreamingOutput() {
               public void write(OutputStream os) throws IOException, WebApplicationException {
                  try (ImageChipper chipper = ImageChipper.borrow(firstLayer)) {
                     LOGGER.debug("chipByGeo " + bboxEnvelope.toString() + " " + outputDim.toString());
                     chipper.chipByGeo(bboxEnvelope, outputDim, os, true, false );
                  } catch (IllegalArgumentException e) {
                     throw new WebApplicationException(Response.status(Status.BAD_REQUEST).build());
                  } catch (FileNotFoundException e) {
                     throw new WebApplicationException(Response.status(Status.NOT_FOUND).build());
                  } catch (Exception e) {
                     LOGGER.error(e.getMessage(), e);
                     throw new WebApplicationException(Response.serverError().build());
                  }
               }
            };
            response = Response.ok(stream, "image/png").build();
         }
      }
      return response;
   }
}
