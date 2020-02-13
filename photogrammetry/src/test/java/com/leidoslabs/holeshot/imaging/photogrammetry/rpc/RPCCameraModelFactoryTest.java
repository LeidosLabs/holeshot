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
