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

import com.google.common.base.Joiner;

import com.leidoslabs.holeshot.security.edh2.FullEDH2Group;
import com.leidoslabs.holeshot.security.edh2.PairedToken;
import com.leidoslabs.holeshot.security.edh2.PairedTokenGrouping;
import com.leidoslabs.holeshot.security.edh2.PartialEDH2Group;
import com.leidoslabs.holeshot.security.guide.GUIDE;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.URISyntaxException;
import java.text.ParseException;
import java.util.*;

/**
 * A common base class containing utilities useful during the implementation of various security
 * policies.
 */
public abstract class SecurityPolicyBase implements SecurityPolicy {

    private static final Logger LOGGER = LoggerFactory.getLogger(SecurityPolicyBase.class);

    protected SecurityPolicyBase() {
    }

    /**
     * This helper method encodes a control set into a visibiltiy string using the paired token
     * namespaces to determine how the boolean expression should be constructed. This method builds a
     * visibility expression where all the values from the sensitivity namespaces are combined with
     * ANDs and the values from the shareability namespaces are combined with ORs within that
     * namespace (i.e. a user must have accesses for all of the sensitivity values and at least one
     * access from each sharability namespace group.)
     * <p>
     * For example if "CLS", "SCI" and "COI" are in the sensitivity set and "CTRY" is the shareability
     * set "CLS:U SCI:FOO COI:BAR CTRY:USA CTRY:GBR" would be encoded as "U&FOO&BAR&(USA|GBR)".
     *
     * @param controlSet
     * @param sensitivityNamespaces
     * @param shareabilityNamespaces
     * @return
     * @throws SecurityPolicyException for invalid paired tokens in ControlSet
     */
    protected String encodeAccessLogic(Set<String> controlSet,
                                       String[] sensitivityNamespaces, String[] shareabilityNamespaces)
            throws SecurityPolicyException {

        try {
            PairedTokenGrouping grouping = PairedTokenGrouping.fromStrings(controlSet);

            List<String> visibilityExpressions = new LinkedList<>();
            for (String namespace : sensitivityNamespaces) {
                visibilityExpressions.addAll(grouping.getPairWithDefault(namespace));
            }

            encodeSharabilityAccessLogic(grouping, visibilityExpressions, shareabilityNamespaces);


            Joiner joiner = Joiner.on('&').skipNulls();
            return joiner.join(visibilityExpressions);
        } catch (ParseException pe) {
            throw new SecurityPolicyException("ControlSet contains an invalid paired token.", pe);
        }
    }

    protected void encodeSharabilityAccessLogic(PairedTokenGrouping grouping, List<String> visibilityExpressions,
                                                String[] shareabilityNamespaces) {

        for (String namespace : shareabilityNamespaces) {
            visibilityExpressions.add(buildOrExpression(grouping.getPairWithDefault(namespace)));
        }
    }

    /**
     * This helper method joins a set of strings using single vertical bars '|' and wraps the group in
     * parenthesis. Ex: The set {"FOO","BAR","BAZ"} becomes "(FOO|BAR|BAZ)".
     *
     * @param strings the set of strings to join with |
     * @return the joined string result
     */
    protected static final String buildOrExpression(Collection<String> strings) {
        Joiner j = Joiner.on('|').skipNulls();
        String expression = j.join(strings);
        if (StringUtils.isNotEmpty(expression)) {
            return "(" + expression + ")";
        }
        return null;
    }

    /**
     * Get the sensitivity namespaces associated with this security policy
     * @return An array of sensitivity namespace strings
     */
    abstract protected String[] getSensitivityNamespaces();

    /**
     * Get the shareability namespaces associated with this security policy
     * @return A list of shareability namespace strings
     */
    abstract protected String[] getShareabilityNamespaces();

    /**
     * Whether this policy contains ranked sensitivity namespaces
     * @return true if any of the sensitivity namespaces are ranked
     */
    abstract protected boolean hasRankedSensitivity();

    /**
     * Get the namespaces that have ranked sensitivity levels
     * @return A subset of getSensitivityNamespaces containing those which are ranked
     */
    abstract protected String[] getRankedSensitivityNamespaces();

    /*
     * @param sipValue The security level value you would like the rank of
     * @return The rank of sipValue, or -1 if not ranked
     * @throws IllegalArgumentException If sipValue is not a valid value in this policy
     */
    abstract protected int getSensitivityRank(String sipValue) throws IllegalArgumentException;

