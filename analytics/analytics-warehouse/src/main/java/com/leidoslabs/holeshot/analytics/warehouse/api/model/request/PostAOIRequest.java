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
import com.leidoslabs.holeshot.analytics.common.model.aoi.UserAOI;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;

import java.io.IOException;
import java.util.List;

public class PostAOIRequest implements PostRequest<UserAOI>{

	private List<UserAOI> aois;

	public PostAOIRequest() {}

	public PostAOIRequest(List<UserAOI> aois) {
		this.aois = aois;
	}

	public List<UserAOI> getLists() {
		return aois;
	}

	public void setAois(List<UserAOI> aois) {
		this.aois = aois;
	}
	
	@Override
	public List<UserAOI> getData() {
		return this.aois;
	}

	@JsonIgnore
	public XContentBuilder getMapping() throws IOException{
		XContentBuilder builder = XContentFactory.jsonBuilder();
		builder.startObject();
		{
		    builder.startObject("properties");
		    {
		        builder.startObject("elements");
		        {
	        		builder.startObject("properties");
	        		{
	        			builder.startObject("geometry");
	        			{
	        				builder.field("type", "geo_shape");
	        			}
	        			builder.endObject();
	        			builder.startObject("importance");
	        			{
	        				builder.field("type", "float");
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
	        		}
		            builder.endObject();
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
		
		return builder;
	}

}
