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
import com.leidoslabs.holeshot.analytics.common.model.TileScores;

import java.io.IOException;
import java.util.List;

import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;

public class PostScoresRequest implements PostRequest<TileScores> {

    private List<TileScores> scores;

    public PostScoresRequest() { }

    public PostScoresRequest(List<TileScores> scores) {
        this.scores = scores;
    }

    public List<TileScores> getScores() {
        return scores;
    }

    public void setScores(List<TileScores> scores) {
        this.scores = scores;
    }

    @Override
    public List<TileScores> getData() {
        return this.getScores();
    }
    
    @JsonIgnore
    public XContentBuilder getMapping() throws IOException {
		XContentBuilder builder = XContentFactory.jsonBuilder();
		builder.startObject();
		{
		    builder.startObject("properties");
		    {
		        builder.startObject("tiles");
		        {
	        		builder.startObject("properties");
	        		{
	        			builder.startObject("rset");
	        			{
	        				builder.field("type", "long");
	        			}
	        			builder.endObject();
	        			builder.startObject("score");
	        			{
	        				builder.field("type", "long");
	        			}
	        			builder.endObject();
	        			builder.startObject("x");
	        			{
	        				builder.field("type", "long");
	        			}
	        			builder.endObject();
	        			builder.startObject("y");
	        			{
	        				builder.field("type", "long");
	        			}
	        			builder.endObject();
	        		}
		            builder.endObject();
		        }
		        builder.endObject();
    			builder.startObject("date");
    			{
    				builder.field("type", "date");
    				builder.field("format", "epoch_millis");
    			}
    			builder.endObject();
    			builder.startObject("image");
    			{
    				builder.field("type", "keyword");
    			}
    			builder.endObject();
		    }
		    builder.endObject();
		}
		builder.endObject();
    	return builder;
    	//String mapStr = "{\n      \"properties\" : {\n        \"date\" : {\n          \"type\" : \"date\",\n          \"format\" : \"epoch_millis\"\n        },\n        \"id\" : {\n          \"type\" : \"text\",\n          \"fields\" : {\n            \"keyword\" : {\n              \"type\" : \"keyword\",\n              \"ignore_above\" : 256\n            }\n          }\n        },\n        \"image\" : {\n          \"type\" : \"keyword\"\n        },\n        \"tiles\" : {\n          \"properties\" : {\n            \"rSet\" : {\n              \"type\" : \"long\"\n            },\n            \"score\" : {\n              \"type\" : \"long\"\n            },\n            \"x\" : {\n              \"type\" : \"long\"\n            },\n            \"y\" : {\n              \"type\" : \"long\"\n            }\n          }\n        }\n      }\n    }";  
    	//return mapStr;
    }
}