    /**
     * {@inheritDoc}
     */
    @Override
    public void rollUpPortions(FullEDH2Group rollup, PartialEDH2Group... portions)
            throws SecurityPolicyException {

        try {
            PairedTokenGrouping rollupGrouping = PairedTokenGrouping.fromStrings(rollup.getControlSet());

            for (PartialEDH2Group portion : portions) {
                rollup.getPolicyRef().addAll(portion.getPolicyRef());
                rollup.getAuthRef().addAll(portion.getAuthRef());

                PairedTokenGrouping portionGrouping =
                        PairedTokenGrouping.fromStrings(portion.getControlSet());
                for (String namespace : getSensitivityNamespaces()) {
                    Set<String> portionSet = portionGrouping.get(namespace);
                    if (CollectionUtils.isNotEmpty(portionSet)) {
                        Set<String> rollupSet = rollupGrouping.get(namespace);
                        if (rollupSet == null) {
                            rollupSet = new HashSet<>();
                            portionGrouping.put(namespace, rollupSet);
                        }
                        rollupSet.addAll(portionSet);
                    }
                }

                for (String namespace : getShareabilityNamespaces()) {
                    Set<String> portionSet = portionGrouping.get(namespace);
                    if (CollectionUtils.isNotEmpty(portionSet)) {
                        Set<String> rollupSet = rollupGrouping.get(namespace);
                        if (rollupSet == null) {
                            rollupSet = new HashSet<>();
                            portionGrouping.put(namespace, rollupSet);
                            rollupSet.addAll(portionSet);
                        } else {
                            rollupSet.retainAll(portionSet);
                            if (rollupSet.isEmpty()) {
                                // If this happens then more than one of the portions had a shareability restriction
                                // and there is no common denominator between them. Given the flat nature of the
                                // EDH2 controlSet namespace we can't roll up the control sets and maintain the same
                                // shareability logic so we're going to throw an exception.
                                throw new SecurityPolicyException(
                                        "Unable to roll-up portion markings for namespace: " + namespace);
                            }
                        }
                        portionSet.clear();
                    }
                }
                for (Map.Entry<String, Set<String>> me : portionGrouping.entrySet()) {
                    Set<String> set = rollupGrouping.get(me.getKey());
                    if (set == null) {
                        rollupGrouping.put(me.getKey(), me.getValue());
                    } else {
                        set.addAll(me.getValue());
                    }
                }
            }

            if (hasRankedSensitivity()) {
                chooseHighestRankedValues(rollupGrouping);
            }

            for (PairedToken token : rollupGrouping.flatten()) {
                rollup.getControlSet().add(token.toString());
            }
        } catch (ParseException pe) {
            throw new SecurityPolicyException("ControlSet contains an invalid paired token", pe);
        }
    }

    /**
     * This method chooses the highest classification from a set of possible values;
     * This ranking is enforced by the ranking values set on the enumeration. @See
     * CLSValues
     *
     * @param grouping the grouping to process; this is modified by this method
     * @throws SecurityPolicyException thrown if one of the CLS namespaces contains an unknown value
     */
    protected void chooseHighestRankedValues(PairedTokenGrouping grouping) throws SecurityPolicyException {
        for (Map.Entry<String, Set<String>> tokenGroup : grouping.entrySet()) {
            String namespace = tokenGroup.getKey();
            for (String ranked : getRankedSensitivityNamespaces()) {
                if (namespace.startsWith(ranked)) {
                    Set<String> values = tokenGroup.getValue();
                    if (values.size() > 1) {
                        int max = -1;
                        String maxString = "";
                        for (String value : values) {
                            try {
                                int current = getSensitivityRank(value);
                                if (max < current) {
                                    max = current;
                                    maxString = value;
                                }
                            } catch (IllegalArgumentException | NullPointerException e) {
                                throw new SecurityPolicyException("Unknown value " + value
                                        + " with classification namespace " + namespace, e);
                            }
                        }
                        values.clear();
                        values.add(maxString);

                    }
                }
            }
        }
    }

