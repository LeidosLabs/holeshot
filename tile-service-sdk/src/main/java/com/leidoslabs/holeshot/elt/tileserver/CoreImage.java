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
package com.leidoslabs.holeshot.elt.tileserver;

import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;

public class CoreImage {
  private BufferedImage bufferedImage;
  
  public CoreImage(BufferedImage bufferedImage) {
    this.bufferedImage = bufferedImage;
  }
  public BufferedImage getBufferedImage() {
    return bufferedImage;
  }
  
  /**
   * 
   * @return
   */
  public long getSizeInBytes() {
    // mat and bufferedImage share bytes, so we only have to account for them once
    long result = 0;
    if (bufferedImage != null) {
      DataBuffer dataBuffer = bufferedImage.getData().getDataBuffer();
      // Each bank element in the data buffer is a 32-bit integer
      result = ((long) dataBuffer.getSize()) * 4l;
    }
    return result;
  }
  
}
