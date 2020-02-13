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

import com.leidoslabs.holeshot.security.edh2.FullEDH2Group;
import com.leidoslabs.holeshot.security.edh2.PartialEDH2Group;

import java.util.HashSet;
import java.util.Set;

import com.leidoslabs.holeshot.security.auth.EmptyAccessStringException;

/**
 * Static utility methods for manipulating security data
 */
public class SecurityPolicyUtil {

  /**
   * This method performs an intersection of two sets of security access
   * strings.
   *
   * @param lowerControlSet
   *          the EDH2 controlSet of lower classification
   * @param controlSet
   *          the EDH2 controlSet
   * @return a set of Strings representing the intersection of the lower
   *         classification and the controlSet
   */
  public static Set<String> generateLowerClassification(Set<String> lowerControlSet,
          Set<String> controlSet) {
    Set<String> returnSet = new HashSet<String>();
    returnSet.addAll(controlSet);
    returnSet.retainAll(lowerControlSet);

    if (returnSet.isEmpty()) {
      throw new EmptyAccessStringException(
              "Intersection of allowed accesses and requested accesses yields a blank access string.");
    }
    return returnSet;
  }

  /**
   * This method validates the portion markings of a resource against all of the security policies
   * listed in the PolicyRef field.
   *
   * @param securityPolicyFactory a factory used to create the requisite policies
   * @param resource the resource to validate
   * @param isFull true if the resource should be checked against the full EDH2 group rules; this
   *               parameter has no effect if the resource is only a partial EDH2 group.
   * @return a validation result containing any errors found
   */
  public static SecurityPolicy.ValidationResult validateAllPolicies(
      SecurityPolicyFactory securityPolicyFactory, PartialEDH2Group resource, boolean isFull) {
    SecurityPolicy.ValidationResult result = new SecurityPolicy.ValidationResult();

    if (resource.getPolicyRef().isEmpty()) {
      result.reject("EDH2 header does not have a PolicyRef field set.");
    }

    try {
      for (String secPolicyRef : resource.getPolicyRef()) {
        // TODO: Add support for "exempted" policy references;
        SecurityPolicy policy = securityPolicyFactory.getPolicy(secPolicyRef);
        if (policy != null) {
          SecurityPolicy.ValidationResult validationResult;
          if (isFull && resource instanceof FullEDH2Group) {
            validationResult = policy.validateResource((FullEDH2Group) resource);
          } else {
            validationResult = policy.validatePortion(resource);
          }
          if (!validationResult.isValid()) {
            for (String error : validationResult.getErrors()) {
              result.reject(error);
            }
          }
        } else {
          result.reject("Unable to find implementation of PolicyRef: " + secPolicyRef);
        }
      }
    } catch (SecurityPolicyException spe) {
      result.reject("EDH2 header does not comply with security policy rules: " + spe.getMessage());
    }

    return result;
  }

}
