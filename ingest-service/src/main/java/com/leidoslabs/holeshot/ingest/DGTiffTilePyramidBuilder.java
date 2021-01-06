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
package com.leidoslabs.holeshot.ingest;

import java.awt.geom.Point2D;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.leidoslabs.holeshot.imaging.ImageKey;
import com.leidoslabs.holeshot.imaging.metadata.TilePyramidDescriptor;
import com.leidoslabs.holeshot.imaging.photogrammetry.rpc.RPCCameraModel;
import com.leidoslabs.holeshot.imaging.photogrammetry.rpc.RPCCameraModelFactory;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;

/**
 * Build a tile pyramid from a DG-style GeoTIFF with metadata, potentially a multi-image mosaic
 */
public class DGTiffTilePyramidBuilder extends MultiPartTilePyramidBuilder {

   private static final Logger LOGGER = LoggerFactory.getLogger(DGTiffTilePyramidBuilder.class);
   private static final Pattern TILED_IMAGE_NAME = Pattern.compile(".*_R([0-9]+)C([0-9]+)-.*TIF");

   private static final int DEFAULT_PRECISION = 16;


   private static class PyramidMetadata {
      private TilePyramidDescriptor tilePyramidDescriptor;
      private Element til;

      public PyramidMetadata(TilePyramidDescriptor tilePyramidDescriptor, Element til) {
         super();
         this.tilePyramidDescriptor = tilePyramidDescriptor;
         this.til = til;
      }
      public TilePyramidDescriptor getTilePyramidDescriptor() {
         return tilePyramidDescriptor;
      }
      public Element getTil() {
         return til;
      }
      public int getTilesPerImageX() {
         return Integer.parseInt(til.getElementsByTagName("TILESIZEX").item(0).getTextContent()) / TILE_SIZE;
      }
      public int getTilesPerImageY() {
         return Integer.parseInt(til.getElementsByTagName("TILESIZEY").item(0).getTextContent()) / TILE_SIZE;
      }

   }

   private static BigDecimal parseDecimal(String value, int precision) {
      BigDecimal bd = new BigDecimal(value);
      bd.setScale(precision, RoundingMode.FLOOR);
      return bd;
   }
   private static BigDecimal parseDecimal(String value) {
      return parseDecimal(value, DEFAULT_PRECISION);
   }

    /**
     * Construction, defines how the pyramid build should access image data and publish processed tiles/metadata
     * @param accessor MultiPartImageAccessor implementation which can retrieve the image parts for processing
     * @param listener Component implementation that can handle finished tiles and metadata
     * @param metadataOnly flag=true if only metadata should be processed, no tiles generated
     */
   public DGTiffTilePyramidBuilder(MultiPartImageAccessor accessor, TilePyramidListener listener, boolean metadataOnly) {
      super(accessor,listener, metadataOnly);
   }

    /**
     * Build pyramid metadata, tiles, and MRF index.
     * @param name The name of the image sans file extensions, to be used to access metadata and image files
     * @throws TilePyramidException If the tile pyramid building throws at any stage for any reason
     */
   @Override
   public void buildTilePyramid(String name) throws TilePyramidException {
      final PyramidMetadata metadata = buildMetadata(name);
      String stage = "imagetiles";
      try {
         buildTiles(name, metadata);

         final ImageKey imageKey = getImageKey();
         stage = "MRF";
         listener.handleMRF(imageKey);
         
         stage = "metadata";
         listener.handleMetadata(imageKey, metadata.getTilePyramidDescriptor());
      } catch (Exception e) {
         throw new TilePyramidException(String.format("Unable to process %s through callback", stage), e);
      }
   }

