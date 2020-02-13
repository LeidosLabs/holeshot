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

package com.leidoslabs.holeshot.elt;

import static org.lwjgl.opengl.GL20.glUseProgram;
import static org.lwjgl.opengl.GL30.GL_DRAW_FRAMEBUFFER;
import static org.lwjgl.opengl.GL30.GL_READ_FRAMEBUFFER;
import static org.lwjgl.opengl.GL30.glBindFramebuffer;

import java.awt.Component;
import java.awt.Toolkit;
import java.awt.geom.Point2D;
import java.io.Closeable;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.SystemUtils;
import org.apache.commons.math3.analysis.polynomials.PolynomialFunction;
import org.apache.commons.math3.fitting.PolynomialCurveFitter;
import org.apache.commons.math3.fitting.WeightedObservedPoint;
import org.dyn4j.geometry.Vector2;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.ControlListener;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.MouseMoveListener;
import org.eclipse.swt.events.MouseWheelListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.image.common.util.CloseableUtils;
import org.joml.Vector2d;
import org.joml.Vector3d;
import org.locationtech.jts.geom.Coordinate;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.swt.GLCanvas;
import org.lwjgl.opengl.swt.GLData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableSet;
import com.leidoslabs.holeshot.elt.coord.ELTCoordinate;
import com.leidoslabs.holeshot.elt.coord.GeodeticELTCoordinate;
import com.leidoslabs.holeshot.elt.coord.ImageWorld;
import com.leidoslabs.holeshot.elt.coord.ImageWorldMouseListener;
import com.leidoslabs.holeshot.elt.coord.OpenGLELTCoordinate;
import com.leidoslabs.holeshot.elt.coord.ScreenELTCoordinate;
import com.leidoslabs.holeshot.elt.drawing.PlacemarkRenderer;
import com.leidoslabs.holeshot.elt.drawing.PolygonRenderer;
import com.leidoslabs.holeshot.elt.gpuimage.HistogramType;
import com.leidoslabs.holeshot.elt.imagechain.ImageChain;
import com.leidoslabs.holeshot.elt.imagechain.ImageChain.ImageChainBuilder;
import com.leidoslabs.holeshot.elt.imageop.CumulativeHistogram;
import com.leidoslabs.holeshot.elt.imageop.DRAParameters;
import com.leidoslabs.holeshot.elt.imageop.Histogram;
import com.leidoslabs.holeshot.elt.imageop.ImageOpFactory;
import com.leidoslabs.holeshot.elt.imageop.SummedArea;
import com.leidoslabs.holeshot.elt.imageop.ToneTransferCurve;
import com.leidoslabs.holeshot.elt.imageop.ogl.OglImageOpFactory;
import com.leidoslabs.holeshot.elt.layers.BaseLayer;
import com.leidoslabs.holeshot.elt.layers.DataLayer;
import com.leidoslabs.holeshot.elt.layers.Renderer;
import com.leidoslabs.holeshot.elt.observations.Observation;
import com.leidoslabs.holeshot.elt.observations.PointObservation;
import com.leidoslabs.holeshot.elt.observations.PolygonObservation;
import com.leidoslabs.holeshot.elt.tileserver.TileRef;
import com.leidoslabs.holeshot.elt.tileserver.TileserverImage;
import com.leidoslabs.holeshot.elt.utils.GeometryUtils;
import com.leidoslabs.holeshot.elt.websocket.ELTWebSocket;
import com.leidoslabs.holeshot.imaging.coord.ImageScale;
import com.leidoslabs.holeshot.imaging.photogrammetry.CameraModel;

/**
 * ELT (Electronic Light Table) Canvas allows for the loading of imagery from the HOLESHOT Tileserver. It handles the:
 * <ul>
 *    <li>Fetching and caching of imagery tiles from the server</li>
 *    <li>Mosaicing of image tiles within OpenGL canvas</li>
 *    <li>SIPS processing for histogram equalization of the imagery</li>
 *    <li>Roaming, Zooming, Rotation of imagery</li> 
 *    <li>The creation and visualization of KML vector layers over top of the imagery</li>
 * </ul>
 * 
 * 
 * Drop-in replacement for SWT's {@link org.eclipse.swt.opengl.GLCanvas} class.
 * <p>
 * It supports creating OpenGL 3.0 and 3.2 core/compatibility contexts as well as multisampled framebuffers.
 * 
 * @author robertsrg
 *
 */
