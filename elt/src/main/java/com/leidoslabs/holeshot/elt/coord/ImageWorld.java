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

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Point2D;
import java.time.Instant;
import java.util.Arrays;

import javax.media.jai.PerspectiveTransform;

import org.dyn4j.dynamics.Body;
import org.dyn4j.dynamics.BodyFixture;
import org.dyn4j.dynamics.Step;
import org.dyn4j.dynamics.StepListener;
import org.dyn4j.dynamics.World;
import org.dyn4j.geometry.Circle;
import org.dyn4j.geometry.Mass;
import org.dyn4j.geometry.MassType;
import org.dyn4j.geometry.Transform;
import org.dyn4j.geometry.Vector2;
import org.joml.Matrix4d;
import org.joml.Vector2d;
import org.joml.Vector3d;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Polygon;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.leidoslabs.holeshot.elt.tileserver.TileRef;
import com.leidoslabs.holeshot.elt.tileserver.TileserverImage;
import com.leidoslabs.holeshot.elt.utils.GeometryUtils;
import com.leidoslabs.holeshot.imaging.coord.GeodeticCoordinate;
import com.leidoslabs.holeshot.imaging.coord.ImageCoordinate;
import com.leidoslabs.holeshot.imaging.coord.ImageScale;
import com.leidoslabs.holeshot.imaging.photogrammetry.CameraModel;

/**
 * The physical model for the viewport (Defines extent, rotation, etc)
 */
public class ImageWorld {
   private static final Logger LOGGER = LoggerFactory.getLogger(ImageWorld.class);

   private static Vector2d NORTH_VECTOR = new Vector2d(0.0, 1.0);

   private final ImageScale imageScale;
   private Envelope currentViewport;
   public static final int DEFAULT_WIDTH = 512;
   public static final int DEFAULT_HEIGHT = 512;

   private TileRef topTile;
   private final World world;
   private final Body cameraBody;
   private long lastTimeMS;
   private Polygon geodeticViewport;
   private Matrix4d targetTransform;


   /**
    * Constructor, initialize world and camerabody
    */
   public ImageWorld() {
      this.imageScale = ImageScale.forRset(0);
      this.world = new World();
      this.world.setGravity(World.ZERO_GRAVITY);
      this.cameraBody = new Body(1);
      BodyFixture fixture = new BodyFixture(new Circle(1.0));
      fixture.setDensity(1.0);
      this.cameraBody.addFixture(fixture);
      cameraBody.setMassType(MassType.NORMAL);
      cameraBody.setMass(new Mass(new Vector2(0.0, 0.0), 1.0, 1.0));

      this.world.addBody(this.cameraBody);
      this.world.addListener(new ImageWorldStepListener());
   }
   
   /**
    * Initialize ImageWorld with a tile
    * @param topTile
    */
   public ImageWorld(TileRef topTile) {
      this();
      setTopTile(topTile);
   }

   private class ImageWorldStepListener implements StepListener {
      @Override
      public void begin(Step step, World world) {
      }

      @Override
      public void updatePerformed(Step step, World world) {
      }

      @Override
      public void postSolve(Step step, World world) {
      }

      @Override
      public void end(Step step, World world) {
         final Vector2 cameraCenter = cameraBody.getWorldCenter();
         ELTCoordinate<?> newWorldCenter = new OpenGLELTCoordinate(ImageWorld.this, new Vector3d(cameraCenter.x, cameraCenter.y, imageScale.getRset()), imageScale);

         zoomTo(newWorldCenter, imageScale);
      }

   }

   /**
    * Sets the world's update required flag to true
    */
   public void setUpdateRequired() {
      this.world.setUpdateRequired(true);
   }

   private Envelope getOriginalOrtho() {
      final TileserverImage tileserverImage = topTile.getImage();

      final ImageScale topTileScale = ImageScale.forRset(topTile.getRset());

      Point2D currentViewportDim = new Point2D.Double(currentViewport.getWidth(), currentViewport.getHeight());
      Point2D currentViewportNormalizedDim = tileserverImage.normalize(topTileScale.scaleUpToR0(currentViewportDim));

      double offsetX = 1.0 - currentViewportNormalizedDim.getX();
      double offsetY = 1.0 - currentViewportNormalizedDim.getY();

      return new Envelope(-1.0 + offsetX, 1.0 - offsetX, -1.0 + offsetY, 1.0 - offsetY);
   }

