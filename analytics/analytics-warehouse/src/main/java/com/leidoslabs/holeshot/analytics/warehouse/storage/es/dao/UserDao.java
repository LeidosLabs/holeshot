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

package com.leidoslabs.holeshot.analytics.warehouse.storage.es.dao;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.leidoslabs.holeshot.analytics.common.model.User;
import com.leidoslabs.holeshot.analytics.common.model.aoi.UserAOI;
import com.leidoslabs.holeshot.analytics.warehouse.api.model.request.PostUserRequest;

public class UserDao extends ElasticsearchIndexDao<PostUserRequest> {
	private static final Logger LOGGER = LoggerFactory.getLogger(UserDao.class);

	private final String index = "users";
	
	public UserDao(RestHighLevelClient client) {
		super(client);
	}
	
	public List<User> getUsers() throws IOException {
    	
		ObjectMapper mapper = new ObjectMapper();
		
		SearchRequest searchRequest = new SearchRequest(this.index);
        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
        
        sourceBuilder.query(QueryBuilders.matchAllQuery());
        searchRequest.source(sourceBuilder);
        SearchResponse searchResponse = getClient().search(searchRequest, RequestOptions.DEFAULT);
        return Stream.of(searchResponse.getHits().getHits())
                .map(hit -> {
                    try {
                        return mapper.readValue(hit.getSourceAsString(), new TypeReference<User>(){});
                    } catch (JsonProcessingException e) {
                    	LOGGER.error("Error deserializing results");
                        e.printStackTrace();
                        return null;
                    }
                }).collect(Collectors.toList());
	}
	
	@Override
	String getIndex() {
		return index;
	}

}
