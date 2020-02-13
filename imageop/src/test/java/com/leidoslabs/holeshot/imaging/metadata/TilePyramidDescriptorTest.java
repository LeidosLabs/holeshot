package com.leidoslabs.holeshot.imaging.metadata;

import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.leidoslabs.holeshot.imaging.nitf.ImageSegmentMetadataSerializer;
import org.image.common.geojson.GeoJsonModule;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;

import org.codice.imaging.nitf.core.image.ImageSegment;
import org.junit.Before;
import org.junit.Test;

import java.util.Date;

public class TilePyramidDescriptorTest {


  private GeometryFactory geometryFactory;
  private ObjectMapper mapper;


  @Before
  public void setup() {
    SimpleModule nitfMetadataModule =
        new SimpleModule("NITFMetadataModule", new Version(1, 0, 0, null));
    nitfMetadataModule.addSerializer(ImageSegment.class,new ImageSegmentMetadataSerializer());

    mapper = new ObjectMapper();
    mapper.enable(SerializationFeature.INDENT_OUTPUT);
    mapper.registerModule(new GeoJsonModule());
    mapper.registerModule(nitfMetadataModule);
    geometryFactory = new GeometryFactory();
  }

  @Test
  public void sampleJSON() throws Exception {
    TilePyramidDescriptor descriptor = new TilePyramidDescriptor();
    descriptor.setIdentifier("guide://000000/d104a821-62d3-4ec2-924f-5318a45ae22e");
    descriptor.setCreateDateTime(new Date());
    descriptor.getResponsibleEntity().add("ORIG:USA.O1");
    descriptor.getAuthRef().add("BASE:ABC123");
    descriptor.getPolicyRef().add("urn:edm:policy:control:xxxx");
    descriptor.getControlSet().add("CLS:U");
    descriptor.setName("20010220065958IKONOS_01     01978AA00000");
    descriptor.setDescription("Sample tile pyramid descriptor illustrating proposed schema");
    descriptor.setMinRLevel(0);
    descriptor.setMaxRLevel(5);
    descriptor.getTiles().add("https://foo.bar.com/tileserver/20170101xxxx000000000000/{z}/tile-{x}-{y}-{b}.png");
    descriptor.setMetadata("https://foo.bar.com/catalog/d104a821-62d3-4ec2-924f-5318a45ae22e/metadata");
    descriptor.setTileWidth(512);
    descriptor.setTileHeight(512);
    descriptor.setWidth(15);
    descriptor.setHeight(20);
    Coordinate[] coords = new Coordinate[5];
    coords[0] = new Coordinate(10,10);
    coords[1] = new Coordinate( 20, 15);
    coords[2] = new Coordinate(20, 35);
    coords[3] = new Coordinate(11, 33);
    coords[4] = coords[0];

    descriptor.setBounds(geometryFactory.createPolygon(coords));

    System.out.println(mapper.writeValueAsString(descriptor));

  }
}
