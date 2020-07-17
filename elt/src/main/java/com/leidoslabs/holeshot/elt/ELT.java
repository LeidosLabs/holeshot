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

import static com.leidoslabs.holeshot.elt.ELTDisplayExecutor.ExecMode.ASYNCHRONOUS;
import static com.leidoslabs.holeshot.elt.ELTDisplayExecutor.ExecMode.SYNCHRONOUS;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.eclipse.swt.widgets.Display;
import org.locationtech.jts.geom.Coordinate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.leidoslabs.holeshot.credentials.HoleshotCredentials;
import com.leidoslabs.holeshot.elt.coord.GeodeticELTCoordinate;
import com.leidoslabs.holeshot.elt.coord.ImageWorld;
import com.leidoslabs.holeshot.elt.observations.PointObservation;
import com.leidoslabs.holeshot.elt.tileserver.TileserverUrlBuilder;
import com.leidoslabs.holeshot.elt.utils.EHCache;
import com.leidoslabs.holeshot.elt.websocket.ELTWebSocket;
import com.leidoslabs.holeshot.imaging.coord.ImageScale;
import com.leidoslabs.holeshot.imaging.photogrammetry.CameraModel;
import com.leidoslabs.holeshot.imaging.photogrammetry.rpc.RPCCameraModelFactory;
import com.leidoslabs.holeshot.tileserver.v1.TilePyramidDescriptor;
import com.leidoslabs.holeshot.tileserver.v1.TileServerClient;
import com.leidoslabs.holeshot.tileserver.v1.TileServerClientBuilder;

/**
 * The main class for the HOLESHOT ELT (Electronic Light Table).  
 * 
 * This ELT allows for: 
 *    - reading and displaying of imagery from the HOLESHOT Tileserver.
 *    - Roaming, Zooming, and rotation of imagery
 *    - SIPS Processing at frame-rate speeds (https://nsgreg.nga.mil/NSGDOC/files/doc/Document/SIPS_v24_21Aug2019.pdf)  
 *    - Display of KML
 *    - REST/WebSocket interface for command, control, and telemetry of the ELT
 * 
 * ELT is a singleton that aggregates multiple ELTFrames.
 * 
 * @author robertsrg
 *
 */
public class ELT implements Runnable {
	private static final Logger LOGGER = LoggerFactory.getLogger(ELT.class);

	private final static boolean LOCAL_TEST_MODE = false;

	/**
	 * The singleton instance for this class
	 */
	private static ELT elt = null;

	/**
	 * Test mode for when you're running the ELT within the AWS environment.  Switches fetching of tiles to HTTP, rather than HTTPS
	 */
	private final static boolean AWS_TEST_MODE = false;
	private static final String TILESERVER_LOCAL_DOMAIN_NAME=AWS_TEST_MODE ? "tileserver.leidoslabs.com" : null;

	/**
	 * A map of application keys to their associated ELTFrame.  Application Keys are specified by the users of the ELT at frame creation.
	 */
	private HashMap<String, List<ELTFrame>> apps = new HashMap<String, List<ELTFrame>>();

	/**
	 * The API for communicating with the HOLESHOT Tileserver.
	 */
	private TileServerClient tileServerClient;

	/**
	 * The Credentials used to authenticate with the HOLESHOT Tileserver and Catalog.
	 */
	private HoleshotCredentials creds = HoleshotCredentials.getApplicationDefaults();

	/**
	 * The ELT is intended to be run as a Windows service and is accessible via the Windows Systems Tray.
	 */
	private ELTSystemTray eltSystemTray;


	/**
	 * The SWT Display for the ELT.  The ELT utilizes SWT primarily to take advantage of its browser widget for displaying KML Description popups. 
	 */
	private final Display display;

	/**
	 * Embedded web server used to handle REST and WebSockets API.
	 */
	private JettyServer jettyServer;

	/**
	 * Flag to indicate whether the ELT is decorated or not with overlays, buttons, etc.  A non-decorated ELT is utilized by the ImageChipper when
	 * the ELT is run server-side.
	 */
	private final boolean isDecorated;

	/**
	 * Flag indicating whether progressive rendering is turned on.   When this flag is set to false, the ELT will wait until all tiles are fetched
	 * before rendering a frame.  This is useful when using this code serverside as an image chip renderer.
	 */
	private final boolean isProgressiveRender;

