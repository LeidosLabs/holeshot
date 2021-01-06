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

import java.nio.ByteBuffer;

/**
 * Created by parrise on 9/21/17.
 */
public class BitTwiddlingTest {

  private static final int[] BIT_MASK = {0xFF, 0x7F, 0x3F, 0x1F, 0xF, 0x07, 0x03, 0x01};

  protected static int getBitsAsInt(byte[] bytes, int offset, int len) {

    int val = 0;
    int bys = offset / 8;
    int bye = (offset + len) / 8;
    int bis = offset % 8;
    int bie = (offset + len) % 8;
    for (int b=bys;b<=bye;b++) {
      val = val << 8;
      val += bytes[b] & (b == bys ? BIT_MASK[bis] : 0xFF);
    }
    val = val >> (8 - bie);
    return val;
  }

  protected static void setBitsFromInt(byte[] bytes, int offset, int len, int val) {

    int bie = (offset + len) % 8;
    val = val << bie;

    int bys = offset / 8;
    int bye = (offset + len) / 8;
    for (int b=bye;b>=bys;b--) {
      bytes[b] |= (val & 0xFF);
      val = val >> 8;
    }

  }


  public static String intToString(int number, int groupSize) {
    StringBuilder result = new StringBuilder();

    for(int i = 31; i >= 0 ; i--) {
      int mask = 1 << i;
      result.append((number & mask) != 0 ? "1" : "0");

      if (i % groupSize == 0)
        result.append(" ");
    }
    result.replace(result.length() - 1, result.length(), "");

    return result.toString();
  }

  @Test
  public void doit() {

    byte[] testBytes = { 0x00, 0x02, 0x0F, 0x02 };

    int testInt = ((testBytes[0] & 0xFF) << 24) + ((testBytes[1] & 0xFF) << 16) + ((testBytes[2] & 0xFF) << 8) + (testBytes[3] & 0xFF);
    System.out.println(intToString(testInt,8));

    System.out.println("");
    System.out.println(intToString(getBitsAsInt(testBytes,16,8),8));
    System.out.println(intToString(getBitsAsInt(testBytes,12,9),8));
    System.out.println(intToString(getBitsAsInt(testBytes,23,8),8));
    System.out.println(intToString(getBitsAsInt(testBytes,14,17),8));
  }

  @Test
  public void doit2() {

    byte[] testBytes = { 0x00, 0x02, 0x0F, 0x02 };

    int testInt = ((testBytes[0] & 0xFF) << 24) + ((testBytes[1] & 0xFF) << 16) + ((testBytes[2] & 0xFF) << 8) + (testBytes[3] & 0xFF);
    System.out.println(intToString(testInt,8));

    System.out.println("");
    System.out.println(intToString(getBitsAsInt(testBytes,16,8),8));
    System.out.println(intToString(getBitsAsInt(testBytes,12,9),8));
    System.out.println(intToString(getBitsAsInt(testBytes,23,8),8));
    System.out.println(intToString(getBitsAsInt(testBytes,14,17),8));
  }

}