   private void buildTiles(String name, PyramidMetadata metadata) {
      if (!isMetadataOnly()) {
         NodeList tileNodes = metadata.getTil().getElementsByTagName("TILE");
         for (int i = 0, len = tileNodes.getLength(); i < len; i++) {
            Element tile = (Element) tileNodes.item(i);

            String imageName = tile.getElementsByTagName("FILENAME").item(0).getTextContent();

            int imageRow = -1;
            int imageColumn = -1;
            if (tileNodes.getLength() == 1) {
               imageColumn = imageRow = 0;
            } else {
               Matcher m = TILED_IMAGE_NAME.matcher(imageName);
               if (m.matches()) {
                  imageRow = Integer.parseInt(m.group(1)) -1;
                  imageColumn = Integer.parseInt(m.group(2)) -1;
               }
            }

            if (imageRow >= 0 && imageColumn >= 0) {
               try {
                  final String partPrefix = getNamePrefix(name);
                  final int tilesPerImageX = metadata.getTilesPerImageX();
                  final int tilesPerImageY = metadata.getTilesPerImageY();

                  processImage(partPrefix + "/" + imageName, getImageKey(), tilesPerImageY * imageRow, tilesPerImageX * imageColumn);
               } catch (TilePyramidException e) {
                  LOGGER.warn("Unable to process image file {}! Skipping....", imageName,e);
               }
            }
         }
      }
   }
   private PyramidMetadata buildMetadata(String name) throws TilePyramidException {
      Document xmlProductDescription = null;
      try {
         String xmlPartName = name + ".XML";
         InputStream xmlStream = accessor.getPart(name);
         if (xmlStream == null) {
            throw new TilePyramidException("XML product metadata file " + name + " was not found." );
         }

         DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
         factory.setValidating(false);
         factory.setIgnoringElementContentWhitespace(true);
         DocumentBuilder builder = factory.newDocumentBuilder();
         xmlProductDescription = builder.parse(xmlStream);
      } catch (SAXException | ParserConfigurationException | IOException e) {
         throw new TilePyramidException("Unable to read XML product metadata file: " + name, e);
      }


      Element imd = (Element) xmlProductDescription.getElementsByTagName("IMD").item(0);


      Element image = (Element) imd.getElementsByTagName("IMAGE").item(0);
      LOGGER.info("DEBUG VALUE: {}",image.getElementsByTagName("FIRSTLINETIME").item(0).getTextContent());
      LocalDateTime collectionTime = LocalDateTime.from(
            DateTimeFormatter.ISO_DATE_TIME.parse(image.getElementsByTagName("FIRSTLINETIME").item(0).getTextContent()));

      setImageKey(new ImageKey(imd.getElementsByTagName("PRODUCTORDERID").item(0).getTextContent(),
            ZonedDateTime.of(collectionTime, ZoneOffset.UTC),
            ZonedDateTime.now(ZoneOffset.UTC)));

      Element til = (Element) xmlProductDescription.getElementsByTagName("TIL").item(0);
      // TODO: Update DG Pryamid Builder to support a wider range of tile size / overlap cases
      if (((Integer.parseInt(til.getElementsByTagName("TILESIZEX").item(0).getTextContent()) % TILE_SIZE) != 0) ||
            ((Integer.parseInt(til.getElementsByTagName("TILESIZEY").item(0).getTextContent()) % TILE_SIZE) != 0) ||
            (Integer.parseInt(til.getElementsByTagName("TILEOVERLAP").item(0).getTextContent()) != 0)) {
         throw new TilePyramidException("Implementation does not handle overlapping tiles or this tile size.");
      }

      int w = Integer.parseInt(imd.getElementsByTagName("NUMCOLUMNS").item(0).getTextContent());
      int h = Integer.parseInt(imd.getElementsByTagName("NUMROWS").item(0).getTextContent());
      LOGGER.info("Original Image Segment Dimensions: {}x{}", w, h);

      // TODO: Implement higher level R-Set generation that combines reduced tile from each image part
      int minRLevel = 0;
      int maxRLevel = (int) Math.ceil(Math.log(Math.max(w/TILE_SIZE,h/TILE_SIZE))/Math.log(2.0));

      //     int maxRLevel = (int) Math.ceil(Math.log(Math.max(tilesPerImageX,tilesPerImageY))/Math.log(2.0));
      //     int idealMaxRLevel = (int) Math.ceil(Math.log(Math.max(w/TILE_SIZE,h/TILE_SIZE))/Math.log(2.0));
      //     LOGGER.warn("Tile pyramid generation stopping at level {} but should really go to {}",maxRLevel,idealMaxRLevel);
      Element rpb = (Element) xmlProductDescription.getElementsByTagName("RPB").item(0);
      ObjectNode extendedMetadata = JsonNodeFactory.instance.objectNode();
      extendedMetadata.set("RPC00B", buildRPCFromXML((Element) rpb.getElementsByTagName("IMAGE").item(0)));

      String bandId = imd.getElementsByTagName("BANDID").item(0).getTextContent();
      switch (bandId) {
      case "Multi": {
         extendedMetadata.put("IREP", "MULTI");
         extendedMetadata.put("ICAT","MS");
         extendedMetadata.put("NBANDS", 8);
      }
      case "RGB": {
         extendedMetadata.put("IREP","RGB");
         extendedMetadata.put("ICAT","MS");
         extendedMetadata.put("NBANDS",3);
         break;
      }
      case "P": {
         extendedMetadata.put("IREP","MONO");
         extendedMetadata.put("ICAT", "VIS");
         extendedMetadata.put("NBANDS", 1);
         break;
      }
      default:
         LOGGER.warn("BANDID {} not supported by pyramid builder! Metadata is incomplete.",bandId);
      }

      extendedMetadata.put("NBPP",Integer.parseInt(imd.getElementsByTagName("BITSPERPIXEL").item(0).getTextContent()));
      extendedMetadata.put("ABPP",Integer.parseInt(imd.getElementsByTagName("BITSPERPIXEL").item(0).getTextContent()));
      extendedMetadata.put("NCOLS",w);
      extendedMetadata.put("NROWS",h);

      extendedMetadata.put("FDT", getImageKey().getFDT());
      extendedMetadata.put("IDATIM", getImageKey().getIDATIM());

      ObjectMapper jsonMapper = new ObjectMapper();
      RPCCameraModel cameraModel =
            RPCCameraModelFactory.buildRPCCameraFromMetadata(jsonMapper.convertValue(extendedMetadata, Map.class));

      Coordinate[] boundaryCoords = new Coordinate[5];
      boundaryCoords[0] = cameraModel.imageToWorld(new Point2D.Double(0, 0));
      boundaryCoords[1] = cameraModel.imageToWorld(new Point2D.Double(w, 0));
      boundaryCoords[2] = cameraModel.imageToWorld(new Point2D.Double(w, h));
      boundaryCoords[3] = cameraModel.imageToWorld(new Point2D.Double(0, h));
      boundaryCoords[4] = boundaryCoords[0];
      GeometryFactory gf = new GeometryFactory();

      TilePyramidDescriptor metadata = new TilePyramidDescriptor();
      final String imageName = getImageKey().getName();
      metadata.setTileWidth(TILE_SIZE);
      metadata.setTileHeight(TILE_SIZE);
      metadata.setWidth(w);
      metadata.setHeight(h);
      metadata.setBounds(gf.createPolygon(boundaryCoords));
      metadata.setName(imageName);
      metadata.setMinRLevel(minRLevel);
      metadata.setMaxRLevel(maxRLevel);
      // TODO: Allow for a producer code on this GUIDE
      // TODO: Fix this, input to UUID should really be a hashed version of the namespace and string
      metadata.setIdentifier("guide://000000/" + UUID.nameUUIDFromBytes(String.format("IMAGE:%s", imageName).getBytes()));
      metadata.setMetadata(extendedMetadata);

      return new PyramidMetadata(metadata, til);
   }

