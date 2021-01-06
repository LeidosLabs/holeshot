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
public class Degrees implements Angle {
	private final double angleInDegrees;
	
	public Degrees() {
		this(0.0);
	}
	public Degrees(double angleInDegrees) {
		this.angleInDegrees = Math.toDegrees(MathUtils.normalizeAngle(Math.toRadians(angleInDegrees), Math.PI));
	}
	public Degrees(Angle angle) {
		this(angle.getDegrees());
	}

	@Override
	public double getDegrees() {
		return angleInDegrees;
	}

	@Override
	public double getRadians() {
		return Math.toRadians(angleInDegrees);
	}

	@Override
	public Angle copy() {
		return new Degrees(angleInDegrees);
	}
	@Override
	public Angle add(Angle angle) {
		return new Degrees(this.angleInDegrees + angle.getDegrees());
	}
	@Override
	public Angle sub(Angle angle) {
		return new Degrees(this.angleInDegrees - angle.getDegrees());
	}
	@Override
	public Angle mul(double scalar) {
		return new Degrees(this.angleInDegrees * scalar);
	}

}
