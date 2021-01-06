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
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import com.leidoslabs.holeshot.analytics.common.model.User;
import org.apache.commons.collections4.CollectionUtils;
import org.eclipse.swt.widgets.Display;
import org.locationtech.jts.geom.Coordinate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.collections4.CollectionUtils;
import org.eclipse.swt.widgets.Display;
import org.locationtech.jts.geom.Coordinate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.primitives.Doubles;
import com.leidoslabs.holeshot.credentials.HoleshotCredentials;
import com.leidoslabs.holeshot.elt.analytics.CacheListClient;
import com.leidoslabs.holeshot.elt.analytics.TelemetryClient;
import com.leidoslabs.holeshot.elt.coord.GeodeticELTCoordinate;
import com.leidoslabs.holeshot.elt.observations.PointObservation;
import com.leidoslabs.holeshot.elt.tileserver.TileserverImage;
import com.leidoslabs.holeshot.elt.tileserver.TileserverUrlBuilder;
import com.leidoslabs.holeshot.elt.utils.EHCache;
import com.leidoslabs.holeshot.elt.viewport.ImageProjection;
import com.leidoslabs.holeshot.elt.viewport.ImageWorld;
import com.leidoslabs.holeshot.elt.websocket.ELTWebSocket;
import com.leidoslabs.holeshot.tileserver.v1.TileServerClient;
import com.leidoslabs.holeshot.tileserver.v1.TileServerClientBuilder;

/**
 * The main class for the HOLESHOT ELT (Electronic Light Table).
 *
 * This ELT allows for:
 *    - reading and displaying of imagery from the HOLESHOT Tileserver.
 *    - Roaming, Zooming, and rotation of imagery
 *    - SIPS Processing at frame-rate speeds (https://nsgreg.nga.mil/NSGDOC/files/doc/Document/SIPS_v24_21Aug2019.pdf)
 *    - REST/WebSocket interface for command, control, and telemetry of the ELT
 *
 * ELT is a singleton that aggregates multiple ELTFrames.
 *
 * @author robertsrg
 *
 */
public class ELT implements Runnable {
	private static final Logger LOGGER = LoggerFactory.getLogger(ELT.class);

	// Interval between telemetry uploads in seconds
	private static long telemetryInterval = 300;
	private static boolean cacheListDisabled = true;
	// Interval between telemetry uploads in seconds
	private static long cacheListInterval = 60 * 60 * 24;

	/**
	 * The singleton instance for this class
	 */
	private static ELT elt = null;

	/**
	 * Test mode for when you're running the ELT within the AWS environment.  Switches fetching of tiles to HTTP, rather than HTTPS
	 */
	private final static boolean AWS_TEST_MODE = false;
	private static final String TILESERVER_LOCAL_DOMAIN_NAME=AWS_TEST_MODE ? "tileserver-dev.leidoslabs.com" : null;

	/**
	 * A map of application keys to their associated ELTFrame.  Application Keys are specified by the users of the ELT at frame creation.
	 */
	private HashMap<String, List<ELTFrame>> apps = new HashMap<String, List<ELTFrame>>();

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

