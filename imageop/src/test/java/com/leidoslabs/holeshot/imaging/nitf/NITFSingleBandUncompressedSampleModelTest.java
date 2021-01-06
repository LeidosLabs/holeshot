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