@SuppressWarnings("serial")
public class ELTCanvas extends GLCanvas implements Closeable {
   private static final Logger LOGGER = LoggerFactory.getLogger(ELTCanvas.class);
   private static final float BRIGHTNESS_INCREMENT = 0.1f;
   private static final float CONTRAST_INCREMENT = 0.1f;
   private static final float GAMMA_INCREMENT = 0.1f;

   /**
    * The currently loaded image
    */
   private TileserverImage tileserverImage;
   
   /**
    * The location to set the center point of the viewport to
    */
   private ELTCoordinate<?> zoomTo;
   /**
    * The scale/zoom-level to set the viewport to
    */
   private final ImageScale zoomToScale;
   
   /**
    * The amount to rotate by on the next frame rendering
    */
   private double rotateBy;

   /**
    * The top tile in the currently displayed image
    */
   private TileRef topTile;
   
   /**
    * The physical model for the viewport (Defines extent, rotation, etc)
    */
   private ImageWorld imageWorld;
   
   /**
    * A list of registered listeners for mouse events within the viewport
    */
   private List<ImageWorldMouseListener> mouseListeners;

   /**
    * The mouse position at the start of the last MouseDown event.
    */
   private Point oldMousePosition;
   
   /**
    * The mouse position at the start of the last MouseMove event.
    */
   private java.awt.Point mousePosition;
   
   /**
    * Flag indicating whether we're currently roaming
    */
   private boolean roaming;
   
   /**
    * Flag indicating whether we're currently rotating
    */
   private boolean rotating;
   
   /**
    * Flag indicating whether we're currently performing Dynamic Range Adjustment on the image
    */
   private boolean performingDRA;
   
   /**
    * Flag indicating whether the DRA portions of the image chain have been executed or not.
    */
   private boolean draUpdated;

   /**
    * List of ImageOps involved in DRA(Dynamic Range Adjustment) calculations
    */
   private static final Set<Class> ACTIVE_DRA_IMAGEOPS = ImmutableSet.of(Histogram.class, CumulativeHistogram.class, SummedArea.class, DRAParameters.class);
   
   
   private static final double MAX_ROTATE_RATE = 150.0;
   
   /**
    * Function that defines the speed at which rotation should occur based on the amount that the mouse has moved.
    */
   private PolynomialFunction rotateRateFunction;

   /**
    * Manages the layers (Raster and KML) in the canvas.
    */
   private LayerManager layerManager = new LayerManager(this);
   
   /**
    * A layer for the polygons being rendered in the canvas
    */
   private DataLayer polygonLayer;
   
   /**
    * A layer for the placemarks within the canva
    */
   private DataLayer placemarkLayer;

   /**
    * A flag indicating whether the canvas has been fully rendered with the highest resolution tiles required for the viewport.
    * When progressive rendering is on, lower resolution tiles will be used to fill in the gaps until the higher resolution tiles have been
    * fetched from the server.
    */
   private boolean fullyRendered;

   /**
    * The application id that was given when the ELT Canvas was constructed.
    */
   private String appId;
   
   /**
    * The display context for the ELT that ensures that the item being executed is done so in sync with SWT and OpenGL threads.
    */
   private final ELTDisplayContext eltDisplayContext;
   
   
   private static GLData getELTGLData() {
      GLData data = new GLData();
      data.profile = GLData.Profile.CORE;
      data.majorVersion = 4;
      data.minorVersion = 0;
      data.samples = 0; // 4; 4x multisampling
      data.swapInterval = null; // for enabling v-sync (swapbuffers sync'ed to monitor refresh)
      data.doubleBuffer = true;
      data.depthSize = (SystemUtils.IS_OS_WINDOWS) ? 32 : 24;

      return data;
   }

