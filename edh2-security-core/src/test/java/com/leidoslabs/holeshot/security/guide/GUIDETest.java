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

package com.leidoslabs.holeshot.security.guide;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.UUID;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Test Suite for the {@link GUIDE} utility class
 * 
 * @author parrise, strawg
 */
public class GUIDETest {

  /**
   * Test UUID value
   */
  String testUuid = UUID.randomUUID().toString();
  
  /** 
   * Test valid non-UUID value
   */
  String testSuffix = "ABC_123-def.456";

  /**
   * Test path-parameter representation of a GUIDE. The test URI string below is the URI
   * representation of this path parameter
   */
  String validPathParameter = GUIDE.OMAAS_GUIDE_PE_CODE + "_" + testUuid.toString();
  
  /**
   * Test path-parameter representation of a valid non-UUID GUIDE
   */
  String validPathParameterNonUUID = GUIDE.OMAAS_GUIDE_PE_CODE + "_" + testSuffix;
  
  /**
   * Test URI string representation of a GUIDE. The test path parameter above is the path parameter
   * representation of this URI
   */
  String validUriString = GUIDE.GUIDE_URI_PREFIX + GUIDE.OMAAS_GUIDE_PE_CODE + "/" + testUuid;
  
  /**
   * Test URI string representation of a non-UUID GUIDE
   */
  String validUriStringNonUUID = GUIDE.GUIDE_URI_PREFIX + GUIDE.OMAAS_GUIDE_PE_CODE + "/" + testSuffix;


  /**
   * Test strings for an invalid representation of the path-parameter
   */
  String invalidSeparatorPathParam = validPathParameter.replace("_", "/");
  String invalidPeCodePathParam = validPathParameter.replace(GUIDE.OMAAS_GUIDE_PE_CODE, "ABCDEF");
  String invalidUuidPathParam = validPathParameter.replace(testUuid.toString(), "123456*2&");
  String invalidLengthPathParam = validPathParameter.replace(testUuid.toString(), "this_suffix-is_too.long_the-max-is-36_Characters");

  /**
   * Verify GUIDE Strings built from the same name are equal
   */
  @Test
  public void testNamedGuide() {

    String guide1 = GUIDE.namedGUIDE("999999", "FOO");
    String guide2 = GUIDE.namedGUIDE("999999", "FOO");
    assertEquals(guide1, guide2);
  }

  /**
   * Verify GUIDE URIs built from the same name are equal
   */
  @Test
  public void testNamedGuideUri() throws URISyntaxException {

    URI guide1 = GUIDE.namedGUIDEURI("999999", "FOO");
    URI guide2 = GUIDE.namedGUIDEURI("999999", "FOO");
    assertEquals(guide1, guide2);
  }

  /**
   * Verify a GUIDE URI is correctly built from a prefix and a UUID
   * 
   * @throws URISyntaxException failure constructing URI
   */
  @Test
  public void testBuildUri() throws URISyntaxException {

    URI guide = GUIDE.buildUri("000000", testUuid);

    URI expectedUri = new URI(GUIDE.GUIDE_URI_PREFIX + "000000" + "/" + testUuid);

    assertEquals(expectedUri, guide);
  }

  /**
   * Verify a GUIDE String is correctly built from a prefix and a UUID
   * 
   */
  @Test
  public void testBuild() {

    String guide = GUIDE.build("111111", testUuid);

    String expectedUri = GUIDE.GUIDE_URI_PREFIX + "111111" + "/" + testUuid;

    assertEquals(expectedUri, guide);
  }

  /**
   * Verify the path-parameter representation of the GUIDE URI is produced correctly.
   * 
   * @throws URISyntaxException failure constructing URI
   */
  @Test
  public void testAsPathParam() throws URISyntaxException {

    URI guide = GUIDE.buildUri(GUIDE.OMAAS_GUIDE_PE_CODE, testUuid);

    assertEquals(GUIDE.OMAAS_GUIDE_PE_CODE + "_" + testUuid, GUIDE.pathParamFromUri(guide));

  }