   /**
    * @return GeodeticViewport cords as OpenGL cords
    */
   public Envelope getCurrentOrtho() {
      Envelope currentOrtho;
      if (geodeticViewport == null) {
         final Envelope originalOrtho = getOriginalOrtho();
         currentOrtho =
               GeometryUtils.multiply(originalOrtho, Math.pow(2.0, imageScale.getRsetForScaleX() - (double)topTile.getRset()),
                     Math.pow(2.0, imageScale.getRsetForScaleY() - (double)topTile.getRset()));

         Vector2 worldCenter = cameraBody.getWorldCenter();
         currentOrtho = GeometryUtils.add(currentOrtho, new Vector3d(worldCenter.x, worldCenter.y, 0.0));
      } else {
         Coordinate[] openGLCoords = Arrays.stream(geodeticViewport.getCoordinates())
               .map(c-> new GeodeticELTCoordinate(this, c, ImageScale.forRset(0)))
               .map(g-> g.getOpenGLCoordinate())
               .map(v->new Coordinate(v.x, v.y, v.z))
               .toArray(Coordinate[]::new);
         final GeometryFactory factory = new GeometryFactory();
         currentOrtho = factory.createPolygon(openGLCoords).getEnvelopeInternal();
      }
      return currentOrtho;
   }

   private TileserverImage getImage() {
      return this.getTopTile().getImage();
   }
   private CameraModel getCameraModel() {
      return getImage().getCameraModel();
   }


   /**
    * @return Center of current ortho
    */
   public Vector3d getCurrentOrthoCenter() {
      return GeometryUtils.toVector3d(getCurrentOrtho().centre());
   }

   /**
    * Set the geodetic viewport. Updates camera body and target transform
    * @param lowerLeftCoord lower left cord of geodetic viewport
    * @param lowerRightCoord lower right cord of geodetic viewport
    * @param upperRightCoord upper right cord of geodetic viewport
    * @param upperLeftCoord upper left cord of geodetic viewport
    * @throws NoninvertibleTransformException
    * @throws CloneNotSupportedException
    */
   public void setGeodeticViewport(Coordinate lowerLeftCoord, Coordinate lowerRightCoord, Coordinate upperRightCoord, Coordinate upperLeftCoord) throws NoninvertibleTransformException, CloneNotSupportedException {

      final GeometryFactory geometryFactory = new GeometryFactory();
      geodeticViewport = geometryFactory.createPolygon(new Coordinate[] {
            lowerLeftCoord, lowerRightCoord, upperRightCoord, upperLeftCoord, lowerLeftCoord
      });

      Coordinate[] geodeticViewportCoordinates = geodeticViewport.getCoordinates();

      final Coordinate[] openGLArray = Arrays.stream(geodeticViewportCoordinates)
            .map(c-> new GeodeticELTCoordinate(this, c, ImageScale.forRset(0)))
            .map(g->g.getOpenGLCoordinate())
            .map(v->GeometryUtils.toCoordinate(v))
            .toArray(Coordinate[]::new);

      final Polygon openGLPoly = geometryFactory.createPolygon(openGLArray);
      final Envelope openGLEnvelope = openGLPoly.getEnvelopeInternal();
      final Coordinate openGLCenter = openGLEnvelope.centre();

      this.cameraBody.getTransform().setTranslation(openGLCenter.x, openGLCenter.y);

      PerspectiveTransform perTr = PerspectiveTransform.getQuadToQuad( openGLArray[0].x, openGLArray[0].y,
            openGLArray[1].x, openGLArray[1].y,
            openGLArray[2].x, openGLArray[2].y,
            openGLArray[3].x, openGLArray[3].y,
            -1.0, -1.0,
            1.0, -1.0,
            1.0, 1.0,
            -1.0, 1.0);

      double[][] mat = new double[3][3];
      mat = perTr.getMatrix( mat );
      targetTransform = new Matrix4d(mat[0][0], mat[1][0],0.0, mat[2][0],
                                     mat[0][1], mat[1][1],0.0, mat[2][1],
                                     0.0, 0.0,1.0, 0.0,
                                     mat[0][2],mat[1][2],0.0,mat[2][2]);

//      Arrays.stream(openGLArray)
//      .map(coord->new Vector3d(coord.x, coord.y, 0.0))
//      .forEach(p->System.out.println(p.toString() + "-->" + p.mulProject(targetTransform).toString()));


      if (LOGGER.isDebugEnabled()) {
         LOGGER.debug("desiredViewport == " + geodeticViewport.toString());
         LOGGER.debug("actualViewport == " + this.getGeodeticViewport().toString());
      }
   }

   
   /**
    * Sets geodeticViewport to null
    */
   public void setOrthoViewport() {
      this.geodeticViewport = null;
   }

