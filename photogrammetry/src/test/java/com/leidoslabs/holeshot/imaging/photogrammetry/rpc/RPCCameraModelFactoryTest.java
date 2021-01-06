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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.leidoslabs.holeshot.imaging.photogrammetry.CameraModel;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static junit.framework.TestCase.assertNotNull;

public class RPCCameraModelFactoryTest {

  @Test
  public void buildRPCCameraFromMetadata_rpc00b() throws Exception {

    ObjectMapper mapper = new ObjectMapper();
    Object o = mapper.readValue(Thread.currentThread().getContextClassLoader().getResourceAsStream("20040129070249000.json"),Object.class);
    CameraModel cameraModel = RPCCameraModelFactory.buildRPCCameraFromMetadata((Map<String,Object>)o);
    assertNotNull(cameraModel);

  }
}