   /**
    * Add a mouse listener to the canvas
    * @param mouseListener ImageWorldMouseListener 
    */
   public void addELTMouseListener(ImageWorldMouseListener mouseListener) {
      mouseListeners.add(mouseListener);
   }

   private void notifyMouseListeners() {
      if (mousePosition != null) {
         ScreenELTCoordinate screenCoordinate = new ScreenELTCoordinate(imageWorld, GeometryUtils.toVector2d(mousePosition), imageWorld.getImageScale());
         mouseListeners.forEach(l->l.mouseMoved(screenCoordinate));
      }
   }

   private class ELTCanvasDisplayContext extends ELTDisplayContext {
      @Override
      public void asyncExec(Runnable runnable) {
         getDisplay().asyncExec(runnable);
      }
      @Override
      protected void setOpenGLContextCurrent() {
         setCurrent();
      }
      @Override
      public void syncExec(Runnable runnable) {
         getDisplay().syncExec(runnable);
      }
      @Override
      public synchronized boolean setContextThread() {
         return super.setContextThread() || (Thread.currentThread() != getDisplay().getThread());
      }
   }

   /**
    * Constructor. Set instance variables, initializes base layer, polygon, and placemark layer.
    * Adds closing, mouse, and keyboard listeners
    * @param composite Composite class capable of handling controls (e.g. ELTFrame)
    * @param imageMetadataURL metadataURL
    * @param appId
    * @param progressiveRender
    * @throws IOException
    * @throws InterruptedException
    * @throws ExecutionException
    */
   public ELTCanvas(Composite composite, URL imageMetadataURL, String appId, boolean progressiveRender) throws IOException, InterruptedException, ExecutionException {
      super(composite, SWT.DOUBLE_BUFFERED | SWT.NO_BACKGROUND, getELTGLData());

      this.eltDisplayContext = new ELTCanvasDisplayContext();
      this.tileserverImage = new TileserverImage(imageMetadataURL);
      this.fullyRendered = false;
      this.zoomToScale = ImageScale.forRset(0);
      this.draUpdated = false;

      setImage(tileserverImage, false);

      ImageOpFactory imageOpFactory = new OglImageOpFactory();

      ImageChain imageChain = new ImageChainBuilder(imageOpFactory, imageWorld, eltDisplayContext)
            .rawImage(progressiveRender)
            .histogram(HistogramType.RGB)
            .summedArea()
            .cumulativeHistogram()
            .draParameters(false)
            .dynamicRangeAdjust()
            .toneTransferCurve()
            .build();

      polygonLayer =  new DataLayer(new PolygonRenderer(imageWorld, eltDisplayContext));
      placemarkLayer = new DataLayer(new PlacemarkRenderer(imageWorld, eltDisplayContext));

      layerManager.setBaseLayer(new BaseLayer(imageChain, tileserverImage));
      layerManager.addDataLayer(polygonLayer);
      layerManager.addDataLayer(placemarkLayer);

      composite.addListener(SWT.Close, new Listener() {
         @Override
         public void handleEvent(Event event) {
            try {
               CloseableUtils.close(ELTCanvas.this);
            } catch (IOException e) {
               LOGGER.error(e.getMessage(), e);
            }
         }
      });

      this.setCurrent();
      GL.createCapabilities();

      this.mouseListeners = new ArrayList<ImageWorldMouseListener>();
      this.appId = appId;

      roaming = false;
      rotating = false;
      performingDRA = true;
      createRotateRateFunction();

      this.addControlListener(new ELTFrameGLEventListener());

      GLCanvasMouseListener mouseListener = new GLCanvasMouseListener();
      this.addMouseListener(mouseListener);
      this.addMouseMoveListener(mouseListener);
      this.addMouseWheelListener(mouseListener);

      GLCanvasKeyListener keyListener = new GLCanvasKeyListener();

      this.addKeyListener(keyListener);

   }

   public String getAppId() { return appId; }

