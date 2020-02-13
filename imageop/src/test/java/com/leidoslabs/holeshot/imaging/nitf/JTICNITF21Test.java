package com.leidoslabs.holeshot.imaging.nitf;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.leidoslabs.holeshot.imaging.TileReader;

import org.codice.imaging.nitf.core.common.NitfReader;
import org.codice.imaging.nitf.core.common.ParseStrategy;
import org.codice.imaging.nitf.core.common.impl.NitfInputStreamReader;
import org.codice.imaging.nitf.core.header.impl.NitfParser;
import org.codice.imaging.nitf.core.image.ImageSegment;
import org.codice.imaging.nitf.core.impl.SlottedParseStrategy;
import org.junit.Before;
import org.junit.Test;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.MultiPixelPackedSampleModel;
import java.awt.image.Raster;
import java.awt.image.SampleModel;
import java.awt.image.WritableRaster;
import java.io.File;

import javax.imageio.ImageIO;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Created by parrise on 8/31/17.
 */
public class JTICNITF21Test {

  private ObjectMapper mapper;
  @Before
  public void setupTest() {
    SimpleModule nitfMetadataModule =
        new SimpleModule("NITFMetadataModule", new Version(1, 0, 0, null));
    nitfMetadataModule.addSerializer(ImageSegment.class, new ImageSegmentMetadataSerializer());
    mapper = new ObjectMapper();
    mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
    mapper.enable(SerializationFeature.INDENT_OUTPUT);
    mapper.registerModule(nitfMetadataModule);
  }


  @Test
  public void i_3001a() throws Exception {
    ParseStrategy parseStrategy = getParsedImage("/JitcNitf21Samples/i_3001a.ntf");
    ImageSegment imageSegment = parseStrategy.getDataSource().getImageSegments().get(0);
    TileReader tileReader = NITFTileReaderFactory.createTileReader(imageSegment);
    assertNotNull(tileReader);
    assertEquals(1, imageSegment.getNumberOfBlocksPerColumn());
    assertEquals(1, imageSegment.getNumberOfBlocksPerRow());

    SampleModel sampleModel = tileReader.getSampleModel();
    assertNotNull(sampleModel);
    assertEquals(1024, sampleModel.getHeight());
    assertEquals(1024, sampleModel.getWidth());
    assertEquals(1, sampleModel.getNumBands());
    assertEquals(DataBuffer.TYPE_BYTE, tileReader.getSampleModel().getDataType());

    ColorModel colorModel = tileReader.getColorModel();
    assertNotNull(colorModel);
    assertEquals(1, colorModel.getNumComponents());
    assertEquals(8, colorModel.getComponentSize(0));
    assertFalse(colorModel.hasAlpha());
    assertFalse(colorModel.isAlphaPremultiplied());

    Raster tile = tileReader.readTile(0, 0);
    BufferedImage image = new BufferedImage(colorModel,
        Raster.createWritableRaster(tile.getSampleModel(), tile.getDataBuffer(), new Point(0, 0)),
        colorModel.isAlphaPremultiplied(), null);
    ImageIO.write(image, "png", new File("i_3001a.png"));

  }

