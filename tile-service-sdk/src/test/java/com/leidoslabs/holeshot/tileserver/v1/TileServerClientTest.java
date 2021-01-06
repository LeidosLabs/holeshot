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
