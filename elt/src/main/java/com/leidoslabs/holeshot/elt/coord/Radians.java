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
package com.leidoslabs.holeshot.elt.coord;

import org.apache.commons.math3.util.MathUtils;

/**
 * Immutable
 * 
 * @author robertsrg
 *
 */
public class Radians implements Angle {
	private final double angleInRadians;
	
	public Radians() {
		this(0.0);
	}
	public Radians(double angleInRadians) {
		this.angleInRadians = MathUtils.normalizeAngle(angleInRadians, Math.PI);
	}
	public Radians(Angle angle) {
		this(angle.getRadians());
	}

	@Override
	public double getDegrees() {
		return Math.toDegrees(angleInRadians);
	}

	@Override
	public double getRadians() {
		return angleInRadians;
	}

	@Override
	public Angle copy() {
		return new Radians(angleInRadians);
	}
	@Override
	public Angle add(Angle angle) {
		return new Radians(this.angleInRadians + angle.getRadians());
	}
	@Override
	public Angle sub(Angle angle) {
		return new Radians(this.angleInRadians - angle.getRadians());
	}

	@Override
	public Angle mul(double scalar) {
		return new Radians(this.angleInRadians * scalar);
	}

}
