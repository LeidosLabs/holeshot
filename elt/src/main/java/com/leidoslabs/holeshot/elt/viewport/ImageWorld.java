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

import java.awt.Rectangle;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Point2D;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Arrays;

import javax.media.jai.PerspectiveTransform;

import org.dyn4j.Listener;
import org.dyn4j.dynamics.World;
import org.joml.Matrix4d;
import org.joml.Matrix4dc;
import org.joml.RoundingMode;
import org.joml.Vector2d;
import org.joml.Vector2dc;
import org.joml.Vector2i;
import org.joml.Vector2ic;
import org.joml.Vector3d;
import org.joml.Vector3dc;
import org.joml.Vector4d;
import org.joml.Vector4dc;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Polygon;
import org.opengis.util.FactoryException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.leidoslabs.holeshot.elt.coord.Angle;
import com.leidoslabs.holeshot.elt.coord.ELTCoordinate;
import com.leidoslabs.holeshot.elt.coord.GeodeticELTCoordinate;
import com.leidoslabs.holeshot.elt.coord.MapScale;
import com.leidoslabs.holeshot.elt.coord.ProjectedCoordinate;
import com.leidoslabs.holeshot.elt.coord.Radians;
import com.leidoslabs.holeshot.elt.coord.ScreenELTCoordinate;
import com.leidoslabs.holeshot.elt.tileserver.TileRef;
import com.leidoslabs.holeshot.elt.tileserver.TileserverImage;
import com.leidoslabs.holeshot.elt.utils.GeometryUtils;
import com.leidoslabs.holeshot.imaging.coord.ImageScale;
import com.leidoslabs.holeshot.imaging.photogrammetry.CameraModel;

/**
 * @author robertsrg
 *
 */
public class ImageWorld {
	private static final Logger LOGGER = LoggerFactory.getLogger(ImageWorld.class);

	private static final Matrix4dc IDENTITY = new Matrix4d();
	private static final Vector3dc CLIP_ORIGIN  = new Vector3d(0.0, 0.0, 0.0);
	private static final Vector3dc X_AXIS = new Vector3d(1.0, 0.0, 0.0);
	private static final Vector3dc Y_AXIS = new Vector3d(0.0, 1.0, 0.0);
	private static final double EPSILON = 1E-3;
	//	private static final double EPSILON = 1E-5;
	private static final Vector2dc EPSILON2D = new Vector2d(EPSILON, EPSILON);

	public static final WebMercatorProjection WEB_MERCATOR_PROJECTION = new WebMercatorProjection();

	private Vector2ic screenSize;
	private final World world;
	private Matrix4dc targetTransform;
	private final MapOpenGLView view;
	private long lastTimeMS;
	private WGS84Projection projection;
	private MapScale fullProjectionZoom;

	public ImageWorld(Vector2ic screenSize) {
		this.world = new World();
		this.world.setGravity(World.ZERO_GRAVITY);
		this.world.getSettings().setMaximumTranslation(1.0E10);
		this.world.getSettings().setStepFrequency(1.0/240.0);
		this.view = new MapOpenGLView(this.world, this);
		setProjection(WEB_MERCATOR_PROJECTION);
		this.lastTimeMS = 0;
		this.screenSize = new Vector2i(screenSize);
		this.targetTransform = null;
	}

	public MapScale getMapScale() {
		return view.getMapScale();
	}

	public Vector2ic getViewportCenter() {
		Rectangle viewport = getCurrentViewport();
		return new Vector2i(new Vector2d(viewport.getCenterX(), viewport.getCenterY()).add(EPSILON2D), RoundingMode.FLOOR);
	}
	public void setScreenSize(Vector2ic screenSize) {
		this.screenSize = new Vector2i(screenSize);
	}
	public Vector2ic getScreenSize() {
		return this.screenSize;
	}
	
