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

package com.leidoslabs.holeshot.security.secpolicy;

/**
 * A security policy factory constructs an implementation of a specific security policy given a
 * policy reference URN.
 */
public interface SecurityPolicyFactory {

  /**
   * Construct and retrieve the appropriate security policy.
   *
   * @param policyRef the URN identifying the policy
   * @return the security policy implementation
   */
  SecurityPolicy getPolicy(String policyRef) throws SecurityPolicyException;

}
