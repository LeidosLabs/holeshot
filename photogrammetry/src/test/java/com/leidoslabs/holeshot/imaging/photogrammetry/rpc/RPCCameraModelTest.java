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

package com.leidoslabs.holeshot.imaging.photogrammetry.rpc;

import org.locationtech.jts.geom.Coordinate;

import org.junit.Before;
import org.junit.Test;

import java.awt.geom.Point2D;
import java.util.HashMap;
import java.util.Map;

import static junit.framework.TestCase.assertNotNull;
import static org.junit.Assert.assertEquals;

/**
 * Created by parrise on 2/22/17.
 */
public class RPCCameraModelTest {

  private Map<String, Object> nominalMetadata;

  @Before
  public void setup() {
    nominalMetadata = new HashMap<>();

    Map<String,String> rpcTRE = new HashMap<>();
    nominalMetadata.put("RPC00B",rpcTRE);

    rpcTRE.put("ERR_BIAS", "0005.14");
    rpcTRE.put("ERR_RAND", "0000.50");

    rpcTRE.put("SAMP_OFF", "06163");
    rpcTRE.put("SAMP_SCALE", "06164");
    rpcTRE.put("LINE_OFF", "006927");
    rpcTRE.put("LINE_SCALE", "006927");
    rpcTRE.put("LAT_OFF", "+27.2197");
    rpcTRE.put("LAT_SCALE", "+00.0716");
    rpcTRE.put("LONG_OFF", "+056.3653");
    rpcTRE.put("LONG_SCALE", "+000.0727");
    rpcTRE.put("HEIGHT_OFF", "-0009");
    rpcTRE.put("HEIGHT_SCALE", "+3234");

    rpcTRE.put("SAMP_NUM_COEFF_1", "-2.204284E+7");
    rpcTRE.put("SAMP_NUM_COEFF_2", "+9.999999E+8");
    rpcTRE.put("SAMP_NUM_COEFF_3", "+6.250255E+6");
    rpcTRE.put("SAMP_NUM_COEFF_4", "-1.342506E+8");
    rpcTRE.put("SAMP_NUM_COEFF_5", "-3.107754E+7");
    rpcTRE.put("SAMP_NUM_COEFF_6", "-1.101236E+7");
    rpcTRE.put("SAMP_NUM_COEFF_7", "+5.563589E+6");
    rpcTRE.put("SAMP_NUM_COEFF_8", "-1.101821E+7");
    rpcTRE.put("SAMP_NUM_COEFF_9", "-1.698152E+5");
    rpcTRE.put("SAMP_NUM_COEFF_10", "+1.657516E+6");
    rpcTRE.put("SAMP_NUM_COEFF_11", "+1.773720E+5");
    rpcTRE.put("SAMP_NUM_COEFF_12", "-5.277249E+4");
    rpcTRE.put("SAMP_NUM_COEFF_13", "+3.404038E+5");
    rpcTRE.put("SAMP_NUM_COEFF_14", "+1.492974E+4");
    rpcTRE.put("SAMP_NUM_COEFF_15", "+2.384937E+5");
    rpcTRE.put("SAMP_NUM_COEFF_16", "+1.576174E+3");
    rpcTRE.put("SAMP_NUM_COEFF_17", "-3.092346E+4");
    rpcTRE.put("SAMP_NUM_COEFF_18", "+8.307716E+4");
    rpcTRE.put("SAMP_NUM_COEFF_19", "-8.854074E+4");
    rpcTRE.put("SAMP_NUM_COEFF_20", "-4.077669E+3");

    rpcTRE.put("SAMP_DEN_COEFF_1", "+8.560069E+8");
    rpcTRE.put("SAMP_DEN_COEFF_2", "-9.431402E+6");
    rpcTRE.put("SAMP_DEN_COEFF_3", "-2.600458E+7");
    rpcTRE.put("SAMP_DEN_COEFF_4", "-1.520253E+7");
    rpcTRE.put("SAMP_DEN_COEFF_5", "+1.983045E+5");
    rpcTRE.put("SAMP_DEN_COEFF_6", "+1.213286E+5");
    rpcTRE.put("SAMP_DEN_COEFF_7", "+3.161597E+5");
    rpcTRE.put("SAMP_DEN_COEFF_8", "-4.535808E+4");
    rpcTRE.put("SAMP_DEN_COEFF_9", "+2.744588E+5");
    rpcTRE.put("SAMP_DEN_COEFF_10", "+7.995991E+4");
    rpcTRE.put("SAMP_DEN_COEFF_11", "-1.081926E+3");
    rpcTRE.put("SAMP_DEN_COEFF_12", "-2.324262E+1");
    rpcTRE.put("SAMP_DEN_COEFF_13", "+6.363693E+1");
    rpcTRE.put("SAMP_DEN_COEFF_14", "-4.086394E+2");
    rpcTRE.put("SAMP_DEN_COEFF_15", "-3.527186E+1");
    rpcTRE.put("SAMP_DEN_COEFF_16", "+8.591534E+1");
    rpcTRE.put("SAMP_DEN_COEFF_17", "-9.908944E+2");
    rpcTRE.put("SAMP_DEN_COEFF_18", "+1.995737E+2");
    rpcTRE.put("SAMP_DEN_COEFF_19", "-1.545197E+3");
    rpcTRE.put("SAMP_DEN_COEFF_20", "-1.442957E+2");

    rpcTRE.put("LINE_NUM_COEFF_1", "+1.705526E+7");
    rpcTRE.put("LINE_NUM_COEFF_2", "+4.409292E+6");
    rpcTRE.put("LINE_NUM_COEFF_3", "-9.999999E+8");
    rpcTRE.put("LINE_NUM_COEFF_4", "+6.029603E+7");
    rpcTRE.put("LINE_NUM_COEFF_5", "+1.088467E+7");
    rpcTRE.put("LINE_NUM_COEFF_6", "-4.977649E+5");
    rpcTRE.put("LINE_NUM_COEFF_7", "+4.250149E+6");
    rpcTRE.put("LINE_NUM_COEFF_8", "-3.148845E+5");
    rpcTRE.put("LINE_NUM_COEFF_9", "+3.036354E+7");
    rpcTRE.put("LINE_NUM_COEFF_10", "-4.262737E+5");
    rpcTRE.put("LINE_NUM_COEFF_11", "-4.712155E+4");
    rpcTRE.put("LINE_NUM_COEFF_12", "+2.661822E+3");
    rpcTRE.put("LINE_NUM_COEFF_13", "-2.302774E+5");
    rpcTRE.put("LINE_NUM_COEFF_14", "-2.449691E+3");
    rpcTRE.put("LINE_NUM_COEFF_15", "+6.146575E+4");
    rpcTRE.put("LINE_NUM_COEFF_16", "-3.203005E+5");
    rpcTRE.put("LINE_NUM_COEFF_17", "-5.104847E+3");
    rpcTRE.put("LINE_NUM_COEFF_18", "+2.006485E+4");
    rpcTRE.put("LINE_NUM_COEFF_19", "-1.298168E+4");
    rpcTRE.put("LINE_NUM_COEFF_20", "+1.209481E+3");

    rpcTRE.put("LINE_DEN_COEFF_1", "+8.729689E+8");
    rpcTRE.put("LINE_DEN_COEFF_2", "-9.618288E+6");
    rpcTRE.put("LINE_DEN_COEFF_3", "-2.651987E+7");
    rpcTRE.put("LINE_DEN_COEFF_4", "-1.550378E+7");
    rpcTRE.put("LINE_DEN_COEFF_5", "+2.022340E+5");
    rpcTRE.put("LINE_DEN_COEFF_6", "+1.237328E+5");
    rpcTRE.put("LINE_DEN_COEFF_7", "+3.224245E+5");
    rpcTRE.put("LINE_DEN_COEFF_8", "-4.625686E+4");
    rpcTRE.put("LINE_DEN_COEFF_9", "+2.798973E+5");
    rpcTRE.put("LINE_DEN_COEFF_10", "+8.154434E+4");
    rpcTRE.put("LINE_DEN_COEFF_11", "-1.103364E+3");
    rpcTRE.put("LINE_DEN_COEFF_12", "-2.370318E+1");
    rpcTRE.put("LINE_DEN_COEFF_13", "+6.489792E+1");
    rpcTRE.put("LINE_DEN_COEFF_14", "-4.167367E+2");
    rpcTRE.put("LINE_DEN_COEFF_15", "-3.597079E+1");
    rpcTRE.put("LINE_DEN_COEFF_16", "+8.761778E+1");
    rpcTRE.put("LINE_DEN_COEFF_17", "-1.010529E+3");
    rpcTRE.put("LINE_DEN_COEFF_18", "+2.035283E+2");
    rpcTRE.put("LINE_DEN_COEFF_19", "-1.575816E+3");
    rpcTRE.put("LINE_DEN_COEFF_20", "-1.471550E+2");

    rpcTRE.put("SUCCESS", "1");
  }

