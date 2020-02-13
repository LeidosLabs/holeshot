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
package com.leidoslabs.holeshot.tileserver.v1;

import static com.leidoslabs.holeshot.security.edh2.EDH2Constants.EDH_AUTH_REF;
import static com.leidoslabs.holeshot.security.edh2.EDH2Constants.EDH_CONTROL_SET;
import static com.leidoslabs.holeshot.security.edh2.EDH2Constants.EDH_CREATE_DATE_TIME;
import static com.leidoslabs.holeshot.security.edh2.EDH2Constants.EDH_DATA_SET;
import static com.leidoslabs.holeshot.security.edh2.EDH2Constants.EDH_IDENTIFIER;
import static com.leidoslabs.holeshot.security.edh2.EDH2Constants.EDH_IS_EXTERNAL;
import static com.leidoslabs.holeshot.security.edh2.EDH2Constants.EDH_IS_RESOURCE;
import static com.leidoslabs.holeshot.security.edh2.EDH2Constants.EDH_POLICY;
import static com.leidoslabs.holeshot.security.edh2.EDH2Constants.EDH_POLICY_REF;
import static com.leidoslabs.holeshot.security.edh2.EDH2Constants.EDH_PREFIX;
import static com.leidoslabs.holeshot.security.edh2.EDH2Constants.EDH_RESPONSIBLE_ENTITY;
import static com.leidoslabs.holeshot.security.edh2.EDH2Constants.EDH_SPECIFICATION;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import org.image.common.cache.Cacheable;
import org.image.common.cache.CacheableUtil;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.leidoslabs.holeshot.security.edh2.FullEDH2Group;
import com.leidoslabs.holeshot.security.edh2.PolicyRule;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Polygon;

/**
 * Placeholder class to be replaced!!!!! TODO: Need to incorporate R. Robert's changes to the
 * metadata classes here....
 * 
 * @deprecated
 */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class TilePyramidDescriptor implements FullEDH2Group, Serializable, Cacheable {

  // BEGIN EDH2 Full Resource Attributes
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
  // END EDH2 Full Resource Attributes

  @JsonProperty("name")
  private String name;
  @JsonProperty("description")
  private String description;
  @JsonProperty("version")
  private String version;
  @JsonProperty("attribution")
  private String attribution;
  @JsonProperty("template")
  private String template;
  @JsonProperty("legend")
  private String legend;
  @JsonProperty("tiles")
  private List<String> tiles = new ArrayList<>();
  @JsonProperty("metadata")
  private Object metadata;
  @JsonProperty("tileWidth")
  private int tileWidth;
  @JsonProperty("tileHeight")
  private int tileHeight;
  @JsonProperty("width")
  private int width;
  @JsonProperty("height")
  private int height;
  @JsonProperty("minrlevel")
  private Integer minRLevel;
  @JsonProperty("maxRLevel")
  private Integer maxRLevel;
  @JsonProperty("boundingBox")
  private double[] boundingBox;
  @JsonProperty("bounds")
  private Polygon bounds;

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public String getVersion() {
    return version;
  }

  public void setVersion(String version) {
    this.version = version;
  }

  public String getAttribution() {
    return attribution;
  }

  public void setAttribution(String attribution) {
    this.attribution = attribution;
  }

  public String getTemplate() {
    return template;
  }

  public void setTemplate(String template) {
    this.template = template;
  }

  public String getLegend() {
    return legend;
  }

  public void setLegend(String legend) {
    this.legend = legend;
  }

  public List<String> getTiles() {
    return tiles;
  }

  public void setTiles(List<String> tiles) {
    this.tiles = tiles;
  }

  public Integer getMinRLevel() {
    return minRLevel;
  }

  public void setMinRLevel(Integer minRLevel) {
    this.minRLevel = minRLevel;
  }

  public Integer getMaxRLevel() {
    return maxRLevel;
  }

  public void setMaxRLevel(Integer maxRLevel) {
    this.maxRLevel = maxRLevel;
  }

  public double[] getBoundingBox() {
    return boundingBox;
  }

  public void setBoundingBox(double[] boundingBox) {
    this.boundingBox = boundingBox;
  }

  public Polygon getBounds() {
    return bounds;
  }

  public void setBounds(Polygon bounds) {
    this.bounds = bounds;
    double[] extent = new double[4];
    Envelope e = bounds.getEnvelopeInternal();
    extent[0] = e.getMinX();
    extent[1] = e.getMinY();
    extent[2] = e.getMaxX();
    extent[3] = e.getMaxY();
    this.boundingBox = extent;
  }

  public int getTileWidth() {
    return tileWidth;
  }

  public void setTileWidth(int tileWidth) {
    this.tileWidth = tileWidth;
  }

  public int getTileHeight() {
    return tileHeight;
  }

  public void setTileHeight(int tileHeight) {
    this.tileHeight = tileHeight;
  }

  public int getWidth() {
    return width;
  }

  public void setWidth(int width) {
    this.width = width;
  }

  public int getHeight() {
    return height;
  }

  public void setHeight(int height) {
    this.height = height;
  }

  public Object getMetadata() {
    return metadata;
  }

  public void setMetadata(Object metadata) {
    this.metadata = metadata;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getIdentifier() {
    return identifier;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void setIdentifier(String identifier) {
    this.identifier = identifier;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Date getCreateDateTime() {
    return createDateTime;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void setCreateDateTime(Date createDateTime) {
    this.createDateTime = createDateTime;
  }

  @Override
  public Set<String> getResponsibleEntity() {
    return responsibleEntity;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Set<String> getDataSet() {
    return dataSet;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Set<String> getAuthRef() {
    return authRef;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Set<String> getPolicyRef() {
    return policyRef;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Set<PolicyRule> getPolicy() {
    return policyRules;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Set<String> getControlSet() {
    return controlSet;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Boolean isResource() {
    return isResourceEDH;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void setResourceFlag(boolean isResource) {
    isResourceEDH = isResource;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Boolean isExternal() {
    return isExternalEDH;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void setExternalFlag(boolean isExternal) {
    isExternalEDH = isExternal;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getSpecification() {
    return edhSpecification;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void setSpecification(String specification) {
    edhSpecification = specification;
  }

  public long getSizeInBytes() {
    return CacheableUtil.getDefault().getSizeInBytesForObjects(identifier, createDateTime, responsibleEntity, dataSet, authRef,
        policyRef, policyRules, controlSet, isExternalEDH, isResourceEDH, edhSpecification, name,
        description, version, attribution, template, legend, tiles, metadata, tileWidth, tileHeight,
        width, height, minRLevel, maxRLevel, boundingBox, bounds);
  }
}
