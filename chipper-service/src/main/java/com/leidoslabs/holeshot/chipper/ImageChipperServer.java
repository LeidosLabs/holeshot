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

package com.leidoslabs.holeshot.chipper;

import java.io.File;
import java.util.EnumSet;

import javax.servlet.DispatcherType;

import org.apache.commons.daemon.Daemon;
import org.apache.commons.daemon.DaemonContext;
import org.apache.commons.daemon.DaemonInitException;
import org.apache.commons.daemon.support.DaemonLoader;
import org.apache.log4j.PropertyConfigurator;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.NCSARequestLog;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.DefaultServlet;
import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.servlet.ServletContainer;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.slf4j.ext.XLogger;
import org.slf4j.ext.XLoggerFactory;

import com.leidoslabs.holeshot.tileserver.v1.TileServerClientBuilder;

/**
 * Jetty ImageChipperServer, stands up REST API for image chipper. Runs as Daemon
 */
public class ImageChipperServer implements Daemon {
   private static final XLogger LOGGER = XLoggerFactory.getXLogger(ImageChipperServer.class);
   private static final String HTTP_PORT_PROPERTY="jetty.port";

   // Bind to all addresses
   private static final String SERVER_HOST = "0.0.0.0";
   private static final int HTTP_PORT = Integer.parseInt(System.getProperty(HTTP_PORT_PROPERTY, "80"));
   private static final int IDLE_TIMEOUT_IN_MILLISECONDS = 30000;

   private static final int THREAD_POOL_SIZE = 500;

   private Server server;

   public ImageChipperServer() throws IllegalArgumentException, Exception {
   }

   /**
    * Entry point, loads and starts server daemon with command line args
    * @param args
    * @throws Exception
    */
   public static void main(String[] args) throws Exception {
      DaemonLoader.load(ImageChipperServer.class.getCanonicalName(), args);
      DaemonLoader.start();
   }


   @Override
   /**
    * Destroy Jetty Server
    */
   public void destroy() {
      if (server != null) {
         try {
            server.stop();
         } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
         } finally {
            server = null;
         }
      }
   }

   @Override
   /**
    * Initialize Jetty server, attaches api servlet etc.
    */
   public void init(DaemonContext daemonContext) throws DaemonInitException, Exception {
      if (server == null) {
         ImageChipperServerOptions daemonOptions = ImageChipperServerOptions.getInstance();
         daemonOptions.initialize(daemonContext.getArguments());

         final String localDomainName = daemonOptions.getDomainName();
         if (localDomainName != null) {
            TileServerClientBuilder.setLocalDomainName(localDomainName);
         }
         
         String logConfig = System.getProperty("log4j.configuration");
         if (logConfig != null) {
            PropertyConfigurator.configure(logConfig);
         }
         
//         GLFWErrorCallback.createPrint(System.err).set();

         GraphicsContext.createGCLibraryInitializer().initializeGL();
         ImageChipper.initializePool();

         server = new Server(new QueuedThreadPool(THREAD_POOL_SIZE));
         addConnectors(server);

         NCSARequestLog requestLog = new NCSARequestLog(String.join(File.separator, System.getProperty("java.io.tmpdir"), "jetty-yyyy_mm_dd.request.log"));
         requestLog.setAppend(true);
         requestLog.setExtended(false);
         requestLog.setLogTimeZone("GMT");
         requestLog.setLogLatency(true);
         server.setRequestLog(requestLog);

         final ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
         context.setContextPath("/");
         server.setHandler(context);

         // Set up API resources
         final ResourceConfig config = new ResourceConfig();
         config.packages(ImageChipper.class.getPackage().getName());

         ServletHolder apiServlet = new ServletHolder(new ServletContainer(config));

         context.addServlet(apiServlet, "/api/*");
         apiServlet.setInitOrder(0);

         context.addServlet(new ServletHolder(new HealthCheckServlet()), HealthCheckServlet.SERVER_CONTEXT_PATH);

         // Tells the Jersey Servlet which REST service/class to load.
         apiServlet.setInitParameter(
               "jersey.config.server.provider.classnames",
               ImageChipperService.class.getCanonicalName());

         // Add CORS support
         FilterHolder filterHolder = context.addFilter(org.eclipse.jetty.servlets.CrossOriginFilter
               .class, "/*", EnumSet.of(DispatcherType.REQUEST));
         filterHolder.setInitParameter("allowedOrigins", "*");
         filterHolder.setInitParameter("allowedMethods", "GET,POST,PUT,DELETE,HEAD,OPTIONS");


         // Setup Swagger-UI static resources
         if (context.getResourceBase() == null) {
            String resourceBasePath = ImageChipperServer.class.getResource("/webapp").toExternalForm();
            context.setWelcomeFiles(new String[] { "index.html" });
            context.setResourceBase(resourceBasePath);
            context.addServlet(new ServletHolder(new DefaultServlet()), "/*");
         }
      }
   }


   private ServerConnector addConnectors(Server server) {
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

      return http;
   }

   @Override
   /**
    * Start jetty server
    */
   public void start() throws Exception {
      server.start();
   }

   @Override
   /**
    * Stop server
    */
   public void stop() throws Exception {
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
