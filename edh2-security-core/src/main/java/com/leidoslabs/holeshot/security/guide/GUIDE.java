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
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * GUIDE is a government standard for URIs, meant so users can copy a valid GUIDE URI into their
 * browser and it will be resolved to a URI to forward the request on to the original resource. The
 * OMaaS REST services use an additional representation of GUIDE as path parameters to a REST
 * Resource. This is a utility class that facilitates conversion, validation and construction of
 * GUIDE representations, both the URI representation and the Path Parameter representation. The
 * construction methods for this class specifically use the Java UUID utility class to produce the
 * UUIDs.
 * 
 * The URI format of a GUIDE URI is: guide://<PE_CODE>/<UUID>, where <PE_CODE> is a 4-6 digit
 * Producer Environment (PE) code, and <UUID> is a generated unique identifier that is unique for
 * the Producer Environment. <UUID> is one of the standard Universally Unique Identifier Formats:
 * (See: http://en.wikipedia.org/wiki/Universally_unique_identifier ).
 * 
 * Example of a Guide URI representation: guide://999999/1a821699-a676-4b63-8f13-ac9d7302f955
 * 
 * The path-parameter representation of a {@link GuideIdentifier} follows the format:
 * <PE_CODE>_<UUID>, where <PE_CODE> is a 4-6 digit Producer Environment code, and <UUID> is a
 * generated unique identifier that is unique for the Producer Environment. <UUID> is one of the
 * standard Universally Unique Identifier Formats: (See:
 * http://en.wikipedia.org/wiki/Universally_unique_identifier ). utilities.
 * 
 * Example of a Guide Path Parameter representation: 999999_1a821699-a676-4b63-8f13-ac9d7302f955
 * 
 * @author parrise, strawg
 *
 */
public final class GUIDE {

  private static final Logger LOGGER = LoggerFactory.getLogger(GUIDE.class);

  /**
   * The Producer Environment Code to use when OMaaS produces GUIDE resources.
   */
  public static final String OMAAS_GUIDE_PE_CODE = "2001";

  /**
   * The prefix for GUIDE URIs
   */
  public static final String GUIDE_URI_PREFIX = "guide://";

  /**
   * Regular expression for detecting valid GUIDE as a path parameter format
   */
  public static final String GUIDE_PATH_PARAM_REGEX_STR =
      "[0-9]{4,6}_[0-9a-zA-Z._\\-]{1,36}";

  /**
   * The pre-compiled regular expression pattern for the valid GUIDE as a path parameter format.
   */
  private static Pattern guidePathParamRegex = Pattern.compile(GUIDE_PATH_PARAM_REGEX_STR);

  /**
   * Regular expression for detecting valid GUIDE URI string format
   */
  public static final String GUIDE_URI_STRING_REGEX_STR =
      GUIDE_URI_PREFIX + "[0-9]{4,6}/[0-9a-zA-Z._\\-]{1,36}";
  /**
   * The pre-compiled regular expression pattern for the valid GUIDE as a URI format.
   */
  private static Pattern guideUriRegex = Pattern.compile(GUIDE_URI_STRING_REGEX_STR);

  /**
   * Create a URI GUIDE ID from a client provided prefix and identifier.
   *
   * @param prefix the prefix
   * @param id the unique ID within the organization
   * @return the GUIDE
   * @throws URISyntaxException if the prefix or id cannot be encoded in the URI syntax
   */
  public static URI buildUri(final String prefix, final String id) throws URISyntaxException {
    return new URI(build(prefix, id));
  }

  /**
   * Create a String GUIDE ID from a client provided prefix and identifier.
   *
   * @param prefix the prefix
   * @param id the unique ID within the organization
   * @return the GUIDE
   */
  public static final String build(final String prefix, final String id) {
    return GUIDE_URI_PREFIX + prefix + "/" + id;
  }

  /**
   * Create a String GUIDE by combining a UUID with the provided prefix.
   *
   * @param prefix the prefix
   * @return the GUIDE
   */
  public static final String randomGUIDE(final String prefix) {
    return build(prefix, UUID.randomUUID().toString());
  }

  /**
   * Create a URI GUIDE by combining a UUID with the provided prefix.
   *
   * @param prefix the prefix
   * @return the GUIDE
   * @throws URISyntaxException if the prefix cannot be encoded in the URI syntax
   */
  public static final URI randomGUIDEURI(final String prefix) throws URISyntaxException {
    return new URI(randomGUIDE(prefix));
  }

  /**
   * Create a String GUIDE by combining a UUID generated from the provided name with the prefix.
   *
   * @param prefix the prefix
   * @param name the name used to generate the UUID
   * @return the GUIDE
   */
  public static final String namedGUIDE(final String prefix, final String name) {
    return build(prefix, UUID.nameUUIDFromBytes(name.getBytes()).toString());
  }

  /**
   * Create a URI GUIDE by combining a UUID generated from the provided name with the prefix.
   *
   * @param prefix the prefix
   * @param name the name used to generate the UUID
   * @return the GUIDE
   * @throws URISyntaxException if the prefix cannot be encoded in the URI syntax
   */
  public static final URI namedGUIDEURI(final String prefix, final String name)
      throws URISyntaxException {
    return new URI(namedGUIDE(prefix, name));
  }

  /**
   * Return true if the given value is a valid representation of a GUIDE as a path-parameter. The
   * path-parameter representation must be of the format <PE_CODE>_<UUID>, where <PE_CODE> is a 4-6
   * digit Producer Environment code, and <UUID> is the standard SHA-1 or UUID generated unique
   * identifier.
   * 
   * @param p the path-parameter value to validate as a representation of a GUIDE as a
   *        path-parameter.
   * 
   * @return true if the given path-parameter value is a valid representation of a GUIDE as a
   *         path-parameter.
   */
  public static boolean isValidGuidePathParam(String p) {

    return (p != null && GUIDE.guidePathParamRegex.matcher(p).matches());
  }

  /**
   * Return true if the given {@link URI} is a valid GUIDE URI
   * 
   * The URI must be of the format: guide://<PE_CODE>/<UUID> , where <PE_CODE> is a 4-6 digit
   * Producer Environment code, and <UUID> is the standard SHA-1 or UUID generated unique
   * identifier.
   * 
   * @param u the {@link URI} to validate as a guide uri
   * @return true if the given {@link URI} is a valid GUIDE URI
   */
  public static boolean isValidGuideUri(URI u) {

    return u != null && GUIDE.guideUriRegex.matcher(u.toString()).matches();
  }

  /**
   * Extract the UUID component from the path-parameter representation of a GUIDE. The
   * path-parameter representation must be of the format <PE_CODE>_<UUID>, where <PE_CODE> is a 4-6
   * digit Producer Environment code, and <UUID> is the standard SHA-1 or UUID generated unique
   * identifier.
   * 
   * @param pathParam the path-parameter representation of a GUIDE
   * 
   * @return the UUID component of the GUIDE
   */
  public static UUID uuidFromPathParam(String pathParam) {

    if (isValidGuidePathParam(pathParam)) {

      String uuidString = pathParam.replaceFirst("[0-9]{4,6}_", "");

      return UUID.fromString(uuidString);
    }

    return null;
  }



  /**
   * Construct a new GUIDE URI from the path-parameter representation. The path-parameter
   * representation must be of the format <PE_CODE>_<UUID>, where <PE_CODE> is a 4-6 digit Producer
   * Environment code, and <UUID> is the standard SHA-1 or UUID generated unique identifier.
   * 
   * @param pathParam the path-parameter representation of a GUIDE URI
   * 
   * @return a new URI object, or null if the given representation is invalid
   * 
   * @throws URISyntaxException the value resulted in an invalid URI
   */
  public static URI uriFromPathParam(String pathParam) throws URISyntaxException {

    URI u = null;

    if (isValidGuidePathParam(pathParam)) {

      String uriString = GUIDE_URI_PREFIX + pathParam.replaceFirst("_", "/");

      u = new URI(uriString);
    }

    return u;
  }

  public static final String safeGuideUriFromPathParam(String pathParamId) {
    String result = null;
    try {
      result = GUIDE.uriFromPathParam(pathParamId).toString();
    } catch (URISyntaxException use) {
      LOGGER.warn("Couldn't create GUIDE from '{}'", pathParamId,use);
    }
    return result;
  }


  /**
   * Extract the path parameter representation of a GUIDE from the given URI. If the URI is not a
   * valid GUIDE URI, then null is returned.
   * 
   * @param uri the GUIDE URI to extract the path parameter representation from.
   * 
   * @return the path parameter representation of the GUIDE
   */
  public static String pathParamFromUri(URI uri) {

    String pathParam = null;

    if (isValidGuideUri(uri)) {

      pathParam = uri.toString().replace(GUIDE_URI_PREFIX, "").replaceFirst("/", "_");
    }

    return pathParam;
  }
}