	// getGSD() is an expensive operation that's done frequently.   While it does vary across an image, it doesn't vary much.   Caching in the interest of performance.
	// TODO: Determine if this is an acceptable approach for a WebMercator projection.
	private void setFullProjectionZoom() {
		fullProjectionZoom = projection.getFullProjectionZoom(projection.getCentroid().getGeodeticCoordinate());
	}

	public MapScale getFullProjectionZoom() {
		MapScale result;
		if (!(projection instanceof ImageProjection)) {
			result = projection.getFullProjectionZoom(getGeodeticCenter().getGeodeticCoordinate());
		} else {
			result = fullProjectionZoom;
		}
		return result;
	}

	public Vector2dc getFullProjectionSizeAtScale() {
		return getFullProjectionSizeAtScale(getMapScale());
	}

	public Vector2dc getFullProjectionSizeAtScale(MapScale mapscale) {
		return projection.getFullProjectionSizeAtScale(getGeodeticCenter().getGeodeticCoordinate(), mapscale);
	}

	public Rectangle getCurrentViewport() {
		Rectangle screen = new Rectangle(0,0, screenSize.x(), screenSize.y());
		return screen;
	}
	public Matrix4dc getOpenGLModel() {
		return IDENTITY;
	}

	public MapScale getTopScale() {
		return projection.getTopScale();
	}
	public double getScaleDiff() {
		double fullProjectionScale = getFullProjectionZoom().getZoom();
		double topScale = getTopScale().getZoom();
		double scaleDiff = fullProjectionScale - topScale;
		return scaleDiff;
	}

	public Vector2dc getScaledProjection() {
	    final Vector2dc viewport = GeometryUtils.toVector2d(getCurrentViewport());
		final Vector2dc scaledProjection = viewport.mul(Math.pow(2.0, getScaleDiff()), new Vector2d());
		return scaledProjection;
	}


	public Matrix4dc getOpenGLProjection() {
		Vector2dc scaledProjection = getScaledProjection();
		Matrix4d result = new Matrix4d().setOrthoSymmetric(scaledProjection.x(), scaledProjection.y(), 1.0E1, -1.0E1);

		if (projection.getXAxisDirection()<0.0) {
			result = result.reflect(X_AXIS, CLIP_ORIGIN);

		}
		if (projection.getYAxisDirection()<0.0) {
			result = result.reflect(Y_AXIS, CLIP_ORIGIN);
		}

		return result;
	}

	public GeodeticELTCoordinate getGeodeticCenter() {
		Coordinate geoCenter = projection.transformFrom(GeometryUtils.toCoordinate(view.getWorldCenter()));
		return new GeodeticELTCoordinate(this, geoCenter);
	}

	public Matrix4dc getOpenGLMVPMatrix() {
		Matrix4dc result;
		if (targetTransform == null) {
			result = getOpenGLProjection().mul(view.getTSRMatrix(), new Matrix4d()).mul(getOpenGLModel());
		} else {
			result = targetTransform;
		}
		return result;
	}

	public Matrix4dc getImageToProjected(TileserverImage image) {
		Matrix4dc result;

		if (this.projection instanceof ImageProjection) {
			result = IDENTITY;
		} else {
			final Coordinate[] imageBounds = image.getR0ImageBounds().getCoordinates();
			final Coordinate[] projectedBounds = getProjectedImage(image).getCoordinates();

			PerspectiveTransform perTr = PerspectiveTransform.getQuadToQuad( 
					imageBounds[0].x, imageBounds[0].y,
					imageBounds[1].x, imageBounds[1].y,
					imageBounds[2].x, imageBounds[2].y,
					imageBounds[3].x, imageBounds[3].y,
					projectedBounds[0].x, projectedBounds[0].y,
					projectedBounds[1].x, projectedBounds[1].y,
					projectedBounds[2].x, projectedBounds[2].y,
					projectedBounds[3].x, projectedBounds[3].y);

			double[][] mat = new double[3][3];
			mat = perTr.getMatrix( mat );
			result = new Matrix4d(
					mat[0][0], mat[0][1], 0.0, mat[0][2],
					mat[1][0], mat[1][1], 0.0, mat[1][2],
					0.0,       0.0,       1.0, 0.0,
					mat[2][0], mat[2][1], 0.0, mat[2][2]).transpose();
			
		} 

		return result;
	}