   /**
    * @return Camera perspective projection transformation
    */
   public Matrix4d getCurrentProjection() {
      Matrix4d result;

      if (geodeticViewport == null) {
         final Transform cameraBodyTransform = cameraBody.getTransform();
         final double currentRotation = cameraBodyTransform.getRotation();
         final Envelope currentOrtho = getCurrentOrtho();
         result = new Matrix4d().setOrtho(currentOrtho.getMinX(), currentOrtho.getMaxX(),
               currentOrtho.getMinY(), currentOrtho.getMaxY(), 0.1, 1000.0).translate(0.0, 0.0, -1.0);
         final Vector3d currentOrthoCenter = getCurrentOrthoCenter();

         result.translate(currentOrthoCenter.x, currentOrthoCenter.y, 0.0);
         result.rotate(currentRotation, new Vector3d(0.0, 0.0, 1.0));
         result.translate(-currentOrthoCenter.x, -currentOrthoCenter.y, 0.0);
      } else {
         result = targetTransform;
      }

      return result;
   }


   /**
    * @return Empty Matrix4d
    */
   public Matrix4d getCurrentModel() {
      return new Matrix4d();
   }

   public Envelope getCurrentViewport() {
      return currentViewport;
   }

   /**
    * @return Center of viewport
    */
   public Vector2d getCurrentViewportCenter() {
      return GeometryUtils.toVector2d(currentViewport.centre());
   }

   /**
    * @return visible OpenGLImage, i.e. intersection of openGLviewport and top tiles opengl bounds
    */
   public Polygon getOpenGLImageVisible() {
      final Polygon openGLImageVisible = (Polygon) getOpenGLViewport().intersection(topTile.getOpenGLSpaceBounds());
      return openGLImageVisible;
   }

   /**
    * @return OpenGLViewport as Polygon
    */
   public Polygon getOpenGLViewport() {
      final Coordinate[] viewportCoords = Arrays
            .stream(new double[][] {{currentViewport.getMinX(), currentViewport.getMaxY()},
               {currentViewport.getMaxX(), currentViewport.getMaxY()},
               {currentViewport.getMaxX(), currentViewport.getMinY()},
               {currentViewport.getMinX(), currentViewport.getMinY()},
               {currentViewport.getMinX(), currentViewport.getMaxY()},})
            .map(a -> new ScreenELTCoordinate(this, new Vector2d(a[0], a[1]), imageScale))
            .map(s -> s.getOpenGLCoordinate()).map(v -> new Coordinate(v.x, v.y, 0.0))
            .toArray(Coordinate[]::new);

      Polygon openGLViewport = null;
      try {
         openGLViewport = topTile.getImage().getGeometryFactory().createPolygon(viewportCoords);
      } catch (IllegalArgumentException e) {
         e.printStackTrace();
         throw e;
      }
      return openGLViewport;
   }

   /**
    * @return rotation of camera body, in radians [-pi, pi]
    */
   public double getRotation() {
      return cameraBody.getTransform().getRotation();
   }

   public TileRef getTopTile() {
      return topTile;
   }
   
   /**
    * Sets a new top tile and reshapes viewport accordingly
    * @param topTile
    */
   public void setTopTile(TileRef topTile) {
      cameraBody.setTransform(new Transform());
      this.imageScale.setTo(ImageScale.forRset(0));
      this.topTile = topTile;
      setRotate(0.0);
      imageScale.setRset(topTile.getRset());

      Envelope newViewport = new Envelope();
      if (currentViewport == null) {
         newViewport = new Envelope(0.0, DEFAULT_WIDTH, 0.0, DEFAULT_HEIGHT);
      } else {
         newViewport = currentViewport;
      }

      reshapeViewport((int)newViewport.getMinX(), (int)newViewport.getMinY(), (int)newViewport.getWidth(), (int)newViewport.getHeight());

      final Vector3d orthoCenter = getCurrentOrthoCenter();
      cameraBody.translate(orthoCenter.x, orthoCenter.y);
   }
   
   /**
    * @return percentage of viewport area visible
    */
   public double getPercentVisible() {
      final Polygon openGLImageVisible = getOpenGLImageVisible();
      final Polygon openGLViewport = getOpenGLViewport();
      return openGLImageVisible.getArea() / openGLViewport.getArea();
   }

