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

package com.leidoslabs.holeshot.imaging.nitf;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.leidoslabs.holeshot.imaging.TileReader;

import org.codice.imaging.nitf.core.common.NitfReader;
import org.codice.imaging.nitf.core.common.impl.NitfInputStreamReader;
import org.codice.imaging.nitf.core.dataextension.DataExtensionSegment;
import org.codice.imaging.nitf.core.header.impl.NitfParser;
import org.codice.imaging.nitf.core.image.ImageSegment;
import org.codice.imaging.nitf.core.impl.SlottedParseStrategy;
import org.codice.imaging.nitf.render.NitfRenderer;
import org.junit.Ignore;
import org.junit.Test;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.Raster;
import java.io.File;
import java.io.FileInputStream;
import java.util.Map;

import javax.imageio.ImageIO;

public class NITFTileReaderTest {

  @Test
  @Ignore
  public void testTileReader() throws Exception {

    FileInputStream inputStream = new FileInputStream(new File("/Users/parrise/Environment/data/po_154871_rgb_0000000.ntf"));
    SlottedParseStrategy parseStrategy = new SlottedParseStrategy();
    NitfReader reader = new NitfInputStreamReader(inputStream);
    NitfParser.parse(reader, parseStrategy);


    SimpleModule nitfMetadataModule =
        new SimpleModule("NITFMetadataModule", new Version(1, 0, 0, null));
    nitfMetadataModule.addSerializer(RenderedImageSegment.class,new RenderedImageSegmentMetadataSerializer());
    ObjectMapper mapper = new ObjectMapper();
    mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
    mapper.enable(SerializationFeature.INDENT_OUTPUT);
    mapper.registerModule(nitfMetadataModule);

    System.out.println(mapper.writeValueAsString(parseStrategy.getNitfHeader()));

    /*
    System.out.println("FILE TREs:");
    for (Map.Entry<String,String> me: parseStrategy.getNitfHeader().getTREsFlat().entrySet()) {
      System.out.println("  " + me.getKey() + ":=" + me.getValue());
    }
    for (DataExtensionSegment des : parseStrategy.getDataSource().getDataExtensionSegments()) {
      System.out.println("DES TREs:");
      for (Map.Entry<String,String> me: des.getTREsFlat().entrySet()) {
        System.out.println("  " + me.getKey() + ":=" + me.getValue());
      }
    }
*/

    for (int i = 0; i < parseStrategy.getDataSource().getImageSegments().size(); ++i) {
      ImageSegment imageSegment = parseStrategy.getDataSource().getImageSegments().get(i);
      System.out.println("Processing Image Segment: " + i);

      System.out.println("NumberOfColumns: " + imageSegment.getNumberOfColumns());
      System.out.println("NumberOfRows: " + imageSegment.getNumberOfRows());
      System.out.println("NumberOfBlocksPerColumn: " + imageSegment.getNumberOfBlocksPerColumn());
      System.out.println("NumberOfBlocksPerRow: " + imageSegment.getNumberOfBlocksPerRow());
      System.out.println("NPPB-Horizontal: " + imageSegment.getNumberOfPixelsPerBlockHorizontal());
      System.out.println("NPPB-Vertical: " + imageSegment.getNumberOfPixelsPerBlockVertical());

      System.out.println(mapper.writeValueAsString(imageSegment));

      /*
      Map<String,String> tres = imageSegment.getTREsFlat();
      System.out.println("ImageSegment TREs:");
      for (Map.Entry<String,String> me: tres.entrySet()) {
        System.out.println("  " + me.getKey() + ":=" + me.getValue());
      }
*/

      TileReader tileReader = NITFTileReaderFactory.createTileReader(imageSegment);
      System.out.println(tileReader.getSampleModel());
      ColorModel colorModel = tileReader.getColorModel();
      for (int tileY=0;tileY<imageSegment.getNumberOfBlocksPerColumn();tileY++) {
        for (int tileX = 0; tileX < imageSegment.getNumberOfBlocksPerRow(); tileX++) {
          System.out.println("Reading tile (" + tileX + "," + tileY + ")");
          Raster tile = tileReader.readTile(tileX,tileY);
          BufferedImage image = new BufferedImage(colorModel,Raster.createWritableRaster(tile.getSampleModel(), tile.getDataBuffer(), new Point(0,0)),colorModel.isAlphaPremultiplied(),null);
          ImageIO.write(image,"jpeg",new File("/Users/parrise/Environment/data/hacking-tiles/tile-" + tileX + "-" + tileY + ".jpg"));
        }
      }
    }

  }


  @Test
  @Ignore
  public void tileReadBenchmark() throws Exception {

    long start, end;

    for (int trials=0;trials<20;trials++) {
      start = System.currentTimeMillis();
      FileInputStream inputStream = new FileInputStream(new File("/Users/parrise/Environment/data/po_154871_rgb_0000000.ntf"));
      SlottedParseStrategy parseStrategy = new SlottedParseStrategy();
      NitfReader reader = new NitfInputStreamReader(inputStream);
      NitfParser.parse(reader, parseStrategy);
      end = System.currentTimeMillis();

      System.out.println("Time to Parse File: " + (end - start));

      for (int i = 0; i < parseStrategy.getDataSource().getImageSegments().size(); ++i) {

        start = System.currentTimeMillis();
        ImageSegment imageSegment = parseStrategy.getDataSource().getImageSegments().get(i);
        TileReader tileReader = NITFTileReaderFactory.createTileReader(imageSegment);

        for (int tileY = 0; tileY < imageSegment.getNumberOfBlocksPerColumn(); tileY++) {
          for (int tileX = 0; tileX < imageSegment.getNumberOfBlocksPerRow(); tileX++) {
            Raster tile = tileReader.readTile(tileX, tileY);
          }
        }
        end = System.currentTimeMillis();
        System.out.println("Time to access tiles of image segment " + i + ": " + (end - start));
      }
    }
  }

  @Test
  @Ignore
  public void codiceRendererBenchmark() throws Exception {

    long start, end;

    for (int trials=0;trials<20;trials++) {
      start = System.currentTimeMillis();
      FileInputStream inputStream = new FileInputStream(new File("/Users/parrise/Environment/data/po_154871_rgb_0000000.ntf"));
      SlottedParseStrategy parseStrategy = new SlottedParseStrategy();
      NitfReader reader = new NitfInputStreamReader(inputStream);
      NitfParser.parse(reader, parseStrategy);
      end = System.currentTimeMillis();

      System.out.println("Time to Parse File: " + (end - start));

      for (int i = 0; i < parseStrategy.getDataSource().getImageSegments().size(); ++i) {

        start = System.currentTimeMillis();
        ImageSegment imageSegment = parseStrategy.getDataSource().getImageSegments().get(i);
        NitfRenderer renderer = new NitfRenderer();
        BufferedImage img = renderer.render(imageSegment);
        end = System.currentTimeMillis();
        System.out.println("Time to render image segment " + i + ": " + (end - start));
      }
    }
  }



}