  @Test
  public void i_3004g() throws Exception {
    ParseStrategy parseStrategy = getParsedImage("/JitcNitf21Samples/i_3004g.ntf");
    ImageSegment imageSegment = parseStrategy.getDataSource().getImageSegments().get(0);
    TileReader tileReader = NITFTileReaderFactory.createTileReader(imageSegment);
    assertNotNull(tileReader);
    assertEquals(1, imageSegment.getNumberOfBlocksPerColumn());
    assertEquals(1, imageSegment.getNumberOfBlocksPerRow());

    SampleModel sampleModel = tileReader.getSampleModel();
    assertNotNull(sampleModel);
    assertEquals(512, sampleModel.getHeight());
    assertEquals(512, sampleModel.getWidth());
    assertEquals(1, sampleModel.getNumBands());
    assertEquals(DataBuffer.TYPE_BYTE, tileReader.getSampleModel().getDataType());

    ColorModel colorModel = tileReader.getColorModel();
    assertNotNull(colorModel);
    assertEquals(1, colorModel.getNumComponents());
    assertEquals(8, colorModel.getComponentSize(0));
    assertFalse(colorModel.hasAlpha());
    assertFalse(colorModel.isAlphaPremultiplied());

    Raster tile = tileReader.readTile(0, 0);
    BufferedImage image = new BufferedImage(colorModel,
        Raster.createWritableRaster(tile.getSampleModel(), tile.getDataBuffer(), new Point(0, 0)),
        colorModel.isAlphaPremultiplied(), null);
    ImageIO.write(image, "png", new File("i_3004g.png"));
  }

  @Test
  public void i_3025b() throws Exception {
    ParseStrategy parseStrategy = getParsedImage("/JitcNitf21Samples/i_3025b.ntf");
    ImageSegment imageSegment = parseStrategy.getDataSource().getImageSegments().get(0);
    TileReader tileReader = NITFTileReaderFactory.createTileReader(imageSegment);
    assertNotNull(tileReader);
    assertEquals(1, imageSegment.getNumberOfBlocksPerColumn());
    assertEquals(1, imageSegment.getNumberOfBlocksPerRow());

    SampleModel sampleModel = tileReader.getSampleModel();
    assertNotNull(sampleModel);
    assertEquals(64, sampleModel.getHeight());
    assertEquals(64, sampleModel.getWidth());
    assertEquals(1, sampleModel.getNumBands());
    assertEquals(DataBuffer.TYPE_BYTE, tileReader.getSampleModel().getDataType());

    ColorModel colorModel = tileReader.getColorModel();
    assertNotNull(colorModel);
    assertEquals(1, colorModel.getNumComponents());
    assertEquals(8, colorModel.getComponentSize(0));
    assertFalse(colorModel.hasAlpha());
    assertFalse(colorModel.isAlphaPremultiplied());

    Raster tile = tileReader.readTile(0, 0);
    BufferedImage image = new BufferedImage(colorModel,
        Raster.createWritableRaster(tile.getSampleModel(), tile.getDataBuffer(), new Point(0, 0)),
        colorModel.isAlphaPremultiplied(), null);
    ImageIO.write(image, "png", new File("i_3025b.png"));
  }

  @Test
  public void i_3034c() throws Exception {
    ParseStrategy parseStrategy = getParsedImage("/JitcNitf21Samples/i_3034c.ntf");
    ImageSegment imageSegment = parseStrategy.getDataSource().getImageSegments().get(0);
    TileReader tileReader = NITFTileReaderFactory.createTileReader(imageSegment);
    assertNotNull(tileReader);
    assertEquals(1, imageSegment.getNumberOfBlocksPerColumn());
    assertEquals(1, imageSegment.getNumberOfBlocksPerRow());

    SampleModel sampleModel = tileReader.getSampleModel();
    assertNotNull(sampleModel);
    assertEquals(18, sampleModel.getHeight());
    assertEquals(35, sampleModel.getWidth());
    assertEquals(1, sampleModel.getNumBands());
    assertEquals(DataBuffer.TYPE_BYTE, tileReader.getSampleModel().getDataType());

    ColorModel colorModel = tileReader.getColorModel();
    assertNotNull(colorModel);
    assertEquals(3, colorModel.getNumComponents());
    assertEquals(8, colorModel.getComponentSize(0));
    assertFalse(colorModel.hasAlpha());
    assertFalse(colorModel.isAlphaPremultiplied());

    Raster tile = tileReader.readTile(0, 0);
    BufferedImage image = new BufferedImage(colorModel,
        Raster.createWritableRaster(tile.getSampleModel(), tile.getDataBuffer(), new Point(0, 0)),
        colorModel.isAlphaPremultiplied(), null);
    ImageIO.write(image, "png", new File("i_3034c.png"));
  }

