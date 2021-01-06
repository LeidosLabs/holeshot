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

package com.leidoslabs.holeshot.tileserver;

import java.io.File;
import java.io.IOException;
import java.security.InvalidParameterException;

import org.apache.commons.cli.ParseException;
import org.apache.commons.daemon.Daemon;
import org.apache.commons.daemon.DaemonContext;
import org.apache.commons.daemon.support.DaemonLoader;
import org.apache.log4j.PropertyConfigurator;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.NCSARequestLog;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.session.DefaultSessionCache;
import org.eclipse.jetty.server.session.SessionCache;
import org.eclipse.jetty.server.session.SessionDataStore;
import org.eclipse.jetty.server.session.SessionHandler;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.servlet.ServletContainer;
import org.slf4j.ext.XLogger;
import org.slf4j.ext.XLoggerFactory;

import com.leidoslabs.holeshot.credentials.HoleshotCredentials;
import com.leidoslabs.holeshot.tileserver.cache.RedisCache;
import com.leidoslabs.holeshot.tileserver.service.HealthCheckServlet;
import com.leidoslabs.holeshot.tileserver.service.S3Handler;
import com.leidoslabs.holeshot.tileserver.service.mrf.MRFService;
import com.leidoslabs.holeshot.tileserver.service.wmts.WMTSService;
import com.leidoslabs.holeshot.tileserver.session.EHCacheDataStore;
import com.leidoslabs.holeshot.tileserver.session.RedisSessionDataStore;
import com.leidoslabs.holeshot.tileserver.session.TieredSessionDataStore;
import com.leidoslabs.holeshot.tileserver.v1.TileServerClient;
import com.leidoslabs.holeshot.tileserver.v1.TileServerClientBuilder;


public class TileServer implements Daemon {
   private static final XLogger LOGGER = XLoggerFactory.getXLogger(TileServer.class);

   private static final String HTTP_PORT_PROPERTY="jetty.port";
   private static final String HTTPS_PORT_PROPERTY="jetty.secureport";
   private static final String ENABLE_HTTPS="jetty.enablehttps";

   // Bind to all addresses
   private static final String SERVER_HOST = "0.0.0.0";
   private static final int HTTP_PORT = Integer.parseInt(System.getProperty(HTTP_PORT_PROPERTY, "80"));
   private static final int HTTPS_PORT = Integer.parseInt(System.getProperty(HTTPS_PORT_PROPERTY, "443"));
   private final static boolean HTTPS_ENABLED = Boolean.parseBoolean(System.getProperty(ENABLE_HTTPS, "true"));
   private static final int IDLE_TIMEOUT_IN_MILLISECONDS = 30000;
   private static final int SESSION_MAX_INACTIVE_INTERVAL_IN_SECS = 1800;  // 30 mins
   private static final String SERVER_CONTEXT_PATH = "/tileserver/*";

   // TODO This is used to generate the WMTS xml pages and for MRF, it should be read from a config, not hardcoded
   private static final String TILE_SERVER_URL = "https://tileserver-dev.leidoslabs.com";

   private static final int THREAD_POOL_SIZE = 500;
   private int acceptors;

   private Server server;

   public TileServer() {
   }

