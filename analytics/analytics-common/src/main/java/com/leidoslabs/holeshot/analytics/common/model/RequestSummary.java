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

package com.leidoslabs.holeshot.analytics.common.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.leidoslabs.holeshot.analytics.common.model.aoi.AOIConsumable;

/**
 * Summary object for persisting tile request log information
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class RequestSummary implements Indexable, AOIConsumable {

    private String location;
	private long startTime;
    private long endTime;
    private int precisionLevel;
    private int requestCount;
    private String userID;
    private String id;

    public RequestSummary() { }

    /**
     * A condensed summary of logs at a specific geohash over a period of time
     * @param location The geohash of the summarized area of requests
     * @param startTime The start of the summarized window, epoch millis
     * @param endTime The end of the summarized window, epoch millis
     * @param precisionLevel The number of characters in the geohash
     * @param requestCount The summed number of requests falling in the given area and time period
     * @param userID the user for whom the summarized info is relevant, or "global"
     * @param id the unique id of the summary "userID:location:startTime"
     */
    public RequestSummary(String location, long startTime, long endTime, int precisionLevel, int requestCount, String userID, String id) {
        this.location = location;
        this.startTime = startTime;
        this.endTime = endTime;
        this.precisionLevel = precisionLevel;
        this.requestCount = requestCount;
        this.userID = userID == null ? null : userID.toLowerCase();
    }

    public RequestSummary(String location, long startTime, long endTime, int requestCount, String userID) {
        this.location = location;
        this.startTime = startTime;
        this.endTime = endTime;
        this.requestCount = requestCount;
        this.precisionLevel = this.location.length();
        this.userID = userID == null ? null : userID.toLowerCase();
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public long getStartTime() {
        return startTime;
    }

    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    public long getEndTime() {
        return endTime;
    }

    public void setEndTime(long endTime) {
        this.endTime = endTime;
    }

    public int getPrecisionLevel() {
        return precisionLevel;
    }

    public void setPrecisionLevel(int precisionLevel) {
        this.precisionLevel = precisionLevel;
    }

    public int getRequestCount() {
        return requestCount;
    }

    public void setRequestCount(int requestCount) {
        this.requestCount = requestCount;
    }

    public boolean contains(String location) {
        return location.startsWith(this.location);
    }
    
    public String getUserID() {
    	return this.userID;
    }
    
    public void setUserID(String userID) {
    	this.userID = userID == null ? null : userID.toLowerCase();
    }

    @Override
    public String getId() {
        return id != null ? id : this.getUserID() + ":" + this.getLocation() + ":" + this.getStartTime();
    }

    public void setId(String id) {
        this.id = id;
    }
}
