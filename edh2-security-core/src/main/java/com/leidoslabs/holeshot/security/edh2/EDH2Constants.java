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

/**
 * This class defines constants for the Next Generation Enterprise Data Header 
 * (EDH2) Data Encoding Specification (DES) properties used by OMaaS.
 * 
 * TODO: Look for community provided libraries that help implement EDH 
 * compliant classes.
 * 
 * @author parrise
 */
public final class EDH2Constants {
  public static final String EDH_PREFIX = "edh";
  public static final String EDH_IDENTIFIER = "Identifier";
  public static final String EDH_CREATE_DATE_TIME = "CreateDateTime";
  public static final String EDH_RESPONSIBLE_ENTITY = "ResponsibleEntity";
  public static final String EDH_DATA_SET = "DataSet";
  public static final String EDH_AUTH_REF = "AuthRef";
  public static final String EDH_POLICY_REF = "PolicyRef";
  public static final String EDH_POLICY = "Policy";
  public static final String EDH_CONTROL_SET = "ControlSet";
  public static final String EDH_IS_RESOURCE = "IsResource";
  public static final String EDH_IS_EXTERNAL = "IsExternal";
  public static final String EDH_SPECIFICATION = "Specification";
  
  private EDH2Constants() {
  }
}