   /**
    * Converts the Digital Globe XML metadata containing the RPC coefficients into an equivalent
    * JSON structure used by our photogrammetry libraries.
    *
    * @param xmlRPC the DOM Element for the RPC
    * @return the JSON tree for the RPC
    */
   private static JsonNode buildRPCFromXML(Element xmlRPC) {
      final JsonNodeFactory factory = JsonNodeFactory.instance;

      ObjectNode jsonResult = factory.objectNode();

      jsonResult.put("ERR_BIAS", parseDecimal(xmlRPC.getElementsByTagName("ERRBIAS").item(0).getTextContent()));
      jsonResult.put("ERR_RAND", parseDecimal(xmlRPC.getElementsByTagName("ERRRAND").item(0).getTextContent()));

      jsonResult.put("SAMP_OFF", Long.parseLong(xmlRPC.getElementsByTagName("SAMPOFFSET").item(0).getTextContent()));
      jsonResult.put("LINE_OFF", Long.parseLong(xmlRPC.getElementsByTagName("LINEOFFSET").item(0).getTextContent()));
      jsonResult.put("LONG_OFF", parseDecimal(xmlRPC.getElementsByTagName("LONGOFFSET").item(0).getTextContent()));
      jsonResult.put("LAT_OFF", parseDecimal(xmlRPC.getElementsByTagName("LATOFFSET").item(0).getTextContent()));
      jsonResult.put("HEIGHT_OFF", Long.parseLong(xmlRPC.getElementsByTagName("HEIGHTOFFSET").item(0).getTextContent()));
      jsonResult.put("SAMP_SCALE", Long.parseLong(xmlRPC.getElementsByTagName("SAMPSCALE").item(0).getTextContent()));
      jsonResult.put("LINE_SCALE", Long.parseLong(xmlRPC.getElementsByTagName("LINESCALE").item(0).getTextContent()));
      jsonResult.put("LONG_SCALE", parseDecimal(xmlRPC.getElementsByTagName("LONGSCALE").item(0).getTextContent()));
      jsonResult.put("LAT_SCALE", parseDecimal(xmlRPC.getElementsByTagName("LATSCALE").item(0).getTextContent()));
      jsonResult.put("HEIGHT_SCALE", Long.parseLong(xmlRPC.getElementsByTagName("HEIGHTSCALE").item(0).getTextContent()));


      Element sampNumCoeffList = (Element) xmlRPC.getElementsByTagName("SAMPNUMCOEFList").item(0);
      String[] coeffs = sampNumCoeffList.getElementsByTagName("SAMPNUMCOEF").item(0).getTextContent().split(" ");
      for (int i = 0; i < coeffs.length; i++) {
         jsonResult.put("SAMP_NUM_COEFF_" + (i + 1), parseDecimal(coeffs[i]));
      }

      Element sampDenCoeffList = (Element) xmlRPC.getElementsByTagName("SAMPDENCOEFList").item(0);
      coeffs = sampDenCoeffList.getElementsByTagName("SAMPDENCOEF").item(0).getTextContent().split(" ");
      for (int i = 0; i < coeffs.length; i++) {
         jsonResult.put("SAMP_DEN_COEFF_" + (i + 1), parseDecimal(coeffs[i]));
      }

      Element lineNumCoefList = (Element) xmlRPC.getElementsByTagName("LINENUMCOEFList").item(0);
      coeffs = lineNumCoefList.getElementsByTagName("LINENUMCOEF").item(0).getTextContent().split(" ");
      for (int i = 0; i < coeffs.length; i++) {
         jsonResult.put("LINE_NUM_COEFF_" + (i + 1), parseDecimal(coeffs[i]));
      }

      Element lineDenCoefList = (Element) xmlRPC.getElementsByTagName("LINEDENCOEFList").item(0);
      coeffs = lineDenCoefList.getElementsByTagName("LINEDENCOEF").item(0).getTextContent().split(" ");
      for (int i = 0; i < coeffs.length; i++) {
         jsonResult.put("LINE_DEN_COEFF_" + (i + 1), parseDecimal(coeffs[i]));
      }

      return jsonResult;
   }

   private String getNamePrefix(String name) {
      int slashIdx = name.lastIndexOf("/");
      return name.substring(0,slashIdx);
   }
}
