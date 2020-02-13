package com.leidoslabs.holeshot.tileserver.v1;


import java.awt.image.BufferedImage;
import java.io.File;

import javax.imageio.ImageIO;

import org.junit.Ignore;
import org.junit.Test;

import com.leidoslabs.holeshot.credentials.HoleshotCredentials;

public class TileServerClientTest {

  @Ignore
  @Test
  public void exampleUsage() throws Exception {

    HoleshotCredentials credentials = HoleshotCredentials.getApplicationDefaults();

    TileServerClient tileServerClient =
        TileServerClientBuilder.forEndpoint("https://tileserver.leidoslabs.com/tileserver")
            .withCredentials(credentials)
            .build();

    TilePyramidDescriptor metadata = tileServerClient.getMetadata("0000000000", "20040129070249000");
    System.out.println(metadata.getName());

    BufferedImage tile = tileServerClient.getTileAsImage(
        "0000000000",
        "20040129070249000",
        0,
        0,
        0,
        2);

    ImageIO.write(tile, "jpeg", new File("tile-client-result.jpg"));
  }

}
