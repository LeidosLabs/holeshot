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
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.image.common.geojson.GeoJsonModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.leidoslabs.holeshot.analytics.common.model.aoi.UserAOI;
import com.leidoslabs.holeshot.analytics.warehouse.api.model.request.GetAOIRequest;
import com.leidoslabs.holeshot.analytics.warehouse.api.model.request.PostAOIRequest;

public class UserAOIDao extends ElasticsearchIndexDao<PostAOIRequest> {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(UserAOIDao.class);

	private final String index = "useraoi";
	
	public UserAOIDao(RestHighLevelClient client) {
		super(client);
		GeoJsonModule gjsonMod = new GeoJsonModule();
		mapper.registerModules(gjsonMod);
	}
	
    public List<UserAOI> getUserAOIs(GetAOIRequest request, int maxResults) throws IOException {
    	
    	SearchRequest searchRequest = new SearchRequest(this.index);
        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
        sourceBuilder.size(maxResults);
        
        BoolQueryBuilder boolQuery = new BoolQueryBuilder();
        boolQuery.filter(QueryBuilders.termQuery("userID", request.getUserID()));
        boolQuery.filter(QueryBuilders.rangeQuery("startTime").gte(request.getStartTime()));
        
        sourceBuilder.query(boolQuery);
        searchRequest.source(sourceBuilder);
        
        SearchResponse searchResponse = getClient().search(searchRequest, RequestOptions.DEFAULT);
        return Stream.of(searchResponse.getHits().getHits())
                .map(hit -> {
                    try {
                        return mapper.readValue(hit.getSourceAsString(), new TypeReference<UserAOI>(){});
                    } catch (JsonProcessingException e) {
                    	LOGGER.error("error deserializing results");
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