	static {
		if (AWS_TEST_MODE) {
			TileServerClientBuilder.setLocalDomainName(TILESERVER_LOCAL_DOMAIN_NAME);
		}

		// This is here so that we can initialize ImageWorld static structures at startup rather than on the first image load.
		// The initialize() method is currently a no-op, but it will read in and intialize the WEB_MERCATOR_PROJECTION.
		ImageWorld.initialize();
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
		elt = (elt == null) ? new ELT(display, isDecorated, progressiveRender) : elt;
		return getInstance();
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
	 * Constructor
	 * @param display The SWT Display
	 * @param isDecorated Flag to indicate whether the ELT is decorated or not with overlays, buttons, etc.  A non-decorated ELT is utilized
	 *    by the ImageChipper when the ELT is run server-side.
	 * @param progressiveRender Flag indicating whether progressive rendering is turned on.   When this flag is set to false, the ELT will
	 *    wait until all tiles are fetched before rendering a frame.  This is useful when using this code serverside as an image chip renderer.
     */
	private ELT(Display display, boolean isDecorated, boolean progressiveRender) {
		this.display = display;
		this.isDecorated = isDecorated;
		this.isProgressiveRender = progressiveRender;

		LOGGER.debug("Creating a new instance of ELT");

		if (isDecorated) {
		    LOGGER.debug("Creating decorated instance");
		    LOGGER.info("Username: " + UserConfiguration.getUsername());
			jettyServer = new JettyServer();
			jettyServer.startServer();

			if (ELTSystemTray.isSupported()) {
				eltSystemTray = new ELTSystemTray(()->
				{
					try {
						display.asyncExec(() -> {
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

		LOGGER.debug("ELT Instance Created");

	}

	/**
	 * Get the system time
	 * @return System time in milliseconds
	 */
	private static long getTime() {
		return System.nanoTime() / 1000000;
	}

	private void cleanupOldFrames() {
					Iterator<ELTFrame> iter = getAllELTFrames().iterator();
					while (iter.hasNext()) {
						ELTFrame frame = iter.next();
						if (frame.isDisposed()) {
							if (ELTWebSocket.isLinked(frame.getELTCanvas().getAppId())) {
								ELTWebSocket.sendClose(frame.getELTCanvas().getAppId());
							}
							removeELTFrame(frame);
						}
					}
				}

	/**
	 * Start ELT execution
	 */
	public void run() {
		Runnable renderRunnable = new Runnable() {
			public void run() {
				cleanupOldFrames();

				Iterator<ELTFrame> iter = getAllELTFrames().iterator();
				while (iter.hasNext()) {
					ELTFrame frame = iter.next();
					frame.updateAnimation();
					frame.render(1.0);
				}
			}
		};

		new Thread( () ->
		{
			while (true) {
				try {
					display.syncExec(renderRunnable);
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
	 */
	public static void main(String[] args) {
		argParse(args);
        UserConfiguration.init();
		TelemetryClient backgroundTelemetry = null;
		CacheListClient check = null;
		try {
			final AtomicReference<Display> display = new AtomicReference<Display>(null);

            // Hardcoded image-load bootstrap for testing and development.
			Thread initThread = new Thread( () -> {
				try {
				    // Wait for the display and ELT to be initiated before trying to launch the images
					while (display.get() == null || ELT.getInstance() == null) {
						Thread.sleep(500);
					}
					Arrays.stream(new String[] {

							//PORT GROUP							
							//               "XVIEWCHALLENGE-00005/20180116034512",
							//							"XVIEWCHALLENGE-00007/20180116041148",
							//							"XVIEWCHALLENGE-00008/20180116042437",
							//							"XVIEWCHALLENGE-00009/20180116043638",
							//							"XVIEWCHALLENGE-00010/20180116000752",
							//							"XVIEWCHALLENGE-00011/20180116002541",
							//							"XVIEWCHALLENGE-00012/20180116003817",
							//							"XVIEWCHALLENGE-00018/20180116013228",
							//							"XVIEWCHALLENGE-00020/20180116020325",
							//							"XVIEWCHALLENGE-00024/20180116024006",
							//							"XVIEWCHALLENGE-00027/20180116031317",
							//							"XVIEWCHALLENGE-00031/20180116032032",
							//							"XVIEWCHALLENGE-00032/20180116032254",

							// FARM GROUP							
							//							"XVIEWCHALLENGE-00033/20180116032514",
							//							"XVIEWCHALLENGE-00035/20180116032645",

							// CITY GROUP							
							//							"XVIEWCHALLENGE-00038/20180116033141",
							//							"XVIEWCHALLENGE-00040/20180116033449",
							//							"XVIEWCHALLENGE-00041/20180116033512",
							//							"XVIEWCHALLENGE-00042/20180116033654",
							//							"XVIEWCHALLENGE-00043/20180116033728",
							//							"XVIEWCHALLENGE-00046/20180116034008",
							//							"XVIEWCHALLENGE-00047/20180116034105",
							//							"XVIEWCHALLENGE-00053/20180116034838",


//							// AIRPORT GROUP		--RGR
//														"XVIEWCHALLENGE-00069/20180116041020",
//														"XVIEWCHALLENGE-00072/20180116041337",
//														"XVIEWCHALLENGE-00073/20180116041425",
//														"XVIEWCHALLENGE-00074/20180116041628",
//														"XVIEWCHALLENGE-00075/20180116041804",
//														"XVIEWCHALLENGE-00079/20180116042339",
//														"XVIEWCHALLENGE-00080/20180116042444",
//														"XVIEWCHALLENGE-00083/20180116042838",
//														"XVIEWCHALLENGE-00084/20180116042916",
//														"XVIEWCHALLENGE-00086/20180116043024",
//														"XVIEWCHALLENGE-00087/20180116043131",
//														"XVIEWCHALLENGE-00088/20180116043149",
//														"XVIEWCHALLENGE-00089/20180116043455",
//														"XVIEWCHALLENGE-00090/20180116043645",
//														"XVIEWCHALLENGE-00091/20180116043753",
//														"XVIEWCHALLENGE-00092/20180116043903",
//														"XVIEWCHALLENGE-00093/20180116044022",
//														"XVIEWCHALLENGE-00094/20180116044114",
//														"XVIEWCHALLENGE-00095/20180116044219",
//														"XVIEWCHALLENGE-00096/20180116044301",
//														"XVIEWCHALLENGE-00097/20180116044421",
//														"XVIEWCHALLENGE-00098/20180116044439",
//														"XVIEWCHALLENGE-00099/20180116044531",
//														"XVIEWCHALLENGE-00100/20180116000752",
//														"XVIEWCHALLENGE-00102/20180116000752",
//														"XVIEWCHALLENGE-00104/20180116000908",
//														"XVIEWCHALLENGE-00105/20180116001230",
//														"XVIEWCHALLENGE-00106/20180116001543",


							// PORT GROUP					 		
							"XVIEWCHALLENGE-00107/20180116001820",
							"XVIEWCHALLENGE-00108/20180116002104",
							"XVIEWCHALLENGE-00109/20180116002258",
							"XVIEWCHALLENGE-00110/20180116002549",
							"XVIEWCHALLENGE-00111/20180116002652",
							"XVIEWCHALLENGE-00112/20180116002847",
							"XVIEWCHALLENGE-00118/20180116003602",
							"XVIEWCHALLENGE-00121/20180116004000",
							"XVIEWCHALLENGE-00122/20180116004138",
							"XVIEWCHALLENGE-00124/20180116004245",
							"XVIEWCHALLENGE-00125/20180116004340",
							"XVIEWCHALLENGE-00126/20180116004430",
							"XVIEWCHALLENGE-00128/20180116004603",
							"XVIEWCHALLENGE-00129/20180116004647",
							"XVIEWCHALLENGE-00130/20180116004709",
							"XVIEWCHALLENGE-00131/20180116004810",
							"XVIEWCHALLENGE-00136/20180116005103",
							"XVIEWCHALLENGE-00140/20180116005418",
							"XVIEWCHALLENGE-00142/20180116005649",
							"XVIEWCHALLENGE-00143/20180116005858",
							"XVIEWCHALLENGE-00144/20180116010109",
							"XVIEWCHALLENGE-00145/20180116010303",
							"XVIEWCHALLENGE-00147/20180116010654",
							"XVIEWCHALLENGE-00149/20180116011009",
							"XVIEWCHALLENGE-00157/20180116011529",
							"XVIEWCHALLENGE-00158/20180116011621",
							"XVIEWCHALLENGE-00159/20180116011723",
							"XVIEWCHALLENGE-00163/20180116012022"
							
//							"15JUL06SyrianAirfield/20150706084001"

							// AIRPORT GROUP							
							//							"XVIEWCHALLENGE-00178/20180116012919",
							//							"XVIEWCHALLENGE-00180/20180116013251",
							//							"XVIEWCHALLENGE-00181/20180116013430",
							//							"XVIEWCHALLENGE-00193/20180116015518",
							//							"XVIEWCHALLENGE-00201/20180116020520",
							//							"XVIEWCHALLENGE-00203/20180116020933",
							//							"XVIEWCHALLENGE-00205/20180116021141",
							//							"XVIEWCHALLENGE-00207/20180116021233",
							//							"XVIEWCHALLENGE-00216/20180116021955",
							//							"XVIEWCHALLENGE-00217/20180116022037",
							//							"XVIEWCHALLENGE-00221/20180116022234",
							//							"XVIEWCHALLENGE-00223/20180116022338",
							//							"XVIEWCHALLENGE-00237/20180116023614",
							//							"XVIEWCHALLENGE-00238/20180116023735",
							//							"XVIEWCHALLENGE-00239/20180116023908",
							//							"XVIEWCHALLENGE-00241/20180116024141",
							//							"XVIEWCHALLENGE-00252/20180116025604",
							//							"XVIEWCHALLENGE-00254/20180116025842",
							//							"XVIEWCHALLENGE-00259/20180116030917",
							//							"XVIEWCHALLENGE-00261/20180116031127",


							// AIRPORT GROUP							
							//							"XVIEWCHALLENGE-00282/20180116031335",
							//							"XVIEWCHALLENGE-00283/20180116031348",
							//							"XVIEWCHALLENGE-00285/20180116031351",
							//							"XVIEWCHALLENGE-00287/20180116031524",
							//							"XVIEWCHALLENGE-00289/20180116031530",
							//							"XVIEWCHALLENGE-00291/20180116031537",
							//							"XVIEWCHALLENGE-00293/20180116031608",
							//							"XVIEWCHALLENGE-00294/20180116031656",
							//							"XVIEWCHALLENGE-00295/20180116031716",
							//							"XVIEWCHALLENGE-00296/20180116031717",
							//							"XVIEWCHALLENGE-00297/20180116031718",
							//							"XVIEWCHALLENGE-00299/20180116031742",
							//							"XVIEWCHALLENGE-00301/20180116031852",
							//							"XVIEWCHALLENGE-00302/20180116031855",
							//							"XVIEWCHALLENGE-00303/20180116031902",
							//							"XVIEWCHALLENGE-00307/20180116031945",
							//							"XVIEWCHALLENGE-00309/20180116032031",
							//							"XVIEWCHALLENGE-00310/20180116032048",
							//							"XVIEWCHALLENGE-00311/20180116032104",
							//							"XVIEWCHALLENGE-00313/20180116032140",
							//							"XVIEWCHALLENGE-00315/20180116032159",
							//							"XVIEWCHALLENGE-00317/20180116032209",
							//							"XVIEWCHALLENGE-00319/20180116032233",
							//							"XVIEWCHALLENGE-00320/20180116032313",
							//							"XVIEWCHALLENGE-00321/20180116032333",
							//							"XVIEWCHALLENGE-00322/20180116032339",
							//							"XVIEWCHALLENGE-00323/20180116032353",

							// SHORELINE GROUP							
							//							"XVIEWCHALLENGE-00324/20180116032353",
							//							"XVIEWCHALLENGE-00325/20180116032445",


							//	AIRPORT GROUP						
							//							"XVIEWCHALLENGE-00327/20180116032508",
							//							"XVIEWCHALLENGE-00331/20180116032514",
							//							"XVIEWCHALLENGE-00332/20180116032533",
							//							"XVIEWCHALLENGE-00333/20180116032552",
							//							"XVIEWCHALLENGE-00340/20180116032556",
							//							"XVIEWCHALLENGE-00342/20180116032556",


							//							"XVIEWCHALLENGE-00343/20180116032609",

							// AIRPORT GROUP							
							//							"XVIEWCHALLENGE-00345/20180116032642",
							//							"XVIEWCHALLENGE-00350/20180116032655",
							//							"XVIEWCHALLENGE-00354/20180116032756",
							//							"XVIEWCHALLENGE-00355/20180116032801",
							//							"XVIEWCHALLENGE-00357/20180116032835",
							//							"XVIEWCHALLENGE-00358/20180116032835",
							//							"XVIEWCHALLENGE-00359/20180116032841",
							//							"XVIEWCHALLENGE-00360/20180116032858",
							//							"XVIEWCHALLENGE-00362/20180116032900",
							//							"XVIEWCHALLENGE-00365/20180116032912",
							//							"XVIEWCHALLENGE-00367/20180116033001",
							//							"XVIEWCHALLENGE-00368/20180116033001",
							//							"XVIEWCHALLENGE-00370/20180116033002",
							//							"XVIEWCHALLENGE-00371/20180116033017",


							// RURAL GROUP
							//							"XVIEWCHALLENGE-00372/20180116033044",
							//							"XVIEWCHALLENGE-00373/20180116033044",
							//							"XVIEWCHALLENGE-00374/20180116033044",
							//							"XVIEWCHALLENGE-00375/20180116033045",
							//							"XVIEWCHALLENGE-00376/20180116033055",
							//							"XVIEWCHALLENGE-00377/20180116033132",
							//							"XVIEWCHALLENGE-00378/20180116033132",
							//							"XVIEWCHALLENGE-00379/20180116033132",
							//							"XVIEWCHALLENGE-00380/20180116033143",
							//							"XVIEWCHALLENGE-00381/20180116033224",
							//							"XVIEWCHALLENGE-00382/20180116033225",
							//							"XVIEWCHALLENGE-00386/20180116033234",
							//							"XVIEWCHALLENGE-00389/20180116033308",
							//							"XVIEWCHALLENGE-00393/20180116033318",
							//							"XVIEWCHALLENGE-00394/20180116033319",
							//							"XVIEWCHALLENGE-00396/20180116033349",
							//							"XVIEWCHALLENGE-00399/20180116033410",
							//							"XVIEWCHALLENGE-00401/20180116033509",
							//							"XVIEWCHALLENGE-00407/20180116033511",
							//							"XVIEWCHALLENGE-00412/20180116033540",
							//							"XVIEWCHALLENGE-00414/20180116033552",
							//							"XVIEWCHALLENGE-00415/20180116033602",
							//							"XVIEWCHALLENGE-00418/20180116033630",
							//							"XVIEWCHALLENGE-00420/20180116033655",
							//							"XVIEWCHALLENGE-00422/20180116033658",
							//							"XVIEWCHALLENGE-00423/20180116033727",
							//							"XVIEWCHALLENGE-00430/20180116033748",
							//							"XVIEWCHALLENGE-00431/20180116033752",
							//							"XVIEWCHALLENGE-00432/20180116033755",
							//							"XVIEWCHALLENGE-00433/20180116033814",
							//							"XVIEWCHALLENGE-00434/20180116033830",
							//							"XVIEWCHALLENGE-00436/20180116033832",
							//							"XVIEWCHALLENGE-00437/20180116033835",
							//							"XVIEWCHALLENGE-00445/20180116033856",
							//							"XVIEWCHALLENGE-00447/20180116033927",
							//							"XVIEWCHALLENGE-00506/20180116034552",
							//							"XVIEWCHALLENGE-00509/20180116034602",
							//							"XVIEWCHALLENGE-00510/20180116034603",
					}).map(s-> String.format("%s/%s", UserConfiguration.getTileserviceEndpoint(), s))
					.map(s->{
						ELTFrame result = null;
						try {
							result = ELT.getInstance().loadImage("app1", s, false, false, false);
						} catch (Exception e) {
							e.printStackTrace();
						}
						return result;
					})
					.filter(s->s!=null)
					.forEach(f-> display.get().asyncExec(()->f.open()));

				} catch (Throwable e) {
					e.printStackTrace();
				}
//								final ELTFrame frame = ELT.getInstance().getLastLoadedAppImage("app1");
//								final ELTCanvas canvas = frame.getELTCanvas();
//								final TileserverImage[] images = canvas.getLayerManager().getBaseLayer().getImages();
//								final TileserverImage lastImage = images[images.length-1];
//								canvas.zoomToImage(lastImage);

//				try {
//////					ELT.getInstance().loadImage("app2", "https://tileserver.leidoslabs.com/tileserver/15JUL06SyrianAirfield/20150706084001", true, true);
//					ELT.getInstance().openFrame("app2", true);
//				} catch (IOException | InterruptedException | ExecutionException e) {
//					e.printStackTrace();
//				}
			});

			initThread.start();
			display.set(new Display());

			backgroundTelemetry = new TelemetryClient(telemetryInterval);
			backgroundTelemetry.start();
			if (!cacheListDisabled) {
		       check = new CacheListClient(cacheListInterval);
		       check.start();
			}
			else {
				LOGGER.warn("Cache list not enabled. Use arg -pc to enable predictive caching.");
			}
			createInstance(display.get(), true, true).run();
			initThread.join();

		} catch (Throwable e) {
			e.printStackTrace();
		} finally {
			if (backgroundTelemetry != null) {
				backgroundTelemetry.stop();
			}
			if (check != null){
				check.stop();
			}
			System.exit(0);
		}

	}
	
	/**
	 * Arg parser for experimental caching behaviour flags
	 * @param args
	 */
	public static void argParse(String[] args) {
	       Options options = new Options();

	       Option telemetryInterval = new Option("ti", "telemetry-interval", 
	               true, "Interval between telementry uploads in seconds. Default 300");
	       telemetryInterval.setRequired(false);
	       options.addOption(telemetryInterval);
	       
	       Option predictiveCachingEnabled = new Option("pc", "predictive-caching", 
	               false, "flag to enable predictive caching background service");
	       predictiveCachingEnabled.setRequired(false);
	       options.addOption(predictiveCachingEnabled);
	       
	       Option predictiveCachingInterval = new Option("pci", "predictive-caching-interval", 
	               true, "Interval between predictive caching suggestions in seconds. Default 86400");
	       predictiveCachingInterval.setRequired(false);
	       options.addOption(predictiveCachingInterval);
	       
	       CommandLineParser parser = new DefaultParser();
	       HelpFormatter formatter = new HelpFormatter();
	       CommandLine cmd = null;
	       try {
	           cmd = parser.parse(options, args);
	       } catch (ParseException e) {
	           LOGGER.error(e.getMessage());
	           formatter.printHelp("ELT", options);
	           System.exit(1);
	       }
	       if (cmd.hasOption("predictive-caching")) {
	    	   ELT.cacheListDisabled = false;
	       }
	       String pcInterval = cmd.getOptionValue("predictive-caching-interval", "86400");
	       String telemInterval = cmd.getOptionValue("telemetry-interval", "300");
	       try {
	           ELT.telemetryInterval = Long.parseLong(telemInterval);
	           ELT.cacheListInterval = Long.parseLong(pcInterval);
	       }
	       catch (NumberFormatException e){
	           LOGGER.error(e.getMessage());
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
	public ELTFrame loadImage(String appID, String url, boolean newWindow, boolean resetViewport, boolean replaceImage) throws IOException, InterruptedException, ExecutionException, ExecutionException {
		final URL imageMetadataURL = TileserverUrlBuilder.getImageMetadataURL(url);
		final TileserverImage image = new TileserverImage(imageMetadataURL);
		return loadImage(appID, image, newWindow, resetViewport, replaceImage);
	}

	public ELTFrame openFrame(String appID, boolean newWindow) throws IOException, InterruptedException, ExecutionException, ExecutionException {
		final AtomicReference<ELTFrame> result = new AtomicReference<ELTFrame>();
		final AtomicReference<Exception> exception = new AtomicReference<>();

		display.syncExec(()-> {
			try {
				ELTFrame appFrame = getLastLoadedAppImage(appID);
				if (newWindow || (appFrame == null)) {
					appFrame = new ELTFrame(display, appID, isDecorated, isProgressiveRender);
					addAppFrame(appID, appFrame);
				}
				result.set(appFrame);
			} catch (IOException | InterruptedException | ExecutionException e) {
				LOGGER.error(e.getMessage(), e);
				exception.set(e);
			}
		});

		final Exception e = exception.get();
		if (e instanceof IOException) {
			throw (IOException)e;
		} else if (e instanceof InterruptedException) {
			throw (InterruptedException)e;
		} else if (e instanceof ExecutionException) {
			throw (ExecutionException)e;
		}

		return result.get();
	}


	/**
	 * Loads the given image into the ELT
	 * 
	 * @param appID The application context that defines the ELTFrame that the image will be loaded into.
	 * @param image The image to load into the ELT
	 * @param newWindow Boolean flag indicating whether the image should be loaded into a new window  
	 * @param resetViewport Boolean flag indicating whether the viewport should stay at the same geolocation, zoom, and rotation.
	 * @return The ELTFrame that the image is loaded into.
	 * @throws IOException
	 * @throws InterruptedException
	 * @throws ExecutionException
	 */
	public ELTFrame loadImage(String appID, TileserverImage image, boolean newWindow, boolean resetViewport, boolean replaceImage) throws IOException, InterruptedException, ExecutionException, ExecutionException {
		final AtomicReference<ELTFrame> result = new AtomicReference<ELTFrame>();
		final AtomicReference<Exception> exception = new AtomicReference<>();

		display.syncExec(()-> {
			try {
				LOGGER.debug("Entered loadImage");
				ELTFrame appFrame = getLastLoadedAppImage(appID);
				ELTFrame oldFrame = appFrame;
				final String url = image.getImageBaseURL();
				boolean setImageProjection = false;
				if (newWindow || (appFrame == null)) {
					appFrame = new ELTFrame(display, appID, isDecorated, isProgressiveRender);
					if (oldFrame != null && !resetViewport) {
						ImageWorld oldImageWorld = oldFrame.getELTCanvas().getImageWorld();
						appFrame.getELTCanvas().getImageWorld().zoomTo(oldImageWorld.getGeodeticCenter(), oldImageWorld.getMapScale());
					}

					addAppFrame(appID, appFrame);
					LOGGER.debug(String.format("loading %s in new window", url));
				} else {
					LOGGER.debug(String.format("loading %s in existing window", url));
					if (replaceImage) {
						setImageProjection = (appFrame.getELTCanvas().getImageWorld().getProjection() instanceof ImageProjection);
						appFrame.getELTCanvas().getLayerManager().getBaseLayer().clearAllImages();
					}
				}

				if (ELTWebSocket.isLinked(appID)) {

					ELTWebSocket.sendImageLoad(appID, url);
				}
				appFrame.getELTCanvas().addImage(image, resetViewport || (oldFrame == null));
				if (setImageProjection) {
					appFrame.getELTCanvas().setSingleImageMode(appFrame.getELTCanvas().getLayerManager().getBaseLayer().getImages()[0]);
				}
				result.set(appFrame);
				LOGGER.debug("Exiting loadImage");
			} catch (IOException | InterruptedException | ExecutionException e) {
				LOGGER.error(e.getMessage(), e);
				exception.set(e);
			}
		});

		final Exception e = exception.get();
		if (e instanceof IOException) {
			throw (IOException)e;
		} else if (e instanceof InterruptedException) {
			throw (InterruptedException)e;
		} else if (e instanceof ExecutionException) {
			throw (ExecutionException)e;
		}

		return result.get();
	}

	/**
	 * Loads the given image and zooms to the given point and zoom level.
	 * @param appID The application context for the load (Affects the meaning of newWindow and resetViewport)
	 * @param url The URL to the image, up-to-and-including the timestamp.  (e.g. https://tileserver.leidoslabs.com/39393/5959534 )
	 * @param newWindow Boolean flag indicating whether the image should be loaded into an existing window (the last loaded one) or into a new window.
	 * @param lat The latitude of the new center of the viewport.
	 * @param lon The longitude of the new center of the viewport.
	 * @param zoom The rset to zoom into.
	 * @throws IOException
	 * @throws InterruptedException
	 * @throws ExecutionException
	 */
	public void loadImageOnCoordinate(String appID, String url, boolean newWindow, Double lat, Double lon, Integer zoom) throws IOException, InterruptedException, ExecutionException, IllegalArgumentException {
		final URL imageMetadataURL = TileserverUrlBuilder.getImageMetadataURL(url);
		final TileserverImage image = new TileserverImage(imageMetadataURL);
		final ELTFrame eltFrame = loadImage(appID, image, newWindow, false, false);
		final ELTCanvas canvas = eltFrame.getELTCanvas();

		display.syncExec(() -> {
			if(lat != null && lon != null && zoom != null ) {
				canvas.zoomToImage(image, new GeodeticELTCoordinate(canvas.getImageWorld(), new Coordinate(lon, lat, 0.0)), zoom);
			} else {
				throw new IllegalArgumentException(String.format("Must specify lat, lon, and zoom parameters. (lat = %s, lon=%s, zoom=%s)", Objects.toString(lat), Objects.toString(lon), Objects.toString(zoom)));
			}
		});
	}


	/**
	 * Gets the WKT viewport of the last loaded image for the given appID
	 * @param appID The application ID to use for looking up the last loaded image
	 * @return WKT of the Polygon viewport for the given appId
	 * @throws AppNotFoundException 
	 */
	public String getViewportBoundsWKT(String appID) throws AppNotFoundException {
		String result = "";
			ELTFrame app = getLastLoadedAppImage(appID);
		if (app != null) {
			result = app.getELTCanvas().getImageWorld().getGeodeticViewport().toText();
		} else {
			throw new AppNotFoundException(String.format("Can't find window for appId '%s'", appID));
		}
		return result;
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
	 * @throws AppNotFoundException 
	 */
	public void centerOnCoordinate(String appId, double lat, double lon, int zoom) throws AppNotFoundException {
		final ELTFrame lastFrame = getLastLoadedAppImage(appId);
		if (lastFrame != null) {
			ELTCanvas canvas = lastFrame.getELTCanvas();
			TileserverImage[] images = canvas.getLayerManager().getBaseLayer().getImages();

			Coordinate center = new Coordinate(lon, lat, 0.0);

			if (images != null && images.length > 0) {
				TileserverImage lastImage = images[images.length-1];
				ImageWorld imageWorld = canvas.getImageWorld();
				double rset = Doubles.constrainToRange(zoom, 0, lastImage.getMaxRLevel());
				canvas.zoomToImage(lastImage, new GeodeticELTCoordinate(imageWorld, center), rset);
			}
		} else {
			throw new AppNotFoundException(String.format("Can't find window for appId '%s'", appId));
		}
	}

	/**
	 * Highlight the given observation in the given layer on the newest ELTFrame for the given appId
	 * @param appId The application ID used to locate the newest ELTFrame
	 * @param companionID The id of the observation to highlight.
	 * @param layerName The layer name that the observation exists in. (Not currently used, as all features currently exist in the placemark layer)
	 * @throws AppNotFoundException 
	 */
	public void highlightCoordinate(String appId, String companionID,String layerName) throws AppNotFoundException {
		final ELTFrame eltFrame = getLastLoadedAppImage(appId);
		if (eltFrame != null) {
			eltFrame.getELTCanvas().highlightObservation(companionID, layerName);
		} else {
			throw new AppNotFoundException(String.format("Can't find window for appId '%s'", appId));
		}
	}

	/**
	 * De-selects all features on the newest ELTFrame for the given appId
	 * @param appId The application ID used to locate the newest ELTFrame
	 * @throws AppNotFoundException 
	 */
	public void unselectAll(String appId) throws AppNotFoundException {
		final ELTFrame eltFrame = getLastLoadedAppImage(appId);
		if (eltFrame != null) {
			eltFrame.getELTCanvas().unselectAll();
		} else {
			throw new AppNotFoundException(String.format("Can't find window for appId '%s'", appId));
		}
	}

	/**
	 * Deletes the given observation ID from the newest ELTFrame for the given appID
	 * @param appId The application ID used to locate the newest ELTFrame
	 * @param companionId The ID of the observation to delete from the view
	 * @throws AppNotFoundException 
	 */
	public void deleteObservation(String appId, String companionId) throws AppNotFoundException {
		final ELTFrame eltFrame = getLastLoadedAppImage(appId);
		if (eltFrame != null) {
			eltFrame.getELTCanvas().deleteObservation(companionId);
		} else {
			throw new AppNotFoundException(String.format("Can't find window for appId '%s'", appId));
		}
	}

	/**
	 * Creates new placemarks for the given list of observations.
	 *
	 * @param appId The application ID used to locate the newest ELTFrame
	 * @param points The list of observations to create.
	 * @param layerName The layer name to create the placemarks in. (Not currently used, as all features currently exist in the placemark layer)
	 * @throws AppNotFoundException 
	 */
	public void setPlacemarks(String appId, List<PointObservation> points, String layerName) throws AppNotFoundException {
		final ELTFrame eltFrame = getLastLoadedAppImage(appId);
		if (eltFrame != null) {
			eltFrame.getELTCanvas().setPlacemarks(points, layerName);
		} else {
			throw new AppNotFoundException(String.format("Can't find window for appId '%s'", appId));
		}
	}

	/**
	 * Removes the given layer from the newest ELTFrame for the given appId
	 * @param appId The application ID used to locate the newest ELTFrame
	 * @param layerName The layer name to remove
	 * @throws AppNotFoundException 
	 */
	public void removeLayer(String appId, String layerName) {
		final ELTFrame eltFrame = getLastLoadedAppImage(appId);
		if (eltFrame != null) {
			eltFrame.getELTCanvas().removeLayer(layerName);
		}
	}

	/**
	 * Accessor
	 * @return The SWT Display
	 */
	private Display getDisplay() {
		return display;
	}

}
