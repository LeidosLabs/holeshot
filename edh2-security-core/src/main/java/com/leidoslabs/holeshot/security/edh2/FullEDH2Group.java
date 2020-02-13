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

package com.leidoslabs.holeshot.security.edh2;

import java.util.Date;
import java.util.Set;

/**
 * Interface definition for resources conforming to the Smart Data Enterprise Data Header Data
 * Encoding Specification (Smart Data EDH DES). These are the fields used by a full,
 * document-level, marking of the data.
 *
 * Implementation is based on Version 2.0.2 of the document dated 15 January 2015
 */
public interface FullEDH2Group extends PartialEDH2Group {

    /**
     *
     * @return The IC-ID of the data object referred to by the EDH (GUIDE ID String)
     */
  public String getIdentifier();

    /**
     * @param identifier The IC-ID of the data object referred to by the EDH (GUIDE ID String)
     */
  public void setIdentifier(String identifier);

    /**
     * @return The creation date of the data object referred to by the EDH
     */
  public Date getCreateDateTime();

    /**
     * @param createDateTime The creation date of the data object referred to by the EDH
     */
  public void setCreateDateTime(Date createDateTime);

    /**
     * @return Set of paired token strings, each representing a producing entity identifier
     */
  public Set<String> getResponsibleEntity();

    /**
     * @return Set of URN or GUIDE strings representing the relevant dataset
     */
  public Set<String> getDataSet();

    /**
     * @return Set of URN strings referring to the relevant security policy rules
     */
  public Set<PolicyRule> getPolicy();

    /**
     * @return is EDH resource flag
     */
  public Boolean isResource();

    /**
     * @param isResource is EDH resource flag
     */
  public void setResourceFlag(boolean isResource);

    /**
     * @return is external flag
     */
  public Boolean isExternal();

    /**
     * @param isExternal is external flag
     */
  public void setExternalFlag(boolean isExternal);

    /**
     * @return URN; EDH specification link
     */
  public String getSpecification();

    /**
     * @param specification URN; EDH Specification Link
     */
  public void setSpecification(String specification);
}
