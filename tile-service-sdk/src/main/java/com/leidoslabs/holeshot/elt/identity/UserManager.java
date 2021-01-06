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

package com.leidoslabs.holeshot.elt.identity;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.commons.codec.digest.DigestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;



public class UserManager {
	private static final Logger LOGGER = LoggerFactory.getLogger(UserManager.class);
	private static final int INPUT_TIMEOUT_MS = 10000;
	
	private static UserManager instance = null;
	private static boolean anonMode = false;
	
	private Scanner scan;
	private String userName = null;
	
	
	
	public static UserManager getUserManager() {
		if (instance == null) {
			if (!anonMode) {
				try {
					if (insideVPC()) {
						LOGGER.debug("tile-service-sdk use in VPC, logging into anonymous mode");
						anonMode = true;
					} else {
						instance = new UserManager();
						return instance;
					}
				} catch (IOException e) {
					anonMode = true;
					LOGGER.error("Error obtaining userID. Running in anonymous mode" + e.getMessage());
				}
			}
		}
		return instance;
	}
	
	private UserManager() throws IOException{
		if (!credentialsFile()) {
			login();
		}
	}
	
	/**
	 * Projects using TileserverClient without user input (e.g chipper-service, tile-service) can avoid waiting for login timeout
	 * by providing
	 * @return
	 */
	private boolean credentialsFile() throws IOException{
		try(InputStream is = this.getClass().getClassLoader().getResourceAsStream("tileserver-credentials")){
			if (is == null) {
				LOGGER.debug("Did not find tileserver-credentials file in resources. Prompting user login");
				return false;
			}
			LOGGER.debug("Attempting to login via tileserver-credentials file");
			scan = new Scanner(is);
			String userID = scan.nextLine();
			System.out.println("Logging in as " + userID);
			this.setUsername(userID);
		} catch (IOException e) {
			LOGGER.error("Encountered error reading tileserver-credentails, continuing in anonymous mode" + e.toString());
			throw e;
		};
		return true;
	}
	
	private void login() throws IOException {
		scan = new Scanner(System.in);
		System.out.println("Enter userID");
		System.out.println(String.format("After %d seconds, will continue in anonymous mode" , INPUT_TIMEOUT_MS / 1000));
		
		String userID = nextLineTimeOut();
		LOGGER.debug("logging in as " + userID);
		setUsername(userID);
	}
	
	
	private String nextLineTimeOut() throws IOException {
		String result = null;
		FutureTask<String> readNextLine = new FutureTask<String>(() -> {
			  return scan.nextLine();
		});

		ExecutorService executor = Executors.newFixedThreadPool(2);
		executor.execute(readNextLine);

		try {
		  result = readNextLine.get(INPUT_TIMEOUT_MS, TimeUnit.MILLISECONDS);
		} catch (TimeoutException e) {
			LOGGER.warn("Failed to provide input, continuing in anonymous mode");
			throw new IOException();
		} catch (Exception e) {
			LOGGER.error("Error reading from stdin: " + e.getMessage());
			throw new IOException(e);
		}
		return result;
	}
	
	private void setUsername(String userName) {
		this.userName = userName;
	}
	
	public String getUsername() {
		return this.userName;
	}
	
	
	 private static boolean insideVPC() {
	        boolean inside = false;
	        // simple way to determine if we are running in the VPC and need to use http instead of https
	        // TODO: find a better way
	        try {
	            Enumeration<NetworkInterface> cards = NetworkInterface.getNetworkInterfaces();
	            while (cards.hasMoreElements()) {
	                NetworkInterface card = cards.nextElement();
	                Enumeration<InetAddress> ips = card.getInetAddresses();
	                while (ips.hasMoreElements()) {
	                    InetAddress ip = ips.nextElement();
	                    if (ip.getHostAddress().startsWith("10.0.")) {
	                        return true;
	                    }
	                }
	            }
	        } catch (IOException e) {
	            LOGGER.error("Couldn't determine whether client is in VPC");
	            e.printStackTrace();
	            return false;
	        }
	        return inside;
	    }
	
}