   /**
    * Update ImageWorld based on current action (roaming, rotate, zoom). Perform DRA if needed
    * @param interpolation unused
    */
   public void updateAnimation(double interpolation) {
      imageWorld.getCameraBody().setLinearVelocity(0.0, 0.0);

      final boolean zooming = (zoomTo != null);
      handleZoomTo();

      if (roaming || rotating) {
         ScreenELTCoordinate newPoint = new ScreenELTCoordinate(imageWorld, GeometryUtils.toVector2d(mousePosition), imageWorld.getImageScale());
         Vector3d newPointOpenGL = newPoint.getOpenGLCoordinate();

         ScreenELTCoordinate oldPoint = new ScreenELTCoordinate(imageWorld, new Vector2d(oldMousePosition.x, oldMousePosition.y), imageWorld.getImageScale().copy());
         Vector3d oldPointOpenGL = oldPoint.getOpenGLCoordinate();

         if (roaming) {
            Vector2d screenDelta = newPoint.getScreenCoordinate().sub(oldPoint.getScreenCoordinate());
            Vector3d screenDelta3d = new Vector3d(screenDelta.x, screenDelta.y, 0.0);
            
            Vector3d openGLDelta = newPointOpenGL.sub(oldPointOpenGL);
            
            if (openGLDelta.length() > 0.0) {
               double nudgeForce = 0.01 * Math.pow(0.5, topTile.getRset() - imageWorld.getImageScale().getRset()) * screenDelta3d.length();
               Vector3d nudgeVec3d = openGLDelta.normalize().mul(nudgeForce);
               Vector2 nudgeVec = new Vector2(nudgeVec3d.x, nudgeVec3d.y);

               imageWorld.getCameraBody().applyImpulse(nudgeVec);
            }
         }
         if (rotating) {
            Vector2d viewportCenter = imageWorld.getCurrentViewportCenter();
            double screenFlipX = ((newPoint.getScreenCoordinate().x < oldPoint.getScreenCoordinate().x) ? -1.0  : 1.0) *
                  ((oldPoint.getScreenCoordinate().y > viewportCenter.y) ? 1.0 : -1.0);
            double screenFlipY = ((newPoint.getScreenCoordinate().y < oldPoint.getScreenCoordinate().y) ? -1.0  : 1.0) *
                  ((oldPoint.getScreenCoordinate().x > viewportCenter.x) ? -1.0 : 1.0);

            Vector2 delta = new Vector2(Math.copySign(newPoint.getScreenCoordinate().x - oldPoint.getScreenCoordinate().x, screenFlipX),
                  Math.copySign(newPoint.getScreenCoordinate().y - oldPoint.getScreenCoordinate().y, screenFlipY));

            rotateBy = rotateRate(Math.abs(delta.x) > Math.abs(delta.y) ? delta.x : delta.y);
         }
      }

      if (roaming || rotating || zooming) {
         draUpdated = false;
         getParent().getShell().update();

         if (rotateBy != 0.0) {
            imageWorld.rotateBy(rotateBy);
         }

         if (performingDRA) {
            disableDRA();
         }
         notifyMouseListeners();
      } else if (!draUpdated) {
         final ImageChain imageChain = getImageChain();
         draUpdated = (imageChain == null) || imageChain.isFullyRendered();
         if (performingDRA) {
            enableDRA();
         }
      }
      pushWebSocketEvents();
   }
   
   private void enableDRA() {
      enableDRA(true);
   }
   private void disableDRA() {
      enableDRA(false);
   }
   
   private void enableDRA(boolean enabled) {
      final ImageChain imageChain = getImageChain();
      if (imageChain != null) {
         imageChain.getImageOps().stream()
         .filter(i->ACTIVE_DRA_IMAGEOPS.stream().anyMatch(ai->ai.isInstance(i)))
         .forEach(i->i.setRenderingEnabled(enabled));
      }
   }
   
   /**
    * Update ImageWorld based on current action (roaming, rotate, zoom). Perform DRA if needed
    */
   public void updateAnimation() {
      updateAnimation(1.0);
   }