	public Matrix4dc getOpenGLImageMVPMatrix(TileserverImage image) {
		return getOpenGLMVPMatrix().mul(getImageToProjected(image), new Matrix4d());
	}
	public Vector3dc screenToClip(Vector2ic screenCoord) {
		Vector2ic screen = getScreenSize();
		Vector3dc result = getOpenGLMVPMatrix().unproject(screenCoord.x(), screenCoord.y(), 0.0, new int[] { 0, 0, (int)screen.x(), (int)screen.y() }, new Vector3d());
		return result;
	}
	public Vector2ic clipToScreen(Vector3dc clipCoord) {
		return clipToScreen(clipCoord, getCurrentViewport());
	}

	public Vector2ic clipToScreen(Vector3dc clipCoord, Rectangle viewport) {
		Vector2ic viewportOffset = new Vector2i(viewport.x, viewport.y);

		return new Vector2i(new Vector2d(viewport.getWidth() * (clipCoord.x() + 1.0) / 2.0,
				viewport.getHeight() * (1.0-clipCoord.y()) / 2.0).add(EPSILON2D), RoundingMode.FLOOR).add(viewportOffset);
	}

	public Coordinate screenToProjected(Vector2ic screenCoord) {
		Rectangle viewport = getCurrentViewport();
		Vector2ic viewportOffset = new Vector2i(viewport.x, viewport.y);
		Vector2ic relativeScreenCoord = screenCoord.sub(viewportOffset, new Vector2i());

		final Coordinate result = 
				GeometryUtils.toCoordinate(
						getOpenGLMVPMatrix().unproject(relativeScreenCoord.x(), 
								viewport.getHeight() - relativeScreenCoord.y(), 
								0.0,
								new int[] { 0, 0, viewport.width, viewport.height}, new Vector3d()));
		result.setZ(0.0);
		return result;
	}
	public Coordinate clipToProjected(Vector3dc clipCoord) {
		final Vector2ic clipToScreen = clipToScreen(clipCoord);
		final Coordinate result = screenToProjected(clipToScreen);
		return result;
	}
	public Coordinate projectedToGeodetic(Coordinate projected) {
		return getProjection().transformFrom(projected);
	}
	public Coordinate clipToGeodetic(Vector3dc clipCoord) {
		final Coordinate projected = clipToProjected(clipCoord);
		final Coordinate geodetic = projectedToGeodetic(projected);
		return geodetic;
	}
	public Coordinate screenToGeodetic(Vector2ic screenCoord) {
		return projectedToGeodetic(screenToProjected(screenCoord));
	}
	public Coordinate geodeticToProjected(Coordinate geodetic) {
		return getProjection().transformTo(geodetic);
	}
	public Vector3dc projectedToClip(Coordinate projected) {
		Vector4d newVec = GeometryUtils.toVector4d(projected).mul(getOpenGLMVPMatrix(), new Vector4d());
		Vector3dc result = GeometryUtils.toVector3d(newVec.div(newVec.w));
		return result;
	}
	public Vector3dc geodeticToClip(Coordinate geodetic) {
		Vector3dc result = projectedToClip(geodeticToProjected(geodetic));
		return result;
	}
	public Vector2ic geodeticToScreen(Coordinate geodetic) {
		return clipToScreen(geodeticToClip(geodetic));
	}
	public Vector2ic projectedToScreen(Coordinate projected) {
		return clipToScreen(projectedToClip(projected));
	}
	public Vector2ic projectedToScreen(Coordinate projected, Rectangle viewport) {
		return clipToScreen(projectedToClip(projected), viewport);
	}

