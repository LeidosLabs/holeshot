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

@JsonIgnoreProperties(ignoreUnknown = true)
public class CacheList implements Indexable {
	
	private String userID;
	private String image;
	private long date;
	private ScoredTile[] tiles;
	
    public CacheList(ScoredTile[] tiles, String image, long date, String userID) {
        this.tiles = tiles;
        this.image = image;
        this.date = date;
        this.userID = userID;
    }

    public CacheList(List<ScoredTile> tiles, String image, long date, String userID) {
        this( tiles.toArray(new ScoredTile[0]), image, date, userID);
    }

    /**
     * For deserialization
     */
    public CacheList() { }
    
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
    
    public String getUserID() {
    	return this.userID;
    }
    
    public void setUserID(String userID) {
    	this.userID = userID;
    }
	
	
	@Override
	public String getId() {
		return this.getImage() + ":" + this.getDate();
	}

}