    /**
     * This method checks the Identifier field to ensure it exists and is a valid GUIDE URI.
     *
     * @param identifier the value of the identifier field
     * @param result     the validation result to update with findings
     */
    protected void validateIdentifier(String identifier, ValidationResult result) {
        if (identifier == null || identifier.isEmpty()) {
            result.reject("Missing mandatory identifier field.");
        } else {
            try {
                URI guideURI = new URI(identifier);
                if (!GUIDE.isValidGuideUri(guideURI)) {
                    LOGGER.debug("Identifier {} is not a valid GUIDE URI.", identifier);
                    result.reject("Identifier is not a valid GUIDE.  Invalid value: [" + identifier + "]");
                }
            } catch (URISyntaxException urie) {
                LOGGER.debug("Identifier {} is not a valid URI.", identifier, urie);
                result.reject("Identifier is not a well formed GUIDE URI");
            }
        }
    }

    /**
     * This method checks the create date time to ensure it is set.
     *
     * @param date   the value of the create date time field
     * @param result the validation result to update with findings
     */
    protected void validateCreateDateTime(Date date, ValidationResult result) {
        if (date == null) {
            result.reject("Mandatory Create Date Time is missing.");
        }
    }

    /**
     * @return The unique resource name of this policy
     */
    abstract protected String getPolicyURN();

    /**
     * @return The simple name of this policy
     */
    abstract protected String getPolicyName();

    /**
     * This method checks to ensure the USAIC policy URN is one of the values in the policy reference
     * field.
     *
     * @param policyRefs  the values in the PolicyRef field
     * @param portionMark true if this field is to be validated as part of a portion marking
     * @param result      the validation result to update with findings
     */
    protected void validatePolicyRef(Set<String> policyRefs, boolean portionMark,
                                     ValidationResult result) {
        if (CollectionUtils.isEmpty(policyRefs)) {
            if (!portionMark) {
                result.reject("Missing mandatory policy reference.");
            }
        } else {
            if (!policyRefs.contains(getPolicyURN())) {
                result.reject("Policy references do not contain URN for " + getPolicyName() + ".");
            }
        }
    }

    /**
     * This method checks the value of the ResponsibleEntity field to ensure that the role qualifiers
     * used are defined in the specification. It also enforces cardinality rules set for each role
     * qualifier. See Appendix A for terms and rules specific to this processing.
     *
     * @param responsibleEntities the values of the ResponsibleEntity field
     * @param result              the validation result to update with findings
     */
    protected void validateResponsibleEntity(Set<String> responsibleEntities, ValidationResult result) {
        if (responsibleEntities != null) {
            int numCustodian = 0;
            int numOriginator = 0;
            for (String responsibleEntity : responsibleEntities) {
                try {
                    PairedToken token = new PairedToken(responsibleEntity);
                    try {
                        ResponsibleEntityRoleQualifiers qualifier = ResponsibleEntityRoleQualifiers.valueOf(token.getPrefix());
                        if (ResponsibleEntityRoleQualifiers.CUST == qualifier) {
                            ++numCustodian;
                        } else if (ResponsibleEntityRoleQualifiers.ORIG == qualifier) {
                            ++numOriginator;
                        }
                    } catch (IllegalArgumentException e) {
                        LOGGER.debug("{} is not defined in the ResponsibleEntityRoleQualifiers Enum.",
                                token.getPrefix(), e);
                        result.reject(token.getPrefix()
                                + " is not a valid role qualifier for a Responsible Entity");
                    }
                } catch (ParseException pe) {
                    LOGGER.debug("Unable to parse {} into token pair.", responsibleEntity, pe);
                    result.reject(responsibleEntity + " is not a valid paired token.");
                }
            }
            if (numCustodian != 1) {
                result.reject("Policy requires one and only one custodian role in Responsible Entity: "
                        + responsibleEntities);
            }
            if (numOriginator > 1) {
                result.reject("Policy allows no more than one originator role in Responsible Entity: "
                        + responsibleEntities);
            }
        }
    }

    /**
     * This method checks the value of the DataSet field to ensure that the scope qualifiers used are
     * defined in the specification. It also enforces cardinality rules set for each qualifier. See
     * Appendix A for terms and rules specific to this processing.
     *
     * @param dataSets the values of the AuthRef field
     * @param result   the validation result to update with findings
     */
    protected void validateDataSet(Set<String> dataSets, ValidationResult result) {

        if (dataSets != null) {
            int numBase = 0;
            for (String dataSet : dataSets) {
                try {
                    PairedToken token = new PairedToken(dataSet);
                    try {
                        DataSetScopeQualifiers qualifier = DataSetScopeQualifiers.valueOf(token.getPrefix());
                        if (DataSetScopeQualifiers.BASE == qualifier) {
                            ++numBase;
                        }
                    } catch (IllegalArgumentException e) {
                        LOGGER.debug("{} is not defined in the DataSetScopeQualifiers Enum.",
                                token.getPrefix(), e);
                        result.reject("[" + token.getPrefix() + "] is not a valid scope qualifier for a data set.");
                    }
                } catch (ParseException pe) {
                    LOGGER.debug("Unable to parse {} into token pair.", dataSet, pe);
                    result.reject(dataSet + " is not a valid paired token.");
                }
            }
            if (numBase > 1) {
                result.reject("Policy allows no more than one base dataset: " + dataSets);
            }
        }
    }

