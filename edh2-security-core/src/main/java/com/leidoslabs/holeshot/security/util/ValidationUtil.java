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

package com.leidoslabs.holeshot.security.util;

import com.leidoslabs.holeshot.security.edh2.FullEDH2Group;
import com.leidoslabs.holeshot.security.exception.ValidationException;
import com.leidoslabs.holeshot.security.secpolicy.SecurityPolicy;
import com.leidoslabs.holeshot.security.secpolicy.SecurityPolicyException;
import com.leidoslabs.holeshot.security.secpolicy.SecurityPolicyFactory;

/**
 * This class contains common functions used when enforcing standards.
 */
public class ValidationUtil {

  /**
   * This function validates a resource containing EDH2 markings against the appropriate security
   * policy implementation.
   *
   * @param securityPolicyFactory factory providing access to policy implementations
   * @param resource the EDH2 tagged resource to validate
   * @throws ValidationException if the values in the EHD2 header are inconsistent with the policy
   */
  public static final void validateEDH2Header(SecurityPolicyFactory securityPolicyFactory,
      FullEDH2Group resource) throws ValidationException {
    if (resource.getPolicyRef().isEmpty()) {
      throw new ValidationException("EDH2 header does not have a PolicyRef field set.");
    }
    try {
      for (String secPolicyRef : resource.getPolicyRef()) {
        // TODO: Add support for "exempted" policy references;
        SecurityPolicy policy = securityPolicyFactory.getPolicy(secPolicyRef);
        if (policy != null) {
          SecurityPolicy.ValidationResult validationResult = policy.validateResource(resource);
          if (!validationResult.isValid()) {
            throw new ValidationException(
                "EDH2 header does not validate according to security policy rules",
                validationResult.getErrors());
          }
        } else {
          throw new ValidationException("PolicyRef does not exist " + secPolicyRef);
        }
      }
    } catch (SecurityPolicyException spe) {
      throw new ValidationException("EDH2 header does not comply with security policy rules", spe);
    }
  }
}
