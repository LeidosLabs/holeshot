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

import static org.lwjgl.opengl.GL11.GL_COLOR_BUFFER_BIT;
import static org.lwjgl.opengl.GL11.GL_FLOAT;
import static org.lwjgl.opengl.GL11.GL_NEAREST;
import static org.lwjgl.opengl.GL11.GL_RGBA;
import static org.lwjgl.opengl.GL11.glReadPixels;
import static org.lwjgl.opengl.GL11.glViewport;
import static org.lwjgl.opengl.GL30.GL_DRAW_FRAMEBUFFER;
import static org.lwjgl.opengl.GL30.GL_READ_FRAMEBUFFER;
import static org.lwjgl.opengl.GL30.glBindFramebuffer;
import static org.lwjgl.opengl.GL30.glBlitFramebuffer;

import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.io.Closeable;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.FloatBuffer;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

import javax.imageio.ImageIO;
import javax.imageio.stream.ImageOutputStream;
import javax.imageio.stream.MemoryCacheImageOutputStream;

import org.eclipse.jetty.io.EofException;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.graphics.Point;
import org.image.common.util.CloseableUtils;
import org.joml.Vector2d;
import org.locationtech.jts.geom.Envelope;
import org.lwjgl.BufferUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.leidoslabs.holeshot.elt.coord.ImageWorld;
import com.leidoslabs.holeshot.elt.coord.ScreenELTCoordinate;
import com.leidoslabs.holeshot.elt.imagechain.Framebuffer;
import com.leidoslabs.holeshot.elt.layers.BaseLayer;
import com.leidoslabs.holeshot.elt.layers.DataLayer;
import com.leidoslabs.holeshot.elt.observations.Observation;
import com.leidoslabs.holeshot.elt.observations.PointObservation;
import com.leidoslabs.holeshot.elt.websocket.ELTWebSocket;

/**
 * Manages the layers (Raster and KML) in an ELTCanvas.
 */
public class LayerManager implements Closeable {
   private static final Logger LOGGER = LoggerFactory.getLogger(LayerManager.class);

   private Map<String, DataLayer> dataLayers = new LinkedHashMap<>();
   private BaseLayer baseLayer;
   private Point oldMousePosition;
   private ELTCanvas eltCanvas;
   private float[] imageBytes;
   private FloatBuffer imageBuffer;


   /**
    * Constructor without providing ELTCanvas
    */
   public LayerManager() {
      this(null);
   }
   /**
    * Constructor, adds LayerManagerMouseListener to ELTCanvas
    * @param eltCanvas
    */
   public LayerManager(ELTCanvas eltCanvas) {
      this.eltCanvas = eltCanvas;
      if (eltCanvas != null) {
         this.eltCanvas.addMouseListener(new LayerManagerMouseListener());
      }
   }

   /**
    * Add a data layer with a random key
    * @param layer DataLater to add
    */
   public void addDataLayer(DataLayer layer) {
      dataLayers.put(UUID.randomUUID().toString(), layer);
   }

   /**
    * Add a Data Layer with a specified id
    * @param id
    * @param layer
    */
   public void addDataLayer(String id, DataLayer layer) {
      dataLayers.put(id, layer);
   }

   /**
    * Set base layer
    * @param baseLayer
    */
   public void setBaseLayer(BaseLayer baseLayer) {
      this.baseLayer = baseLayer;
   }

   /**
    * @return All data layers as Map
    */
   public Map<String, DataLayer> getAllDataLayers() { return dataLayers; }

   /**
    * Get Data layer by ID
    * @param id
    * @return
    */
   public DataLayer getDataLayer(String id) {
      return dataLayers.get(id);
   }

   public BaseLayer getBaseLayer() {
      return baseLayer;
   }

   public DataLayer removeDataLayer(String layerName) {
      return dataLayers.remove(layerName);
   }

   /**
    * Select Observations (identified by companionID) in layer 
    * @param companionID
    * @param layerName
    */
   public void select(String companionID, String layerName) {
      dataLayers.get(layerName).getData().forEach((obs) -> {
         if (obs.getId().equals(companionID)) {
            obs.select();
         }
      });
   }

   /**
    * Select Observations (identified by companionID) in layer 
    * @param companionID
    * @param layer
    */
   public void select(String companionID, DataLayer layer) {
      layer.getData().forEach((obs) -> {
         if (obs.getId().equals(companionID)) {
            obs.select();
         }
      });
   }

   public void unselectAll() {
      dataLayers.forEach((id, layer) -> layer.getData().forEach(Observation::unselect));
   }

   /**
    * Renders all layers onto a frame buffer, then draws in opengl.
    * @param imageWorld
    */
   public void drawAll(ImageWorld imageWorld) {

      if(baseLayer != null) {
         final Envelope currentViewport = imageWorld.getCurrentViewport();
         Framebuffer fb = render();

         glBindFramebuffer(GL_READ_FRAMEBUFFER, fb.getFboId());
         glBindFramebuffer(GL_DRAW_FRAMEBUFFER, 0);

         glBlitFramebuffer(0, 0, fb.getSize().width, fb.getSize().height, 0, 0,
               (int) currentViewport.getWidth(), (int) currentViewport.getHeight(),
               GL_COLOR_BUFFER_BIT, GL_NEAREST);
      }
   }
   