   /**
    * Render loop. Update ImageWorld and draws, installs opengl program
    */
   public synchronized void renderLoop() {
      try
      {
         imageWorld.update();
         this.setCurrent();
         layerManager.drawAll(imageWorld);
         fullyRendered = layerManager.isFullyRendered();

      } catch (Exception e) {
         e.printStackTrace();
      } finally {
         glUseProgram(0);

         glBindFramebuffer(GL_READ_FRAMEBUFFER, 0);

         glBindFramebuffer(GL_DRAW_FRAMEBUFFER, 0);
      }
   }

   private void handleZoomTo() {
      if (zoomTo != null) {
         imageWorld.zoomTo(zoomTo, zoomToScale);
         zoomTo = null;
      }
   }

   /**
    * Given metadata url, set a new Tileserverimage. Resets ImageWorld, ImageChain, etc
    * @param imageMetadataURL
    * @param resetViewport
    * @throws IOException
    * @throws InterruptedException
    * @throws ExecutionException
    */
   public void setImage(URL imageMetadataURL, boolean resetViewport) throws IOException, InterruptedException, ExecutionException {
      setImage(new TileserverImage(imageMetadataURL), resetViewport);
   }

   private void resetImageChain() {
      final ImageChain imageChain = getImageChain();
      if (imageChain != null) {
         imageChain.reset();
      }

   }
   
   /**
    *  Given metadata url, set a new Tileserverimage. Resets ImageWorld, ImageChain, etc
    * @param image new TileServerImage
    * @param resetViewport
    * @throws IOException
    * @throws InterruptedException
    * @throws ExecutionException
    */
   public void setImage(TileserverImage image, boolean resetViewport) throws IOException, InterruptedException, ExecutionException {

      final boolean retainGeos = (imageWorld != null) && !resetViewport;

      eltDisplayContext.syncExec(() -> resetImageChain());

      Coordinate viewportGeodeticCenter = null;
      double oldAngleOffNorth = 0.0;
      double oldImageRotate = 0.0;
      double oldGSD = 0.0;
      final ImageScale oldImageScale = this.zoomToScale.copy();
      if (retainGeos) {
         oldImageRotate = imageWorld.getRotation();

         final OpenGLELTCoordinate viewportOpenGLCenter = new OpenGLELTCoordinate(imageWorld, imageWorld.getCurrentOrthoCenter(), ImageScale.forRset(0));
         final Point2D viewportImageCenter = viewportOpenGLCenter.getR0ImageCoordinate();
         viewportGeodeticCenter = viewportOpenGLCenter.getGeodeticCoordinate();
         oldAngleOffNorth = imageWorld.getImageAngleOffNorth();
         final CameraModel oldCameraModel = imageWorld.getTopTile().getImage().getCameraModel();
         oldGSD = oldCameraModel.getGSD(viewportImageCenter);
      }

      tileserverImage = image;
      this.topTile = tileserverImage.getTopTile();

      zoomToScale.setRset(this.topTile.getRset());
      zoomTo = null;
      rotateBy = 0.0;

      if(imageWorld == null) {
         imageWorld = new ImageWorld(topTile);
      }
      else {
         imageWorld.setTopTile(topTile);
      }

      if (retainGeos) {
         final double newAngleOffNorth = imageWorld.getImageAngleOffNorth();
         final double viewportRotate = oldImageRotate + (newAngleOffNorth - oldAngleOffNorth);
         zoomToScale.setTo(imageWorld.getImageScaleForGeoDimensions(viewportGeodeticCenter, oldImageScale, oldGSD));
         zoomTo = new GeodeticELTCoordinate(imageWorld, viewportGeodeticCenter, zoomToScale);
         imageWorld.setRotate(viewportRotate);
      } else {
         this.imageWorld.getCameraBody().translateToOrigin();
      }

      if(layerManager.getBaseLayer() != null) {
         layerManager.getBaseLayer().setImage(tileserverImage);
      }
   }

