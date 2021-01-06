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
package com.leidoslabs.holeshot.imaging.coord;

import java.awt.geom.Point2D;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;

import com.leidoslabs.holeshot.imaging.photogrammetry.CameraModel;

/**
 * A representation of a 2-dimensional point and its relation to a digital image
 * @param <C> The source coordinate containing type
 */
public abstract class GeointCoordinate<C> {
	private C sourceCoordinate;

	/**
	 * Create a GeointCoordinate, an extensible wrapper for providing conversions between coordinate systems
	 * @param sourceCoordinate The raw coordinate in the implementing classes base coordinate system
	 */
	protected GeointCoordinate(C sourceCoordinate) {
		this.sourceCoordinate = sourceCoordinate;
	}

	/**
	 * Get this coordinates equivalent Longitude - Latitude point on the earth surface
	 * @return A Longitude - Latitude coordinate
	 */
	public abstract Coordinate getGeodeticCoordinate();


	/**
	 * Get the source coordinate
	 * @return This coordinate in its original coordinate object
	 */
	public C getSourceCoordinate() {
		return this.sourceCoordinate;
	}

	/**
	 * Get the envelope dimensions as an int array
	 * @param envelope The envelope to convert to int array
	 * @return [ minX, minY, width, height ]
	 */
	protected static int[] getData(Envelope envelope) {
		return new int[] {(int) envelope.getMinX(), (int) envelope.getMinY(), (int) envelope.getWidth(),
				(int) envelope.getHeight()};
	}

	/**
	 * Source Coordinate as a string
	 * @return sourceCoordinate.toString
	 */
	@Override
	public String toString() {
		return sourceCoordinate.toString();
	}
}
