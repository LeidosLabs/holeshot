/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.leidoslabs.holeshot.security.reuse;

/**
 * A utility method for comparing byte sequences.
 */
public class WritableComparator {


  /**
   * Lexicographic order of binary data.
   *
   * @param b1 an array of byte.
   * @param s1 a int.
   * @param l1 a int.
   * @param b2 an array of byte.
   * @param s2 a int.
   * @param l2 a int.
   * @return a int.
   */
  public static int compareBytes(byte[] b1, int s1, int l1,
                                 byte[] b2, int s2, int l2) {
    return LexicographicalComparerHolder.compareBytes(
      b1, s1, l1, b2, s2, l2);
  }
  
}