   /**
    * Rotate the camera by specified amount
    * @param rotateBy rotateBy in Radians
    */
   public void rotateBy(double rotateBy) {
      cameraBody.rotateAboutCenter(rotateBy);
   }

   /**
    * Set viewport rotation
    * @param viewportRotate amount to rotate by in radians 
    */
   public void setRotate(double viewportRotate) {
      cameraBody.getTransform().setRotation(viewportRotate);
   }

   /**
    * @return rotation of top tile's bounds measured from north
    */
   public double getAngleOffNorth() {
      final Polygon imageGeoBounds = topTile.getGeodeticBounds();
      final Coordinate[] imageGeoBoundsCoords = imageGeoBounds.getCoordinates();

      Vector2d[] imageGeoBoundsVecs = Arrays.stream(imageGeoBoundsCoords).map(c->new Vector2d(c.x, c.y)).toArray(Vector2d[]::new);
      final Vector2d imageLLGeoVec = imageGeoBoundsVecs[0];
      final Vector2d imageULGeoVec = imageGeoBoundsVecs[3];
      final Vector2d northVec = new Vector2d(0.0, 90.0);
      final double angleOffNorth = northVec.angle(imageLLGeoVec.sub(imageULGeoVec));
      return angleOffNorth;
   }
   
   /**
    * Rotate viewport to align with north
    */
   public void rotateToNorth() {
      final double angleOffNorth = getAngleOffNorth();
      setRotate(-angleOffNorth);
   }
   
   /**
    * Update ImageWorld upon zooming to a coordinate
    * @param zoomTo
    * @param imageScale
    */
   public void zoomTo(ELTCoordinate<?> zoomTo, ImageScale imageScale) {
      this.imageScale.setTo(imageScale);
      final Vector3d openGLCoordinate = zoomTo.getOpenGLCoordinate();
      cameraBody.translateToOrigin();
      cameraBody.translate(openGLCoordinate.x, openGLCoordinate.y);
   }

   /**
    * zoomTo on a UL image coordinate
    * @param zoomTo
    * @param imageScale
    */
   public void zoomToUL(ELTCoordinate<?> zoomTo, ImageScale imageScale) {
      setImageScale(imageScale);
      final Envelope openGLViewport = getOpenGLViewport().getEnvelopeInternal();
      final double width = openGLViewport.getWidth();
      final double height = openGLViewport.getHeight();
      final Vector3d centerOpenGLCoordinate = zoomTo.getOpenGLCoordinate();
      final Vector3d upperLeftOpenGLCoordinate = centerOpenGLCoordinate.add(width/2.0, -(height/2.0),  0.0);
      final OpenGLELTCoordinate newCoord = new OpenGLELTCoordinate(this, upperLeftOpenGLCoordinate, zoomTo.getImageScale());
      zoomTo(newCoord, imageScale);
   }

   /**
    * set ImageScale
    * @param imageScale
    */
   public void setImageScale(ImageScale imageScale) {
      this.imageScale.setTo(imageScale);
   }

   /**
    * @return rset of image
    */
   public int getImageRset() {
      return imageScale.getImageRset(topTile.getRset());
   }

   boolean first = true;

   /**
    * Reshape viewport to new dimensions
    * @param x
    * @param y
    * @param width
    * @param height
    */
   public void reshapeViewport(int x, int y, int width, int height) {
      currentViewport = new Envelope(x, x + width, y, y + height);
   }

   /**
    * @return Viewport angle off north
    */
   public double getViewportAngleOffNorth() {
      final Envelope viewport = currentViewport;
      ScreenELTCoordinate lowerLeft = new ScreenELTCoordinate(this, new Vector2d(viewport.getMinX(), viewport.getMaxY()), imageScale);
      ScreenELTCoordinate upperLeft = new ScreenELTCoordinate(this, new Vector2d(viewport.getMinX(), viewport.getMinY()), imageScale);

      Coordinate lowerLeftGeo = lowerLeft.getGeodeticCoordinate();
      Coordinate upperLeftGeo = upperLeft.getGeodeticCoordinate();

      LOGGER.debug(String.format("LL = %s, UL = %s", lowerLeftGeo.toString(), upperLeftGeo.toString()));

      Vector2d leftSide = new Vector2d(upperLeftGeo.x - lowerLeftGeo.x, upperLeftGeo.y - lowerLeftGeo.y);

      return leftSide.angle(NORTH_VECTOR);
   }
   
