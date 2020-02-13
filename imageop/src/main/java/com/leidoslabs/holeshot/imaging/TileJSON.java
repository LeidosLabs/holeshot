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
package com.leidoslabs.holeshot.imaging;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by parrise on 9/27/17. TileJSON specification
 */
public class TileJSON {


  @JsonProperty("tilejson")
  private String specVersion = "2.2.0";
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
  @JsonProperty("scheme")
  private String scheme;
  @JsonProperty("tiles")
  private List<String> tiles = new ArrayList<>();
  @JsonProperty("grids")
  private List<String> grids = new ArrayList<>();
  @JsonProperty("data")
  private List<String> data = new ArrayList<>();
  @JsonProperty("minzoom")
  private Integer minZoom;
  @JsonProperty("maxzoom")
  private Integer maxZoom;
  @JsonProperty("bounds")
  private int[] bounds;
  @JsonProperty("center")
  private int[] center;



}
