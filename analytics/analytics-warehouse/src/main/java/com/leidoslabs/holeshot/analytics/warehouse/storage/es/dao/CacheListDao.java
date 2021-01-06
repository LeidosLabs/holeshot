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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.leidoslabs.holeshot.analytics.common.model.CacheList;
import com.leidoslabs.holeshot.analytics.warehouse.api.model.request.GetCacheListRequest;
import com.leidoslabs.holeshot.analytics.warehouse.api.model.request.PostCacheListRequest;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class CacheListDao extends ElasticsearchIndexDao<PostCacheListRequest> {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(CacheListDao.class);

    private final String index = "cachelists";
    private static final ObjectMapper mapper = new ObjectMapper();

    public CacheListDao(final RestHighLevelClient client) {
        super(client);
    }
    
    public List<CacheList> getCacheLists(GetCacheListRequest request, int maxResults) throws IOException{
    	SearchRequest searchRequest = new SearchRequest(this.index);
        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
        sourceBuilder.size(maxResults);
        
        BoolQueryBuilder boolQuery = new BoolQueryBuilder();
        boolQuery.filter(QueryBuilders.termQuery("userID", request.getUserID()));
        boolQuery.filter(QueryBuilders.rangeQuery("date").gte(request.getDate()));
        
        sourceBuilder.query(boolQuery);
        searchRequest.source(sourceBuilder);
        
        SearchResponse searchResponse = getClient().search(searchRequest, RequestOptions.DEFAULT);
        return Stream.of(searchResponse.getHits().getHits())
                .map(hit -> {
                    try {
                        return mapper.readValue(hit.getSourceAsString(), new TypeReference<CacheList>(){});
                    } catch (JsonProcessingException e) {
                    	LOGGER.debug("Error deserializing results");
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