   /**
    * @return Canvas's current TileServerImage
    */
   public TileserverImage getImage() {
      return tileserverImage;
   }

   public ImageWorld getImageWorld() {
      return imageWorld;
   }
   private void createRotateRateFunction() {
      final PolynomialCurveFitter fitter = PolynomialCurveFitter.create(3);
      List<WeightedObservedPoint> rotatePoints = Arrays
            .stream(new double[][] {{0.0, 0.0}, {5, 0.05}, {10, 0.1}, {15, 0.25}, {25, 0.5},
               {50, 1.0}, {75, 1.25}, {90, 2.5}, {100, 5},})
            .flatMap(p -> Stream.of(new double[][] {{p[0], p[1]}, {-p[0], -p[1]}}))
            .sorted((p1, p2) -> Double.compare(p1[0], p2[0]))
            .map(p -> new WeightedObservedPoint(1.0, p[0], p[1])).collect(Collectors.toList());

      rotateRateFunction = new PolynomialFunction(fitter.fit(rotatePoints));
   }

   private double rotateRate(double delta) {
      return Math
            .toRadians(rotateRateFunction.value(constrain(delta, -MAX_ROTATE_RATE, MAX_ROTATE_RATE)));
   }


   private class ELTFrameGLEventListener implements ControlListener {
      @Override
      public void controlMoved(ControlEvent e) {
      }
      @Override
      public void controlResized(ControlEvent e) {
         //RGR - This will disable VSYNC, allowing frame rates above your monitor's capabilities (e.g. If your monitor is running
         // at 60Hz, it will max out at 60 FPS, unless this is set)
         // glDrawable.getGL().setSwapInterval(0);

         zoomTo = new OpenGLELTCoordinate(imageWorld, imageWorld.getCurrentOrthoCenter(), imageWorld.getImageScale());

         Point size = ELTCanvas.this.getSize();
         imageWorld.reshapeViewport(0, 0, size.x, size.y);
      }
   }

  /**
   * @param frame center frame window
   */
   public void centerWindow(Component frame) {
      java.awt.Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
      java.awt.Dimension frameSize = frame.getSize();

      if (frameSize.width > screenSize.width)
         frameSize.width = screenSize.width;
      if (frameSize.height > screenSize.height)
         frameSize.height = screenSize.height;

      frame.setLocation((screenSize.width - frameSize.width) >> 1,
            (screenSize.height - frameSize.height) >> 1);
   }

   private double constrain(double value, double min, double max) {
      return Math.max(min, Math.min(value, max));
   }

   private class GLCanvasKeyListener implements KeyListener {
      @Override
      public void keyPressed(KeyEvent ke) {
         //Noop for now
      }

      @Override
      public void keyReleased(KeyEvent ke) {
         if (ke.keyCode == SWT.F4) {
            performingDRA = !performingDRA;
            enableDRA(performingDRA);
            
            LOGGER.debug(String.format("performindDRA set to %b", performingDRA));
         } else {
            ToneTransferCurve ttc = getToneTransferCurve();
            switch (ke.character) {
            case 'b':
               ttc.adjustBrightness(-BRIGHTNESS_INCREMENT);
               break;
            case 'B':
               ttc.adjustBrightness(BRIGHTNESS_INCREMENT);
               break;
            case 'c':
               ttc.adjustContrast(-CONTRAST_INCREMENT);
               break;
            case 'C':
               ttc.adjustContrast(CONTRAST_INCREMENT);
               break;
            case 'g':
               ttc.adjustGamma(GAMMA_INCREMENT);
               break;
            case 'G':
               ttc.adjustGamma(-GAMMA_INCREMENT);
               break;
            case 'r':
               ttc.resetManualAdjustments();
               break;
            }
         }
      }
   }

   /**
    * Canvas MouseListener. 
    * Controls:
    * 	Scroll: Zoom on center
    * 	Double Left Click: Center and zoom on new point
    * 	Shift + Left Click: Draw Polygon
    * 	Mouse move: Update Lat/Lng display
    */
   private class GLCanvasMouseListener implements MouseMoveListener, MouseListener, MouseWheelListener {
      private static final int LEFT_BUTTON = 1;
      private static final int MIDDLE_BUTTON = 2;
      private static final int RIGHT_BUTTON = 3;

