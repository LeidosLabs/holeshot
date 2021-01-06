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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

public class UserConfiguration {

    private static final Logger LOGGER = LoggerFactory.getLogger(UserConfiguration.class);

    public static final String HOLESHOT_ELT_CONFIG_FILE = ".holeshot" + File.separator + "holeshot.properties";
    private static final String telemetry_enabled = "holeshot_telemetry_enabled";
    private static final String user_id = "holeshot_user_id";
    private static final String tileservice_key = "holeshot_tileservice_key";
    private static final String tileservice_endpoint = "holeshot_tileservice_url";
    private static final String analytics_key = "holeshot_analytics_key";
    private static final String telemetry_endpoint = "holeshot_telemetry_url";
    private static final String analytics_endpoint = "holeshot_analytics_url";
    private static final String telemetry_api_key = "holeshot_telemetry_api_key";

    private static final Properties config = new Properties();

    private UserConfiguration() {  }

    public static void init() {

        LOGGER.info("Loading User Configuration");

        File configFile = new File(System.getProperty("user.home") + File.separator +
                HOLESHOT_ELT_CONFIG_FILE);

        LOGGER.info("Checking: " + configFile.getAbsolutePath());
        if (!configFile.exists()) {
            LOGGER.warn("Config file not found: " + configFile.getAbsolutePath());
            LOGGER.warn("Client will not be able to perform telemetry upload or retrieve cache lists");
            return;
        }

        try {
            FileInputStream fis = new FileInputStream(configFile);
            config.load(fis);
            LOGGER.info("Config properties loaded");
            disableTelemetryIfInvalid();
        } catch (IOException ioe) {
            LOGGER.warn("Unable to read properties from configuration file {}", configFile.toString(), ioe);
            LOGGER.warn("Client will not be able to perform telemetry upload or retrieve cache lists");
        }
    }

    public static boolean isTelemetryEnabled() {
        return Boolean.parseBoolean(config.getProperty(telemetry_enabled, "false"));
    }

    public static String getTileserviceApiKey() {
        return config.getProperty(tileservice_key, "none");
    }

    public static String getTileserviceEndpoint() {
        String url = config.getProperty(tileservice_endpoint, "https://tileserver.leidoslabs.com/tileserver");
        return url.endsWith("/") ? url.substring(0,url.length() - 1) : url;
    }

    public static String getUsername() {
        return config.getProperty(user_id, "anonymous");
    }

    public static String getTelemetryEndpoint() {
        String url = config.getProperty(telemetry_endpoint, "none");
        return url.endsWith("/") ? url.substring(0,url.length() - 1) : url;
    }

    public static String getAnalyticsEndpoint() {
        String url = config.getProperty(analytics_endpoint, "none");
        return url.endsWith("/") ? url.substring(0,url.length() - 1) : url;
    }

    public static String getAnalyticsApiKey() {
        return config.getProperty(analytics_key, "none");
    }

    public static String getTelemetryApiKey() {
        return config.getProperty(telemetry_api_key, "none");
    }

    private static void disableTelemetryIfInvalid() {
        if(getUsername().equalsIgnoreCase("anonymous")) {
            config.setProperty(telemetry_enabled, "false");
        }
        if(getTelemetryEndpoint().equalsIgnoreCase("none")) {
            config.setProperty(telemetry_enabled,"false");
        }
        if(getTelemetryApiKey().equalsIgnoreCase("none")) {
            config.setProperty(telemetry_enabled,"false");
        }
    }

}