	public Coordinate imageToGeodetic(TileserverImage image, Point2D imageCoord) {
		final CameraModel camera = image.getCameraModel();
		return camera.imageToWorld(imageCoord);
	}
	public Coordinate imageToProjected(TileserverImage image, Point2D imageCoord) {
		Coordinate result = geodeticToProjected(imageToGeodetic(image, imageCoord));
		return result;
	}
	public Vector3dc imageToClip(TileserverImage image, Point2D imageCoord) {
		Vector3dc result = projectedToClip(imageToProjected(image, imageCoord));
		return result;
	}
	public Vector2ic imageToScreen(TileserverImage image, Point2D imageCoord) {
		return clipToScreen(imageToClip(image, imageCoord));
	}
	public Point2D geodeticToImage(TileserverImage image, Coordinate geodeticCoord) {
		final CameraModel camera = image.getCameraModel();
		return camera.worldToImage(geodeticCoord);
	}
	public Point2D projectedToImage(TileserverImage image, Coordinate projectedCoord) {
		// TODO: When we eventually get to having image-space projections, this should be changed to be an identity (e.g. Projected == image )
		return geodeticToImage(image, projectedToGeodetic(projectedCoord));
	}
	public Point2D clipToImage(TileserverImage image, Vector3dc clipCoord) {
		return projectedToImage(image, clipToProjected(clipCoord));
	}
	public Point2D screenToImage(TileserverImage image, Vector2ic screenCoord) {
		return clipToImage(image, screenToClip(screenCoord));
	}

	public Polygon getProjectedImage(TileserverImage image) {
		return GeometryUtils.GEOMETRY_FACTORY.createPolygon(
				Arrays.stream(image.getR0ImageBounds().getCoordinates())
				.map(c-> GeometryUtils.toPoint2D(c))
				.map(p-> imageToProjected(image, p))
				.toArray(Coordinate[]::new));
	}
	public Polygon getClipImage(TileserverImage image) {
		return GeometryUtils.GEOMETRY_FACTORY.createPolygon(
				Arrays.stream(image.getR0ImageBounds().getCoordinates())
				.map(c-> GeometryUtils.toPoint2D(c))
				.map(p-> GeometryUtils.toCoordinate(imageToClip(image, p)))
				.toArray(Coordinate[]::new));
	}

	public Polygon getProjectedImageVisible(TileserverImage image) {
		return getProjectedTileVisible(image.getTopTile());
	}
	public Polygon getProjectedTile(TileRef tile) {
		return GeometryUtils.GEOMETRY_FACTORY.createPolygon(
				Arrays.stream(tile.getImageSpaceBounds().getCoordinates())
				.map(c-> GeometryUtils.toPoint2D(c))
				.map(p-> imageToProjected(tile.getImage(), p))
				.toArray(Coordinate[]::new));
	}
	public Polygon getScreenTile(TileRef tile) {
		return GeometryUtils.GEOMETRY_FACTORY.createPolygon(Arrays.stream(getProjectedTile(tile).getCoordinates())
				.map(c->GeometryUtils.toCoordinate(projectedToScreen(c)))
				.toArray(Coordinate[]::new));
	}

	public Polygon getProjectedTileVisible(TileRef tile) {
		Polygon result = null;
		Geometry intersection = getProjectedTile(tile).intersection(getProjectedViewport());

		if (intersection instanceof Polygon) {
			result = (Polygon)intersection;
		} 
		return result;
	}

	public double getPercentVisible(TileserverImage image) {
		double result = 0.0;
		final Polygon projectedImageVisible = getProjectedImageVisible(image);
		if (projectedImageVisible != null) {
			result = projectedImageVisible.getArea() / getProjectedViewport().getArea();
		}
		return result;
	}

