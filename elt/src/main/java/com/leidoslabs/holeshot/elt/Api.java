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

import java.io.FileNotFoundException;
import java.util.Arrays;
import java.util.stream.Collectors;

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
import javax.ws.rs.core.Response.Status;

import org.locationtech.jts.geom.Envelope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.leidoslabs.holeshot.elt.layers.BaseLayer;
import com.leidoslabs.holeshot.elt.tileserver.TileserverImage;
import com.leidoslabs.holeshot.elt.tileserver.TileserverUrlBuilder;
import com.leidoslabs.holeshot.elt.viewport.ImageProjection;
import com.leidoslabs.holeshot.elt.viewport.WGS84Projection;
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
			@DefaultValue("true") @QueryParam("resetViewport") boolean resetViewport,
			@DefaultValue("true") @QueryParam("replaceImage") boolean replaceImage
			) {
		Response result;
		try {
			ELT.getInstance().loadImage(appId, url, newWindow, resetViewport, replaceImage);
			String ret = "{\"loadImage\":\"" + url + "\"}";
			result = Response.ok(ret, MediaType.TEXT_PLAIN).build();
		} catch (FileNotFoundException fnfe) {
			result = Response.status(Status.NOT_FOUND).entity(fnfe.toString()).type(MediaType.TEXT_PLAIN).build();
		} catch(Throwable e) {
			result = Response.serverError().entity(e.getMessage()).build();
			e.printStackTrace();
		}
		return result;
	}

	/**
	 * Returns the bounding box of the extent for the given image URL.  
	 *    longitude_west, latitude_south, longitude_east, latitude_north
	 * @param url The url to the image to retrieve the bounding box for, up to
	 *   and including the timestamp.
	 *   e.g. https://tileserver.leidoslabs.com/tileserver/0000000000/838383
	 * @return The bounding box of the given image ( longitude_west, latitude_south, longitude_east, latitude_north )
	 */
	@GET
	@Path("boundingbox/{url}")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getBoundingBox(
			@PathParam("url") String url) {
		Response result;

		// Parse JSON from metadata.json by using tilepyramiddescriptor
		try {
			final TileserverImage image = new TileserverImage(TileserverUrlBuilder.getImageMetadataURL(url));
			final Envelope imageBounds = image.getGeodeticBounds().getEnvelopeInternal();

			String bbox = String.format("%.6f,%.6f,%.6f,%.6f", imageBounds.getMinX(), imageBounds.getMinY(), imageBounds.getMaxX(), imageBounds.getMaxY());
			result = Response.ok("Bounding Box: " + bbox, MediaType.TEXT_PLAIN).build();
		} catch (FileNotFoundException fnfe) {
			result = Response.status(Status.NOT_FOUND).entity(fnfe.toString()).type(MediaType.TEXT_PLAIN).build();
		} catch(Throwable e) {
			result = Response.serverError().entity(e.getMessage()).build();
			e.printStackTrace();
		}

		return result; 
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
		Response result;

		// Parse JSON from metadata.json by using tilepyramiddescriptor
		try {
			final ELTFrame frame = ELT.getInstance().getLastLoadedAppImage(appId);

			if (frame != null) {
				final ELTCanvas canvas = frame.getELTCanvas();
				final TileserverImage image = new TileserverImage(TileserverUrlBuilder.getImageMetadataURL(url));
				canvas.zoomToImage(image);
				result = Response.ok("", MediaType.TEXT_PLAIN).build();
			} else {
				result = Response.status(Status.NOT_FOUND).entity("AppID doesn't have any open frames").type(MediaType.TEXT_PLAIN).build();
			}
		} catch (FileNotFoundException fnfe) {
			result = Response.status(Status.NOT_FOUND).entity(fnfe.toString()).type(MediaType.TEXT_PLAIN).build();
		} catch(Throwable e) {
			result = Response.serverError().entity(e.getMessage()).build();
			e.printStackTrace();
		}
		return result;
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
		String json = 
				String.format("[%s]",
						ELT.getInstance().getAllELTFrames().stream().map( f ->
						String.format("{\"appId\":\"%s\",%s}",f.getAppId(), getLoadedImagesJsonArray(f))).collect(Collectors.joining(",")));

		return Response.ok(json, MediaType.APPLICATION_JSON).build();
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
		Response result;
		try {
			String wkt = ELT.getInstance().getViewportBoundsWKT(appId);
			result = Response.ok(wkt, MediaType.TEXT_PLAIN).build();
			LOGGER.debug(wkt);
		} catch (AppNotFoundException anfe) {
			result = Response.status(Status.NOT_FOUND).entity(anfe.toString()).type(MediaType.TEXT_PLAIN).build();
		} catch(Throwable e) {
			result = Response.serverError().entity(e.getMessage()).build();
			e.printStackTrace();
		}
		return result;
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
			@DefaultValue("true") @QueryParam("newWindow") boolean newWindow) {
		Response result;
		try {
			ELT.getInstance().loadImageOnCoordinate(appID, url, newWindow, Double.valueOf(lat), Double.valueOf(lon), Integer.valueOf(zoom));
			result = Response.ok("", MediaType.TEXT_PLAIN).build();
		} catch (FileNotFoundException fnfe) {
			result = Response.status(Status.NOT_FOUND).entity(fnfe.toString()).type(MediaType.TEXT_PLAIN).build();
		} catch(Throwable e) {
			result = Response.serverError().entity(e.getMessage()).build();
			e.printStackTrace();
		}
		return result;
		
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
	@GET
	@Path("zoomTo/{appId}")
	@Produces(MediaType.APPLICATION_JSON)
	public Response goToPoint(
			@PathParam("appId") String appId,
			@QueryParam("lat") double lat,
			@QueryParam("lon") double lon,
			@QueryParam("zoom") int zoom)
	{
		Response result;
		try {
		    ELT.getInstance().centerOnCoordinate(appId, lat, lon, zoom);
			result = Response.ok("", MediaType.TEXT_PLAIN).build();
		} catch (AppNotFoundException anfe) {
			result = Response.status(Status.NOT_FOUND).entity(anfe.toString()).type(MediaType.TEXT_PLAIN).build();
		} catch(Throwable e) {
			result = Response.serverError().entity(e.getMessage()).build();
			e.printStackTrace();
		}
		return result;
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

	private static String getLoadedImagesJsonArray(ELTFrame frame) {
		final BaseLayer baseLayer = frame.getELTCanvas().getLayerManager().getBaseLayer();
		WGS84Projection projection = baseLayer.getImageWorld().getProjection();
		TileserverImage[] images;
		if (projection instanceof ImageProjection) {
			images = new TileserverImage[] { ((ImageProjection)projection).getImage()};
		} else {
			images = baseLayer.getImages();
		}
		String json = String.format("\"images\": [%s]",
				Arrays.stream(images).map(i->String.format("{ \"imageId\": \"%s:%s\" }", i.getCollectionID(), i.getTimestamp())).collect(Collectors.joining(",")));
		return json;
	}
}
