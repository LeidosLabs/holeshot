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
package com.leidoslabs.holeshot.tileserver.service.wmts.handlers;

import com.leidoslabs.holeshot.catalog.v1.CatalogClient;
import com.leidoslabs.holeshot.catalog.v1.CatalogCredentials;
import com.leidoslabs.holeshot.catalog.v1.CatalogEntry;
import net.opengis.ows._1.*;
import net.opengis.wmts._1.*;
import net.opengis.wmts._1.ObjectFactory;
import org.locationtech.jts.geom.Envelope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.NotFoundException;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.math.BigInteger;

/**
 * Service Meteadata Request handler for requests on WMTSCapabilities.xml 
 */
public class GetServiceMetadataRequestHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(GetServiceMetadataRequestHandler.class);
    private final CatalogEntry entry;
    private final String tileServerUrl;

    public GetServiceMetadataRequestHandler(String tileServerUrl, String imageId, String timestamp) throws IOException, NotFoundException {
        this.tileServerUrl = tileServerUrl;
        LOGGER.debug("Creating Image Catalog Client");
        CatalogClient catalogClient = new CatalogClient(CatalogCredentials.getApplicationDefaults());
        LOGGER.debug("Catalog client intialized, retrieving entry for " + imageId + ":" + timestamp);
        entry = catalogClient.getCatalogEntry(imageId + ":" + timestamp);
        if(entry == null) {
            LOGGER.warn("No catalog entry was found, the Service Metadata page cannot be generated");
            throw new NotFoundException("No entry found for " + imageId + ":" + timestamp);
        }
        LOGGER.debug("Successfully retrieved catalog entry");
    }

    /**
     * Fetch Capabilities (OpenGIS), which includes content (matrix set, dataset description summary)
     * service provider info and service identification.
     * @return
     */
    public Response getResponse() {
        Response response = null;

        LOGGER.debug("Generating WMTS ServiceMetadata using " + entry.getImageId() + ":" + entry.getTimestamp());

        ObjectFactory wmtsFactory = new ObjectFactory();
        Capabilities capabilities = wmtsFactory.createCapabilities();
        capabilities.setContents(getContents(wmtsFactory));
        capabilities.setServiceProvider(getServiceProvider());
        capabilities.setServiceIdentification(getServiceIdentification());
        capabilities.setVersion("1.0.0");

        response = Response.ok(capabilities).build();

        return response;
    }

    private ServiceIdentification getServiceIdentification() {
        ServiceIdentification si = new ServiceIdentification();
        LanguageStringType title = new LanguageStringType();
        LanguageStringType description = new LanguageStringType();
        title.setValue("Image Web Map Tile Service");
        description.setValue("A WMTS Compliant wrapper for the image tile service for this image");
        si.getTitle().add(title);
        si.getAbstract().add(description);

        return si;
    }

    private ServiceProvider getServiceProvider() {
        ServiceProvider sp = new ServiceProvider();
        OnlineResourceType site = new OnlineResourceType();
        site.setHref(tileServerUrl);
        site.setTitle("Leidos Image Tile Services");
        sp.setProviderName("Leidos");
        sp.setProviderSite(site);
        return sp;
    }

    private ContentsType getContents(ObjectFactory wmtsFactory) {
        ContentsType contents = wmtsFactory.createContentsType();
        contents.getDatasetDescriptionSummary().add(wmtsFactory.createLayer(getLayers(wmtsFactory)));
        contents.getTileMatrixSet().add(getTileMatrixSet());
        return contents;
    }

    private LayerType getLayers(ObjectFactory wmtsFactory) {
        return createLayer(wmtsFactory, entry);
    }

    private TileMatrixSet getTileMatrixSet() {
        TileMatrixSet tileMatrixSet = new TileMatrixSet();

        tileMatrixSet.setIdentifier(new CodeType());
        tileMatrixSet.getIdentifier().setValue(entry.getTimestamp());
        tileMatrixSet.setSupportedCRS("CRS:1");

        int tileWidth = 512;
        int tileHeight = 512;

        int[][] matrixDims = getMatrixBounds(tileWidth, tileHeight, entry.getMaxRLevel());

        for (int i = 0; i <= entry.getMaxRLevel() ; i++) {
            TileMatrix tileMatrix = new TileMatrix();

            tileMatrix.setIdentifier(new CodeType());
            tileMatrix.getIdentifier().setValue(Integer.toString(i));

            tileMatrix.setScaleDenominator( Math.pow(2, i) ); // * GSD

            tileMatrix.setTileWidth(BigInteger.valueOf(tileWidth));
            tileMatrix.setTileHeight(BigInteger.valueOf(tileHeight));
            tileMatrix.setMatrixWidth(BigInteger.valueOf(matrixDims[i][0]));
            tileMatrix.setMatrixHeight(BigInteger.valueOf(matrixDims[i][1]));

            tileMatrixSet.getTileMatrix().add(tileMatrix);
        }

        return tileMatrixSet;
    }

    private int[][] getMatrixBounds(int tileWidth, int tileHeight, int numRSets) {

        int[][] bounds = new int[entry.getMaxRLevel() + 1][2];

        int xPixels = entry.getNROWS();
        int yPixels = entry.getNCOLS();

        for (int i = 0; i <= numRSets ; i++) {
            bounds[i][0] = xPixels % tileWidth == 0 ? xPixels / tileWidth : xPixels / tileWidth + 1;
            bounds[i][1] = yPixels % tileHeight == 0 ? yPixels / tileHeight : yPixels / tileHeight + 1;
            xPixels = xPixels / 2;
            yPixels = yPixels / 2;
        }

        return bounds;

    }

    private LayerType createLayer(ObjectFactory wmtsFactory, CatalogEntry imageMetadata) {
        LayerType layer = wmtsFactory.createLayerType();

        layer.setIdentifier(new CodeType());
        layer.getIdentifier().setValue(entry.getImageId());

        WGS84BoundingBoxType geoBBox = new WGS84BoundingBoxType();
        geoBBox.setCrs("EPSG:4326");
        Envelope geoEnvelope = imageMetadata.getBounds().getEnvelopeInternal();
        geoBBox.getLowerCorner().add(geoEnvelope.getMinX());
        geoBBox.getLowerCorner().add(geoEnvelope.getMinY());
        geoBBox.getUpperCorner().add(geoEnvelope.getMaxX());
        geoBBox.getUpperCorner().add(geoEnvelope.getMaxY());
        layer.getWGS84BoundingBox().add(geoBBox);

        Style style = wmtsFactory.createStyle();
        style.setIsDefault(true);
        style.setIdentifier(new CodeType());
        style.getIdentifier().setValue("tile");
        layer.getStyle().add(style);

        layer.getFormat().add("image/png");

        Dimension dimension = wmtsFactory.createDimension();
        dimension.setIdentifier(new CodeType());
        dimension.getIdentifier().setValue("Band");
        for (int i = 0; i < imageMetadata.getNBANDS(); i++) {
            dimension.getValue().add(Integer.toString(i));
        }
        layer.getDimension().add(dimension);

        MetadataType metadataType = new MetadataType();
        metadataType.setTitle("Image Metadata");
        metadataType.setHref(tileServerUrl + "/tileserver/" + imageMetadata.getImageId() + "/" + imageMetadata.getTimestamp() + "/metadata.json");
        layer.getMetadata().add(metadataType);

        TileMatrixSetLink tileMatrixSetLink = new TileMatrixSetLink();
        tileMatrixSetLink.setTileMatrixSet(entry.getTimestamp());
        layer.getTileMatrixSetLink().add(tileMatrixSetLink);


        // See Section 10.2.1, table 32 of WMTS Spec
        // There is only one style and one TileMatrixSet (the image pyramid) so those are hardcoded,
        // with the TileMatrixSet id being the timestamp to make the URL easily readable
        // example: https://tileserver.leidoslabs.com/058618316010_01_P002/20170726070444/tile/3/17/12/1.png
        // This would retrieve band 1 of the tile located at x: 17, y: 12 in RSet 3 of that image
        URLTemplateType urlTemplate = new URLTemplateType();
        urlTemplate.setFormat("image/png");
        urlTemplate.setResourceType("tile");
        urlTemplate.setTemplate(tileServerUrl + "/" + entry.getImageId() + "/" + entry.getTimestamp() + "/tile/{TileMatrix}/{TileCol}/{TileRow}/{Band}.png");
        layer.getResourceURL().add(urlTemplate);

        return layer;
    }

}
