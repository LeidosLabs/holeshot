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
package com.leidoslabs.holeshot.elt.tileserver;
import java.util.Comparator;

/**
 * Base representation for tile references
 */
public class AbstractTileRef <T extends AbstractTileRef<?>> implements Comparable<T> {
	private final String collectionID;
	private final String timestamp;
	private final int rset;
	private final int x;
	private final int y;
	private final int band;

	//TileRefs are compared first by of collection id, then by timestamp, rset, band, x, and y in the case of ties
	private static final Comparator<AbstractTileRef<?>> TILEREF_COMPARATOR = 
			Comparator.comparing(AbstractTileRef<?>::getCollectionID)
			.thenComparing(AbstractTileRef::getTimestamp)
			.thenComparing(AbstractTileRef::getRset)
			.thenComparing(AbstractTileRef::getBand)
			.thenComparing(AbstractTileRef::getY)
			.thenComparing(AbstractTileRef::getX);


	/**
	 * Initialize tile reference from tile's distinguishing information
	 * @param collectionID
	 * @param timestamp
	 * @param rset
	 * @param x
	 * @param y
	 * @param band
	 */
	public AbstractTileRef(String collectionID, String timestamp, int rset, int x, int y, int band) {
		this.collectionID = collectionID;
		this.timestamp = timestamp;
		this.rset = rset;
		this.x = x;
		this.y = y;
		this.band = band;
	}
	
	public String getCollectionID() {
		return collectionID;
	}
	public String getTimestamp() {
		return timestamp;
	}
	public int getRset() {
		return rset;
	}
	public int getX() {
		return x;
	}
	public int getY() {
		return y;
	}
	public int getBand() {
		return band;
	}
	
	@Override
	public int compareTo(T tileRef) {
		return TILEREF_COMPARATOR.compare(this,  tileRef);
	}
}
