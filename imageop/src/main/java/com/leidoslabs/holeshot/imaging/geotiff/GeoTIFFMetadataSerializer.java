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
package com.leidoslabs.holeshot.imaging.geotiff;

import java.awt.geom.Point2D;
import java.io.IOException;
import java.util.Arrays;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.leidoslabs.holeshot.imaging.photogrammetry.CameraModel;
import com.leidoslabs.holeshot.imaging.photogrammetry.rpc.RPCBTre;
import com.leidoslabs.holeshot.imaging.photogrammetry.rpc.RPCCameraModel;
import com.leidoslabs.holeshot.imaging.photogrammetry.rpc.RpcSolver;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;

/**
 * JSONSerializer for serializing GeoTIFF metadata
 */
public class GeoTIFFMetadataSerializer extends JsonSerializer<GeoTIFFImageSourceSegment> {
   private static final Logger LOGGER = LoggerFactory.getLogger(GeoTIFFMetadataSerializer.class);

   private static int X_SAMPLES = -1;
   private static int Y_SAMPLES = -1;

   @Override
   /**
    * Serialize all GeoTIFF metadata as a JSON. Use RPCBTreSerializer to serialize RPCs
    */
   public void serialize(GeoTIFFImageSourceSegment imageSegment,
         JsonGenerator jsonGenerator,
         SerializerProvider serializerProvider) throws IOException {
      if (imageSegment == null) {
         jsonGenerator.writeNull();
         return;
      }
      Map<String, Object> meta = imageSegment.getMetadata();
      jsonGenerator.writeStartObject();

      final long rows = getAsLong(meta, "ImageLength");
      final long cols = getAsLong(meta, "ImageWidth");

      jsonGenerator.writeNumberField("FDT", imageSegment.getImageKey().getFDT());
      jsonGenerator.writeNumberField("IDATIM", imageSegment.getImageKey().getIDATIM());
      jsonGenerator.writeNumberField("NROWS", rows);
      jsonGenerator.writeNumberField("NCOLS", cols);
      jsonGenerator.writeNumberField("NBANDS", getAsLong(meta, "SamplesPerPixel"));
      
      jsonGenerator.writeNumberField("NBPP", getAsShort(meta, "BitsPerSample"));

      // TODO: Need to be able to handle it if the image actually comes with RPCs
      CameraModel originalCameraModel = imageSegment.getCameraModel();
      RpcSolver rpcSolver = new RpcSolver(false, false);
      final int minX = imageSegment.getRenderedImage().getMinX();
      final int minY = imageSegment.getRenderedImage().getMinY();
      final int maxX = minX + imageSegment.getRenderedImage().getWidth() - 1;
      final int maxY = minY + imageSegment.getRenderedImage().getHeight() - 1;
      final Envelope imageBounds = new Envelope(minX, maxX, minY, maxY);
      rpcSolver.solveCoefficients(imageBounds, originalCameraModel, X_SAMPLES, Y_SAMPLES, false);
      
      RPCCameraModel rpcCameraModel = rpcSolver.getRPCCameraModel();
      
      
      if (LOGGER.isDebugEnabled()) {
         final double[][] corners = new double[][] {
            { minX, minY },
            { minX, maxY },
            { maxX, maxY },
            { maxX, minY },
         };

         Arrays.stream(corners).map(a -> new Point2D.Double(a[0], a[1]))
         .forEach(p-> {
            Coordinate originalCoord = originalCameraModel.imageToWorld(p);
            Coordinate rpcCoord = rpcCameraModel.imageToWorld(p);

            LOGGER.debug(String.format("imageCorner = %s, origCameraModel = %s, rpcCameraModel = %s", p.toString(), originalCoord.toString(), rpcCoord.toString()));
         });
      }
      
      
      RPCBTre rpcBTre = rpcSolver.getNitfRpcBTag();
      
      jsonGenerator.writeObject(rpcBTre);
      
      jsonGenerator.writeEndObject();
   }

   private static long getAsLong(Map<String, Object> map, String key) {
      long result = -1;
      Object value = map.get(key);
      if (value instanceof Long) {
         result = (Long)value;
      } else if (value instanceof Integer) {
         result = ((Integer)value).longValue();
      } else  if (value instanceof Short) {
         result = ((Short)value).longValue();
      } else if (value instanceof Long[]) {
         result = ((Long[])value)[0].longValue();
      } else if (value instanceof Integer[]) {
         result = ((Integer[])value)[0].longValue();
      } else  if (value instanceof Short[]) {
         result = ((Short[])value)[0].longValue();
      }
      return result;
   }
   private static short getAsShort(Map<String, Object> map, String key) {
      short result = -1;
      Object value = map.get(key);
      if (value instanceof Long) {
         result = ((Long)value).shortValue();
      } else if (value instanceof Integer) {
         result = ((Integer)value).shortValue();
      } else  if (value instanceof Short) {
         result = (Short)value;
      } else if (value instanceof Long[]) {
         result = ((Long[])value)[0].shortValue();
      } else if (value instanceof Integer[]) {
         result = ((Integer[])value)[0].shortValue();
      } else  if (value instanceof Short[]) {
         result = ((Short[])value)[0];
      }
      return result;
   }
   
}
