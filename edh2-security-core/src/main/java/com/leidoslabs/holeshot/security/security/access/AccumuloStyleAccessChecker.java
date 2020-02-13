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

import javax.inject.Inject;
import javax.inject.Named;

import com.leidoslabs.holeshot.security.edh2.EDH2Constants;
import com.leidoslabs.holeshot.security.edh2.PartialEDH2Group;
import com.leidoslabs.holeshot.security.reuse.Authorizations;
import com.leidoslabs.holeshot.security.reuse.ColumnVisibility;
import com.leidoslabs.holeshot.security.reuse.VisibilityEvaluator;
import com.leidoslabs.holeshot.security.reuse.VisibilityParseException;
import com.leidoslabs.holeshot.security.secpolicy.SecurityPolicy;
import com.leidoslabs.holeshot.security.secpolicy.SecurityPolicyException;
import com.leidoslabs.holeshot.security.secpolicy.SecurityPolicyFactory;

import com.google.common.collect.Iterables;

/**
 * Converts user credentials to accumulo-style credentials, and the resources edhControlSet to
 * accumulo-style visibility string expression, then compares to see if the user has access for the
 * access string. Using the accumulo style to re-use proven implementation.
 */
@Named
public class AccumuloStyleAccessChecker implements AccessChecker {

  /**
   * Used for getting the policy for building visibility expressions (required for knowledge of
   * SENSITIVITY and SHAREABILITY namespaces
   */
  private final SecurityPolicyFactory registry;

  /**
   * Constructor
   * 
   * @param registry for getting security policy
   */
  @Inject
  public AccumuloStyleAccessChecker(SecurityPolicyFactory registry) {
    this.registry = registry;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.omaas.security.AccessChecker#hasAccess(java.lang.String, java.util.Set)
   */
  @Override
  public boolean hasAccess(final String userCredentials, PartialEDH2Group edh2Resource)
      throws SecurityPolicyException, VisibilityParseException {
    boolean response = true;
    
    if (Iterables.isEmpty(edh2Resource.getPolicyRef())) {
      throw new SecurityPolicyException(EDH2Constants.EDH_POLICY_REF + "not defined");
    }
    
    // For each policy confirm has access
    for (String secPolicyRef : edh2Resource.getPolicyRef()) {
      // TODO: Add support for "exempted" policy references;
      SecurityPolicy policy = registry.getPolicy(secPolicyRef);
      if (policy != null) {
        // Create evaluator based on users authorizations
        Authorizations auths = new Authorizations(userCredentials.split(","));
        VisibilityEvaluator visibilityEvaluator = new VisibilityEvaluator(auths);

        // Create visibility of resource
        ColumnVisibility columnVisibility =
            new ColumnVisibility(policy.buildVisibilityExpression(edh2Resource));

        // Evaluate if user credentials should have visibility to resource and intersect with
        // response from each policy
        response = response && visibilityEvaluator.evaluate(columnVisibility);

      } else {
        throw new SecurityPolicyException(EDH2Constants.EDH_POLICY + "[" + secPolicyRef
            + "] does not exist");
      }
    }
    return response;
  }
}
