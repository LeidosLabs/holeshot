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

import java.util.Collection;
import java.util.LinkedHashSet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;


public class PairedTokenGroupingTest {

  @Test
  public void testGroupByQualifier() throws Exception {
    Collection<PairedToken> tokens = new LinkedHashSet<>();
    tokens.add(new PairedToken("CLS:U"));
    tokens.add(new PairedToken("COI:FOO"));
    tokens.add(new PairedToken("COI:BAR"));
    tokens.add(new PairedToken("COI:FOO"));

    PairedTokenGrouping result = new PairedTokenGrouping();
    result.addAll(tokens);
    assertNotNull(result);
    assertEquals(2,result.size());
    assertEquals(1,result.get("CLS").size());
    assertTrue(result.get("CLS").contains("U"));
    assertEquals(2,result.get("COI").size());
    assertTrue(result.get("COI").contains("FOO"));
    assertTrue(result.get("COI").contains("BAR"));

    tokens.clear();
    result = new PairedTokenGrouping();
    result.addAll(tokens);
    assertNotNull(result);
    assertEquals(0,result.size());
  }
}
