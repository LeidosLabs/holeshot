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

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * This class provides a basis for implementing a set of security rules that operate on EDH2
 * metadata.
 */
public interface SecurityPolicy {

  /**
   * Get the reference ID associated with this security policy.
   *
   * @return the URN
   */
  public abstract String getPolicyRef();

  /**
   * This method constructs a boolean visibility expression from the control set in this EDH2
   * resource. The interpretation of the control set paired tokens is specific to each
   * implementation of a security policy but a typical example of processing would be along the
   * lines of: edhControlSet: "CLS:U SCI:FOO COI:BAR CTRY:USA CTRY:GBR" -> "U&FOO&BAR&(USA|GBR)".
   *
   * @param edh2Resource the EDH2 resource to represent as a visibility expression
   * @return the visibility expression
   * @throws SecurityPolicyException for invalid paired tokens in ControlSet
   */
  public abstract String buildVisibilityExpression(PartialEDH2Group edh2Resource)
      throws SecurityPolicyException;

  /**
   * This method attempts to combine the security controls and policy references used by one or
   * more portion markings into an aggregate set for use by the overall resource.
   *
   * @param rollup   the markings for the overall resource; this object is modified by this method.
   * @param portions the individual portion marks to roll up
   * @throws SecurityPolicyException if invalid markings or an invalid state is reached during roll-up
   */
  public abstract void rollUpPortions(FullEDH2Group rollup, PartialEDH2Group... portions)
      throws SecurityPolicyException;

  /**
   * This method analyzes a full EDH2 group (resource marking) and assesses whether or not the
   * values are valid given the rules of the security policy.
   *
   * @param edh2Resource he EDH2 resource markings to validate
   * @return a validation result containing an assessment and rationale
   */
  public abstract ValidationResult validateResource(FullEDH2Group edh2Resource);

  /**
   * This method analyzes a partial EDH2 group (portion marking) and assesses whether or not the
   * values are valid given the rules of the security policy.
   *
   * @param edh2Portion the EDH2 portion markings to validate
   * @return a validation result containing an assessment and rationale
   */
  public abstract ValidationResult validatePortion(PartialEDH2Group edh2Portion);


  /**
   * A utility class allowing the boolean result of a validation check to be combined with
   * one or more reasons the validation failed.
   */
  public static class ValidationResult {
    private boolean valid = true;
    private Set<String> errors = null;

    /**
     * Flag this as a failed validation result and add a reason for the railure.
     *
     * @param reason a message describing the reason for the failure
     */
    public void reject(String reason) {
      valid = false;
      if (errors == null) {
        errors = new HashSet<>();
      }
      errors.add(reason);
    }

    /**
     * Gets the overall validation result; if this flag is false then any error messages can be
     * accessed via getErrors().
     *
     * @return true if valid; false otherwise
     */
    public boolean isValid() {
      return valid;
    }

    /**
     * Gets the set of errors encountered during the validation attempt.
     *
     * @return the set of errors
     */
    public Set<String> getErrors() {
      if (errors == null) {
        return Collections.emptySet();
      }
      return errors;
    }
  }
}
