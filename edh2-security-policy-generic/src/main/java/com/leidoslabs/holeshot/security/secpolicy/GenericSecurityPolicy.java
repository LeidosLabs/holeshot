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

import com.leidoslabs.holeshot.security.edh2.PairedTokenGrouping;
import com.leidoslabs.holeshot.security.edh2.PartialEDH2Group;

import java.io.Serializable;
import java.text.ParseException;
import java.util.Set;

public class GenericSecurityPolicy extends SecurityPolicyBase implements Serializable {

    /**
     * Get the reference ID associated with this security policy.
     *
     * @return the URN
     */
    @Override
    public String getPolicyRef() {
        return SecurityPolicies.SECURITY_POLICY_URN_GENERIC;
    }


    private static final String[] SENSITIVITY_NAMESPACES = {ControlSetNamespaces.CLS.toString(),
            ControlSetNamespaces.LBL.toString()};

    private static final String[] SHAREABILITY_NAMESPACES = {ControlSetNamespaces.ENTITY.toString()};

    public enum SIPValues {
        U, // UNRESTRICTED
        R  //Restricted
    }

    public enum ControlSetNamespaces {
        CLS, //Proctection Class
        LBL, //Custom SIP label, must ensure someone has this in their attributes.
        ENTITY
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String buildVisibilityExpression(PartialEDH2Group edh2Resource) throws SecurityPolicyException {


        if (!edh2Resource.getPolicyRef().contains(getPolicyRef())) {
            throw new SecurityPolicyException("Attempt to apply security policy not matched in HDR");
        }

        return encodeAccessLogic(edh2Resource.getControlSet(), SENSITIVITY_NAMESPACES,
                SHAREABILITY_NAMESPACES);
    }



    /**
     * {@inheritDoc}
     */
    @Override
    protected String[] getSensitivityNamespaces() {
        return GenericSecurityPolicy.SENSITIVITY_NAMESPACES;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected String[] getShareabilityNamespaces() {
        return GenericSecurityPolicy.SHAREABILITY_NAMESPACES;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean hasRankedSensitivity() {
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected String[] getRankedSensitivityNamespaces() {
        return new String[]{ControlSetNamespaces.CLS.toString()};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected int getSensitivityRank(String sipValue) throws IllegalArgumentException{
        SIPValues sip = SIPValues.valueOf(sipValue);
        if (sip == SIPValues.R) return 1;
        if (sip == SIPValues.U) return 0;
        return -1;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected String getPolicyURN() {
        return SecurityPolicies.SECURITY_POLICY_URN_GENERIC;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected String getPolicyName() {
        return "GENERIC";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateControlSet(Set<String> controlSets, boolean portionMark, ValidationResult result) {
        try {
            PairedTokenGrouping controlSetGroups = PairedTokenGrouping.fromStrings(controlSets);

            String entity = ControlSetNamespaces.ENTITY.toString();
            String cls = ControlSetNamespaces.CLS.toString();
            String lbl = ControlSetNamespaces.LBL.toString();

            for (String key : controlSetGroups.keySet()) {
                try {
                    ControlSetNamespaces ns = ControlSetNamespaces.valueOf(key);
                } catch (IllegalArgumentException e) {
                    result.reject("ControlSet paired token namespace \"" + key + "\" is not defined.");
                }
            }

            if (!controlSetGroups.containsKey(cls)) {
                result.reject("ControlSet does not include a CLS tag, it must have exactly one of CLS:U or CLS:R tags.");
            }else if (controlSetGroups.get(cls).size() != 1) {
                result.reject("ControlSet has more than 1 CLS tag, it must have exactly one of CLS:U or CLS:R tags.");
            }else if (!(controlSetGroups.get(cls).contains("U") ||  controlSetGroups.get(cls).contains("R"))) {
                result.reject("ControlSet contains invalid CLS tag of CLS:" + controlSetGroups.get(cls).iterator().next() +
                        " it must have exactly one of CLS:U or CLS:R tags.");
            }else if (controlSetGroups.get(cls).contains("U") && controlSetGroups.size() > 1) {
                result.reject("ControlSet contains CLS:U and at least one other tag, CLS:U data can't be tagged with other tags.");
            }
        } catch (ParseException e) {
            result.reject("ControlSet contains an invalid paired token.");
        }
    }

}