	public Polygon getProjectedViewport() {
		final Rectangle viewport = getCurrentViewport();
		
		final Coordinate[] projectedCoordinates = 
				Arrays.stream(new double[][] {
					{ viewport.getMinX(), viewport.getMaxY() - 1 },
					{ viewport.getMaxX() - 1, viewport.getMaxY() - 1 },
					{ viewport.getMaxX() - 1, viewport.getMinY() },
					{ viewport.getMinX(), viewport.getMinY() },
					{ viewport.getMinX(), viewport.getMaxY() - 1 }
				}).map(c-> new Vector2i((int)c[0], (int)c[1])).map(p->screenToProjected(p)).toArray(Coordinate[]::new);
		
		final Polygon projectedViewport = GeometryUtils.GEOMETRY_FACTORY.createPolygon(projectedCoordinates);

		return projectedViewport;
	}

	public void setLinearVelocity(Vector3dc nudgeVec) {
		view.setLinearVelocity(nudgeVec);
	}

	public void clearLinearVelocity() {
		view.clearLinearVelocity();
	}


	public void moveTo(ELTCoordinate<?> moveTo) {
		final Coordinate geodeticCoordinate = moveTo.getGeodeticCoordinate();
		if (!this.getGeodeticCenter().getGeodeticCoordinate().equals(geodeticCoordinate)) {
			final Coordinate projectedCoordinate = geodeticToProjected(geodeticCoordinate);

			view.setMapCenter(GeometryUtils.toVector3d(projectedCoordinate));
		}

	}
	public void zoomTo(ELTCoordinate<?> zoomTo, MapScale scale) {
		moveTo(zoomTo);
		scaleTo(scale);
	}
	public void scaleTo(MapScale scale) {
		MapScale adjustedScale = scale;
		
		// Zoom to integer boundaries when not in an image projection.
		if (!(projection instanceof ImageProjection)) {
			adjustedScale = new MapScale(Math.floor(scale.getZoom() + 0.5));
		}
		if (!adjustedScale.equals(getMapScale())) {
			view.getMapScale().set(adjustedScale);
		}
	}

	public void zoomToUL(ELTCoordinate<?> zoomTo, MapScale mapScale) {
		zoomTo(zoomTo, mapScale);
		
		Vector3dc oldULProjected = GeometryUtils.toVector3d(new ScreenELTCoordinate(this, new Vector2i(0,0)).getProjectedCoordinate());
		Vector3dc newULProjected = GeometryUtils.toVector3d(zoomTo.getProjectedCoordinate());
		
		Vector3d newCenterProjected = newULProjected.mul(2.0, new Vector3d()).sub(oldULProjected);
		
		moveTo(new ProjectedCoordinate(this, GeometryUtils.toCoordinate(newCenterProjected)));
	}

	public void rotateBy(Angle rotateBy) {
		view.rotateBy(rotateBy);
	}
	public void rotateTo(Angle rotateOffNorth) {
		view.setMapRotationOffNorth(rotateOffNorth);
	}
	public Angle getRotation() {
		return view.getMapRotationOffNorth();
	}

	public Polygon getGeodeticViewport() {
		final Polygon geodeticViewport = GeometryUtils.GEOMETRY_FACTORY.createPolygon(
				Arrays.stream(getProjectedViewport().getCoordinates()).map(c->projectedToGeodetic(c)).toArray(Coordinate[]::new));
		return geodeticViewport;
	}

