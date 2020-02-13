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

import com.google.common.base.Strings;

/**
 * A combination of a prefix with another token separated by a colon. See Smart Data Enterprise Data
 * Header Data Encoding Specification version 2.0.2 dated 15 January 2015.
 */
public class PairedToken {

  private String prefix;
  private String value;

  /**
   * Construct a paired token from an independent prefix and value.
   *
   * @param prefix the prefix
   * @param value the value
   */
  public PairedToken(String prefix, String value) {
    if (Strings.isNullOrEmpty(prefix)) {
      throw new IllegalArgumentException("PairedToken prefix cannot be empty.");
    }
    if (Strings.isNullOrEmpty(value)) {
      throw new IllegalArgumentException("PairedToken value cannot be empty.");
    }
    this.prefix = prefix;
    this.value = value;
  }

  /**
   * Construct a paired token by parsing an encoded <prefix>:<value> string.
   *
   * @param prefixedValue the encoded token
   * @throws ParseException if the colon is missing or either the values is empty
   */
  public PairedToken(String prefixedValue) throws ParseException {
    int colonIndex = prefixedValue.indexOf(":");
    if (colonIndex == -1) {
      throw new ParseException("PairedTokens must be of the form <prefix>:<value>.", colonIndex);
    }
    this.prefix = prefixedValue.substring(0, colonIndex);
    if (Strings.isNullOrEmpty(prefix)) {
      throw new ParseException("PairedToken is missing a prefix " + prefixedValue, 0);
    }
    this.value = prefixedValue.substring(colonIndex + 1);
    if (Strings.isNullOrEmpty(value)) {
      throw new ParseException("PairedToken is missing a value " + prefixedValue, colonIndex + 1);
    }
  }

  public String getPrefix() {
    return prefix;
  }

  public void setPrefix(String prefix) {
    this.prefix = prefix;
  }

  public String getValue() {
    return value;
  }

  public void setValue(String value) {
    this.value = value;
  }

  @Override
  public String toString() {
    return String.format("%s:%s", this.prefix, this.value);
  }
}
