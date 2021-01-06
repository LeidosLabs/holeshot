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

import static org.lwjgl.opengl.GL11.GL_TEXTURE_2D;
import static org.lwjgl.opengl.GL11.glDisable;
import static org.lwjgl.opengl.GL11.glEnable;
import static org.lwjgl.opengl.GL20.glUseProgram;
import static org.lwjgl.opengl.GL30.GL_DRAW_FRAMEBUFFER;
import static org.lwjgl.opengl.GL30.GL_READ_FRAMEBUFFER;
import static org.lwjgl.opengl.GL30.glBindFramebuffer;

import java.awt.Component;
import java.awt.Toolkit;
import java.io.Closeable;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
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
import org.eclipse.swt.events.MouseTrackAdapter;
import org.eclipse.swt.events.MouseWheelListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.image.common.util.CloseableUtils;
import org.joml.Vector2d;
import org.joml.Vector2dc;
import org.joml.Vector2i;
import org.joml.Vector2ic;
import org.joml.Vector3d;
import org.locationtech.jts.geom.Coordinate;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GLUtil;
import org.lwjgl.opengl.swt.GLCanvas;
import org.lwjgl.opengl.swt.GLData;
import org.opengis.util.FactoryException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.primitives.Doubles;
import com.leidoslabs.holeshot.elt.basemap.osm.OsmBasemap;
import com.leidoslabs.holeshot.elt.coord.Angle;
import com.leidoslabs.holeshot.elt.coord.ELTCoordinate;
import com.leidoslabs.holeshot.elt.coord.GeodeticELTCoordinate;
import com.leidoslabs.holeshot.elt.coord.MapScale;
import com.leidoslabs.holeshot.elt.coord.Radians;
import com.leidoslabs.holeshot.elt.coord.ScreenELTCoordinate;
import com.leidoslabs.holeshot.elt.drawing.PlacemarkRenderer;
import com.leidoslabs.holeshot.elt.drawing.PolygonRenderer;
import com.leidoslabs.holeshot.elt.imageop.ImageOpFactory;
import com.leidoslabs.holeshot.elt.imageop.ogl.OglImageOpFactory;
import com.leidoslabs.holeshot.elt.layers.BaseLayer;
import com.leidoslabs.holeshot.elt.layers.DataLayer;
import com.leidoslabs.holeshot.elt.observations.Observation;
import com.leidoslabs.holeshot.elt.observations.PointObservation;
import com.leidoslabs.holeshot.elt.observations.PolygonObservation;
import com.leidoslabs.holeshot.elt.tileserver.TileRef;
import com.leidoslabs.holeshot.elt.tileserver.TileserverImage;
import com.leidoslabs.holeshot.elt.utils.GeometryUtils;
import com.leidoslabs.holeshot.elt.viewport.ImageProjection;
import com.leidoslabs.holeshot.elt.viewport.ImageWorld;
import com.leidoslabs.holeshot.elt.viewport.ImageWorldMouseListener;
import com.leidoslabs.holeshot.elt.viewport.ViewportImageListener;
import com.leidoslabs.holeshot.elt.viewport.WGS84Projection;
import com.leidoslabs.holeshot.elt.viewport.WebMercatorProjection;
import com.leidoslabs.holeshot.elt.websocket.ELTWebSocket;

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
	 * The amount to rotate by on the next frame rendering
	 */
	private Angle rotateBy;

	/**
	 * The top tile in the currently displayed image
	 */
	private TileRef topTile;

	/**
	 * The physical model for the viewport (Defines extent, rotation, etc)
	 */
	private final ImageWorld imageWorld;

	/**
	 * A list of registered listeners for mouse events within the viewport
	 */
	private final List<ImageWorldMouseListener> mouseListeners;

	private final List<Consumer<String>> nameListeners;

	/**
	 * The mouse position at the start of the last MouseDown event.
	 */
	private Point oldMousePosition;

	/**
	 * The mouse position at the start of the last MouseMove event.
	 */
	private Point mousePosition;

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

	private final boolean isProgressiveRender;

	private ImageOpFactory imageOpFactory;

	private static GLData getELTGLData() {
		GLData data = new GLData();
		data.profile = GLData.Profile.CORE;
		data.majorVersion = 4;
		data.minorVersion = 0;
		data.samples = 0; // 4; 4x multisampling
		data.doubleBuffer = true;
		data.depthSize = (SystemUtils.IS_OS_WINDOWS) ? 32 : 24;
		data.stencilSize = 8;
		data.swapInterval = 0;

		return data;
	}

	public void addViewportImageListener(ViewportImageListener imageListener) {
		layerManager.getBaseLayer().addViewportImageListener(imageListener);
	}

	/**
	 * Add a mouse listener to the canvas
	 * @param mouseListener ImageWorldMouseListener 
	 */
	public void addELTMouseListener(ImageWorldMouseListener mouseListener) {
		mouseListeners.add(mouseListener);
	}

	public void addNameListener(Consumer<String> nameListener) {
		nameListeners.add(nameListener);
	}

	public ELTDisplayContext getELTDisplayContext() {
		return eltDisplayContext;
	}

	private void notifyMouseListeners() {
		if (mousePosition != null) {
			ScreenELTCoordinate screenCoordinate = new ScreenELTCoordinate(imageWorld, toVector2i(mousePosition));
			mouseListeners.forEach(l->l.mouseMoved(screenCoordinate));
		}
	}
	private void notifyNameListeners() {
		nameListeners.forEach(c->c.accept(this.getDisplayName()));
	}

	private class ELTCanvasDisplayContext extends ELTDisplayContext {
		public ELTCanvasDisplayContext( ) throws InterruptedException, ExecutionException {
			super(handle);
		}

		@Override
		public void asyncExec(Runnable runnable) {
			getDisplay().asyncExec(() -> {
				if (!ELTCanvas.this.isDisposed()) {
					setOpenGLContextCurrent();
					runnable.run();
				}
			});
		}
		@Override
		protected void setOpenGLContextCurrent() {
			setCurrent();
		}
		@Override
		public void syncExec(Runnable runnable) {
			getDisplay().syncExec(() -> {
				if (!ELTCanvas.this.isDisposed()) {
					setOpenGLContextCurrent();
					runnable.run();
				}
			});
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
	public ELTCanvas(Composite composite, String appId, boolean progressiveRender) throws IOException, InterruptedException, ExecutionException {
		super(composite, SWT.DOUBLE_BUFFERED | SWT.NO_BACKGROUND, getELTGLData());

		this.imageOpFactory = new OglImageOpFactory();

		this.imageWorld = new ImageWorld(toVector2i(this.getSize()));
		this.isProgressiveRender = progressiveRender;
		this.fullyRendered = false;
		this.rotateBy = new Radians(0.0);
		this.draUpdated = false;

		this.setCurrent();
		GL.createCapabilities();

		// To add OpenGL Debug statements.
		//		GLUtil.setupDebugMessageCallback(System.out);

		this.eltDisplayContext = new ELTCanvasDisplayContext();

		polygonLayer =  new DataLayer(new PolygonRenderer(imageWorld, eltDisplayContext));
		placemarkLayer = new DataLayer(new PlacemarkRenderer(imageWorld, eltDisplayContext));

		layerManager.setBasemap(new OsmBasemap(imageWorld, eltDisplayContext));
		layerManager.setBaseLayer(new BaseLayer(this));
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

		this.mouseListeners = new ArrayList<ImageWorldMouseListener>();
		this.nameListeners = new ArrayList<Consumer<String>>();
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

		// Give the focus to the Canvas so that it's key listener can work.
		this.addMouseTrackListener(new MouseTrackAdapter() {
			@Override
			public void mouseEnter(MouseEvent e) {
				ELTCanvas.this.setFocus();
			}
		});

	}

	public String getAppId() { return appId; }

	public boolean isProgressiveRender() {
		return isProgressiveRender;
	}

	public LayerManager getLayerManager() {
		return this.layerManager;
	}

	/**
	 * Update ImageWorld based on current action (roaming, rotate, zoom). Perform DRA if needed
	 * @param interpolation 
	 */
	public void updateAnimation(double interpolation) {
		if (roaming || rotating) {
			ScreenELTCoordinate newPoint = new ScreenELTCoordinate(imageWorld, toVector2i(mousePosition));
			Vector2dc newPointProjected = GeometryUtils.toVector2d(newPoint.getProjectedCoordinate());

			ScreenELTCoordinate oldPoint = new ScreenELTCoordinate(imageWorld, new Vector2i(oldMousePosition.x, oldMousePosition.y));
			Vector2dc oldPointProjected = GeometryUtils.toVector2d(oldPoint.getProjectedCoordinate());

			if (roaming) {
				Vector2dc projectedDelta = newPointProjected.sub(oldPointProjected, new Vector2d());

				if (projectedDelta.length() > 0.0) {
					double nudgeForce = projectedDelta.length() * 8.0;
					Vector2dc nudgeVec = projectedDelta.normalize(new Vector2d()).mul(nudgeForce);
					imageWorld.setLinearVelocity(new Vector3d(nudgeVec, 0.0));
				}
			}
			if (rotating) {
				Vector2ic viewportCenter = imageWorld.getViewportCenter();
				double projectionFlipX = imageWorld.getProjection().getXAxisDirection();
				double projectionFlipY = imageWorld.getProjection().getYAxisDirection();

				double screenFlipX = -((newPoint.getScreenCoordinate().x() < oldPoint.getScreenCoordinate().x()) ? -projectionFlipX  : projectionFlipX) *
						((oldPoint.getScreenCoordinate().y() > viewportCenter.y()) ? projectionFlipY : -projectionFlipY);
				double screenFlipY = -((newPoint.getScreenCoordinate().y() < oldPoint.getScreenCoordinate().y()) ? -projectionFlipY  : projectionFlipY) *
						((oldPoint.getScreenCoordinate().x() > viewportCenter.x()) ? -projectionFlipX : projectionFlipX);

				Vector2 delta = new Vector2(Math.copySign(newPoint.getScreenCoordinate().x() - oldPoint.getScreenCoordinate().x(), screenFlipX),
						Math.copySign(newPoint.getScreenCoordinate().y() - oldPoint.getScreenCoordinate().y(), screenFlipY));

				rotateBy = rotateRate(Math.abs(delta.x) > Math.abs(delta.y) ? delta.x : delta.y);
			}
		}

		if (roaming || rotating) {
			draUpdated = false;
			getParent().getShell().update();

			if (rotateBy.getRadians() != 0.0) {
				imageWorld.rotateBy(rotateBy);
			}

			if (performingDRA) {
				disableDRA();
			}
			notifyMouseListeners();
		} else if (!draUpdated) {
			draUpdated = this.layerManager.isFullyRendered();
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
		layerManager.getBaseLayer().enableDRA(enabled);
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
	public void renderLoop() {
		try
		{
			if (imageWorld.update()) {
				this.setCurrent();
				glEnable(GL_TEXTURE_2D);
				layerManager.drawAll(imageWorld);
				fullyRendered = layerManager.isFullyRendered();
			}

		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			glDisable(GL_TEXTURE_2D);

			glUseProgram(0);

			glBindFramebuffer(GL_READ_FRAMEBUFFER, 0);

			glBindFramebuffer(GL_DRAW_FRAMEBUFFER, 0);
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
	public void addImage(URL imageMetadataURL, boolean resetViewport) throws IOException, InterruptedException, ExecutionException {
		addImage(new TileserverImage(imageMetadataURL), resetViewport);
	}

	public void zoomToImage(TileserverImage image) {
		imageWorld.zoomToImage(image);
	}
	public void zoomToImage(TileserverImage image, ELTCoordinate<?> center, double rset) {
		imageWorld.zoomToImage(image, center, rset);
	}

	private void resetImageChain() {
		layerManager.getBaseLayer().resetImageChain();
	}

	/**
	 *  Given metadata url, set a new Tileserverimage. Resets ImageWorld, ImageChain, etc
	 * @param image new TileServerImage
	 * @param resetViewport
	 * @throws IOException
	 * @throws InterruptedException
	 * @throws ExecutionException
	 */
	public void addImage(TileserverImage image, boolean resetViewport) throws IOException, InterruptedException, ExecutionException {
		eltDisplayContext.syncExec(() -> resetImageChain());
		if (resetViewport) {
			final Coordinate newGeodeticCenter = GeometryUtils.toCoordinate(image.getGeodeticBounds().getCentroid());
			final ELTCoordinate<?> newCenter = new GeodeticELTCoordinate(imageWorld, newGeodeticCenter);
			imageWorld.zoomTo(newCenter, new MapScale(Math.floor(imageWorld.getMapScale(image, newGeodeticCenter, image.getMaxRLevel()).getZoom())));
			imageWorld.rotateTo(new Radians(0));
		}
		layerManager.getBaseLayer().addImage(image);

		notifyNameListeners();
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

	private Radians rotateRate(double delta) {
		return new Radians(Math
				.toRadians(rotateRateFunction.value(Doubles.constrainToRange(delta, -MAX_ROTATE_RATE, MAX_ROTATE_RATE))));
	}


	private class ELTFrameGLEventListener implements ControlListener {
		@Override
		public void controlMoved(ControlEvent e) {
		}
		@Override
		public void controlResized(ControlEvent e) {
			Point size = ELTCanvas.this.getSize();
			System.out.println("size == " + size.toString());
			if (size.x > 0 && size.y > 0) {
				imageWorld.setScreenSize(toVector2i(size));
			}

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
				final BaseLayer base = layerManager.getBaseLayer();
				switch (ke.character) {
				case 'b':
					base.setTTC(ttc->ttc.adjustBrightness(-BRIGHTNESS_INCREMENT));
					break;
				case 'B':
					base.setTTC(ttc->ttc.adjustBrightness(BRIGHTNESS_INCREMENT));
					break;
				case 'c':
					base.setTTC(ttc->ttc.adjustContrast(-CONTRAST_INCREMENT));
					break;
				case 'C':
					base.setTTC(ttc->ttc.adjustContrast(CONTRAST_INCREMENT));
					break;
				case 'g':
					base.setTTC(ttc->ttc.adjustGamma(GAMMA_INCREMENT));
					break;
				case 'G':
					base.setTTC(ttc->ttc.adjustGamma(-GAMMA_INCREMENT));
					break;
				case 'r':
					base.setTTC(ttc->ttc.resetManualAdjustments());
					break;
				case 'N':
				case 'n':
					base.getImageWorld().rotateTo(new Radians(0.0));
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
				if (!roaming) {
					imageWorld.clearLinearVelocity();
				}
			} else if (e.button == RIGHT_BUTTON) {
				rotating = !rotating;
			}

			if (Arrays.asList(MIDDLE_BUTTON, RIGHT_BUTTON).contains(e.button)) {
				if (roaming || rotating) {
					oldMousePosition = new Point(e.x, e.y);
				} else {
					oldMousePosition = null;
					rotateBy = new Radians(0.0);
				}
			}
		}

		@Override
		public void mouseUp(MouseEvent e) {
			if (e.button == RIGHT_BUTTON) {
				rotating = false;
				rotateBy = new Radians(0.0);
			} else if (e.button == LEFT_BUTTON) {
				if (oldMousePosition != null) {
					Vector2ic clickPoint = new Vector2i(e.x, e.y);
					if (Math.sqrt(Math.pow(clickPoint.x() - oldMousePosition.x, 2) + Math.pow(clickPoint.y() - oldMousePosition.y, 2)) < 20) {
						if ((e.stateMask & SWT.SHIFT) == SWT.SHIFT) {
							LOGGER.debug("Making a point.");
							PointObservation obs = new PointObservation(new ScreenELTCoordinate(imageWorld, clickPoint).getGeodeticCoordinate());
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
											new ScreenELTCoordinate(imageWorld, new Vector2i(oldMousePosition.x, oldMousePosition.y)).getGeodeticCoordinate(),
											new ScreenELTCoordinate(imageWorld, new Vector2i(clickPoint.x(), oldMousePosition.y)).getGeodeticCoordinate(),
											new ScreenELTCoordinate(imageWorld, new Vector2i(clickPoint.x(), clickPoint.y())).getGeodeticCoordinate(),
											new ScreenELTCoordinate(imageWorld,new Vector2i(oldMousePosition.x, clickPoint.y())).getGeodeticCoordinate()}));
						}
					}
				}
			} 
		}

		@Override
		public void mouseDoubleClick(MouseEvent e) {
			if (e.button == LEFT_BUTTON) {
				if((e.stateMask & (SWT.SHIFT | SWT.CONTROL)) == 0) {
					final ScreenELTCoordinate screenCoordinate = new ScreenELTCoordinate(imageWorld, new Vector2i(e.x, e.y));
					imageWorld.zoomTo(screenCoordinate, getNextMapScale(1));
				}
			}
		}


		@Override
		public void mouseMove(MouseEvent e) {
			mousePosition = new Point(e.x, e.y);
			notifyMouseListeners();
		}

		private static final int COUNT_PER_CLICK = 3;

		@Override
		public void mouseScrolled(MouseEvent e) {
			final int count = (int)Math.ceil((double)e.count / COUNT_PER_CLICK);
			imageWorld.scaleTo(getNextMapScale(count));
		}
	}


	private MapScale getNextMapScale(int increment) {
		MapScale nextScale;
		WGS84Projection proj = imageWorld.getProjection();
		if (proj instanceof ImageProjection) {
			ImageProjection imageProj = (ImageProjection)proj;
			TileserverImage image = imageProj.getImage();
			Coordinate geoCenter = imageWorld.getGeodeticCenter().getGeodeticCoordinate();
			double imageRset = imageWorld.getRset(image, geoCenter, imageWorld.getMapScale());
			double nextRset = Doubles.constrainToRange(Math.round(imageRset - increment), -2.0, (double)image.getTilePyramidDescriptor().getMaxRLevel()+4.0);
			nextScale = imageWorld.getMapScale(image, geoCenter, nextRset);
		} else {
			nextScale = new MapScale(imageWorld.getMapScale().getZoom() + increment);
		}

		return nextScale;

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
		layerManager.select(companionID, placemarkLayer);
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

	@Override
	public void close() throws IOException {
		CloseableUtils.close(layerManager, eltDisplayContext);
	}

	private static Vector2ic toVector2i(Point size) {
		return new Vector2i(size.x, size.y);
	}


	public ImageOpFactory getImageOpFactory() {
		return imageOpFactory;
	}

	/**
	 * @return
	 */
	public String getDisplayName() {
		final WGS84Projection projection = imageWorld.getProjection();

		String displayName;
		if (projection instanceof ImageProjection) {
			displayName = projection.getName();
		} else {
			final TileserverImage[] images = layerManager.getBaseLayer().getImages();
			displayName = String.format("%s - %d images", projection.getName(), images.length);
		}
		return displayName;

	}

	/**
	 * @param webMercatorProjection
	 */
	public void setMultiImageMode(WebMercatorProjection webMercatorProjection) {
		imageWorld.setProjection(ImageWorld.WEB_MERCATOR_PROJECTION);
		layerManager.getBaseLayer().setMultiImageMode();
		notifyNameListeners();
	}

	/**
	 * @param image
	 */
	public void setSingleImageMode(TileserverImage image) {
		try {
			imageWorld.setProjection(image);
			layerManager.getBaseLayer().setSingleImageMode(image);
			imageWorld.scaleTo(getNextMapScale(0));
			notifyNameListeners();
		} catch (FactoryException e) {
			e.printStackTrace();
		}
	}
}
