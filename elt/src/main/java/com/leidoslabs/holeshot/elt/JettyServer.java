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

import java.util.EnumSet;

import javax.servlet.DispatcherType;
import javax.servlet.ServletException;
import javax.websocket.DeploymentException;
import javax.websocket.server.ServerContainer;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.DefaultServlet;
import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.websocket.jsr356.server.deploy.WebSocketServerContainerInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.leidoslabs.holeshot.elt.websocket.ELTWebSocket;

/**
 * Embedded web server used to handle REST and WebSockets API
 */
public class JettyServer {
   private static final Logger LOGGER = LoggerFactory.getLogger(JettyServer.class);

	private Thread serverThread;

	/**
	 * Initialize server thread, capable of handling REST API defined in Api.java
	 */
	public JettyServer() {
		serverThread = new Thread() {
			@Override
			public void run() {
				ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
				context.setContextPath("/");

				Server jettyServer = new Server(8080);
				jettyServer.setHandler(context);

				ServerContainer webSocketContainer;
            try {
               webSocketContainer = WebSocketServerContainerInitializer.configureContext(context);
               webSocketContainer.setDefaultMaxBinaryMessageBufferSize(Integer.MAX_VALUE);
                webSocketContainer.setDefaultMaxTextMessageBufferSize(Integer.MAX_VALUE);
               webSocketContainer.setDefaultMaxSessionIdleTimeout(600000); //ms
               webSocketContainer.addEndpoint(ELTWebSocket.class);
            } catch (ServletException e1) {
               e1.printStackTrace();
            } catch (DeploymentException de) {
               LOGGER.error("Unable to start WebSocket", de);
               // Possibly show error to user because the WebSocket interface won't function
            }


				// Set up API resources
				ServletHolder apiServlet = context.addServlet(
						org.glassfish.jersey.servlet.ServletContainer.class, "/api/*");
				apiServlet.setInitOrder(0);

				// Tells the Jersey Servlet which REST service/class to load.
				apiServlet.setInitParameter(
						"jersey.config.server.provider.classnames",
						Api.class.getCanonicalName());

				// Add CORS support
				FilterHolder filterHolder = context.addFilter(org.eclipse.jetty.servlets.CrossOriginFilter
						.class, "/*", EnumSet.of(DispatcherType.REQUEST));
				filterHolder.setInitParameter("allowedOrigins", "*");
				filterHolder.setInitParameter("allowedMethods", "GET,POST,PUT,DELETE,HEAD,OPTIONS");


				// Setup Swagger-UI static resources
				String resourceBasePath = ELT.class.getResource("/webapp").toExternalForm();
				context.setWelcomeFiles(new String[] { "index.html" });
				context.setResourceBase(resourceBasePath);
				context.addServlet(new ServletHolder(new DefaultServlet()), "/*");

				// makes swagger page visible at /swagger instead of just at localhost:8080
				//    ServletHolder holder = new ServletHolder(new DefaultServlet());
				//    holder.setInitParameter("pathInfoOnly", "true");
				//    context.addServlet(holder, "/swagger/*");


				try {
					jettyServer.start();
					jettyServer.join();
				} catch (Throwable e) {
					e.printStackTrace();
				} finally {
					jettyServer.destroy();
				}
			}
		};
	}

	
	/**
	 * Wait for server thread to complete
	 * @throws InterruptedException
	 */
	public void serverJoin() throws InterruptedException {
		this.serverThread.join();
	}

	/**
	 * Start server thread
	 */
	public void startServer() {
		this.serverThread.start();
	}

   public void stop() {
     this.serverThread.interrupt();

   }

}
