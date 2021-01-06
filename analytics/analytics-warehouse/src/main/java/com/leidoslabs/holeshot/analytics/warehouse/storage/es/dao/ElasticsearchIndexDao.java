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
import com.fasterxml.jackson.databind.ObjectMapper;
import com.leidoslabs.holeshot.analytics.common.model.RequestSummary;
import com.leidoslabs.holeshot.analytics.warehouse.api.model.IndexSuccessEvent;
import com.leidoslabs.holeshot.analytics.warehouse.api.model.request.PostAOIRequest;
import com.leidoslabs.holeshot.analytics.warehouse.api.model.request.PostRequest;

import org.elasticsearch.action.admin.cluster.health.ClusterHealthRequest;
import org.elasticsearch.action.bulk.BulkItemResponse;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.client.Request;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.Requests;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.indices.CreateIndexRequest;
import org.elasticsearch.client.indices.CreateIndexResponse;
import org.elasticsearch.client.indices.GetIndexRequest;
import org.elasticsearch.cluster.metadata.IndexMetaData;
import org.elasticsearch.common.xcontent.XContentType;
import org.image.common.geojson.GeoJsonModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;

public abstract class ElasticsearchIndexDao<T extends PostRequest<?>> {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(ElasticsearchIndexDao.class);

    protected final RestHighLevelClient client;
    protected ObjectMapper mapper;

    public ElasticsearchIndexDao(final RestHighLevelClient client) {
        this.client = client;
        mapper = new ObjectMapper();
    }

    abstract String getIndex();

    RestHighLevelClient getClient() {
        return client;
    }

    public IndexSuccessEvent indexItems(T request) throws IOException {

        BulkRequest bulkRequest = new BulkRequest();
        request.getData().forEach(item -> {
            try {
                bulkRequest.add(new IndexRequest(getIndex())
                        .id(item.getId())
                        .source(mapper.writeValueAsString(item), XContentType.JSON));
            } catch (JsonProcessingException jpe) {
                jpe.printStackTrace();
            }
        });
        BulkResponse response = client.bulk(bulkRequest, RequestOptions.DEFAULT);
        int failures = 0;
        int successes = 0;
        for(BulkItemResponse bulkItemResponse : response) {
            if(bulkItemResponse.isFailed()) {
                failures++;
                LOGGER.error(bulkItemResponse.getFailureMessage());
            } else {
                successes++;
            }
        }
        
        return new IndexSuccessEvent(successes, failures, this.getIndex());
    }

    // TODO this function should be run once during cloudformation to initialize the index
    public void createIndex(T request) throws IOException {
    	CreateIndexRequest ciRequest = new CreateIndexRequest(this.getIndex());
    	ciRequest.mapping(request.getMapping());
    	CreateIndexResponse ciResponse = client.indices().create(ciRequest, RequestOptions.DEFAULT);
    }
    
}