	/**
	 * Set the viewport to a very specific bounding box.  
	 * This is primarily used by the chipping server to impose a warp on the chip to put it into WGS84.
	 * 
	 * @param lowerLeftCoord lower left cord of geodetic viewport
	 * @param lowerRightCoord lower right cord of geodetic viewport
	 * @param upperRightCoord upper right cord of geodetic viewport
	 * @param upperLeftCoord upper left cord of geodetic viewport
	 * @throws NoninvertibleTransformException
	 * @throws CloneNotSupportedException
	 */
	public void setGeodeticViewport(TileserverImage image, Coordinate lowerLeftCoord, Coordinate lowerRightCoord, Coordinate upperRightCoord, Coordinate upperLeftCoord) throws NoninvertibleTransformException, CloneNotSupportedException {

		Polygon geodeticViewport = GeometryUtils.GEOMETRY_FACTORY.createPolygon(new Coordinate[] {
				lowerLeftCoord, lowerRightCoord, upperRightCoord, upperLeftCoord, lowerLeftCoord
		});

		final Coordinate[] projectedCoords = Arrays.stream(geodeticViewport.getCoordinates())
				.map(c->geodeticToProjected(c))
				.toArray(Coordinate[]::new);

//		this.moveTo(new GeodeticELTCoordinate(this, GeometryUtils.toCoordinate(geodeticViewport.getCentroid())));

	    final double yAxisDir = getProjection().getYAxisDirection();
		PerspectiveTransform perTr = PerspectiveTransform.getQuadToQuad( 
				projectedCoords[0].x, projectedCoords[0].y, //LL
				projectedCoords[3].x, projectedCoords[3].y, //UL
				projectedCoords[2].x, projectedCoords[2].y, //UR
				projectedCoords[1].x, projectedCoords[1].y, //LR
				-1.0, yAxisDir,
				-1.0, -yAxisDir,
				1.0, -yAxisDir,
				1.0, yAxisDir);
		
		double[][] mat = new double[3][3];
		mat = perTr.getMatrix( mat );
		
		targetTransform = new Matrix4d(mat[0][0], mat[0][1], 0.0, mat[0][2],
                                       mat[1][0], mat[1][1], 0.0, mat[1][2],
                                       0.0,       0.0,       1.0, 0.0,
                                       mat[2][0], mat[2][1], 0.0, mat[2][2]).transpose();
//		Arrays.stream(projectedCoords).forEach(c-> {
//			Vector2dc screen = new Vector2d(512,512);
//			Vector4dc v = GeometryUtils.toVector4d(c);
//			Vector4d newVec = v.mul(targetTransform, new Vector4d());
//			newVec.div(newVec.w);
//
//			Vector2dc screenCoord = new Vector2d(((newVec.x() + 1.0) / 2.0) * screen.x(), ((newVec.y() + 1.0)/2.0) * screen.y());
//			Vector3d newVec3 = targetTransform.unproject(screenCoord.x(), screenCoord.y(), 0.0, new int[] { 0, 0, (int)screen.x(), (int)screen.y() }, new Vector3d());
//			System.out.println(c.toString() + " == " + newVec.toString() + " == " + screenCoord.toString() + " == " + newVec3.toString());
//
//		});


		
		final Vector2ic newScreenSize = getScreenSize();
		rotateTo(new Radians(0.0));

		Envelope chipRegion = new Envelope();
		Arrays.stream(projectedCoords).forEach(c->chipRegion.expandToInclude(c));

		final double scaleX = newScreenSize.x() / chipRegion.getWidth() ;
		final double scaleY = newScreenSize.y() / chipRegion.getHeight();
		ImageScale imageScale = new ImageScale(scaleX, scaleY);
		final double rset = Math.max(imageScale.getRsetForScaleX(), imageScale.getRsetForScaleY());

		Vector2dc chipCenter = GeometryUtils.toVector2d(chipRegion.centre());  
		Coordinate geoCenter = image.getCameraModel().imageToWorld(GeometryUtils.toPoint2D(chipCenter));
 		zoomToImage(image, new GeodeticELTCoordinate(this, geoCenter), rset);

	}
	
	public boolean isGeodeticViewportSet() {
		return targetTransform != null;
	}
	public void clearGeodeticViewport() {
		targetTransform = null;
	}

	public boolean update() {
		boolean isUpdated = false;
		long now = Instant.now().toEpochMilli();
		if (lastTimeMS > 0) {
			long diffTimeMS = now - lastTimeMS;
			double diffTimeS = ((double)diffTimeMS)/1000.0;
			isUpdated = world.update(diffTimeS,-1.0, 1);
		}
		lastTimeMS = now;
		return isUpdated;
	}