   public static void main(String[] args) {
      DaemonLoader.load(TileServer.class.getCanonicalName(), args);
      DaemonLoader.start();
   }

   
   @Override
   public void destroy() {
      LOGGER.entry();
      if (server != null) {
         try {
            server.stop();
         } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
         } finally {
            server = null;
         }
      }
      LOGGER.exit();
   }

   /**
    * Configure jetty server, client, s3 handler, Servlet, etc from Daemon args.
    */
   @Override
   public void init(DaemonContext daemonContext) throws ParseException, InvalidParameterException {
      LOGGER.entry();

      if (server != null) {
         LOGGER.warn("Tile Server daemon init called, but server is already initialized");
         return;
      }

      TileServerOptions daemonOptions = TileServerOptions.getInstance();

      daemonOptions.initialize(daemonContext.getArguments());
      String logConfig = System.getProperty("log4j.configuration");
      if (logConfig != null) {
         LOGGER.info("Using logging configuration: " + logConfig);
         PropertyConfigurator.configure(logConfig);
      }

      server = new Server(new QueuedThreadPool(THREAD_POOL_SIZE));
      addConnectors(server);

      NCSARequestLog requestLog = new NCSARequestLog(String.join(File.separator, System.getProperty("java.io.tmpdir"), "jetty-yyyy_mm_dd.request.log"));
      requestLog.setAppend(true);
      requestLog.setExtended(false);
      requestLog.setLogTimeZone("GMT");
      requestLog.setLogLatency(true);
      server.setRequestLog(requestLog);

      final ServletContextHandler context = new ServletContextHandler(null, getSessionHandler(), null, null, null);
      final S3Handler s3Handler = new S3Handler(daemonOptions.getTileBucket(), daemonOptions.getTileBucketRegion(), acceptors);
      final HoleshotCredentials creds = HoleshotCredentials.getApplicationDefaults();

      final TileServerClient tileserverClient = 
            TileServerClientBuilder
            .forEndpoint(TILE_SERVER_URL)
            .withCredentials(creds)
            .build();
      
      final ResourceConfig config = 
            new ResourceConfig()
            .register(new WMTSService(TILE_SERVER_URL, s3Handler))
            .register(new MRFService(tileserverClient, s3Handler, TILE_SERVER_URL, daemonOptions.getTileBucketRegion(), daemonOptions.getTileBucket()));
      config.packages(true, TileServer.class.getPackage().getName());
      ServletHolder wmtsServlet = new ServletHolder(new ServletContainer(config));
      wmtsServlet.setInitOrder(0);

      context.addServlet(wmtsServlet, SERVER_CONTEXT_PATH);
      context.addServlet(new ServletHolder(new HealthCheckServlet()), HealthCheckServlet.SERVER_CONTEXT_PATH);

      server.setHandler(context);

      LOGGER.exit();
   }

   /**
    * Adds http connectors to the jetty server
    * @param server
    */
   private void addConnectors(Server server) {
      HttpConfiguration httpConfig = new HttpConfiguration();

      httpConfig.setOutputBufferSize(32768);
      httpConfig.setRequestHeaderSize(8192);
      httpConfig.setResponseHeaderSize(8192);
      httpConfig.setSendServerVersion(true);
      httpConfig.setSendDateHeader(false);

      ServerConnector http = new ServerConnector(server, new HttpConnectionFactory(httpConfig));
      http.setPort(HTTP_PORT);
      http.setIdleTimeout(IDLE_TIMEOUT_IN_MILLISECONDS);
      http.setHost(SERVER_HOST);

      server.setConnectors(new Connector[] { http });
      acceptors = http.getAcceptors();
   }

   /**
    * Retrieves Session Handler, with datastore from EHCache and Redis
    * @return
    */
   private SessionHandler getSessionHandler() {
      SessionHandler sessions = new SessionHandler();
      SessionCache cache = new DefaultSessionCache(sessions);
      SessionDataStore memoryDatastore = new EHCacheDataStore();

      SessionDataStore datastore;
      if  (RedisCache.getInstance().isAvailable()) {
         RedisSessionDataStore redisStore = new RedisSessionDataStore();

         datastore = new TieredSessionDataStore(new SessionDataStore[] { memoryDatastore, redisStore });
      } else {
         datastore = memoryDatastore;
      }

      cache.setSessionDataStore(datastore);
      sessions.setSessionCache(cache);
      sessions.setMaxInactiveInterval(SESSION_MAX_INACTIVE_INTERVAL_IN_SECS);
      sessions.getSessionCookieConfig().setSecure(true);
      sessions.getSessionCookieConfig().setHttpOnly(true);
      return sessions;
   }

   @Override
   public void start() throws Exception {
      server.start();
   }

   @Override
   public void stop() {
      try {
         if (server != null) {
            if (!(server.isStopping() || server.isStopped())) {
               try {
                  server.stop();
               } catch (Exception ex) {
                  LOGGER.error(ex.getMessage(), ex);
               }
            }
            server.join();
         }
      } catch (Exception e) {
         LOGGER.error(e.getMessage(), e);
      }
   }

}

