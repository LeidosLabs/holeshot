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
package org.image.common.cache;

public interface Cacheable {
   /**
   * Retrieves the approximate size of this object in bytes. Implementors are encouraged to calculate the exact size
   * for smaller objects, but use approximate values for objects that include such large components that the
   * approximation would produce an error so small that the extra computation would be wasteful.
   *
   * @return this <code>Cacheable</code> object's size in bytes
   */
  long getSizeInBytes();
}
