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

import javax.inject.Named;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * A default implementation of the SecurityPolicyFactory
 */
@Named("SecurityPolicyFactory")
public class SecurityPolicyFactoryImpl implements SecurityPolicyFactory, Serializable {

  private static final Map<String, SecurityPolicy> POLICIES = new HashMap<>();

  static {
    POLICIES.put(SecurityPolicies.SECURITY_POLICY_URN_GENERIC, new GenericSecurityPolicy());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public SecurityPolicy getPolicy(String policyRef) throws SecurityPolicyException {
    SecurityPolicy result = POLICIES.get(policyRef);
    if (result == null) {
      throw new SecurityPolicyException("Policy implementation not found for: " + policyRef);
    }
    return result;
  }

}