	public Polygon getOpenGLViewport() {
		final Rectangle viewport = getCurrentViewport();
		final Coordinate[] viewportCoords = Arrays
				.stream(new double[][] {{viewport.getMinX(), viewport.getMaxY()},
					{viewport.getMaxX(), viewport.getMaxY()},
					{viewport.getMaxX(), viewport.getMinY()},
					{viewport.getMinX(), viewport.getMinY()},
					{viewport.getMinX(), viewport.getMaxY()},})
				.map(a -> new ScreenELTCoordinate(this, new Vector2i((int)a[0], (int)a[1])))
				.map(s -> s.getOpenGLCoordinate()).map(v -> GeometryUtils.toCoordinate(v))
				.toArray(Coordinate[]::new);

		return GeometryUtils.GEOMETRY_FACTORY.createPolygon(viewportCoords);
	}	

	public void addListener(Listener listener) {
		world.addListener(listener);
	}

	public WGS84Projection getProjection() {
		return projection;
	}

	/**
	 * @param webMercatorProjection
	 */
	public void setProjection(WGS84Projection projection) {
		if (!projection.equals(this.projection)) {
			GeodeticELTCoordinate geoCenter = null;
			Angle projectionRotationDiff = null;
			
			boolean oldProjectionExists = this.projection != null;
			if (oldProjectionExists) {
				geoCenter = getGeodeticCenter();
				Angle oldAngleOffNorth = getRotation();
				Angle newAngleOffNorth = projection.getProjectionAngleOffNorth();
				projectionRotationDiff = newAngleOffNorth.sub(oldAngleOffNorth);
			}

			//			System.out.println(String.format("oldAngleOffNorth = %f newAngleOffNorth = %f diff = %f", oldAngleOffNorth.getDegrees(), newAngleOffNorth.getDegrees(), projectionRotationDiff.getDegrees()));

			this.projection = projection;
			setFullProjectionZoom();
			
			if (oldProjectionExists) {
				moveTo(geoCenter);
				rotateTo(projectionRotationDiff);
			}
		} 
		
		// When working with map projections, ensure that zoom is on integer increments.
		scaleTo(getMapScale());
	}

	public void setProjection(TileserverImage image) throws FactoryException {
		setProjection(new ImageProjection(image));
	}

	public static double getRset(TileserverImage image, Coordinate latLon, MapScale mapScale) {
		final double mapGSD = mapScale.getGSD(latLon);
		final double r0GSD = image.getGSD(latLon, 0.0);

		double doubleResult = new BigDecimal(Math.log(mapGSD/r0GSD) / Math.log(2.0)).setScale(5, RoundingMode.HALF_EVEN).doubleValue();

		if (Double.isNaN(doubleResult)) {
			System.out.println(String.format("getRset returning NaN.  mapGSD = %f, r0GSD = %f, result = %f latlon = %s mapScale = %f", mapGSD, r0GSD, doubleResult, latLon.toString(), mapScale.getZoom()));
		} 
		return doubleResult;

	}
	public MapScale getMapScale(TileserverImage image, Coordinate latLon, double rset) {
		return MapScale.mapScaleForGSD(latLon, image.getGSD(latLon, rset));
	}


	/**
	 * @param image
	 */
	public void zoomToImage(TileserverImage image) {
		final Coordinate center = GeometryUtils.toCoordinate(image.getGeodeticBounds().getCentroid());
		final ELTCoordinate<?> eltCenter = new GeodeticELTCoordinate(this, center);
		zoomToImage(image, eltCenter, image.getMaxRLevel());
	}

	public void zoomToImage(TileserverImage image, ELTCoordinate<?> center, double rset) {
		final MapScale mapScale = new MapScale(getMapScale(image, center.getGeodeticCoordinate(), rset).getZoom());
		zoomTo(center, mapScale);
	}

	public static void initialize() {
	}
}