    /**
     * This method checks the value of the AuthRef field to ensure that the activity scope qualifiers
     * used are defined in the specification. See Appendix A for terms and rules specific to this
     * processing. For a full EDH2 group this method also ensures that at least one value is present.
     *
     * @param authRefs    the values of the AuthRef field
     * @param portionMark true if this field is to be validated as part of a portion marking
     * @param result      the validation result to update with findings
     */
    protected void validateAuthRef(Set<String> authRefs, boolean portionMark, ValidationResult result) {

        if (CollectionUtils.isEmpty(authRefs)) {
            if (!portionMark) {
                // TODO: Check to make sure that it is ok for AuthRef to be optional on the USAIC portion
                // markings
                result
                        .reject("AuthRef is missing but it is mandatory in the USAIC implementation profile.");
            }
        } else {
            for (String authRef : authRefs) {
                try {
                    PairedToken token = new PairedToken(authRef);
                    try {
                        AuthRefActivityScopeQualifiers.valueOf(token.getPrefix());
                    } catch (IllegalArgumentException e) {
                        LOGGER.debug("{} is not defined in the AuthRefActivityScopeQualifiers Enum.",
                                token.getPrefix(), e);
                        result.reject("[" + token.getPrefix() + "] is not a valid scope qualifier for an Auth Ref.");
                    }
                } catch (ParseException pe) {
                    result.reject(authRef + " is not a valid paired token.");
                }
            }
        }
    }

    /**
     * This method checks the value of the ControlSet field to ensure that the namespaces are used as
     * defined in the specification.
     *
     * @param controlSets the values of the control set field
     * @param portionMark true if this control set is to be validated as a portion marking
     * @param result      the validation result to update with findings
     */
    abstract protected void validateControlSet(Set<String> controlSets, boolean portionMark,
                                               ValidationResult result);

    /**
     * {@inheritDoc}
     */
    @Override
    public ValidationResult validateResource(FullEDH2Group edh2Resource) {
        ValidationResult result = new ValidationResult();

        validateIdentifier(edh2Resource.getIdentifier(), result);
        validateCreateDateTime(edh2Resource.getCreateDateTime(), result);
        // TODO: Validate Policy
        validatePolicyRef(edh2Resource.getPolicyRef(), false, result);
        validateResponsibleEntity(edh2Resource.getResponsibleEntity(), result);
        validateDataSet(edh2Resource.getDataSet(), result);
        validateAuthRef(edh2Resource.getAuthRef(), false, result);
        validateControlSet(edh2Resource.getControlSet(), false, result);

        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ValidationResult validatePortion(PartialEDH2Group edh2Portion) {
        ValidationResult result = new ValidationResult();

        validateAuthRef(edh2Portion.getAuthRef(), true, result);
        validatePolicyRef(edh2Portion.getPolicyRef(), true, result);
        validateControlSet(edh2Portion.getControlSet(), true, result);

        return result;
    }

    /**
     * Enumeration of the ResponsibleEntity role qualifiers as defined in Appendix A.
     */
    public enum ResponsibleEntityRoleQualifiers {

        /**
         * The organization that has created or otherwise holds the primary rights to a Resource
         */
        ORIG,

        /**
         * The organization that introduced a Resource into an information environment and holds primary
         * accountability for it within that environment.
         */
        CUST
    }


    /**
     * Enumeration of the DataSet scope qualifiers as defined in Appendix A.
     */
    public enum DataSetScopeQualifiers {

        /**
         * The baseline data set applicable to a Resource.
         */
        BASE
    }


    /**
     * Enumeration of the AuthRef activity scope qualifiers as defined in Appendix A.
     */
    public enum AuthRefActivityScopeQualifiers {

        /**
         * The baseline data set applicable to a Resource.
         */
        BASE
    }
}