  @Test
  public void i_3034f() throws Exception {
    ParseStrategy parseStrategy = getParsedImage("/JitcNitf21Samples/i_3034f.ntf");
    ImageSegment imageSegment = parseStrategy.getDataSource().getImageSegments().get(0);
    TileReader tileReader = NITFTileReaderFactory.createTileReader(imageSegment);

    assertNotNull(tileReader);
    assertEquals(1, imageSegment.getNumberOfBlocksPerColumn());
    assertEquals(1, imageSegment.getNumberOfBlocksPerRow());

    System.out.println(mapper.writeValueAsString(parseStrategy.getNitfHeader()));
    System.out.println(mapper.writeValueAsString(imageSegment));

    SampleModel sampleModel = tileReader.getSampleModel();
    assertNotNull(sampleModel);
    //assertTrue(sampleModel instanceof MultiPixelPackedSampleModel);
    assertEquals(18, sampleModel.getHeight());
    assertEquals(35, sampleModel.getWidth());
    assertEquals(1, sampleModel.getNumBands());
    assertEquals(DataBuffer.TYPE_BYTE, tileReader.getSampleModel().getDataType());

    ColorModel colorModel = tileReader.getColorModel();
    assertNotNull(colorModel);
    assertEquals(3, colorModel.getNumComponents());
    assertEquals(8, colorModel.getComponentSize(0));
    assertFalse(colorModel.hasAlpha());
    assertFalse(colorModel.isAlphaPremultiplied());

    Raster tile = tileReader.readTile(0, 0);

    System.out.println("Length of Data Buffer: " + tile.getDataBuffer().getSize());

    DataBufferByte dataBufferByte = (DataBufferByte) tile.getDataBuffer();
    byte[] bytes = dataBufferByte.getData();
    int count=0;
    for (int i=0;i<bytes.length;i++) {

      byte packed = bytes[i];
      for (int b=0;b<8;b++) {
        System.out.print(((packed & (0x1 << (7-b))) > 0) ? "1" : "0");
        if (++count % sampleModel.getWidth() == 0) {
          System.out.println("");
        }
      }
    }
    System.out.println("*******************");

    for (int y=0;y<sampleModel.getHeight();y++) {
      for (int x=0;x<sampleModel.getWidth();x++) {

        System.out.print(sampleModel.getSample(x,y,0,tile.getDataBuffer()) == 1 ? "1" : "0");

        //System.out.print(" "  + tile.getDataBuffer().getElem(x + y*sampleModel.getWidth()));
      }
      System.out.println("");
    }

    System.out.println("*******************");

    byte[] dobj = new byte[1];
    for (int y=0;y<sampleModel.getHeight();y++) {
      for (int x=0;x<sampleModel.getWidth();x++) {
        sampleModel.getDataElements(x,y,dobj,tile.getDataBuffer());
        System.out.print(dobj[0] == 1 ? "1" : "0");

        //System.out.print(" "  + tile.getDataBuffer().getElem(x + y*sampleModel.getWidth()));
      }
      System.out.println("");
    }

    BufferedImage image = new BufferedImage(colorModel,
        Raster.createWritableRaster(tile.getSampleModel(), tile.getDataBuffer(), new Point(0, 0)),
        colorModel.isAlphaPremultiplied(), null);
    ImageIO.write(image, "png", new File("i_3034f.png"));
  }

  private static ParseStrategy getParsedImage(String resource) throws Exception {
    SlottedParseStrategy parseStrategy = new SlottedParseStrategy();
    NitfReader reader = new NitfInputStreamReader(JTICNITF21Test.class.getResourceAsStream(resource));
    NitfParser.parse(reader, parseStrategy);
    return parseStrategy;
  }
}