  /**
   * Verify the path-parameter representation of the GUIDE URI is produced correctly.
   * 
   * @throws URISyntaxException failure constructing URI
   */
  @Test
  public void testRandomGuide() throws URISyntaxException {

    String guide = GUIDE.randomGUIDE(GUIDE.OMAAS_GUIDE_PE_CODE);

    assertNotNull(guide);

    assertTrue(GUIDE.isValidGuideUri(new URI(guide)));

    String guide2 = GUIDE.randomGUIDE(GUIDE.OMAAS_GUIDE_PE_CODE);

    assertFalse(guide.equals(guide2));

    String expectedPrefix = GUIDE.GUIDE_URI_PREFIX + GUIDE.OMAAS_GUIDE_PE_CODE + "/";

    assertTrue(guide.startsWith(expectedPrefix));
    assertTrue(guide2.startsWith(expectedPrefix));
  }

  /**
   * Verify the path-parameter representation of the GUIDE URI is produced correctly.
   * 
   * @throws URISyntaxException failure constructing URI
   */
  @Test
  public void testRandomGuideUri() throws URISyntaxException {

    URI guide = GUIDE.randomGUIDEURI(GUIDE.OMAAS_GUIDE_PE_CODE);

    assertNotNull(guide);

    assertTrue(GUIDE.isValidGuideUri(guide));

    URI guide2 = GUIDE.randomGUIDEURI(GUIDE.OMAAS_GUIDE_PE_CODE);

    assertFalse(guide.equals(guide2));

    String expectedPrefix = GUIDE.GUIDE_URI_PREFIX + GUIDE.OMAAS_GUIDE_PE_CODE + "/";

    assertTrue(guide.toString().startsWith(expectedPrefix));
    assertTrue(guide2.toString().startsWith(expectedPrefix));
  }

  /**
   * Verify the path-parameter validation method is correctly validating valid and invalid path
   * parameter cases.
   */
  @Test
  public void testValidatePathParameter() {

    assertTrue(GUIDE.isValidGuidePathParam(validPathParameter));
    assertTrue(GUIDE.isValidGuidePathParam(validPathParameterNonUUID));
    assertFalse(GUIDE.isValidGuidePathParam(invalidSeparatorPathParam));
    assertFalse(GUIDE.isValidGuidePathParam(invalidPeCodePathParam));
    assertFalse(GUIDE.isValidGuidePathParam(invalidUuidPathParam));
    assertFalse(GUIDE.isValidGuidePathParam(invalidLengthPathParam));
    assertFalse(GUIDE.isValidGuidePathParam(null));
    assertFalse(GUIDE.isValidGuidePathParam(""));
    assertFalse(GUIDE.isValidGuidePathParam(" "));
  }

  /**
   * Verify the GUIDE URI validation method is correctly validating valid and invalid path parameter
   * cases
   * 
   * @throws URISyntaxException
   */
  @Test
  public void testValidGuideUri() throws URISyntaxException {

    assertTrue(GUIDE.isValidGuideUri(new URI(validUriString)));
    
    assertTrue(GUIDE.isValidGuideUri(new URI(validUriStringNonUUID)));

    assertFalse(GUIDE.isValidGuideUri(new URI(invalidSeparatorPathParam)));

    assertFalse(GUIDE.isValidGuideUri(new URI(invalidPeCodePathParam)));

    assertFalse(GUIDE.isValidGuideUri(new URI(invalidUuidPathParam)));
    
    assertFalse(GUIDE.isValidGuideUri(new URI(invalidLengthPathParam)));
  }

  /**
   * Verify the builder method from a path-parameter correctly returns a null object when passed
   * invalid path-parameters
   * 
   * @throws URISyntaxException failure constructing the URI
   */
  @Test
  public void testFromInvalidPathParam() throws URISyntaxException {

    URI guide = null;

    guide = GUIDE.uriFromPathParam(invalidSeparatorPathParam);

    assertNull(guide);

    guide = GUIDE.uriFromPathParam(invalidPeCodePathParam);

    assertNull(guide);

    guide = GUIDE.uriFromPathParam(invalidUuidPathParam);

    assertNull(guide);
    
    guide = GUIDE.uriFromPathParam(invalidLengthPathParam);
  }


