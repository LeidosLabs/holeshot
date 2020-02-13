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
package com.leidoslabs.holeshot.credentials;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class provides credentials for the tile server that are loaded either from environment
 * variables or a properties file.
 */
public class HoleshotCredentials {

  private static final Logger LOGGER = LoggerFactory.getLogger(HoleshotCredentials.class);

  public static final String LEIDOS_SDK_SECRET_ACCESS_KEY = "leidos_sdk_secret_access_key";
  public static final String LEIDOS_SDK_SETTINGS_FILE = ".leidos-sdk" + File.separator + "credentials";

  private Properties properties;
  private static HoleshotCredentials applicationDefault = null;

  public String getSecretAccessKey() {
    return properties.getProperty(LEIDOS_SDK_SECRET_ACCESS_KEY);
  }

  protected HoleshotCredentials(Properties properties) {
    this.properties = properties;
  }

  public Properties getProperties() {
     return this.properties;
  }

  /**
   * This method returns a set of credentials from standard locations. If the credentials are set
   * in the environment variables then they are used, if not a configuration file in the users
   * home directory is checked.
   *
   * @return the tile server credentials.
   */
  public static synchronized HoleshotCredentials getApplicationDefaults() {
    if (applicationDefault == null) {
       Properties properties = new Properties();

       File propertiesFile = new File(System.getProperty("user.home") + File.separator +
             LEIDOS_SDK_SETTINGS_FILE);

       String secretKey = StringUtils.defaultIfEmpty(System.getenv(LEIDOS_SDK_SECRET_ACCESS_KEY.toUpperCase()), "").trim();
       if (!secretKey.isEmpty()) {
          LOGGER.info("Using SDK Credentials taken from environment {}",
                LEIDOS_SDK_SECRET_ACCESS_KEY.toUpperCase());

          properties.put(LEIDOS_SDK_SECRET_ACCESS_KEY, secretKey);
       } else if (propertiesFile.exists()) {
          LOGGER.info("Using SDK credentials taken from configuration file {}", propertiesFile.toString());
          try {
             FileInputStream fis = new FileInputStream(propertiesFile);
             properties.load(fis);
          } catch (IOException ioe) {
             LOGGER.error("Unable to read credentials from configuration file {}", propertiesFile.toString(), ioe);
          }
       } else {
          LOGGER.warn("Unable to load credentials from either environment or configuration files.");
       }
       applicationDefault = new HoleshotCredentials(properties);
    }

    return applicationDefault;
  }

  /**
   * This method creates a set of credentials from specified values.
   *
   * @param secretKey the secret access key of the user
   * @return the tile server credentials
   */
  public static HoleshotCredentials forRegisteredUser(String secretKey) {
    Properties properties = new Properties();
    properties.setProperty(LEIDOS_SDK_SECRET_ACCESS_KEY, secretKey);
    return new HoleshotCredentials(properties);
  }

}
