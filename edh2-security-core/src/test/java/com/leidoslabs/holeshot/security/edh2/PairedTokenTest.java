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

package com.leidoslabs.holeshot.security.edh2;

import org.junit.Test;

import static org.junit.Assert.*;

import java.text.ParseException;

public class PairedTokenTest {

  @Test
  public void testTokenParsing() throws Exception {
    PairedToken pt = new PairedToken("BASE:foo.bar.baz");
    assertEquals("BASE", pt.getPrefix());
    assertEquals("foo.bar.baz", pt.getValue());
  }

  @Test
  public void testToString() throws Exception {
    PairedToken pt = new PairedToken("FOO","BAR");
    assertEquals("FOO",pt.getPrefix());
    assertEquals("BAR",pt.getValue());
    assertEquals("FOO:BAR",pt.toString());
  }

  @Test(expected=ParseException.class)
  public void invalidPairParseException() throws Exception {
    PairedToken pt = new PairedToken("FOO");
  }

}
