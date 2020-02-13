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

import java.text.ParseException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * Utility class that organizes paired tokens by their namespace groups. The tokens themselves are
 * parsed and then placed into a map keyed by namespace.
 */
public class PairedTokenGrouping extends HashMap<String, Set<String>> {

  /**
   * Adds all of the paired tokens to the
   * 
   * @param pairedTokens
   */
  public void addAll(Collection<PairedToken> pairedTokens) {
    for (PairedToken token : pairedTokens) {
      Set<String> valueSet = this.get(token.getPrefix());
      if (valueSet == null) {
        valueSet = new TreeSet<>();
        this.put(token.getPrefix(), valueSet);
      }
      valueSet.add(token.getValue());
    }
  }

  /**
   * Returns the set of values associated with the namespace or the empty set if no values are in
   * this grouping.
   *
   * @param key the namespace to retrieve
   * @return the set of values or an empty set
   */
  public Set<String> getWithDefault(String key) {
    Set<String> result = get(key);
    if (result == null) {
      result = Collections.emptySet();
    }
    return result;
  }

  /**
   * Returns the set of prefixed values associated with the namespace or the empty set if no values
   * are in this grouping.
   *
   * @param key the namespace to retrieve
   * @return the set of values or an empty set
   */
  public Set<String> getPairWithDefault(String key) {
    Set<String> values = getWithDefault(key);
    // Now prepend the namespace
    Set<String> result = new HashSet<String>();
    for (String s : values) {
      result.add(key + ":" + s);
    }
    return result;
  }

  /**
   * Converts this paired token group back into a flat set of paired tokens.
   *
   * @return the paired tokens
   */
  public Set<PairedToken> flatten() {
    Set<PairedToken> result = new HashSet<>();
    for (Map.Entry<String, Set<String>> me : this.entrySet()) {
      String qualifier = me.getKey();
      for (String value : me.getValue()) {
        result.add(new PairedToken(qualifier, value));
      }
    }
    return result;
  }

  /**
   * This utility method constructs a paired token grouping from a set of Strings. See the
   * PairedToken(String) constructor.
   *
   * @param encodedPairs the encoded paired token strings
   * @return the paired token grouping
   */
  public static PairedTokenGrouping fromStrings(Collection<String> encodedPairs)
      throws ParseException {

    Set<PairedToken> pairedTokens = new HashSet<>();
    for (String s : encodedPairs) {
      pairedTokens.add(new PairedToken(s));
    }

    PairedTokenGrouping result = new PairedTokenGrouping();
    result.addAll(pairedTokens);
    return result;
  }
}
