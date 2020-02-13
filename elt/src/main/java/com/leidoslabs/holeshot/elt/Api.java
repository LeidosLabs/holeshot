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

package com.leidoslabs.holeshot.elt;

import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.ExecutionException;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.commons.lang3.mutable.MutableObject;
import org.eclipse.swt.widgets.Display;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.leidoslabs.holeshot.elt.websocket.ELTWebSocket;


/**
 * The HOLESHOT ELT contains an embedded Jetty Web Server that allows for 
 * automation via a REST interface as well as state notification via Web Sockets.
 * This Api defines these interfaces.
 * @author robertsrg
 *
 */
@Path("/elt")
public class Api {
   private static final Logger LOGGER = LoggerFactory.getLogger(Api.class);

   /**
    * Load a new image into the ELT
    * 
    * Tile URL format: https://leidoslabs.com/tileserver/[collectionID]/[timestamp]/[rlevel]/[column]/[row]/[band].png
    * e.g. https://tileserver.leidoslabs.com/tileserver/0000000000/20010220065958000/0/0/0/1.png?skipcache=true
    * 
    *  Metadata URL format: https://leidoslabs.com/tileserver/[collectionID]/[timestamp]/metadata.json
    *  e.g. https://tileserver.leidoslabs.com/tileserver/0000000000/20010220065958000/metadata.json
    * @param appId - A unique identifier for setting the context for the request.
    *   Items like newWindow and resetViewport are relative to this context 
    * @param url The url to the image to load, up to and including the timestamp.
    *   e.g. https://tileserver.leidoslabs.com/tileserver/0000000000/939393
    * @param newWindow A boolean flag( true|false ) indicating whether to load 
    *   the image into an existing window or into a new window.
    * @param resetViewport A boolean flag indicating whether to reset the 
    *   viewport to fit the entire image in the viewport or to leave it at it's 
    *   current zoom and rotation. 
    * @return the HTTP Response
    */

	@POST
    @Path("image/{appId}/{url}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response loadImage(
    		@PathParam("appId") String appId,
            @PathParam("url") String url,
			@DefaultValue("true") @QueryParam("newWindow") boolean newWindow,
			@DefaultValue("true") @QueryParam("resetViewport") boolean resetViewport
			) {
		try {
         ELT.getInstance().loadImage(appId, url, newWindow, resetViewport);
      } catch (IOException | InterruptedException | ExecutionException e) {
         e.printStackTrace();
      }
		String ret = "{\"loadImage\":\"" + url + "\"}";
        return Response.ok(ret, MediaType.TEXT_PLAIN).build();
    }

	/**
	 * Returns the bounding box of the extent for the given image URL.  
	 *    longitude_west, latitude_south, longitude_east, latitude_north
	 * @param appId The application context for the request
	 * @param url The url to the image to retrieve the bounding box for, up to
	 *   and including the timestamp.
     *   e.g. https://tileserver.leidoslabs.com/tileserver/0000000000/838383
	 * @return The bounding box of the given image ( longitude_west, latitude_south, longitude_east, latitude_north )
	 */
	@GET
    @Path("boundingbox/{appId}/{url}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getBoundingBox(
    		@PathParam("appId") String appId,
		    @PathParam("url") String url) {
      final MutableObject<String> resp = new MutableObject<String>();
      Display.getDefault().syncExec(() -> {
         resp.setValue(ELT.getInstance().getBoundingBox(url));
      });
    	return Response.ok("Bounding Box: " + resp.getValue(), MediaType.TEXT_PLAIN).build();
    }

	/**
	 * Moves the ELT to the bounding box of the given image.  
	 * TODO: Find out why we have this method?  It seems really obscure.  Is anyone using it?
	 * 
	 * @param appId The application context for the request
	 * @param url The url of the image to zoom to, up to and including the timestamp.
    *   e.g. https://tileserver.leidoslabs.com/tileserver/0000000000/349393
	 * @return The HTTP Result
	 */
	@PUT
    @Path("boundingbox/{appId}/{url}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response goToBoundingBox(
    		@PathParam("appId") String appId,
		    @PathParam("url") String url) {
	   final MutableObject<String> resp = new MutableObject<String>();
      Display.getDefault().syncExec(() -> {
    	   resp.setValue("");
      });
    	return Response.ok(resp.getValue(), MediaType.TEXT_PLAIN).build();
    }

	/**
	 * Retrieve currently loaded application contexts and images.
	 * @return List of the currently loaded applications and their associated images
	 *    e.g. [ { "appId":"foo1", "imageId": "0000000000/838383"}, ... ]
	 */
    @GET
    @Path("apps")
    @Produces(MediaType.APPLICATION_JSON)
    public Response availableApps() {
	    ArrayList<String> elements = new ArrayList<>();
        ELT.getInstance().getAllELTFrames().forEach( f ->
                elements.add("{\"appId\":\""+f.getAppId()+"\",\"imageId\":\""+f.getELTCanvas().getImage().getCollectionID() + ":" + f.getELTCanvas().getImage().getTimestamp() + "\"}")
        );

        return Response.ok("[" + String.join(",", elements) + "]", MediaType.APPLICATION_JSON).build();
    }

