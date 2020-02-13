package com.leidoslabs.holeshot.imaging.nitf;

import org.junit.Test;

import java.awt.image.SampleModel;

import static org.junit.Assert.assertEquals;

/**
 * Created by parrise on 9/20/17.
 */
public class NITFSingleBandUncompressedSampleModelTest {

  @Test
  public void createDataBuffer_sizeBoudaryConditions() throws Exception {
    SampleModel sm = new NITFSingleBandUncompressedSampleModel(1, 1, 7, 7);
    assertEquals(1, sm.createDataBuffer().getSize());

    sm = new NITFSingleBandUncompressedSampleModel(3, 1, 3, 3);
    assertEquals(2, sm.createDataBuffer().getSize());

    sm = new NITFSingleBandUncompressedSampleModel(1, 3, 8, 8);
    assertEquals(3, sm.createDataBuffer().getSize());
  }
}
