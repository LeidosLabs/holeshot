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

package com.leidoslabs.holeshot.analytics.warehouse.api.model.request;

import com.leidoslabs.holeshot.analytics.warehouse.api.model.LowerLeftUpperRightBBox;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.stream.Stream;

public class GetSummariesRequest {
    private Long startTime;
    private Long endTime;
    private Integer precisionLevel;
    private String bbox;
    private String userID;

    private static final long currentMillis = LocalDateTime.now().atZone(ZoneId.of("UTC")).toInstant().toEpochMilli();
    private static final long defaultRangeMillis = 24 * 60 * 60 * 1000;

    public GetSummariesRequest(Long startTime, Long endTime, Integer precisionLevel, String bbox, String userID) {
        this.startTime = startTime;
        this.endTime = endTime;
        this.precisionLevel = precisionLevel;
        this.bbox = bbox;
        this.userID = userID;
    }

    public GetSummariesRequest() { }

    public Long getStartTime() {
        if(startTime == null) {
            startTime = currentMillis - defaultRangeMillis;
        }
        return startTime;
    }

    public void setStartTime(Long startTime) {
        this.startTime = startTime;
    }

    public Long getEndTime() {
        if(endTime == null) {
            endTime = currentMillis;
        }
        return endTime;
    }

    public void setEndTime(Long endTime) {
        this.endTime = endTime;
    }

    public Integer getPrecisionLevel() {
        return precisionLevel;
    }

    public void setPrecisionLevel(Integer precisionLevel) {
        this.precisionLevel = precisionLevel;
    }

    public void setBbox(String bbox) {
        this.bbox = bbox;
    }
    
    public String getUserID() {
    	return this.userID;
    }
    
    public void setUserID(String userID) {
    	this.userID = userID;
    }

    /**
     * Get an object representation of the bbox with accessors for left/right/top/bottom
     * @throws NumberFormatException If the string of doubles cannot be parsed
     */
    public LowerLeftUpperRightBBox getBbox() throws NumberFormatException {
        if(bbox == null) {
            return null;
        }
        try {
            return new LowerLeftUpperRightBBox(Stream.of(bbox.split(",")).map(Double::valueOf).toArray(Double[]::new));
        } catch (Exception e) {
            e.printStackTrace();
            throw new NumberFormatException("Could not parse bounding box " + bbox);
        }
    }

}
