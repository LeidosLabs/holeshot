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
package com.leidoslabs.supercluster;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map;

import com.google.gson.JsonElement;

/**
 * Wrapper class of an array of json, for easy gson parsing
 */
class Tile {
	
	public ArrayList<TileFeature> features = new ArrayList<TileFeature>();

	// for testing
	
	public boolean equals(Object other) {

		if(!(other instanceof Tile)) {
			return false;
		}

		Tile t = (Tile) other;

		HashSet<TileFeature> set1 = new HashSet<TileFeature>(features);
		HashSet<TileFeature> set2 = new HashSet<TileFeature>(t.features);

		return set1.equals(set2);
	}

	/*
		class for formatting Features into JSON with gson
	*/
	static class TileFeature {
		public int id;
		public int type = 1;
		public double[][] geometry = new double[1][2];
		public Map<String, JsonElement> tags;

		public TileFeature(double x, double y, Map<String, JsonElement> tags) {
			geometry[0][0] = x;
			geometry[0][1] = y;
			this.tags = tags;
		}

		//for testing
		public boolean equals(Object other) {
			
			if(!(other instanceof TileFeature)) {
				return false;
			}

			TileFeature t = (TileFeature) other;

			return 
				Math.abs(geometry[0][0] - t.geometry[0][0]) < .01 &&
				Math.abs(geometry[0][1] - t.geometry[0][1]) < .01; 
		}

		public int hashCode() {
			return Double.hashCode(geometry[0][0] * geometry[0][1]);
		}
	}
}