  @Test
  public void worldToImage_nominal() throws Exception {
    RPCCameraModel camera = RPCCameraModelFactory.buildRPCCameraFromMetadata(nominalMetadata);
    assertNotNull(camera);

    Point2D ll = camera.worldToImage(new Coordinate(56.305048278781925, 27.15809908265984, camera.getDefaultElevation()),null);
    assertEquals(0,Math.round(ll.getX()));
    assertEquals(13854,Math.round(ll.getY()));
    Point2D ul = camera.worldToImage(new Coordinate(56.30427165650819, 27.283168257998714, camera.getDefaultElevation()),null);
    assertEquals(0,Math.round(ul.getX()));
    assertEquals(0,Math.round(ul.getY()));
    Point2D ur = camera.worldToImage(new Coordinate(56.42882300863914, 27.28373227420364, camera.getDefaultElevation()),null);
    assertEquals(12327,Math.round(ur.getX()));
    assertEquals(0,Math.round(ur.getY()));
    Point2D lr = new Point2D.Double();
    camera.worldToImage(new Coordinate(56.429460609798625, 27.15866007920754, camera.getDefaultElevation()),lr);
    assertEquals(12327,Math.round(lr.getX()));
    assertEquals(13854,Math.round(lr.getY()));
  }

  @Test
  public void imageToWorld_nominal() throws Exception {
    RPCCameraModel camera = RPCCameraModelFactory.buildRPCCameraFromMetadata(nominalMetadata);
    assertNotNull(camera);

    Coordinate ll = camera.imageToWorld(new Point2D.Double(0,13854));
    assertEquals(56.305048278781925, ll.x, 1E-6);
    assertEquals(27.15809908265984, ll.y, 1E-6);
    assertEquals( -9.0, ll.z, 1E-6);
    Coordinate ul = camera.imageToWorld(new Point2D.Double(0,0));
    assertEquals(56.30427165650819, ul.x, 1E-6);
    assertEquals(27.283168257998714, ul.y, 1E-6);
    assertEquals( -9.0, ul.z, 1E-6);
    Coordinate ur = camera.imageToWorld(new Point2D.Double(12327,0));
    assertEquals(56.42882300863914, ur.x, 1E-6);
    assertEquals(27.28373227420364, ur.y, 1E-6);
    assertEquals( -9.0, ur.z, 1E-6);
    Coordinate lr = new Coordinate();
    camera.imageToWorld(new Point2D.Double(12327,13854),lr);
    assertEquals(56.429460609798625, lr.x, 1E-6);
    assertEquals(27.15866007920754, lr.y, 1E-6);
    assertEquals( -9.0, lr.z, 1E-6);
  }

  @Test
  public void getGSD_nominal() throws Exception {
    RPCCameraModel camera = RPCCameraModelFactory.buildRPCCameraFromMetadata(nominalMetadata);
    assertNotNull(camera);

    assertEquals(1.0,camera.getGSD(new Point2D.Double(0,0)),1E-2);
  }

  @Test
  public void getArea_nominal() throws Exception {
    RPCCameraModel camera = RPCCameraModelFactory.buildRPCCameraFromMetadata(nominalMetadata);
    assertNotNull(camera);

    assertEquals(0.01,camera.getImageAreaInSquareKM(100,100),1E-2);
  }

  @Test
  public void getUpIsUp_nominal() throws Exception {
    RPCCameraModel camera = RPCCameraModelFactory.buildRPCCameraFromMetadata(nominalMetadata);
    assertNotNull(camera);

    assertEquals(116,camera.getUpIsUp(),1);
  }

  @Test
  public void getNorthIsUp_nominal() throws Exception {
    RPCCameraModel camera = RPCCameraModelFactory.buildRPCCameraFromMetadata(nominalMetadata);
    assertNotNull(camera);

    assertEquals(359,camera.getNorthIsUp(),1);
  }
}
