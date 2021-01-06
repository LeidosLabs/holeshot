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

package com.leidoslabs.holeshot.catalog.v1;

import java.util.Properties;

import com.leidoslabs.holeshot.credentials.HoleshotCredentials;


public class CatalogCredentials extends HoleshotCredentials {

    private static final String HOLESHOT_CATALOG_KEY = "holeshot_catalog_key";
    private static CatalogCredentials applicationDefaults = null;

    private static Properties getApplicationDefaultProperties() {
        return HoleshotCredentials.getApplicationDefaults().getProperties();
    }

    /**
     * Retrieves credentials from a properties file at the default system location defined by HoleshotCredentials
     * @return CatalogCredentials containing catalog access key
     */
    public static synchronized CatalogCredentials getApplicationDefaults() {
    	if (applicationDefaults == null) {
           applicationDefaults = new CatalogCredentials(getApplicationDefaultProperties());
    	}
    	return applicationDefaults;
    }
    public CatalogCredentials(Properties properties) {
        super(properties);
    }

    public String getCatalogKey() {
        return getProperties().getProperty(HOLESHOT_CATALOG_KEY);
    }
}
