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

package com.leidoslabs.holeshot.elt.websocket;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.*;
import java.util.stream.Stream;

import javax.websocket.*;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;

import com.leidoslabs.holeshot.elt.UserConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.leidoslabs.holeshot.elt.AppNotFoundException;
import com.leidoslabs.holeshot.elt.ELT;
import com.leidoslabs.holeshot.elt.observations.Observation;
import com.leidoslabs.holeshot.elt.observations.PointObservation;
import com.leidoslabs.holeshot.elt.websocket.events.WebSocketEvent;
import com.leidoslabs.holeshot.elt.websocket.geojson.Feature;
import com.leidoslabs.holeshot.elt.websocket.geojson.Geometry;
import com.leidoslabs.holeshot.elt.websocket.geojson.Point;
import com.leidoslabs.holeshot.elt.websocket.geojson.Polygon;
import org.locationtech.jts.geom.Coordinate;

@ServerEndpoint(value="/events/{appId}")
/**
 * WebSocket for ELT, serves ELT instance to remote clients
 */
public class ELTWebSocket {
	private static final Logger LOGGER = LoggerFactory.getLogger(ELTWebSocket.class);

	private static final Map<String, List<Session>> sessionMap = Collections.synchronizedMap(new HashMap<>());
	private static Type eventType = new TypeToken<WebSocketEvent>(){}.getType();
	private static Gson gson = new GsonBuilder().registerTypeAdapter(Geometry.class, new Geometry.TypeAdapter<>()).create();
	private static boolean pauseEvents = false;

	/**
	 * When a remote client connects, map the session to the ELT app it is talking to, so that the ELT can
	 * send messages back to the remote client (browser)
	 * @param session The session identifier that will be used to send and receive messages
	 * @param appId The ID of the ELT Frame or remote client that the connection will be married to
	 */
	@OnOpen
	public void onOpen(@PathParam("appId") String appId, Session session) {
		LOGGER.debug("WebSocket Connection Established: " + session);
		List<Session> appSessions = sessionMap.get(appId);
		if(appSessions == null) {
			appSessions = Collections.synchronizedList(new ArrayList<>());
		}

		appSessions.add(session);
		sessionMap.put(appId, appSessions);
	}

	@OnMessage
	/**
	 * Response to web socket messages
	 * @param appId
	 * @param message
	 * @param session
	 */
	public void onMessage(@PathParam("appId") String appId, String message, Session session) {
		handleEvent(message, appId, session);
	}

	@OnClose
	/**
	 * Response to web socket closure
	 * @param appId
	 * @param reason
	 * @param session
	 */
	public void onClose(@PathParam("appId") String appId, CloseReason reason, Session session) {
		LOGGER.debug("WebSocket Connection to " + appId + " Closed: " + reason);
		pauseEvents = true;
		ELT.getInstance().removeLayer(appId, session.getId());
		sessionMap.get(appId).remove(session);
		if(sessionMap.get(appId).size() == 0) {
			sessionMap.remove(appId);
		}
		pauseEvents = false;
	}

	@OnError
	public void onError(Throwable e) {
		LOGGER.error(e.getMessage(), e);
	}

	/**
	 * Handle a WebsocketEvent event, sent as a string. Can modify elt instance or 
	 * broadcast to other sessions depending on event type
	 * @param message message string
	 * @param appId Application ID
	 * @param session
	 */
	public static void handleEvent(String message, String appId, Session session) {
		try {
			WebSocketEvent event = gson.fromJson(message, eventType);

			switch(event.getEvent()) {
			case WebSocketEvent.CENTER_EVENT:
				Point centerPoint = (Point) event.getGeoJSON().getFeatures()[0].getGeometry();
				ELT.getInstance().centerOnCoordinate(appId, centerPoint.getLat(), centerPoint.getLon(), 0);
				break;
			case WebSocketEvent.LOAD_EVENT:
				List<PointObservation> points = new ArrayList<>();
				for (Feature feature : event.getGeoJSON().getFeatures()) {
					if(feature.getGeometry().getType().equals("Point")) {
						Point loadPoint = (Point) feature.getGeometry();
						LOGGER.debug(feature.getProperties().get("title"));
						points.add(new PointObservation(new Coordinate(loadPoint.getLon(), loadPoint.getLat(), 0.0), feature.getProperties()));
					}
				}
				ELT.getInstance().setPlacemarks(appId, points, session.getId());
				break;
			case WebSocketEvent.HIGHLIGHT_EVENT:
				ELT.getInstance().unselectAll(appId);
				if(event.getGeoJSON().getFeatures().length != 0) {
					String companionID = event.getGeoJSON().getFeatures()[0].getProperties().get("id");
					LOGGER.debug("Received highlight event with id: " + companionID);
					ELT.getInstance().highlightCoordinate(appId, companionID, session.getId());
				}
				break;
			case WebSocketEvent.DELETE_EVENT:
				String draftToDelete = event.getGeoJSON().getFeatures()[0].getProperties().get("id");
				LOGGER.debug("Received delete event with id: " + draftToDelete);
				ELT.getInstance().deleteObservation(appId, draftToDelete);
				break;
			case WebSocketEvent.SPATIAL_FILTER_EVENT:
				Polygon polygon = (Polygon) event.getGeoJSON().getFeatures()[0].getGeometry();
				broadcastBoundsEvent(polygon.getCoordinates()[0]);
				break;
			case WebSocketEvent.TIME_FILTER_EVENT:
				broadcastEvent(event);
				break;

			}
		} catch (AppNotFoundException e) {
			LOGGER.warn(e.getMessage(), e);
		}

	}

