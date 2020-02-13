/*
 * Licensed to Leidos, Inc. under one or more contributor license agreements.  
 * See the NOTICE file distributed with this work for additional information regarding copyright ownership.
 * Leidos, Inc. licenses this file to You under the Apache License, Version 2.0
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

package com.leidoslabs.holeshot.chipper.wms;

import java.net.URL;
import java.time.ZonedDateTime;
import java.util.List;

import org.apache.commons.lang3.Range;

import net.opengis.sld.StyledLayerDescriptor;

/**
 * WMSRequest contract for map requests
 */
public interface GetMapRequest extends WMSRequest {
   public double[] getBbox();
   public int getWidth();
   public int getHeight();
   public String getFormat();
   public boolean isTransparent();
   public String getBgColor();
   public String getExceptions();
   public Range<ZonedDateTime> getTime();
   public URL getSld();
   public StyledLayerDescriptor getSldBody();
   public List<String> getLayers();
   public List<String> getStyles();
   public String getCrs();
}
