/*
 * Licensed to The Leidos Corporation under one or more contributor license agreements.  
 * See the NOTICE file distributed with this work for additional information regarding copyright ownership.
 * The Leidos Corporation licenses this file to You under the Apache License, Version 2.0
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

import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.SampleModel;

/**
 * Created by parrise on 9/20/17.
 * SampleModel for Single banded NIFT images.
 */
public class NITFSingleBandUncompressedSampleModel extends NITFUncompressedSampleModel {


  private boolean isLeftJustified;

  public NITFSingleBandUncompressedSampleModel(int w,
                                               int h,
                                               int nbpp,
                                               int abpp) {

    super(w,h,1,nbpp,abpp);

    this.isLeftJustified = false;
  }

  @Override
  public Object getDataElements(int x, int y, Object obj, DataBuffer data) {

    byte[] bdata;

    if (obj == null) {
      bdata = new byte[numDataElems];
    } else {
      bdata = (byte[])obj;
    }

    int value = getSample(x,y,0,data);
    if (numDataElems == 1) {
      bdata[0] = (byte) (value & 0xFF);
    } else {
      bdata[0] = (byte) (value >> 8);
      bdata[1] = (byte) (value & 0xFF);
    }
    return bdata;
  }

  @Override
  public void setDataElements(int x, int y, Object obj, DataBuffer data) {
    byte[] bdata = (byte[])obj;
    int val = 0;
    for (int i=0;i<bdata.length;i++) {
      val = val << 8;
      val |= bdata[i];
    }
    setSample(x,y,0,val,data);
  }

  @Override
  public int getSample(int x, int y, int b, DataBuffer data) {
    DataBufferByte byteData = (DataBufferByte) data;
    int pixelIndex = y * width + x;
    int bitOffset = pixelIndex * this.abpp;
    int value = this.getBitsAsInt(byteData.getData(),bitOffset,this.abpp);
    if (this.isLeftJustified) {
      value = value >> (this.abpp - this.nbpp);
    }
    return value;
  }

  @Override
  public void setSample(int x, int y, int b, int s, DataBuffer data) {
    DataBufferByte byteData = (DataBufferByte) data;
    int pixelIndex = y * width + x;
    int bitOffset = pixelIndex * this.abpp;
    this.setBitsFromInt(byteData.getData(),bitOffset,this.abpp,s);
  }

  @Override
  public SampleModel createCompatibleSampleModel(int w, int h) {
    return new NITFSingleBandUncompressedSampleModel(w,h,this.nbpp,this.abpp);
  }

  @Override
  public SampleModel createSubsetSampleModel(int[] bands) {
    // TODO: Check to make sure bands is a valid value?
    return new NITFSingleBandUncompressedSampleModel(width,height,this.nbpp,this.abpp);
  }

  @Override
  public DataBuffer createDataBuffer() {
    int sizeBytes = (width * height * abpp + 7)/8;
    return new DataBufferByte(sizeBytes);
  }

}