      public GLCanvasMouseListener() {
         super();
      }


      @Override
      public void mouseDown(MouseEvent e) {

         oldMousePosition = new Point(e.x, e.y);
         if (e.button == MIDDLE_BUTTON) {
            roaming = !roaming;
         } else if (e.button == RIGHT_BUTTON) {
            rotating = !rotating;
         }

         if (Arrays.asList(MIDDLE_BUTTON, RIGHT_BUTTON).contains(e.button)) {
            if (roaming || rotating) {
               oldMousePosition = new Point(e.x, e.y);
            } else {
               oldMousePosition = null;
               rotateBy = 0.0;
            }
         }
      }

      @Override
      public void mouseUp(MouseEvent e) {
         if (e.button == RIGHT_BUTTON) {
            rotating = false;
            rotateBy = 0.0;
         }

         Vector2d clickPoint = new Vector2d(e.x, e.y);
         if (e.button == LEFT_BUTTON) {
            if (oldMousePosition != null) {
               if (Math.sqrt(Math.pow(clickPoint.x - oldMousePosition.x, 2) + Math.pow(clickPoint.y - oldMousePosition.y, 2)) < 20) {
                  if ((e.stateMask & SWT.SHIFT) == SWT.SHIFT) {
                     LOGGER.debug("Making a point.");
                     PointObservation obs = new PointObservation(new ScreenELTCoordinate(imageWorld, clickPoint, imageWorld.getImageScale()).getGeodeticCoordinate());
                     placemarkLayer.addData(obs);
                     if (ELTWebSocket.isLinked(appId)) {
                        ELTWebSocket.sendPoint(appId, obs.getId(), obs.getCoordinates());
                     }
                  }
               } else {
                  if ((e.stateMask & SWT.SHIFT) == SWT.SHIFT) {
                     LOGGER.debug("Making a polygon.");
                     polygonLayer.addData(new PolygonObservation(
                           new Coordinate[]{
                                 new ScreenELTCoordinate(imageWorld, new Vector2d(oldMousePosition.x, oldMousePosition.y), imageWorld.getImageScale().copy()).getGeodeticCoordinate(),
                                 new ScreenELTCoordinate(imageWorld, new Vector2d(clickPoint.x, oldMousePosition.y), imageWorld.getImageScale().copy()).getGeodeticCoordinate(),
                                 new ScreenELTCoordinate(imageWorld, new Vector2d(clickPoint.x, clickPoint.y), imageWorld.getImageScale().copy()).getGeodeticCoordinate(),
                                 new ScreenELTCoordinate(imageWorld,new Vector2d(oldMousePosition.x, clickPoint.y), imageWorld.getImageScale().copy()).getGeodeticCoordinate()}));
                  }
               }
            }
         }
      }

      @Override
      public void mouseDoubleClick(MouseEvent e) {
         if (e.button == LEFT_BUTTON) {
            if((e.stateMask & (SWT.SHIFT | SWT.CONTROL)) == 0) {
               if (Math.max(zoomToScale.getScaleX(), zoomToScale.getScaleY()) <= 2.0) {
                  zoomToScale.mul(2.0);
               }
               final Vector3d openGLCoord = new ScreenELTCoordinate(imageWorld, new Vector2d(e.x, e.y), imageWorld.getImageScale()).getOpenGLCoordinate();
               zoomTo =
                     new OpenGLELTCoordinate(imageWorld, openGLCoord, imageWorld.getImageScale());
            }
         }
      }


      @Override
      public void mouseMove(MouseEvent e) {
         mousePosition = new java.awt.Point(e.x, e.y);
         notifyMouseListeners();
      }

      private static final int COUNT_PER_CLICK = 3;

