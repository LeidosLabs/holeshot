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
package com.leidoslabs.holeshot.elt.viewport;

import org.dyn4j.dynamics.Body;
import org.dyn4j.dynamics.BodyFixture;
import org.dyn4j.dynamics.World;
import org.dyn4j.geometry.Circle;
import org.dyn4j.geometry.Mass;
import org.dyn4j.geometry.MassType;
import org.dyn4j.geometry.Vector2;
import org.joml.Matrix4d;
import org.joml.Matrix4dc;
import org.joml.Vector2d;
import org.joml.Vector2dc;
import org.joml.Vector3d;
import org.joml.Vector3dc;

import com.leidoslabs.holeshot.elt.coord.Angle;
import com.leidoslabs.holeshot.elt.coord.MapScale;
import com.leidoslabs.holeshot.elt.coord.Radians;

/**
 * @author robertsrg
 *
 */
public class MapOpenGLView {
	private static final Vector2 ZERO_VELOCITY = new Vector2(0.0, 0.0);
	
	private final MapScale mapScale;
	private final Body cameraBody;
	private Angle rotateOffNorth;
	private final ImageWorld imageWorld;

	public MapOpenGLView(World world, ImageWorld imageWorld) {
		this.imageWorld = imageWorld;
		this.mapScale = new MapScale();
		this.cameraBody = new Body(1);
		BodyFixture fixture = new BodyFixture(new Circle(1E-6));
		fixture.setDensity(1.0E-6);
		this.cameraBody.addFixture(fixture);
		this.cameraBody.setMassType(MassType.NORMAL);
		this.cameraBody.setMass(new Mass(new Vector2(0.0, 0.0), 0.1,1.0));
		this.rotateOffNorth = new Radians(0.0);
		world.addBody(this.cameraBody);
	}
	
	public Vector3dc getWorldCenter() {
		final Vector2 worldCenter = cameraBody.getWorldCenter();
		return new Vector3d(worldCenter.x, worldCenter.y, 0.0);
	}

	public Vector3dc getLinearVelocity() {
		final Vector2 vel = cameraBody.getLinearVelocity();
		return new Vector3d(vel.x, vel.y, 0.0);
	}
	public void setLinearVelocity(Vector3dc nudgeVec) {
		Vector2 nudgeVec2 = new Vector2(nudgeVec.x(), nudgeVec.y());
		if (!nudgeVec2.equals(cameraBody.getLinearVelocity())) {
			cameraBody.setLinearVelocity(nudgeVec2);
			cameraBody.setAsleep(false);
		}
	}
	public void clearLinearVelocity() {
		if (!ZERO_VELOCITY.equals(cameraBody.getLinearVelocity())) {
			cameraBody.setLinearVelocity(0.0, 0.0);
			cameraBody.setAsleep(true);
		}
	}
	public Matrix4dc getTSRMatrix() {
		final Vector2dc scale = getScaleFactor();
		final double rotate = rotateOffNorth.mul(-1.0).getRadians();
		final Vector3dc translate = getWorldCenter().mul(new Vector3d(scale, 1.0), new Vector3d()).mul(-1.0);
		
//		System.out.println(String.format("scale = %s rotate = %f translate = %s", scale.toString(), rotate, translate.toString()));
		
		final Matrix4dc tsrMatrix = new Matrix4d()
				.setTranslation(translate)
				.scale(scale.x(),  scale.y(),  1.0)
				.rotateLocalZ(rotate);
		
		return tsrMatrix;
	}
	
	public Vector2dc getScaleFactor() {
		MapScale topScale = imageWorld.getTopScale();
		double relativeScale = Math.pow(2.0, mapScale.getZoom() - topScale.getZoom());
		return new Vector2d(relativeScale);
	}
	
	/**
	 * @return the mapRotationOffNorth
	 */
	public Angle getMapRotationOffNorth() {
		return rotateOffNorth;
	}
	
	/**
	 * @param mapRotationOffNorth the mapRotationOffNorth to set
	 */
	public void setMapRotationOffNorth(Angle mapRotationOffNorth) {
		rotateOffNorth = mapRotationOffNorth;
	}
	
	public void rotateBy(Angle rotateBy) {
		rotateOffNorth = rotateOffNorth.add(rotateBy);
	}
	/**
	 * @return the mapScale
	 */
	public MapScale getMapScale() {
		return mapScale;
	}
	
	@Override
	public String toString() {
		return (String.format("scale = %f, translate = %s rotate = %f", mapScale.getZoom(), getWorldCenter().toString(), getMapRotationOffNorth().getDegrees()));
	}

	/**
	 * @param projectedCoordinate
	 */
	public void setMapCenter(Vector3dc projectedCenter) {
		if (!projectedCenter.equals(getWorldCenter())) {
			Vector3dc scaledTranslate = projectedCenter;
			cameraBody.translateToOrigin();
			cameraBody.translate(scaledTranslate.x(), scaledTranslate.y());
		}
	}
}
