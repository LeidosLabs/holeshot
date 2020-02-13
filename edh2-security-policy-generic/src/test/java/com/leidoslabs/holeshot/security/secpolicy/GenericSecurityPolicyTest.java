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
import com.leidoslabs.holeshot.security.guide.GUIDE;

import org.junit.Test;

import java.util.Date;

import static org.junit.Assert.*;

public class GenericSecurityPolicyTest {


    @Test
    public void testGetters() {

        SecurityPolicy policy = new GenericSecurityPolicy();

        assertEquals(SecurityPolicies.SECURITY_POLICY_URN_GENERIC , policy.getPolicyRef());
    }

    @Test
    public void testRollUp() throws Exception {

        SecurityPolicy policy = new GenericSecurityPolicy();

        PartialEDH2Group portion1 = buildTestResource("CLS:U");
        PartialEDH2Group portion2 = buildTestResource("CLS:R LBL:C2");
        FullEDH2Group rollup = new TestEDH2Resource();

        policy.rollUpPortions(rollup, portion1, portion2);
        assertTrue(policy.validatePortion(rollup).isValid());
        assertTrue(rollup.getControlSet().contains("CLS:R"));
        assertTrue(rollup.getControlSet().contains("LBL:C2"));
        assertEquals(2, rollup.getControlSet().size());

        PartialEDH2Group portion3 = buildTestResource("CLS:R ENTITY:LM ENTITY:LEIDOS ");
        PartialEDH2Group portion4 = buildTestResource("CLS:R LBL:C2 ENTITY:LM ENTITY:USG" );

        rollup = new TestEDH2Resource();
        policy.rollUpPortions(rollup, portion3, portion4);
        assertTrue(policy.validatePortion(rollup).isValid());
        assertTrue(rollup.getControlSet().contains("CLS:R"));
        assertTrue(rollup.getControlSet().contains("LBL:C2"));
        assertTrue(rollup.getControlSet().contains("ENTITY:LM"));
        assertEquals(3, rollup.getControlSet().size());


    }


    @Test(expected = SecurityPolicyException.class)
    public void testImpossibleRollUp() throws Exception {
        SecurityPolicy policy = new GenericSecurityPolicy();

        FullEDH2Group rollup = new TestEDH2Resource();

        PartialEDH2Group portion5 = buildTestResource("CLS:R ENTITY:LEIDOS ");
        PartialEDH2Group portion6 = buildTestResource("CLS:R LBL:C2 ENTITY:USG" );

        policy.rollUpPortions(rollup, portion5, portion6);
    }

    @Test(expected = SecurityPolicyException.class)
    public void testImpossibleRollUp3() throws Exception {
        SecurityPolicy policy = new GenericSecurityPolicy();

        FullEDH2Group rollup = new TestEDH2Resource();

        PartialEDH2Group portion5 = buildTestResource("CLS:R ENTITY:USG ");
        PartialEDH2Group portion6 = buildTestResource("CLS:K LBL:C2 ENTITY:USG" );

        policy.rollUpPortions(rollup, portion5, portion6);
    }

    @Test(expected = SecurityPolicyException.class)
    public void testImpossibleRollUp2() throws Exception {
        SecurityPolicy policy = new GenericSecurityPolicy();

        FullEDH2Group rollup = new TestEDH2Resource();

        PartialEDH2Group portion5 = buildTestResource("CLS: ENTITY:LEIDOS ");
        PartialEDH2Group portion6 = buildTestResource("CLS:R LBL:C2 ENTITY:USG" );

        policy.rollUpPortions(rollup, portion5, portion6);
    }

    @Test(expected = SecurityPolicyException.class)
    public void testBuildVisibilityExpression2() throws Exception {
        SecurityPolicy policy = new GenericSecurityPolicy();
        PartialEDH2Group rsc = buildTestResource("CLS:U");
        rsc.getPolicyRef().clear();
        policy.buildVisibilityExpression(rsc);
    }

    @Test(expected = SecurityPolicyException.class)
    public void testBuildVisibilityExpression3() throws Exception {
        SecurityPolicy policy = new GenericSecurityPolicy();
        PartialEDH2Group rsc = buildTestResource("CLS:");
        policy.buildVisibilityExpression(rsc);
    }
    @Test
    public void testBuildVisibilityExpression() throws Exception {
        SecurityPolicy policy = new GenericSecurityPolicy();

        // The most basic/simple classification encoding
        assertEquals("CLS:U", policy.buildVisibilityExpression(buildTestResource("CLS:U")));

        // This example shows a classification and nationality restriction (e.g. noforn)
        assertEquals("CLS:R&(ENTITY:USA)",
                policy.buildVisibilityExpression(buildTestResource("CLS:R ENTITY:USA")));

        assertEquals("CLS:R&LBL:RUSS&LBL:USA",
                policy.buildVisibilityExpression(buildTestResource("CLS:R LBL:USA LBL:RUSS")));

        assertTrue(policy.buildVisibilityExpression(
                buildTestResource("CLS:R LBL:USA LBL:RUSS")).matches(
                "CLS:R&(LBL:(USA|RUSS)\\&?){2}"));

        // This example shows that the other classification type namespaces are NOT ignored
        // when building up a security expression.
        assertTrue(policy.buildVisibilityExpression(
                buildTestResource("CLS:R ENTITY:USA ENTITY:GBR ENTITY:NZL")).matches(
                "CLS:R&\\((ENTITY:(GBR|USA|NZL)\\|?){3}\\)"));
    }

