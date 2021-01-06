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

package com.leidoslabs.holeshot.analytics.common.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * An object to hold all of the scored tiles within a particular image
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class TileScores implements Indexable {
    private ScoredTile[] tiles;
    private String image;
    private long date;

    /**
     * Contains scored tiles. The scores can only be assumed to be relevant for a short duration after the given date
     * @param tiles The list of scored tiles, should meet a minimum scoring threshold >0
     * @param image The image the tiles belong to
     * @param date The date at which the scores are most relevant (Probably when they are ingested or scored)
     */
    public TileScores(ScoredTile[] tiles, String image, long date) {
        this.tiles = tiles;
        this.image = image;
        this.date = date;
    }

    public TileScores(List<ScoredTile> tiles, String image, long date) {
        this( tiles.toArray(new ScoredTile[0]), image, date);
    }

    /**
     * For deserialization
     */
    public TileScores() { }

    public ScoredTile[] getTiles() {
        return tiles;
    }

    public void setTiles(ScoredTile[] tiles) {
        this.tiles = tiles;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public long getDate() {
        return date;
    }

    public void setDate(long date) {
        this.date = date;
    }

    @Override
    public String getId() {
        return this.getImage();
    }
}
