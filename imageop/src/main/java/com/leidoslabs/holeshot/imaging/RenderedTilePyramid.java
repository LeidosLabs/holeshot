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

import java.awt.image.RenderedImage;
import java.util.HashMap;
import java.util.Map;

/**
 * Abstract class that holds metadata as a Map<String, String>, and stores Rendered Images for 
 * each rLevel in a Map as well.
 */
public abstract class RenderedTilePyramid {

  private Map<String,String> metadata = new HashMap<>();
  private Map<Integer,RenderedImage> tilePyramid = new HashMap<>();

  public Map<String,String> getMetadata() {
    return metadata;
  }

  public RenderedImage getRLevel(int rLevel) {
    return tilePyramid.get(rLevel);
  }

}
