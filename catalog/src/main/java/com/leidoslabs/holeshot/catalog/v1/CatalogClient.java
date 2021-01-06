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

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.io.WKTWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.TypeRef;
import com.jayway.jsonpath.spi.json.JacksonJsonProvider;
import com.jayway.jsonpath.spi.mapper.JacksonMappingProvider;

public class CatalogClient {
    private static final Logger LOGGER = LoggerFactory.getLogger(CatalogClient.class);

    private static final String CATALOG_URL_PROPERTY="leidos.catalog.url";
    private static final String CATALOG_API_KEYNAME = "X-Api-Key";
    private final CatalogCredentials credentials;
    private String catalogURL;


    private final Configuration configuration = Configuration.builder()
            .jsonProvider(new JacksonJsonProvider())
            .mappingProvider(new JacksonMappingProvider())
            .build();

    public CatalogClient(CatalogCredentials credentials) {
        this.credentials = credentials;
        setCatalogURL(System.getProperty(CATALOG_URL_PROPERTY));
    }


   /**
     * Returns all images in the catalog (or as many as the catalog is configured to return at max)
     * May return an empty list if no images are present
     * @return A list of CatalogEntry
     * @throws IOException
     */
    public List<CatalogEntry> getCatalogEntries() throws IOException {
        List<CatalogEntry> result = new ArrayList<>();
        final URL url = new URL(getCatalogURL() + "/all_imagery");
        HttpURLConnection connection = (HttpURLConnection)url.openConnection();
        connection.setUseCaches(false);
        connection.setRequestProperty(CATALOG_API_KEYNAME, credentials.getCatalogKey());

        try (InputStream is = connection.getInputStream()) {

            final List<CatalogEntry> allImages = JsonPath.using(configuration).parse(is)
                    .read("$.hits.hits[*]._source", new TypeRef<List<CatalogEntry>>() {} );
            result.addAll(allImages);
        }

        return result;
    }
    
    /**
     * Returns all images in the catalog that intersect a given geometry
     * @return A list of CatalogEntry
     * @throws IOException
     */
    public List<CatalogEntry> getCatalogEntries(Geometry geo) throws IOException {
        
        try {
        	ObjectMapper mapper = new ObjectMapper();
        	WKTWriter writer = new WKTWriter();
        	String wkt = writer.write(geo);
            URIBuilder ub = new URIBuilder(getCatalogURL() + "/search");
            ub.addParameter("wkt", wkt);
            HttpGet request = new HttpGet(ub.toString());
            request.addHeader(CATALOG_API_KEYNAME, credentials.getSecretAccessKey());
            

            try (CloseableHttpClient client = HttpClients.createDefault();
                 CloseableHttpResponse response = client.execute(request)) {
            	
                HttpEntity entity = response.getEntity();
                if (entity != null) {
                    try {
                    	return mapper.readValue(EntityUtils.toString(entity), new TypeReference<ArrayList<CatalogEntry>>(){});
                    } catch (IOException e) {
                    	LOGGER.error("Error deserializing catalog");
                        e.printStackTrace();
                        throw e;
                    }
                }
            } catch (IOException e) {
                LOGGER.error("Error getting catalog");
                e.printStackTrace();
                throw e;
            }

        } catch (URISyntaxException e) {
            LOGGER.error("error building catalog url");
            e.printStackTrace();
            throw new IOException(e);
        }
        return null;
    }

    /**
     * Returns the matching image, if one is present. Otherwise returns null.
     * @param collectionId the full unique collectionId of the image
     *                     i.e. "058618316010_01_P002:20170726070444"
     * @return the matching CatalogEntry or null if no match is found
     * @throws IOException
     */
    public CatalogEntry getCatalogEntry(String collectionId) throws IOException {
        CatalogEntry result = null;
        final URL url = new URL(getCatalogURL() + "/" + collectionId);
        HttpURLConnection connection = (HttpURLConnection)url.openConnection();
        connection.setUseCaches(false);
        connection.setRequestProperty(CATALOG_API_KEYNAME, credentials.getCatalogKey());

        try (InputStream is = connection.getInputStream()) {
            final List<CatalogEntry> allImages = JsonPath.using(configuration).parse(is)
                    .read("$.hits.hits[*]._source", new TypeRef<List<CatalogEntry>>() {} );
            if(allImages.size() > 0) {
                result = allImages.get(0);
            }
        }

        return result;

    }

    public String getCatalogURL() {
       return catalogURL;
    }

    public void setCatalogURL(String catalogURL) {
       this.catalogURL = catalogURL;
    }

    public static void main(String[] args) {
        try {
            final CatalogClient client = new CatalogClient(CatalogCredentials.getApplicationDefaults());
            final List<CatalogEntry> allImages = client.getCatalogEntries();
            // allImages.forEach(e->System.out.println(e.toString()));
        } catch (IOException e) {
            LOGGER.error(e.getMessage(), e);
        }
    }

}
