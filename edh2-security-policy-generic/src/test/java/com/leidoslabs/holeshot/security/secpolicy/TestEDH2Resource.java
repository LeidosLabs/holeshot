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

import com.fasterxml.jackson.annotation.JsonProperty;
import com.leidoslabs.holeshot.security.edh2.FullEDH2Group;
import com.leidoslabs.holeshot.security.edh2.PolicyRule;

import java.util.Date;
import java.util.Set;
import java.util.TreeSet;

import static com.leidoslabs.holeshot.security.edh2.EDH2Constants.*;


public class TestEDH2Resource implements FullEDH2Group {

  @JsonProperty(EDH_PREFIX + EDH_IDENTIFIER)
  private String identifier;
  @JsonProperty(EDH_PREFIX + EDH_CREATE_DATE_TIME)
  private Date createDateTime;
  @JsonProperty(EDH_PREFIX + EDH_RESPONSIBLE_ENTITY)
  private final Set<String> responsibleEntity = new TreeSet<>();
  @JsonProperty(EDH_PREFIX + EDH_DATA_SET)
  private final Set<String> dataSet = new TreeSet<>();
  @JsonProperty(EDH_PREFIX + EDH_AUTH_REF)
  private final Set<String> authRef = new TreeSet<>();
  @JsonProperty(EDH_PREFIX + EDH_POLICY_REF)
  private final Set<String> policyRef = new TreeSet<>();
  @JsonProperty(EDH_PREFIX + EDH_POLICY)
  private final Set<PolicyRule> policyRules = new TreeSet<>();
  @JsonProperty(EDH_PREFIX + EDH_CONTROL_SET)
  private final Set<String> controlSet = new TreeSet<>();
  @JsonProperty(EDH_PREFIX + EDH_IS_EXTERNAL)
  private Boolean isExternalEDH = null;
  @JsonProperty(EDH_PREFIX + EDH_IS_RESOURCE)
  private Boolean isResourceEDH = null;
  @JsonProperty(EDH_PREFIX + EDH_SPECIFICATION)
  private String edhSpecification;

  @Override
  public String getIdentifier() {
    return identifier;
  }

  @Override
  public void setIdentifier(String identifier) {
    this.identifier = identifier;
  }

  @Override
  public Date getCreateDateTime() {
    return createDateTime;
  }

  @Override
  public void setCreateDateTime(Date createDateTime) {
    this.createDateTime = createDateTime;
  }

  @Override
  public Set<String> getResponsibleEntity() {
    return responsibleEntity;
  }

  @Override
  public Set<String> getDataSet() {
    return dataSet;
  }

  @Override
  public Set<String> getAuthRef() {
    return authRef;
  }

  @Override
  public Set<String> getPolicyRef() {
    return policyRef;
  }

  @Override
  public Set<PolicyRule> getPolicy() {
    return policyRules;
  }

  @Override
  public Set<String> getControlSet() {
    return controlSet;
  }

  @Override
  public Boolean isResource() {
    return isResourceEDH;
  }

  @Override
  public void setResourceFlag(boolean isResource) {
    isResourceEDH = isResource;
  }

  @Override
  public Boolean isExternal() {
    return isExternalEDH;
  }

  @Override
  public void setExternalFlag(boolean isExternal) {
    isExternalEDH = isExternal;
  }

  @Override
  public String getSpecification() {
    return edhSpecification;
  }

  @Override
  public void setSpecification(String specification) {
    edhSpecification = specification;
  }
}
