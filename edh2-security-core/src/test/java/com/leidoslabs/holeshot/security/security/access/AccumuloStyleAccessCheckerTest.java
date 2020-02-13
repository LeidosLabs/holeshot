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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

import java.net.URISyntaxException;
import java.text.ParseException;
import java.util.Arrays;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import com.leidoslabs.holeshot.security.edh2.FullEDH2Group;
import com.leidoslabs.holeshot.security.secpolicy.SecurityPolicy;
import com.leidoslabs.holeshot.security.secpolicy.SecurityPolicyException;
import com.leidoslabs.holeshot.security.secpolicy.SecurityPolicyFactory;
@RunWith(MockitoJUnitRunner.class)
public class AccumuloStyleAccessCheckerTest {
  public static final String POLICY_REF = "TODO:MOCK_SECPOLICY_URN";

  @InjectMocks
  private AccumuloStyleAccessChecker accessChecker;

  @Mock
  private SecurityPolicy policy;

  @Mock
  private SecurityPolicyFactory registry;

  TestEDH2Resource edh2Resource;

  @Before
  public void setUp() throws Exception {
    edh2Resource = new TestEDH2Resource();
    edh2Resource.getControlSet().addAll(Arrays.asList("CLS:TS", "CTRY:USA", "CTRY:GBR", "COI:A4"));
    edh2Resource.getPolicyRef().add(POLICY_REF);

    when(registry.getPolicy(POLICY_REF)).thenReturn(policy);
    when(policy.buildVisibilityExpression(any(FullEDH2Group.class))).thenReturn(
        "CLS:TS&COI:A4&(CTRY:USA|CTRY:GBR)");
  }

  /**
   * This test makes sure it succeeds with OR cases for country and correct Clearance and COI
   * 
   * @throws URISyntaxException
   * @throws SecurityPolicyException
   * @throws ParseException
   */
  @Test
  public void testIsValid_HasAccess() throws URISyntaxException, SecurityPolicyException,
      ParseException {

    String userCredentials = "CLS:TS,CLS:S,CLS:C,CLS:U,COI:A4,COI:A5,CTRY:USA";
    assertTrue(accessChecker.hasAccess(userCredentials, edh2Resource));

  }

  /**
   * This case tests for wrong country
   * 
   * @throws URISyntaxException
   * @throws ParseException
   * @throws SecurityPolicyException
   */
  @Test
  public void testIsValid_NoAccess_CTRY() throws URISyntaxException, SecurityPolicyException,
      ParseException {
    String userCredentials = "CLS:TS,CLS:S,CLS:C,CLS:U,COI:A4,COI:A5,CTRY:CAN";
    assertFalse(accessChecker.hasAccess(userCredentials, edh2Resource));

  }

  /**
   * This case tests for wrong clearance
   * 
   * @throws URISyntaxException
   * @throws SecurityPolicyException
   * @throws ParseException
   */
  @Test
  public void testIsValid_NoAccess_CLS() throws URISyntaxException, SecurityPolicyException,
      ParseException {
    String userCredentials = "CLS:S,CLS:C,CLS:U,COI:A4,COI:A5,CTRY:GBR";
    assertFalse(accessChecker.hasAccess(userCredentials, edh2Resource));

  }

  /**
   * This case tests for wrong COI
   * 
   * @throws URISyntaxException
   * @throws ParseException
   * @throws SecurityPolicyException
   */
  @Test
  public void testIsValid_NoAccess_COI() throws URISyntaxException, SecurityPolicyException,
      ParseException {
    String userCredentials = "CLS:S,CLS:C,CLS:U,COI:A4,CTRY:GBR";
    assertFalse(accessChecker.hasAccess(userCredentials, edh2Resource));

  }

  /**
   * This case tests for invalid policy defined
   * 
   * @throws URISyntaxException
   * @throws ParseException
   * @throws SecurityPolicyException
   */
  @Test(expected = SecurityPolicyException.class)
  public void testIsValid_invalidPolicy() throws URISyntaxException, SecurityPolicyException,
      ParseException {
    // Registry returns null for an invalid policy when looking up POLICY_REF
    when(registry.getPolicy(POLICY_REF)).thenReturn(null);
    String userCredentials = "CLS:S,CLS:C,CLS:U,COI:A4,CTRY:GBR";

      accessChecker.hasAccess(userCredentials, edh2Resource);
  }

  /**
   * This case tests for no policy defined
   * 
   * @throws URISyntaxException
   * @throws ParseException
   * @throws SecurityPolicyException
   */
  @Test(expected = SecurityPolicyException.class)
  public void testIsValid_noPolicyRef() throws URISyntaxException, SecurityPolicyException,
      ParseException {
    // There is no POLICY_REF defined
    edh2Resource.getPolicyRef().clear();
    String userCredentials = "CLS:S,CLS:C,CLS:U,COI:A4,CTRY:GBR";
    accessChecker.hasAccess(userCredentials, edh2Resource);
  }
}
