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
import com.leidoslabs.holeshot.analytics.common.model.RequestSummary;
import com.leidoslabs.holeshot.analytics.common.model.User;
import com.leidoslabs.holeshot.analytics.warehouse.api.model.IndexSuccessEvent;
import com.leidoslabs.holeshot.analytics.warehouse.api.model.LowerLeftUpperRightBBox;
import com.leidoslabs.holeshot.analytics.warehouse.api.model.request.GetSummariesRequest;
import com.leidoslabs.holeshot.analytics.warehouse.api.model.request.PostSummariesRequest;
import com.leidoslabs.holeshot.analytics.warehouse.api.model.request.PostUserRequest;
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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class RequestSummaryDao extends ElasticsearchIndexDao<PostSummariesRequest> {
	private static final Logger LOGGER = LoggerFactory.getLogger(RequestSummaryDao.class);


    private static final ObjectMapper mapper = new ObjectMapper();
    private final String index = "summaries";

    public RequestSummaryDao(final RestHighLevelClient client) {
        super(client);
    }
    
    @Override
    public IndexSuccessEvent indexItems(PostSummariesRequest request) throws IOException {
    	
    	UserDao udao = new UserDao(client);
        List<User> toAdd = new ArrayList<User>();
        Set<User> knownUsers = new HashSet<User>(udao.getUsers());

    	for (RequestSummary sum : request.getData()) {
    		User curUser = new User(sum.getUserID());
    		if (!curUser.getUserID().equals("global") && !knownUsers.contains(curUser)) {
    			toAdd.add(curUser);
    		}
    	}
    	if (toAdd.size() > 0) {
    	   	PostUserRequest userRequest = new PostUserRequest(toAdd);
        	udao.indexItems(userRequest);
    	}

    	return super.indexItems(request);
    }

    public List<RequestSummary> getSummaries(GetSummariesRequest request, int maxResults) throws IOException {

        SearchRequest searchRequest = new SearchRequest(this.index);
        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
        sourceBuilder.size(maxResults);
        BoolQueryBuilder boolQuery = new BoolQueryBuilder();

        boolQuery.filter(QueryBuilders.termQuery("userID", request.getUserID()));
        //boolQuery.filter(QueryBuilders.rangeQuery("startTime").gte(request.getStartTime()).lte(request.getEndTime()));

        if(request.getPrecisionLevel() != null) {
            boolQuery.filter(QueryBuilders.termQuery("precisionLevel", request.getPrecisionLevel()));
        }

        LowerLeftUpperRightBBox bbox = request.getBbox();
        if(bbox!=null) {
            boolQuery.filter(QueryBuilders.geoBoundingBoxQuery("location").setCorners(bbox.getTop(), bbox.getLeft(), bbox.getBottom(), bbox.getRight()));
        }

        sourceBuilder.query(boolQuery);
        searchRequest.source(sourceBuilder);

        SearchResponse searchResponse = getClient().search(searchRequest, RequestOptions.DEFAULT);
        return Stream.of(searchResponse.getHits().getHits())
                .map(hit -> {
                    try {
                        return mapper.readValue(hit.getSourceAsString(), new TypeReference<RequestSummary>(){});
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