   /**
    * @return Image angle off north
    */
   public double getImageAngleOffNorth() {
      final Rectangle imageBounds = this.topTile.getFullR0RectInImageSpace();
      ImageCoordinate lowerLeft = new ImageCoordinate(getCameraModel(), new Point((int)imageBounds.getMinX(), (int)imageBounds.getMaxY()), ImageScale.forRset(0));
      ImageCoordinate upperLeft = new ImageCoordinate(getCameraModel(), new Point((int)imageBounds.getMinX(), (int)imageBounds.getMinY()), ImageScale.forRset(0));

      Coordinate lowerLeftGeo = lowerLeft.getGeodeticCoordinate();
      Coordinate upperLeftGeo = upperLeft.getGeodeticCoordinate();

      Vector2d leftSide = new Vector2d(upperLeftGeo.x - lowerLeftGeo.x, upperLeftGeo.y - lowerLeftGeo.y);

      return leftSide.angle(NORTH_VECTOR);
   }

   /**
    * @return viewport in geodetic coordinates
    */
   public Polygon getGeodeticViewport() {
      final Envelope viewport = currentViewport;
      ScreenELTCoordinate lowerLeft = new ScreenELTCoordinate(this, new Vector2d(viewport.getMinX(), viewport.getMinY()), imageScale);
      ScreenELTCoordinate upperRight = new ScreenELTCoordinate(this, new Vector2d(viewport.getMaxX(), viewport.getMaxY()), imageScale);
      ScreenELTCoordinate lowerRight = new ScreenELTCoordinate(this, new Vector2d(viewport.getMaxX(), viewport.getMinY()), imageScale);
      ScreenELTCoordinate upperLeft = new ScreenELTCoordinate(this, new Vector2d(viewport.getMinX(), viewport.getMaxY()), imageScale);

      Coordinate[] geoCoords = Arrays.stream(new ScreenELTCoordinate[] { lowerLeft, lowerRight, upperRight, upperLeft, lowerLeft }).map(s -> s.getGeodeticCoordinate()).toArray(Coordinate[]::new);

      return topTile.getImage().getGeometryFactory().createPolygon(geoCoords);
   }

   /**
    * @return Dimensions of the geodetic viewport
    */
   public Vector2d getGeodeticViewportDimensions() {
      final Envelope viewport = currentViewport;
      ScreenELTCoordinate lowerLeft = new ScreenELTCoordinate(this, new Vector2d(viewport.getMinX(), viewport.getMinY()), imageScale);
      ScreenELTCoordinate upperRight = new ScreenELTCoordinate(this, new Vector2d(viewport.getMaxX(), viewport.getMaxY()), imageScale);

      Coordinate lowerLeftGeo = lowerLeft.getGeodeticCoordinate();
      Coordinate upperRightGeo = upperRight.getGeodeticCoordinate();

      return new Vector2d(Math.abs(upperRightGeo.x - lowerLeftGeo.x), Math.abs(upperRightGeo.y - lowerLeftGeo.y));
   }

   /**
    * @param viewportGeodeticCenter
    * @param oldImageScale old scale of image
    * @param oldGSD old ground sample distance
    * @return calculated ImageScale for specified dimension
    */
   public ImageScale getImageScaleForGeoDimensions(Coordinate viewportGeodeticCenter, ImageScale oldImageScale, double oldGSD) {
      final CameraModel cm = topTile.getImage().getCameraModel();

      GeodeticCoordinate geoCenterCoord = new GeodeticCoordinate(getCameraModel(), viewportGeodeticCenter, oldImageScale);
      Point2D viewportImageCenter = geoCenterCoord.getR0ImageCoordinate();
      final double newGSD = cm.getGSD(viewportImageCenter);

      final ImageScale newImageScale = new ImageScale(oldImageScale).mul(newGSD / oldGSD);

      if (LOGGER.isDebugEnabled()) {
         LOGGER.debug(String.format("OldImageScale == %s NewImageScale == %s", oldImageScale.toString(), newImageScale.toString()));
      }

      return newImageScale;
   }

   public Body getCameraBody() {
      return cameraBody;
   }

   /**
    * Update viewport model
    */
   public void update() {
      long now = Instant.now().toEpochMilli();
      if (lastTimeMS > 0) {
         long diffTimeMS = now - lastTimeMS;
         double diffTimeS = ((double)diffTimeMS)/1000.0;
         world.update(diffTimeS);
      }
      lastTimeMS = now;
   }
   public ImageScale getImageScale() {
      return imageScale;
   }

}