      @Override
      public void mouseScrolled(MouseEvent e) {
         final double count = e.count / COUNT_PER_CLICK;
         final Vector3d openGLCoord = new ScreenELTCoordinate(imageWorld, imageWorld.getCurrentViewportCenter(), imageWorld.getImageScale()).getOpenGLCoordinate();
         zoomTo = new OpenGLELTCoordinate(imageWorld, openGLCoord, imageWorld.getImageScale());
         ImageScale maxScale = ImageScale.forRset(-2);
         ImageScale minScale = ImageScale.forRset(topTile.getRset() + 2.0);

         final double scaleIncrement = count < 0 ? 0.5 : 2.0;
         final ImageScale nextScale = zoomToScale.copy();
         // Iterate to retain aspect ratio within min and max scale
         for (int i=0;i<Math.abs(count);++i) {
            nextScale.mul(scaleIncrement);
            if (nextScale.within(minScale, maxScale)) {
               zoomToScale.setTo(nextScale);
            }
         }
      }
   }

   /**
    * @return Get top tile of current TileServerImage
    */
   public TileRef getTopTile() {
      return topTile;
   }

   private void pushWebSocketEvents() {
      if (ELTWebSocket.isLinked(appId)) {
         ELTWebSocket.sendOverlay(appId, imageWorld.getGeodeticViewport().getCoordinates());
      }
   }

   /**
    * Add observation data to placemark layer
    * @param observationsList
    * @param layerName Unused
    */
   public void setPlacemarks(List<PointObservation> observationsList, String layerName) {
      /*if(layerManager.getDataLayer(layerName) == null) {
         final ImageWorld imageWorld = getImageWorld();

         layerManager.addDataLayer(layerName, new DataLayer(new PlacemarkRenderer(imageWorld, eltDisplayContext), true));
      }*/
      placemarkLayer.setData(observationsList);
   }

   /**
    * Remove data later from layer manager
    * @param layerName data layer name
    */
   public void removeLayer(String layerName) {
      layerManager.removeDataLayer(layerName);
   }

   public void unselectAll() { layerManager.unselectAll(); }

   public void highlightObservation(String companionID, String layerName) {
      /*if(layerManager.getDataLayer(layerName) != null) {
          layerManager.select(companionID,layerName);
      }*/
      layerManager.select(companionID, placemarkLayer);
      //layerManager.select(companionID, layerName);
   }

   /**
    * Searches data layers for a observation with given companionID, and deletes it
    * @param companionID
    */
   public void deleteObservation(String companionID) {
	  AtomicReference<Stream<Observation>> obs = new AtomicReference(Stream.empty());
      Map<String, DataLayer> dataLayers = layerManager.getAllDataLayers();
      dataLayers.forEach((id, layer) -> obs.set(Stream.of(obs.get(), layer.getData().stream()).flatMap(s -> s)));
      //dataLayers.entrySet().stream().filter(entry -> entry.getValue().removeAll());
      Optional<Observation> deletedObservation = obs.get().filter(p -> p.getId().equalsIgnoreCase(companionID)).findFirst();
      if(deletedObservation.isPresent()) {
          dataLayers.entrySet().stream().filter(entry -> entry.getValue().remove(deletedObservation.get())).findFirst();
      }
   }

   /**
    * @return Locked check on fullyRendered
    */
   public synchronized boolean isFullyRendered() {
      return this.fullyRendered;
   }

   /**
    * @return performing DRA?
    */
   public boolean isPerformingDRA() {
      return performingDRA;
   }

   private ToneTransferCurve getToneTransferCurve() {
      return getImageChain().getLastImageOp(ToneTransferCurve.class);
   }
   private ImageChain getImageChain() {
      ImageChain imageChain = null;

      if (layerManager != null && layerManager.getBaseLayer() != null) {
         Renderer<TileserverImage> renderer = layerManager.getBaseLayer().getRenderer();
         if (renderer instanceof ImageChain) {
            imageChain = (ImageChain)renderer;
         }
      }
      return imageChain;
   }

   @Override
   public void close() throws IOException {
      CloseableUtils.close(layerManager);
   }
}
