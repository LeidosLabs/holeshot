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

package com.leidoslabs.holeshot.security.auth;

/**
 * These should probably be configurable between both the ES plugin and the omaas software.
 * 
 * @author jonesw
 *
 */
public interface SecurityConstants {
  public static final String SCRIPT_NAME = "accumulosecurity";
  public static final String LANGUAGE = "native";
  public static final String VIZ_PARAM_FIELD = "visibility-field";
  public static final String VIZ_PARAM_VALUE = "security-visibility";
  public static final String AUTH_PARAM_FIELD = "authorizations";
}