  /**
   * Verify UUID can be correctly extracted from a valid path parameter
   */
  @Test
  public void testUuidFromPathParam() {

    UUID uuid = GUIDE.uuidFromPathParam(validPathParameter);

    assertNotNull(uuid);

    assertEquals(uuid.toString(), testUuid);
  }

  /**
   * Verify null is correctly returned when extracting a UUID from an invalid valid path parameter
   */
  @Test
  public void testUuidFromInvalidPathParam() {

    UUID uuid = GUIDE.uuidFromPathParam(invalidPeCodePathParam);

    assertNull(uuid);

    uuid = GUIDE.uuidFromPathParam(invalidSeparatorPathParam);

    assertNull(uuid);

    uuid = GUIDE.uuidFromPathParam(invalidUuidPathParam);

    assertNull(uuid);
    
    uuid = GUIDE.uuidFromPathParam(invalidLengthPathParam);

    assertNull(uuid);

    uuid = GUIDE.uuidFromPathParam(null);

    assertNull(uuid);

    uuid = GUIDE.uuidFromPathParam("");

    assertNull(uuid);

    uuid = GUIDE.uuidFromPathParam(" ");

    assertNull(uuid);
  }

  /**
   * Verify a String representation of the GUIDE can be correctly extracted from a valid path
   * parameter representation
   * 
   * @throws URISyntaxException failure constructing URI
   */
  @Test
  public void testUriFromPathParam() throws URISyntaxException {

    URI guide = GUIDE.uriFromPathParam(validPathParameter);

    assertNotNull(guide);

    URI expectedUri = new URI(validUriString);

    assertEquals(expectedUri, guide);
    
    guide = GUIDE.uriFromPathParam(validPathParameterNonUUID);
    
    assertNotNull(guide);
    
    expectedUri = new URI(validUriStringNonUUID);
    
    assertEquals(expectedUri, guide);
  }

  /**
   * Verify null is correctly returned when extracting a UUID from an invalid valid path parameter
   * representation
   * 
   * @throws URISyntaxException failure to construct URI
   */
  @Test
  public void testUriFromInvalidPathParam() throws URISyntaxException {

    URI guide = GUIDE.uriFromPathParam(invalidPeCodePathParam);

    assertNull(guide);

    guide = GUIDE.uriFromPathParam(invalidSeparatorPathParam);

    assertNull(guide);

    guide = GUIDE.uriFromPathParam(invalidUuidPathParam);

    assertNull(guide);
    
    guide = GUIDE.uriFromPathParam(invalidLengthPathParam);

    assertNull(guide);

    guide = GUIDE.uriFromPathParam(null);

    assertNull(guide);

    guide = GUIDE.uriFromPathParam("");

    assertNull(guide);

    guide = GUIDE.uriFromPathParam(" ");

    assertNull(guide);
  }

  /**
   * Verify a path parameter can be extracted from a valid GUIDE URI.
   * 
   * @throws URISyntaxException failure constructing URI object
   */
  @Test
  public void testPathParamFromValidUri() throws URISyntaxException {

    URI guideUri = new URI(validUriString);

    String pathParam = GUIDE.pathParamFromUri(guideUri);

    assertEquals(validPathParameter, pathParam);
    
    guideUri = new URI(validUriStringNonUUID);
    
    pathParam = GUIDE.pathParamFromUri(guideUri);
    
    assertEquals(validPathParameterNonUUID, pathParam);
    
  }

  /**
   * Verify null is correctly returned when extracting a path parameter from an invalid GUIDE URL.
   * 
   * @throws URISyntaxException failure constructing URI objects
   */
  @Test
  public void testPathParamFromInvalidUri() throws URISyntaxException {

    String pathParam = GUIDE.pathParamFromUri(null);

    assertNull(pathParam);

    pathParam = GUIDE.pathParamFromUri(new URI(invalidSeparatorPathParam));

    assertNull(pathParam);

    pathParam = GUIDE.pathParamFromUri(new URI(invalidUuidPathParam));

    assertNull(pathParam);

    pathParam = GUIDE.pathParamFromUri(new URI(invalidPeCodePathParam));

    assertNull(pathParam);
    
    pathParam = GUIDE.pathParamFromUri(new URI(invalidLengthPathParam));

    assertNull(pathParam);
  }
}