    @Test
    public void testValidateControlSet() throws Exception {

        SecurityPolicy policy = new GenericSecurityPolicy();
        assertTrue(policy.validateResource (buildTestResource("CLS:U")).isValid());


        assertTrue(policy.validateResource (buildTestResource("CLS:R")).isValid());


        assertTrue(policy.validateResource (buildTestResource("CLS:R LBL:123 ENTITY:123")).isValid());


        assertFalse(policy.validateResource (buildTestResource("CLS:U LBL:123")).isValid());


        assertFalse(policy.validateResource (buildTestResource("LBL:123")).isValid());


        assertFalse(policy.validateResource (buildTestResource("CLS:U CLS:R")).isValid());


        assertFalse(policy.validateResource (buildTestResource("CLS:L")).isValid());
    }

    @Test
    public void testValidNonRFC1422Identifier() throws Exception {
        FullEDH2Group vResource = buildTestResource("CLS:U");
        vResource.setIdentifier("guide://2002/c8a7fc1a-df16-a1e5-186d-9a79f06e9478");
        SecurityPolicy policy = new GenericSecurityPolicy();
        SecurityPolicy.ValidationResult result = policy.validateResource(vResource);
        assertTrue(result.isValid());
        assertEquals(0, result.getErrors().size());


        FullEDH2Group invalideResource = buildTestResource("CLS:U");
        invalideResource.setIdentifier("invalid://c8a7fc1a-df16-a1e5-186d-9a79f06e9478");
        result = policy.validateResource(invalideResource);
        assertFalse(result.isValid());
        assertEquals(1, result.getErrors().size());

    }

    @Test
    public void testInvalidBadPairedToken() throws Exception {
        PartialEDH2Group invalidPortion = buildTestResource("CLS:");
        SecurityPolicy policy = new GenericSecurityPolicy();
        SecurityPolicy.ValidationResult result = policy.validatePortion(invalidPortion);
        assertFalse(result.isValid());
        assertEquals(1, result.getErrors().size());
    }

    @Test
    public void testInvalidResource1() throws Exception {
        FullEDH2Group invalidResource = buildTestResource("CLS:U :BADPAIR");
        invalidResource.setIdentifier("http://not.a.valid.guide/path");
        invalidResource.getResponsibleEntity().clear();
        invalidResource.getResponsibleEntity().add("BADQUAL:FOO");
        invalidResource.getResponsibleEntity().add("BAD");
        invalidResource.getResponsibleEntity().add("ORIG:ORIG-1");
        invalidResource.getResponsibleEntity().add("ORIG:ORIG-2");
        invalidResource.getDataSet().add("BAD:FOO.BAR.BAZ");
        invalidResource.getDataSet().add("BASE:");
        SecurityPolicy policy = new GenericSecurityPolicy();
        SecurityPolicy.ValidationResult result = policy.validateResource(invalidResource);
        assertFalse(result.isValid());
        // CS: Malformed ControlSet value
        // ID: Invalid GUIDE Identifier
        // Bad RE Namespace
        // Malformed RE value
        // Missing RE CUST
        // Multiple RE ORIG
        // Bad Data Set scope
        // Malformed Data Set value
        assertEquals(8, result.getErrors().size());
    }

    @Test
    public void testInvalidResource2() throws Exception {
        FullEDH2Group invalidResource = buildTestResource("CLS:U BAD:FOO");
        invalidResource.setIdentifier("~INVALID URI~~^@#!");
        invalidResource.getPolicyRef().clear();
        invalidResource.getPolicyRef().add("SOME OTHER POLICY URN");
        invalidResource.getAuthRef().clear();
        SecurityPolicy policy = new GenericSecurityPolicy();
        SecurityPolicy.ValidationResult result = policy.validateResource(invalidResource);
        assertFalse(result.isValid());
        // Bad ControlSet Namespace
        // Missing USAIC policy ref
        // Invalid URI in ID
        // CLSFGI and CLS are incompatible
        // Missing AuthRef
        assertEquals(5, result.getErrors().size());
    }

    @Test
    public void testInvalidResource3() throws Exception {
        FullEDH2Group invalidResource = buildTestResource("CLS:U");
        invalidResource.setIdentifier("");
        invalidResource.setCreateDateTime(null);
        invalidResource.getPolicyRef().clear();
        invalidResource.getDataSet().clear();
        invalidResource.getDataSet().add("BASE:FOO");
        invalidResource.getDataSet().add("BASE:BAR");
        invalidResource.getAuthRef().add("BAD:foo");
        invalidResource.getAuthRef().add(":baz");
        SecurityPolicy policy = new GenericSecurityPolicy();
        SecurityPolicy.ValidationResult result = policy.validateResource(invalidResource);
        assertFalse(result.isValid());
        // Empty Identifier
        // No Policy Ref
        // Missing create date time
        // Too many base datasets
        // BAD authref
        // Malformed authref
        assertEquals(6, result.getErrors().size());
    }


    private FullEDH2Group buildTestResource(String controlSet) {
        TestEDH2Resource edh2Resource = new TestEDH2Resource();
        edh2Resource.setIdentifier(GUIDE.namedGUIDE("99999", "TEST-RESOURCE"));
        edh2Resource.setCreateDateTime(new Date());
        edh2Resource.getResponsibleEntity().add("CUST:USA.OOO.ooo");
        edh2Resource.getAuthRef().add("BASE:TODO-FORMAT-THIS-STRING");
        edh2Resource.getPolicyRef().add(SecurityPolicies.SECURITY_POLICY_URN_GENERIC);
        for (String s : controlSet.split(" ")) {
            edh2Resource.getControlSet().add(s);
        }
        return edh2Resource;
    }
}
