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

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.locationtech.jts.geom.Polygon;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.MalformedURLException;
import java.net.URL;

@JsonDeserialize(using = CatalogEntryDeserializer.class)
public class CatalogEntry {
    private static final Logger LOGGER = LoggerFactory.getLogger(CatalogEntry.class);

    private String imageLink;
    private Polygon bounds;
    private int maxRLevel;
    private int NCOLS;
    private int NROWS;
    private int NBANDS;
    private String imageId;
    private String timestamp;


    /**
     * @return The location of the metadata.json file on the tileserver
     */
    public URL getMetadataURL() {
        URL metadataURL = null;
        if (imageLink != null) {
            try {
                metadataURL = new URL(String.format("%s/metadata.json", imageLink));
            } catch (MalformedURLException e) {
                LOGGER.error(e.getMessage(), e);
            }
        }
        return metadataURL;

    }

    /**
     * @return The base link for the image on the tileserver
     */
    public String getImageLink() {
        return imageLink;
    }

    /**
     * @param imageLink The base link for the image on the tileserver
     */
    public void setImageLink(String imageLink) {
        this.imageLink = imageLink;
    }

    /**
     * @return The world coordinates of the image bound vertices
     */
    public Polygon getBounds() {
        return bounds;
    }

    /**
     * @param bounds The world coordinates of the image bound vertices
     */
    public void setBounds(Polygon bounds) {
        this.bounds = bounds;
    }

    /**
     * @return The maximum level reduced resolution tileset in this image
     */
    public int getMaxRLevel() {
        return maxRLevel;
    }

    /**
     * @param maxRLevel The maximum level reduced resolution tileset in this image
     */
    public void setMaxRLevel(int maxRLevel) {
        this.maxRLevel = maxRLevel;
    }

    /**
     * @return The number of pixels in the horizontal span of this image
     */
    public int getNCOLS() {
        return NCOLS;
    }

    /**
     * @param NCOLS The number of pixels in the horizontal span of this image
     */
    public void setNCOLS(int NCOLS) {
        this.NCOLS = NCOLS;
    }

    /**
     * @return The number of pixels in the vertical span of this image
     */
    public int getNROWS() {
        return NROWS;
    }

    /**
     * @param NROWS The number of pixels in the vertical span of this image
     */
    public void setNROWS(int NROWS) {
        this.NROWS = NROWS;
    }

    /**
     * @return The number of sensor bands in the image
     */
    public int getNBANDS() {
        return NBANDS;
    }

    /**
     * @param NBANDS The number of sensor bands in the image
     */
    public void setNBANDS(int NBANDS) {
        this.NBANDS = NBANDS;
    }

    /**
     * @return The image identifier AKA collection ID
     */
    public String getImageId() {
        return imageId;
    }

    /**
     * @param imageId The image identifier AKA collection ID
     */
    public void setImageId(String imageId) {
        this.imageId = imageId;
    }

    /**
     * @return Timestamp on image, usually yyyyMMddHHmmss
     */
    public String getTimestamp() {
        return timestamp;
    }

    /**
     * @param timestamp Timestamp on image, usually yyyyMMddHHmmss
     */
    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    /**
     * @return "imageLink == {imageLink} bounds == {bounds}"
     */
    @Override
    public String toString() {
        return String.format("imageLink == %s bounds == %s", (imageLink == null) ? "null" : imageLink, (bounds == null) ? "null" : bounds.toText());
    }

}
