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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.leidoslabs.holeshot.analytics.common.model.RequestSummary;

import java.io.IOException;
import java.util.List;

import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;

public class PostSummariesRequest implements PostRequest<RequestSummary> {

    private List<RequestSummary> summaries;

    public PostSummariesRequest() {
    }

    public PostSummariesRequest(List<RequestSummary> summaries) {
        this.summaries = summaries;
    }

    public List<RequestSummary> getSummaries() {
        return summaries;
    }

    public void setSummaries(List<RequestSummary> summaries) {
        this.summaries = summaries;
    }

    public List<RequestSummary> getData() {
        return getSummaries();
    }

    @JsonIgnore
	public XContentBuilder getMapping() throws IOException {
    	
    	XContentBuilder builder = XContentFactory.jsonBuilder();
		builder.startObject();
		{
		    builder.startObject("properties");
		    {
    			builder.startObject("location");
    			{
    				builder.field("type", "geo_point");
    			}
    			builder.endObject();
    			builder.startObject("startTime");
    			{
    				builder.field("type", "date");
    				builder.field("format", "epoch_millis");
    			}
    			builder.endObject();
    			builder.startObject("endTime");
    			{
    				builder.field("type", "date");
    				builder.field("format", "epoch_millis");
    			}
    			builder.endObject();
    			builder.startObject("userID");
    			{
    				builder.field("type", "keyword");
    			}
    			builder.endObject();
		    }
		    builder.endObject();
		}
		builder.endObject();
		
		//String mapStr =   "{\n      \"properties\": {\n\t      \"location\": {\n          \"type\": \"geo_point\"\n        },\n        \"startTime\": {\n          \"type\":   \"date\",\n          \"format\": \"epoch_millis\"\n        },\n        \"endTime\": {\n          \"type\":   \"date\",\n          \"format\": \"epoch_millis\"\n        },\n        \"precisionLevel\": {\n          \"type\": \"integer\"\n        },\n        \"requestCount\": {\n          \"type\": \"long\"\n        },\n        \"userID\": {\n          \"type\": \"keyword\"\n        }\n      }\n    }";
		//return mapStr;
    	return builder;
	}
}
