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

package com.leidoslabs.holeshot.analytics.warehouse.api.handler;

import static org.apache.http.HttpStatus.SC_OK;
import static org.apache.http.entity.ContentType.APPLICATION_JSON;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

import org.elasticsearch.client.RestHighLevelClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.leidoslabs.holeshot.analytics.common.model.CacheList;
import com.leidoslabs.holeshot.analytics.warehouse.api.model.request.GetCacheListRequest;
import com.leidoslabs.holeshot.analytics.warehouse.api.model.response.GatewayResponse;
import com.leidoslabs.holeshot.analytics.warehouse.storage.es.client.ESClientBuilder;
import com.leidoslabs.holeshot.analytics.warehouse.storage.es.dao.CacheListDao;

public class GetCacheListHandler implements RequestStreamHandler {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(GetCacheListHandler.class);
    private static final int MAX_RESULTS = 10000;
	
	@Override
	public void handleRequest(InputStream inputStream, OutputStream outputStream, Context context) throws IOException {
		ObjectMapper mapper = new ObjectMapper();
		JsonNode queryParams = mapper.readTree(inputStream).findValue("queryStringParameters");
		GetCacheListRequest request = mapper.treeToValue(queryParams, GetCacheListRequest.class);
		try(RestHighLevelClient client = ESClientBuilder.getHighLevelClient()) {
			 CacheListDao dao = new CacheListDao(client);
			 List<CacheList> result = dao.getCacheLists(request, MAX_RESULTS);
			 mapper.writeValue(outputStream, new GatewayResponse<>(mapper.writeValueAsString(result), APPLICATION_JSON, SC_OK));
		}
	}
	


}