	public static void handleBroadcast(String message, String appId) {
		WebSocketEvent event = gson.fromJson(message, eventType);
		switch(event.getEvent()) {
		case WebSocketEvent.SPATIAL_FILTER_EVENT:
			Polygon polygon = (Polygon) event.getGeoJSON().getFeatures()[0].getGeometry();
			broadcastBoundsEvent(polygon.getCoordinates()[0]);
			break;
		case WebSocketEvent.TIME_FILTER_EVENT:
			broadcastEvent(event);
			break;
		}
	}

	public static void sendOverlayBBox(String appId, Coordinate ll, Coordinate ur) {
		WebSocketEvent event = new WebSocketEvent.Builder()
				.ofType(WebSocketEvent.OVERLAY_EVENT)
				.withPolyFromBBox(ll, ur)
				.build();

		sendEvent(appId, event);
	}

	public static void sendCenterPoint(String appId, Coordinate center) {
		WebSocketEvent event = new WebSocketEvent.Builder()
				.ofType(WebSocketEvent.CENTER_EVENT)
				.withPoint(center)
				.build();

		sendEvent(appId, event);
	}

	public static void sendClose(String appId) {
		sessionMap.get(appId).forEach(session -> {
			if(session != null) {
				try {
					session.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		});
	}

	public static void sendOverlay(String appId, Coordinate[] coordinates) {
		if(coordinates.length == 0)
			return;
		WebSocketEvent event = new WebSocketEvent.Builder()
				.ofType(WebSocketEvent.OVERLAY_EVENT)
				.withPolygon(coordinates)
				.build();

		sendEvent(appId, event);
	}

	public static void sendPoint(String appId, String observationId, Coordinate[] coordinates) {
		LOGGER.debug("Sending WS Point event");
		Map<String, String> properties= new HashMap<String, String>();
		properties.put("id", observationId);
		WebSocketEvent event = new WebSocketEvent.Builder()
				.ofType("point")
				.withPolygon(coordinates, properties)
				.build();

		sendEvent(appId, event);
	}

	public static void sendSelection(String appId, String observationId, Coordinate[] coordinates) {
		LOGGER.debug("Sending WS selected point event");
		Map<String, String> properties = new HashMap<String, String>();
		properties.put("id", observationId);
		WebSocketEvent event = new WebSocketEvent.Builder()
				.ofType(WebSocketEvent.HIGHLIGHT_EVENT)
				.withString(coordinates, properties)
				.build();

		sendEvent(appId, event);
	}

	public static void sendImageLoad(String appId, String imageURL) {
	    // use a better regex to extract imageId (last part of URL, https://tileserver/part1/part2 -> part1:part2)
		String imageId = imageURL.replace(UserConfiguration.getTileserviceEndpoint(), "").replaceAll("/", ":");
		System.out.println("imageId: " + imageId);
		Map<String, String> properties= new HashMap<>();
		properties.put("imageID", imageId);
		WebSocketEvent event = new WebSocketEvent.Builder()
				.ofType("loadImage")
				.withProperties(properties)
				.build();
		sendEvent(appId, event);
	}

	public static void broadcastBoundsEvent(Double[][] coordinateArray) {
		//rebuild before broadcasting to make sure we have the bbox member
		WebSocketEvent event = new WebSocketEvent.Builder()
				.ofType("bounds")
				.withCoordinateArray(coordinateArray)
				.build();
		broadcastEvent(event);
	}

	/**
	 * Sends an event to every currently open session
	 * @param event
	 */
	private static void broadcastEvent(WebSocketEvent event) {
		synchronized (sessionMap) {
			Iterator<Map.Entry<String, List<Session>>> sessionIterator = sessionMap.entrySet().iterator();

			while (sessionIterator.hasNext()) {
				sessionIterator.next().getValue().forEach(session -> session.getAsyncRemote().sendText(gson.toJson(event)));
			}
		}
	}

	/**
	 * Sends an event to every session associated with the given appId
	 * @param appId
	 * @param event
	 */
	private static void sendEvent(String appId, WebSocketEvent event) {

		try {
			List<Session> sessions = sessionMap.get(appId);
			if (sessions != null) {
				Iterator<Session> appSubscriberIterator = sessions.iterator();
				while (appSubscriberIterator.hasNext() && !pauseEvents) {
					appSubscriberIterator.next().getAsyncRemote().sendText(gson.toJson(event));
				}
			}
		} catch(ConcurrentModificationException cme) {
			System.err.println("Got a CME, continuing anyways");
		}
	}

	public static boolean isLinked(String appId) {
		return sessionMap.containsKey(appId);
	}

	public static void purgeConnections(String appId) {
		pauseEvents = false;
		if(sessionMap.get(appId) == null) {
			return;
		}
		sessionMap.get(appId).forEach(session -> {
			try {
				session.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		});
	}

}
