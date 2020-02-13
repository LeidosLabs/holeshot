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
import com.leidoslabs.holeshot.tileserver.service.S3Handler;

import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.ext.XLogger;
import org.slf4j.ext.XLoggerFactory;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;


/**
 * Main servlet for the WMTS, handles tile and metadata requests over HTTP
 */
@Path("/")
public class WMTSService {

    private static final Logger LOGGER = LoggerFactory.getLogger(WMTSService.class);
    private static final XLogger REQUEST_LOGGER = XLoggerFactory.getXLogger("requests");
    //private final S3Handler s3Handler = new S3Handler("advanced-analytics-geo-tile-images", "us-east-1", 1);
    private final S3Handler s3Handler;
    private final String tileServerUrl;

    @Context
    private UriInfo info;

    @Context private HttpServletRequest servletRequest;

    public WMTSService(String tileServerUrl, S3Handler s3Handler) {
        this.tileServerUrl = tileServerUrl;
        this.s3Handler = s3Handler;
    }

    
    /**
     * Creates a metadata request for WMTS capabilities, and return it's response
     * @param imageId
     * @param timestamp
     * @return
     * @throws IOException Error Initializing the ServiceMetadata Handler
     */
    @GET
    @Produces({"application/xml"})
    @Path("/wmts/1.0.0/{imageId}/{timestamp}/WMTSCapabilities.xml")
    public Response getServiceMetadata(
            @PathParam("imageId") String imageId,
            @PathParam("timestamp") String timestamp) throws IOException {
        Response response = null;
        GetServiceMetadataRequest getCapabilities;

        try {
            getCapabilities = new GetServiceMetadataRequest(tileServerUrl, imageId, timestamp);
        } catch (IOException ioe) {
            LOGGER.error("Error Initializing the ServiceMetadata Handler", ioe);
            return Response.status(500).build();
        } catch (NotFoundException nfe) {
            LOGGER.error("Cannot build ServiceMetadata, metadata not found in catalog", nfe);
            return Response.status(404).build();
        }

        response = getCapabilities.getResponse();

        return response;
    }

    
    /**
     * Uses S3 Handler to fetch tile from either S3 or Cache
     * @param imageId
     * @param timestamp
     * @param rSet
     * @param col
     * @param row
     * @param band
     * @return
     */
    @GET
    @Produces({"image/png"})
    @Path("{imageId}/{timestamp}/tile/{rSet}/{col}/{row}/{band}.png")
    public Response getTile(@PathParam("imageId") String imageId,
                        @PathParam("timestamp") String timestamp,
                        @PathParam("rSet") String rSet,
                        @PathParam("col") String col,
                        @PathParam("row") String row,
                        @PathParam("band") String band) {
        Response response = null;
        response = s3Handler.getResponse(servletRequest, String.join("/", imageId, timestamp, rSet, col, row, band + ".png"));
        return response;
    }

    /**
     * getTileOld is the same as getTile but without "tile" in the path, making it compatible with the non-wmts client.
     * "tile" is present as having a style associated with the layer is mandatory per WMTS Spec 10.2.1, Table 32
     */
    @GET
    @Produces({"image/png"})
    @Path("{imageId}/{timestamp}/{rSet}/{col}/{row}/{band}.png")
    public Response getTileOld(@PathParam("imageId") String imageId,
                            @PathParam("timestamp") String timestamp,
                            @PathParam("rSet") String rSet,
                            @PathParam("col") String col,
                            @PathParam("row") String row,
                            @PathParam("band") String band) {
        Response response = null;
        logTile(imageId, timestamp, rSet, col, row);
        response = s3Handler.getResponse(servletRequest, String.join("/", imageId, timestamp, rSet, col, row, band + ".png"));
        return response;
    }

    
    /**
     * Fetch metadata as json
     * @param imageId
     * @param timestamp
     * @return
     */
    @GET
    @Produces({"application/json"})
    @Path("{imageId}/{timestamp}/metadata.json")
    public Response getMetadata(@PathParam("imageId") String imageId,
                        @PathParam("timestamp") String timestamp) {
        Response response = null;
        response = s3Handler.getResponse(servletRequest, String.join("/", imageId, timestamp, "metadata.json"));
        return response;
    }
    
    private void logTile(String imageId, String timestamp, String rSet, String col, String row) {
    	Map<String,String> msgMap = new HashMap<>();
        msgMap.put("imageID", imageId+ ":" + timestamp);
        msgMap.put("rSet", rSet);
        msgMap.put("x", col);
        msgMap.put("y", row);
        JSONObject message = new JSONObject(msgMap);
        REQUEST_LOGGER.info(message.toString());
    }
}
