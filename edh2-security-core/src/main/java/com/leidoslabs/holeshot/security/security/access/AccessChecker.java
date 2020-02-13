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

package com.leidoslabs.holeshot.security.security.access;

import java.text.ParseException;

import com.leidoslabs.holeshot.security.edh2.PartialEDH2Group;
import com.leidoslabs.holeshot.security.secpolicy.SecurityPolicyException;

/**
 * Access Checker used to see if the user has the appropriate credentials for the access string.
 */
public interface AccessChecker {
  
  /**
   * Checks if the user has all the credentials required for the access string.
   * 
   * @param userCredentials The user's credentials (e.g.. "CLS:TS,CLS:S,CLS:C,CLS:U,COI:A4,COI:A5,CTRY:USA")
   * @param edh2Resource containing edhControlSet to check
   * @return true if the user has all accesses for the access string. Otherwise, false.
   * @throws SecurityPolicyException for invalid paired tokens in ControlSet
   * @throws ParseException if can't parse user credentials or edh2Resource controlSet
   */
  public boolean hasAccess(String userCredentials, PartialEDH2Group edh2Resource)
      throws SecurityPolicyException, ParseException;

}
