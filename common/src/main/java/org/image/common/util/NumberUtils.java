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
package org.image.common.util;

import java.util.regex.Pattern;

/**
 * Utility class for parsing and unboxing numeric types
 */
public class NumberUtils {
   private static final String DIGITS     = "(\\p{Digit}+)";
   private static final String HEX_DIGITS  = "(\\p{XDigit}+)";
   private static final String EXP        = "[eE][+-]?"+DIGITS;
   private static final String FP_REGEX    =
       ( //"[\\x00-\\x20]*"+  // Optional leading "whitespace"
        "[+-]?(" + // Optional sign character
        "NaN|" +           // "NaN" string
        "Infinity|" +      // "Infinity" string
        "((("+DIGITS+"(\\.)?("+DIGITS+"?)("+EXP+")?)|"+
        "(\\.("+DIGITS+")("+EXP+")?)|"+
        "((" +
         "(0[xX]" + HEX_DIGITS + "(\\.)?)|" +
         "(0[xX]" + HEX_DIGITS + "?(\\.)" + HEX_DIGITS + ")" +
         ")[pP][+-]?" + DIGITS + "))" +
        "[fFdD]?))"); // +
        //"[\\x00-\\x20]*");// Optional trailing "whitespace"

   private static final Pattern DOUBLE_PATTERN = Pattern.compile(FP_REGEX);
   private static final Pattern INTEGER_PATTERN = Pattern.compile("[+-]?[0-9]+");   
   
   public static boolean isDouble(String testString) {
      return DOUBLE_PATTERN.matcher(testString).matches();
   }
   public static boolean isInteger(String testString) {
      return INTEGER_PATTERN.matcher(testString).matches();
   }
   public static int getInteger(Object source) {
      int result = -1;
      if (source instanceof Integer) {
         result = ((Integer)source).intValue();
      } else if (source != null) {
         result = Integer.parseInt(source.toString());
      }
      return result;
   }
   public static long getLong(Object source) {
      long result = -1;
      if (source instanceof Long) {
         result = ((Long)source).longValue();
      } else if (source != null) {
         result = Long.parseLong(source.toString());
      }
      return result;
   }
   public static double getDouble(Object source) {
      double result = -1;
      if (source instanceof Double) {
         result = ((Double)source).doubleValue();
      } else if (source != null) {
         result = Double.parseDouble(source.toString());
      }
      return result;
   }
   public static float getFloat(Object source) {
      float result = -1;
      if (source instanceof Float) {
         result = ((Float)source).floatValue();
      } else if (source != null) {
         result = Float.parseFloat(source.toString());
      }
      return result;
   }
   
}