	/**
	 * The executor that ensures that a given task is performed in the appropriate context, taking into account SWT and OpenGL
	 */
	private final ELTDisplayExecutor eltDisplayExecutor;

	private static final int FRAMES_PER_SECOND = 45;
	private static final long SKIP_TICKS = 1_000_000_000 / FRAMES_PER_SECOND;
	private static final int MAX_FRAMESKIP = 3;
	private long nextGameTick;
	private long fps;
	private long lastFPS;

	static {
		if (AWS_TEST_MODE) {
			TileServerClientBuilder.setLocalDomainName(TILESERVER_LOCAL_DOMAIN_NAME);
		}
	}

	/**
	 * Creates the one and only instance of the ELT for this singleton
	 * @param display The SWT Display
	 * @param isDecorated Flag to indicate whether the ELT is decorated or not with overlays, buttons, etc.  A non-decorated ELT is utilized 
	 *    by the ImageChipper when the ELT is run server-side.
	 * @param progressiveRender Flag indicating whether progressive rendering is turned on.   When this flag is set to false, the ELT will 
	 *    wait until all tiles are fetched before rendering a frame.  This is useful when using this code serverside as an image chip renderer.
	 * @return
	 */
	public static synchronized ELT createInstance(Display display, boolean isDecorated, boolean progressiveRender) {
		try {
			if (elt == null) {
				elt = new ELT(display, isDecorated, progressiveRender);
			}
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
		return elt;
	}

	/**
	 * Retrieve the one and only instance of the ELT.
	 *    {@link #createInstance(Display, boolean, boolean)} must be called prior to this method being invoked.
	 * @return The one and only instance of this singleton
	 */
	public static ELT getInstance() {
		return elt;
	}

	/**
	 * The DisplayContext for the ELT.
	 * Ensures that actions executed are syncd with SWT and OpenGL resources.
	 * 
	 * @author robertsrg
	 *
	 */
	private class DisplayContext extends ELTDisplayContext {
		@Override
		public void asyncExec(Runnable runnable) {
			getDisplay().asyncExec(runnable);
		}
		@Override
		protected void setOpenGLContextCurrent() {
		}
		@Override
		public void syncExec(Runnable runnable) {
			getDisplay().syncExec(runnable);
		}
		@Override
		public synchronized boolean setContextThread() {
			return super.setContextThread() || (Thread.currentThread() != getDisplay().getThread());
		}

	}

	/**
	 * Constructor
	 * @param display The SWT Display
	 * @param isDecorated Flag to indicate whether the ELT is decorated or not with overlays, buttons, etc.  A non-decorated ELT is utilized 
	 *    by the ImageChipper when the ELT is run server-side.
	 * @param progressiveRender Flag indicating whether progressive rendering is turned on.   When this flag is set to false, the ELT will 
	 *    wait until all tiles are fetched before rendering a frame.  This is useful when using this code serverside as an image chip renderer.
	 * @throws IOException
	 */
	private ELT(Display display, boolean isDecorated, boolean progressiveRender) throws IOException {
		this.display = display;
		this.isDecorated = isDecorated;
		this.isProgressiveRender = progressiveRender;
		this.eltDisplayExecutor = new ELTDisplayExecutor(new DisplayContext());

		if (isDecorated) {
			tileServerClient = TileServerClientBuilder
					.forEndpoint("https://tileserver.leidoslabs.com/tileserver")
					.withCredentials(creds)
					.build();

			jettyServer = new JettyServer();
			jettyServer.startServer();

			if (ELTSystemTray.isSupported()) {
				eltSystemTray = new ELTSystemTray(()->
				{
					try {
						eltDisplayExecutor.submit(ASYNCHRONOUS, () -> {
							EHCache.getInstance().shutdown();

							getAllELTFrames().stream().filter(a->!a.isDisposed()).forEach(a->a.close());

							// Make sure shutdown hooks are invoked
							System.exit(0);
						});
					} catch (Exception e) {
						e.printStackTrace();
					}
				});
			}
		}

	}

	/**
	 * Get the system time
	 * @return System time in milliseconds
	 */
	private static long getTime() {
		return System.nanoTime() / 1000000;
	}

	/**
	 * Update the frames per second count and output it to the logger once per second.
	 * This method should be called on each frame.
	 */
	private void updateFPS() {
		if (getTime() - lastFPS > 1000) {
			//         LOGGER.debug(String.format("FPS: %d", fps));
			fps = 0; //reset the FPS counter
			lastFPS += 1000; //add one second
		}
		fps++;
	}

	/**
	 * Start ELT execution
	 */
	public void run() {
		fps=0;
		lastFPS = getTime();


		Runnable renderRunnable = new Runnable() {
			public void run() {
				nextGameTick = System.nanoTime();
				for (int loops = 0; System.nanoTime() > nextGameTick && loops < MAX_FRAMESKIP; ++loops, nextGameTick += SKIP_TICKS) {
					Iterator<ELTFrame> iter = getAllELTFrames().iterator();
					while (iter.hasNext()) {
						ELTFrame frame = iter.next();
						if (frame.isDisposed()) {
							if (ELTWebSocket.isLinked(frame.getELTCanvas().getAppId())) {
								ELTWebSocket.sendClose(frame.getELTCanvas().getAppId());
							}
							removeELTFrame(frame);
						} else {
							frame.updateAnimation();
						}
					}
				}

				double interpolation = (double)(System.nanoTime() + SKIP_TICKS - nextGameTick) / (double) SKIP_TICKS;
				Iterator<ELTFrame> iter = getAllELTFrames().iterator();
				while (iter.hasNext()) {
					ELTFrame frame = iter.next();
					if (!frame.isDisposed()) {
						frame.render(interpolation);
					} else {
						removeELTFrame(frame);
					}
				}
				updateFPS();
			}
		};

		new Thread( () ->
		{
			while (true) {
				try {
					eltDisplayExecutor.submit(SYNCHRONOUS, renderRunnable);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}).start();

		// =====================================
		// Main UI event dispatch loop
		// that handles all UI events from all
		// SWT components created as children of
		// the main Display object
		// =====================================
		while (!display.isDisposed()) {
			if (!display.readAndDispatch()) {
				display.sleep();
			}
		}

		try {
			jettyServer.stop();
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	/**
	 * Main entry point for the ELT
	 * @param args - NOT USED AT THIS TIME
	 */
	public static void main(String[] args) {
		try {
			final AtomicReference<Display> display = new AtomicReference<Display>(null);

			Thread initThread = new Thread( () -> {
				try {
					while (display.get() == null || ELT.getInstance() == null) {
						Thread.sleep(500);
					}
					final String tileserver = LOCAL_TEST_MODE ? "localhost:9080" : "tileserver.leidoslabs.com";
					final String protocol = LOCAL_TEST_MODE ? "http" : "https";
					Arrays.stream(new String[] {
							//                 "XVIEWCHALLENGE-00005/20180116034512",
							"058618316010_01_P006/20180124071004",
							//                 "058618316010_01_P001/20171010070654",
							//               "06JUL07QB021300007JUL06185940-P1BS-005614887010_01_P046/20070706185940",
							//               "08DEC08082220-P1BS-059339019010_01_P001/20190320015541",
							//               "08MAY21080843-P1BS-059339041010_01_P001/20190320020303",
							//               "09DEC27083330-M1BS-059338983010_01_P001/20190320014406",
							//               "09DEC27083330-P1BS-059338983010_01_P001/20190320014814",
							//               "10JAN15083955-M1BS-059338993010_01_P001/20190320012856",
							//               "10JAN15083955-P1BS-059338993010_01_P001/20190320013337",
							//               "12APR08082311-P1BS-059338982010_01_P001/20190320003816",
							//               "12APR10082845-M1BS-059339030010_01_P001/20190320002553",
							//               "12APR10082845-P1BS-059339030010_01_P001/20190320003021",
							//               "12APR20090115-M1BS-059339029010_01_P001/20190320002139",
							//               "12APR20090115-P1BS-059339029010_01_P001/20190320001432",
							//               "12APR23084909-M1BS-059338989010_01_P001/20190320000353",
							//               "12APR23084909-P1BS-059338989010_01_P001/20190320000728",
							//               "12APR24084530-P1BS-059339057010_01_P001/20190319235647",
							//               "12APR25082332-P1BS-059339039010_01_P001/20190319234935",
							//               "12APR29082824-P1BS-059338990010_01_P001/20190319234113",
							//               "12FEB25083357-P1BS-059339021010_01_P001/20190320010819",
							//               "12FEB25083357-P1BS-059339034010_01_P001/20190320011449",
							//               "12JUL08084915-M1BS-059338996010_01_P001/20190319201113",
							//               "12JUL08084915-P1BS-059338996010_01_P001/20190319200434",
							//               "12JUL16085600-M1BS-059338984010_01_P001/20190319175507",
							//               "12JUL16085600-P1BS-059338984010_01_P001/20190319175540",
							//               "12JUL23083011-P1BS-059339023010_01_P001/20190319193707",
							//               "12JUN14083503-M1BS-059338987010_01_P001/20190319202303",
							//               "12JUN14083503-P1BS-059338987010_01_P001/20190319202728",
							//               "12JUN27084012-P1BS-059339046010_01_P001/20190319201508",
							//               "12MAR17084023-P1BS-059339002010_01_P001/20190320010020",
							//               "12MAR22082251-P1BS-059338986010_01_P001/20190320004549",
							//               "12MAR22082251-P1BS-059339042010_01_P001/20190320005315",
							//               "12MAY01085602-M1BS-059338985010_01_P001/20190319232554",
							//               "12MAY01085602-P1BS-059338985010_01_P001/20190319233031",
							//               "12MAY09090048-M1BS-059339048010_01_P001/20190320210135",
							//               "12MAY09090048-P1BS-059339048010_01_P001/20190320205804",
							//               "12MAY09090108-M1BS-059339007010_01_P001/20190319221837",
							//               "12MAY09090108-P1BS-059339007010_01_P001/20190319223515",
							//               "12MAY28090019-M1BS-059338992010_01_P001/20190319204736",
							//               "12MAY28090019-P1BS-059338992010_01_P001/20190319205212",
							//               "12MAY31084858-M1BS-059338988010_01_P001/20190319204301",
							//               "12MAY31084858-P1BS-059338988010_01_P001/20190319203618",
							//               "12OCT20083344-P1BS-059339058010_01_P001/20190320023635",
							//               "12SEP08090346-M1BS-059339011010_01_P001/20190320030401",
							//               "12SEP08090346-M1BS-059339056010_01_P001/20190320030832",
							//               "12SEP08090346-P1BS-059339011010_01_P001/20190320025645",
							//               "12SEP08090346-P1BS-059339056010_01_P001/20190320031240",
							//               "12SEP27090244-M1BS-059339024010_01_P001/20190320025239",
							//               "12SEP27090244-P1BS-059339024010_01_P001/20190320024558",
							//               "13MAY06IK0101216po_235274_pan_0000000/20060513184358",  // NITF problem black stripes from sides
							//               "14AUG06082955-P1BS-059339054010_01_P001/20190320022349",
							//               "14FEB06SyrianAirfield/20021216151629", // NO RPCs
							//               "14MAY13082344-P1BS-059339031010_01_P001/20190320022948",
							//               "17FEB18112528-P1BS-059339022010_01_P001/20190311170351",
							//               "17FEB18112528-P1BS-059339052010_01_P001/20190320021659",
							//               "17MAY18113409-P1BS-059339060010_01_P001/20190320021013",
							//               "17SEP02111248-P1BS-059339028010_01_P001/20190320012126",
							//               "19SEP06IK0101920po_235091_nir_0000000/20060919184408", // Problems with black borders
							//               "23JUN07QB021300007JUN23190439-M1BS-005614884014_01_P003/20070623190439"
					}).map(s->String.format("%s://%s/tileserver/%s", protocol, tileserver, s))
					.map(s->{
						ELTFrame result = null;
						try {
							String replaceMe = String.format("%s://%s/tileserver/", protocol, tileserver);
							result = ELT.getInstance().loadImage(s.replace(replaceMe, "").replaceAll("/", "_"), s, true, true);
						} catch (IOException | InterruptedException | ExecutionException e) {
							e.printStackTrace();
						}
						return result;
					})
					.filter(s->s!=null)
					.forEach(f-> display.get().asyncExec(()->f.open()));
				} catch (Throwable e) {
					e.printStackTrace();
				} 
			});
			initThread.start();
			display.set(new Display());

			createInstance(display.get(), true, true).run();
			initThread.join();

		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			System.exit(0);
		}

	}

	/**
	 * Determines if the given app is known to the ELT
	 * @param appID The AppID to check
	 * @return
	 */
	public boolean exists(String appID) {
		return apps.containsKey(appID);
	}

	/**
	 * Returns a map of the currently running apps and their associated ELTFrames
	 * @return
	 */
	Map<String, List<ELTFrame>> getApps() {
		return apps;
	}

	/**
	 * Loads the given image into the ELT
	 * 
	 * @param appID The application context that defines the ELTFrame that the image will be loaded into.
	 * @param url The url to the image to be loaded, up to and including the timestamp (e.g. https://tileserver.leidoslabs.com/030303/49494 )
	 * @param newWindow Boolean flag indicating whether the image should be loaded into a new window  
	 * @param resetViewport Boolean flag indicating whether the viewport should stay at the same geolocation, zoom, and rotation.
	 * @return The ELTFrame that the image is loaded into.
	 * @throws IOException
	 * @throws InterruptedException
	 * @throws ExecutionException
	 */
	public ELTFrame loadImage(String appID, String url, boolean newWindow, boolean resetViewport) throws IOException, InterruptedException, ExecutionException {
		final AtomicReference<ELTFrame> result = new AtomicReference<ELTFrame>();
		final URL imageMetadataURL = new URL(String.format("%s/metadata.json", url));

		eltDisplayExecutor.submit(SYNCHRONOUS, ()-> {
			try {
				ELTFrame appFrame = getLastLoadedAppImage(appID);
				if (newWindow || (appFrame == null)) {
					if (appFrame != null) {
						appFrame.close();
					}
					appFrame = new ELTFrame(display, imageMetadataURL, appID, isDecorated, isProgressiveRender);
					addAppFrame(appID, appFrame);
					LOGGER.debug(String.format("loading %s in new window", url));
				} else {
					LOGGER.debug(String.format("loading %s in existing window", url));
					if (ELTWebSocket.isLinked(appID)) {ELTWebSocket.sendImageLoad(appID, url);}
					appFrame.getELTCanvas().setImage(imageMetadataURL, resetViewport);
				}
				result.set(appFrame);
			} catch (IOException | InterruptedException | ExecutionException e) {
				LOGGER.error(e.getMessage(), e);
			}
		});
		return result.get();
	}

	/**
	 * Loads the given image and zooms to the given point and zoom level.
	 * @param appID The application context for the load (Affects the meaning of newWindow and resetViewport)
	 * @param url The URL to the image, up-to-and-including the timestamp.  (e.g. https://tileserver.leidoslabs.com/39393/5959534 ) 
	 * @param newWindow Boolean flag indicating whether the image should be loaded into an existing window (the last loaded one) or into a new window.
	 * @param resetViewport Boolean flag indicating whether the window should stay at the same geospatial position, rotation, and zoom level.
	 * @param lat The latitude of the new center of the viewport.
	 * @param lon The longitude of the new center of the viewport.
	 * @param zoom The rset to zoom into.
	 * @throws IOException
	 * @throws InterruptedException
	 * @throws ExecutionException
	 */
	public void loadImageOnCoordinate(String appID, String url, boolean newWindow, Double lat, Double lon, Integer zoom) throws IOException, InterruptedException, ExecutionException {
		loadImage(appID, url, newWindow, false);
		eltDisplayExecutor.submit(SYNCHRONOUS, () -> {
			if(lat != null && lon != null && zoom != null ) {
				this.centerOnCoordinate(appID, lat.doubleValue(), lon.doubleValue(), zoom.intValue());
			}
		});
	}


	/**
	 * Retrieve the Metadata for the given image url
	 * @param url The image URL, up-to-and-including the timestamp (e.g. https://tileserver.leidoslabs.com/390933/9393000 )
	 * @return The metadata for the given image.
	 * @throws IOException
	 */
	private TilePyramidDescriptor getDescriptor(String url) throws IOException {
		TileserverUrlBuilder tileserverURL = new TileserverUrlBuilder(new URL(String.format("%s/metadata.json", url)));

		return tileServerClient.getMetadata(tileserverURL.getCollectionID(), tileserverURL.getTimestamp());
	}

	/**
	 * Retrieves the bounding box for the given image
	 * @param url The image URL, up-to-and-including the timestamp (e.g. https://tileserver.leidoslabs.com/390933/9393000 )
	 * @return A CSV String representing the bounding box for the given image "longitude_west, latitude_south, longitude_east, latitude_north"
	 */
	public String getBoundingBox(String url) {
		String result = "";
		// Parse JSON from metadata.json by using tilepyramiddescriptor
		try {
			TileserverUrlBuilder tileserverURL = new TileserverUrlBuilder(new URL(String.format("%s/metadata.json", url)));

			TilePyramidDescriptor descriptor = getDescriptor(url);

			if (descriptor != null) {
				double[] boundingBox = descriptor.getBoundingBox();
				if (boundingBox != null) {
					result = Arrays.stream(boundingBox).mapToObj(d->Double.toString(d)).collect(Collectors.joining(","));
				}
			}
		} catch(Exception e) {
			e.printStackTrace();
		}
		return result;
	}

	/**
	 * Gets the WKT viewport of the last loaded image for the given appID
	 * @param appID The application ID to use for looking up the last loaded image
	 * @return WKT of the Polygon viewport for the given appId
	 */
	public String getViewportBoundsWKT(String appID) {
		String result = "";
		try {
			ELTFrame app = getLastLoadedAppImage(appID);
			result = app.getELTCanvas().getImageWorld().getGeodeticViewport().toText();
		} catch(Exception e) {
			e.printStackTrace();
		}
		return result;
	}

	/**
	 * Zooms the viewport of the last loaded image under the given appID to the image extent of the given image.
	 * TODO: Find out why we have this method?  It seems really obscure.  Is anyone using it?
	 * 
	 * @param appID The context for the last loaded image.
	 * @param url The image URL, up-to-and-including the timestamp (e.g. https://tileserver.leidoslabs.com/390933/9393000 ).  The metadata
	 *    for this image will be retrieved and used to define the bounding box to zoom the given viewport to.
	 */
	public void goToBoundingBox(String appID, String url) {
		ELTFrame app = getLastLoadedAppImage(appID);

		// using boundingBox from metadata as test, imageworld for conversion to opengl center coord
		// change later to use double latlong parameters instead
		// seems like whatever image is loaded gets 0,0 as center since that's what's onscreen
		// otherwise the center of the image you're doing goTo is relative to where your view is?
		// AKA goToBoundingBox only seems to work once the image has been loaded first? (it might be converting correctly, but it converts relative to current view bc it uses imageworld?)
		try {
			TilePyramidDescriptor descriptor = getDescriptor(url);
			if (descriptor != null) {
				double[] boundingBox = descriptor.getBoundingBox();

				CameraModel cameraModel =
						RPCCameraModelFactory.buildRPCCameraFromMetadata((Map<String,Object>)descriptor.getMetadata());
				double defaultElevation = cameraModel.getDefaultElevation();
				// in boundingBox example's case, it goes [minLong, minLat, maxLong, maxLat]
				// AKA [long_west, lat_south, long_east, lat_north]
				double long_west = boundingBox[0];
				double lat_south = boundingBox[1];
				double long_east = boundingBox[2];
				double lat_north = boundingBox[3];
				double long_center = (long_west+long_east)/2.0;
				double lat_center = (lat_south+lat_north)/2.0;
				Coordinate center = new Coordinate(long_center, lat_center, defaultElevation);

				// get heights and widths of image at R0
				double imageWidth = (double)descriptor.getWidth();
				double imageHeight = (double)descriptor.getHeight();

				// get width and height of view from ImageWorld
				ImageWorld world = app.getELTCanvas().getImageWorld();
				double viewWidth = (double) ImageWorld.DEFAULT_WIDTH;
				double viewHeight = (double) ImageWorld.DEFAULT_HEIGHT;

				// calculating rset for zoomTo to use
				double xScale = viewWidth / imageWidth;
				double yScale = viewHeight / imageHeight;
				double minScale = Math.min(xScale,  yScale);
				ImageScale imageScale = ImageScale.forRset(new ImageScale(minScale, minScale).getImageRset(descriptor.getMaxRLevel()));

				// only need GeodeticCoordinate for center now, use that for zoomTo - so don't even need zoomToBoundingBox in imageWorld?
				GeodeticELTCoordinate geoCoord = new GeodeticELTCoordinate(world, center, imageScale);
				world.zoomTo(geoCoord, imageScale);
			}
		} catch(Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Retrieves the newest ELTFrame for the given appId
	 * @param appId The context for the last loaded image.
	 * @return The newest ELTFrame for the given appId.
	 */
	public ELTFrame getLastLoadedAppImage(String appId) {
		ELTFrame result = null;
		List<ELTFrame> eltFrames = apps.get(appId);
		if (CollectionUtils.isNotEmpty(eltFrames)) {
			result = eltFrames.get(eltFrames.size()-1);
		}
		return result;
	}

	/**
	 * Add the given ELTFrame to the list of appIds for the given app
	 * @param appId The app by which to register the ELTFrame under
	 * @param frame The frame to register.
	 */
	private void addAppFrame(String appId, ELTFrame frame) {
		List<ELTFrame> eltFrames = apps.get(appId);
		if (eltFrames == null) {
			eltFrames = new ArrayList<ELTFrame>();
			apps.put(appId, eltFrames);
		}
		eltFrames.add(frame);
	}

	/**
	 * Retrieves a list of all ELTFrames known to the system.
	 * @return A list of all ELTFrames, spanning all appIds
	 */
	List<ELTFrame> getAllELTFrames() {
		return apps.values().stream().flatMap(t->t.stream()).collect(Collectors.toList());
	}

	/**
	 * Removes the given ELTFrame from the list of frames for the app
	 * @param eltFrame The ELT frame to remove
	 * @return A flag indicating whether the frame existed.
	 */
	private boolean removeELTFrame(ELTFrame eltFrame) {
		boolean removed = false;
		if (eltFrame != null) {
			List<ELTFrame> eltFrames = apps.get(eltFrame.getAppId());
			if (eltFrames != null) {
				removed = eltFrames.remove(eltFrame);
			}
		}
		return removed;
	}

	/**
	 * Centers the newest ELTFrame for the given appID on the given lat/lon and rset 
	 * @param appId The application ID used to look up the newest ELTFrame
	 * @param lat The latitude to set the viewport's center to.
	 * @param lon The longitude to set the viewport's center to.
	 * @param zoom The rset to zoom to.
	 */
	public void centerOnCoordinate(String appId, double lat, double lon, int zoom) {
		Coordinate center = new Coordinate(lon, lat, 0);
		ImageWorld world = getLastLoadedAppImage(appId).getELTCanvas().getImageWorld();
		ImageScale imageScale = world.getImageScale();
		imageScale.setRset((double) zoom);
		GeodeticELTCoordinate geoCoord = new GeodeticELTCoordinate(world, center, imageScale);
		world.zoomTo(geoCoord, imageScale);
	}

	/**
	 * Highlight the given observation in the given layer on the newest ELTFrame for the given appId
	 * @param appId The application ID used to locate the newest ELTFrame
	 * @param companionID The id of the observation to highlight.
	 * @param layerName The layer name that the observation exists in. (Not currently used, as all features currently exist in the placemark layer)
	 */
	public void highlightCoordinate(String appId, String companionID,String layerName) {
		getLastLoadedAppImage(appId).getELTCanvas().highlightObservation(companionID, layerName);
	}

	/**
	 * De-selects all features on the newest ELTFrame for the given appId
	 * @param appId The application ID used to locate the newest ELTFrame
	 */
	public void unselectAll(String appId) {
		getLastLoadedAppImage(appId).getELTCanvas().unselectAll();
	}

	/**
	 * Deletes the given observation ID from the newest ELTFrame for the given appID
	 * @param appId The application ID used to locate the newest ELTFrame
	 * @param companionId The ID of the observation to delete from the view
	 */
	public void deleteObservation(String appId, String companionId) {
		getLastLoadedAppImage(appId).getELTCanvas().deleteObservation(companionId);
	}

	/**
	 * Creates new placemarks for the given list of observations.
	 * 
	 * @param appId The application ID used to locate the newest ELTFrame
	 * @param points The list of observations to create.
	 * @param layerName The layer name to create the placemarks in. (Not currently used, as all features currently exist in the placemark layer)
	 */
	public void setPlacemarks(String appId, List<PointObservation> points, String layerName) {
		getLastLoadedAppImage(appId).getELTCanvas().setPlacemarks(points, layerName);
	}

	/**
	 * Removes the given layer from the newest ELTFrame for the given appId
	 * @param appId The application ID used to locate the newest ELTFrame
	 * @param layerName The layer name to remove
	 */
	public void removeLayer(String appId, String layerName) {
		getLastLoadedAppImage(appId).getELTCanvas().removeLayer(layerName);
	}

	/**
	 * Accessor
	 * @return The SWT Display
	 */
	private Display getDisplay() {
		return display;
	}

}