   /**
    * Dumps Image stream
    * @param outputStream Image Stream to dump
    * @param chipOutputDimension Unused
    * @param warpToFit unused
    */
   public void dumpImage(OutputStream outputStream, Dimension chipOutputDimension, boolean warpToFit) {
      try {
         final Framebuffer fb = getBaseLayer().getRenderer().getResultFramebuffer();
         final int imageWidth = fb.getSize().width;
         final int imageHeight = fb.getSize().height;
         final int imageBands = 4;
         final int imageArea = imageWidth * imageHeight;
         final int imageNumBytes= imageArea * imageBands;
         if (imageBytes == null || imageBytes.length != imageNumBytes) {
            this.imageBytes = new float[imageNumBytes];
            this.imageBuffer = BufferUtils.createFloatBuffer(imageNumBytes);
         }

         fb.bind();
         glViewport(0, 0, imageWidth, imageHeight);
         imageBuffer.rewind();
         glReadPixels(0, 0, imageWidth,  imageHeight, GL_RGBA, GL_FLOAT, imageBuffer);
         imageBuffer.get(imageBytes);
         imageBuffer.rewind();

         BufferedImage image = new BufferedImage(imageWidth, imageHeight, BufferedImage.TYPE_INT_ARGB);
         int[] rgbArray = new int[imageArea];
         final int maxPixel = 255; // (int)Math.pow(2, 16);
         for(int y = 0; y < imageHeight; ++y) {
            for(int x = 0; x < imageWidth; ++x) {
               int r = (int)(imageBuffer.get() * maxPixel) << 16;
               int g = (int)(imageBuffer.get() * maxPixel) << 8;
               int b = (int)(imageBuffer.get() * maxPixel);
               int a = (int)(imageBuffer.get() * maxPixel) << 24;
               int i = ((imageHeight - 1) - y) * imageWidth + x;
               rgbArray[i] = a + r + g + b;
            }
         }

         image.setRGB(0,0,imageWidth, imageHeight, rgbArray, 0, imageWidth);
         BufferedImage imageToWrite = image;
         try (ImageOutputStream ios = new MemoryCacheImageOutputStream(outputStream)) {
            ImageIO.write(imageToWrite, "png", ios);
            ios.flush();
            outputStream.flush();
         }
      } catch (IOException e) {
         if (!wasConnectionClosed(e)) {
            LOGGER.error(e.getMessage(), e);
         }
      }
   }
   private static boolean wasConnectionClosed(Throwable e) {
      return e != null &&
            (e instanceof EofException
                  ||
                  (e instanceof IOException &&
                        e.getMessage().contains("Broken pipe"))
                  || wasConnectionClosed(e.getCause()));
   }
   private Framebuffer render() {
      final AtomicReference<Framebuffer> fb = new AtomicReference<Framebuffer>(baseLayer.draw());
      dataLayers.forEach((id, layer) -> fb.set(layer.draw(fb.get())));
      return fb.get();
   }


   /**
    * Mouse Listener for LayerManager. Create new Observations with left click + control
    */
   public class LayerManagerMouseListener implements MouseListener {

      private static final int LEFT_BUTTON = 1;
      private static final int MIDDLE_BUTTON = 2;
      private static final int RIGHT_BUTTON = 3;
      private Stream<Observation> obs;

      @Override
      public void mouseDown(MouseEvent e) {
         oldMousePosition = new Point(e.x, e.y);
      }

      @Override
      public void mouseUp(MouseEvent e) {
         if (e.button == LEFT_BUTTON) {
            ((ELTFrame) eltCanvas.getParent()).hideEditor();
            unselectAll();
            obs = Stream.empty();
            dataLayers.forEach((id, layer) -> obs = Stream.of(obs, layer.getData().stream()).flatMap(s -> s));
            Vector2d clickPoint = new Vector2d(e.x, e.y);
            if (oldMousePosition != null && Math.sqrt(Math.pow(clickPoint.x - oldMousePosition.x, 2) + Math.pow(clickPoint.y - oldMousePosition.y, 2)) < 20) {
               // Point click
               if ((e.stateMask & SWT.CONTROL) == SWT.CONTROL || (e.stateMask & SWT.BUTTON1) == SWT.BUTTON1) {
                  // If its not a shift-click we are going to need to determine if the click was in proximity to an existing point, so compute that distance first
                  int clickRadius = 8; // pixels
                  PointObservation clickCenter = new PointObservation(new ScreenELTCoordinate(eltCanvas.getImageWorld(), clickPoint, eltCanvas.getImageWorld().getImageScale()).getGeodeticCoordinate());
                  PointObservation maxClick = new PointObservation(new ScreenELTCoordinate(eltCanvas.getImageWorld(), new Vector2d(e.x + clickRadius, e.y), eltCanvas.getImageWorld().getImageScale().copy()).getGeodeticCoordinate());
                  double maxDistance = clickCenter.getGeometry().distance(maxClick.getGeometry());
                  obs
                  .filter(p -> p.getGeometry().distance(clickCenter.getGeometry()) < maxDistance)
                  .min(Comparator.comparingDouble(p -> p.getGeometry().distance(clickCenter.getGeometry())))
                  .ifPresent(p -> {
                     if ((e.stateMask & SWT.CONTROL) == SWT.CONTROL) {
                        dataLayers.entrySet().stream().filter(entry -> entry.getValue().remove(p)).findFirst();  //lazy-eval: only first found will be removed
                     } else {
                        ((ELTFrame) eltCanvas.getParent()).showEditor(p, clickPoint);
                        ELTWebSocket.sendSelection(eltCanvas.getAppId(), p.getId(), p.getCoordinates());
                        p.select();
                     }
                  });
               }
            }
         }
      }

      @Override
      public void mouseDoubleClick(MouseEvent e) {
      }

   }


   /**
    * @return Are all Layers fully rendered?
    */
   public boolean isFullyRendered() {
      return baseLayer.isFullyRendered() &&
            dataLayers.values().stream().noneMatch(layer-> !layer.isFullyRendered());
   }

   @Override
   public void close() throws IOException {
      CloseableUtils.close(Stream.concat(
            dataLayers.values().stream(),
            Stream.of(baseLayer)).toArray(Closeable[]::new));
   }
}
