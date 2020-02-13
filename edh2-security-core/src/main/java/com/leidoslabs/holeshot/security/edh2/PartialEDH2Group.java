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

import java.util.Set;

/**
 * Interface definition for resources conforming to the Smart Data Enterprise Data Header Data
 * Encoding Specification (Smart Data EDH DES). These are the fields used by a partial,
 * portion-level, marking of the data.
 *
 * Implementation is based on Version 2.0.2 of the document dated 15 January 2015
 */
public interface PartialEDH2Group {

    /**
     * @return Set of URN Strings, Security Authorization Link Reference
     */
  public Set<String> getAuthRef();

    /**
     * @return Set of URN Strings, Security Policy Link Reference
     */
  public Set<String> getPolicyRef();

    /**
     * @return Set of paired token strings, classification markings of the object
     */
  public Set<String> getControlSet();
}