    /**
     * Returns the bounds of the current viewport for the given appId
     * @param appId
     * @return WKT of the Polygon viewport for the given appId
     */
    @GET
    @Path("viewport/{appId}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getViewport(@PathParam("appId") String appId) {
        final MutableObject<String> resp = new MutableObject<String>();
        Display.getDefault().syncExec(() -> {
            resp.setValue(ELT.getInstance().getViewportBoundsWKT(appId));
        });
        LOGGER.debug(resp.toString());
        return Response.ok(resp, MediaType.APPLICATION_JSON).build();
    }
    
    
    /**
     * Opens an image, centered on given point and at a given zoom level.
     * 
     * @param appID The application context to load the image into.
	 * @param url The url of the image to zoom to, up to and including the timestamp.
     *   e.g. https://tileserver.leidoslabs.com/tileserver/0000000000/349393
     * @param lat The latitude to set the center of the viewport to
     * @param lon The longitude to set the center of the viewport to
     * @param zoom The Rset to zoom the viewport to
     * @param newWindow A boolean flag( true|false ) indicating whether to load 
     *   the image into an existing window or into a new window.
     * @param resetViewport TODO: Remove this parameter, it doesn't make sense for this method. 
     * @return the HTTP Response
     */
    @GET
    @Path("open/{appID}/{url}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response open(
    		@PathParam("appID") String appID,
    		@PathParam("url") String url,
		    @QueryParam("lat") double lat,
		    @QueryParam("lon") double lon,
		    @QueryParam("zoom") int zoom,
		    @DefaultValue("true") @QueryParam("newWindow") boolean newWindow,
			@DefaultValue("true") @QueryParam("resetViewport") boolean resetViewport) {
    	try {
    		ELT.getInstance().loadImageOnCoordinate(appID, url, newWindow, Double.valueOf(lat), Double.valueOf(lon), Integer.valueOf(zoom));
    	} catch (IOException | InterruptedException | ExecutionException e) {
			e.printStackTrace();
		}
        return Response.ok("", MediaType.TEXT_PLAIN).build();
    }

    /**
     * Center the viewport on a given point and at a given zoom level.
     * 
     * @param appID The application context to load the image into.
     * @param lat The latitude to set the center of the viewport to
     * @param lon The longitude to set the center of the viewport to
     * @param zoom The Rset to zoom the viewport to
     * @return the HTTP Response
     */
    @POST
    @Path("zoomTo/{appId}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response goToPoint(
            @PathParam("appId") String appId,
            @QueryParam("lat") double lat,
            @QueryParam("lon") double lon,
            @QueryParam("zoom") int zoom)
    {
        Display.getDefault().syncExec(() -> {
            ELT.getInstance().centerOnCoordinate(appId, lat, lon, zoom);
        });
        return Response.ok("", MediaType.TEXT_PLAIN).build();
    }

    
    /**
     * Broadcasts the given event to all Web Socket subscribers of the given app
     * 
     * @param message The message to broadcast
     * @param appId The app id to broadcast messages to
     * @return
     */
    @POST
    @Path("event")
    @Produces(MediaType.APPLICATION_JSON)
    public Response event(String message, @QueryParam("appId") String appId) {
        ELTWebSocket.handleBroadcast(message, appId);
        return Response.ok("", MediaType.TEXT_PLAIN).build();
    }
//    @GET
//    @Path("kml/{appId}")
//    @Produces(MediaType.TEXT_PLAIN)
//    public Response getKML(@PathParam("appId") String appId) {
//
//	    List<PointObservation> placemarks = ELT.getInstance().getPlacemarks(appId);
//	    List<PolygonObservation> polygons = ELT.getInstance().getPolygons(appId);
//
//        Kml kml = KmlFactory.createKml();
//        Document kmlDoc = kml.createAndSetDocument().withName(appId + " Placemarks");
//
//        placemarks.forEach(p ->
//                kmlDoc.createAndAddPlacemark()
//                        .createAndSetPoint()
//                            .withExtrude(false)
//                            .withAltitudeMode(AltitudeMode.CLAMP_TO_GROUND)
//                            .addToCoordinates(p.getGeometry().getCoordinate().x, p.getGeometry().getCoordinate().y)
//        );
//
//        OutputStream os = new ByteArrayOutputStream();
//        try {
//            kml.marshal(os);
//        } catch(FileNotFoundException fnfe) {
//          return Response.serverError().build();
//        }
//	    return Response.ok( ((ByteArrayOutputStream) os).toByteArray(), MediaType.TEXT_PLAIN).build();
//    }
//
//    @POST
//    @Path("kml/{appId}")
//    @Consumes(MediaType.TEXT_PLAIN)
//    public Response loadKML(@PathParam("appId") String appId, String message) {
//
//	    Kml kml = Kml.unmarshal(message);
//	    Document doc  =  (Document) kml.getFeature();
//
//	    List<PointObservation> placemarks = new ArrayList<>();
//
//	    doc.getFeature().forEach(p ->
//	        placemarks.add(new PointObservation(new org.locationtech.jts.geom.Coordinate(
//                    ((Point) ((Placemark) p).getGeometry()).getCoordinates().get(0).getLongitude(),
//                    ((Point) ((Placemark) p).getGeometry()).getCoordinates().get(0).getLatitude(), 0.0))));
//
//	    ELT.getInstance().addPlacemarks( appId, placemarks);
//
//	    return Response.ok().build();
//    }
}
