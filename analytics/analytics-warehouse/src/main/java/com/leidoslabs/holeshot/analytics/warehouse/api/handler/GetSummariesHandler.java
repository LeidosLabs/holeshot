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

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.leidoslabs.holeshot.analytics.common.model.RequestSummary;
import com.leidoslabs.holeshot.analytics.warehouse.api.model.request.GetSummariesRequest;
import com.leidoslabs.holeshot.analytics.warehouse.api.model.response.GatewayResponse;
import com.leidoslabs.holeshot.analytics.warehouse.storage.es.client.ESClientBuilder;
import com.leidoslabs.holeshot.analytics.warehouse.storage.es.dao.RequestSummaryDao;
import org.elasticsearch.client.RestHighLevelClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

import static org.apache.http.HttpStatus.SC_OK;
import static org.apache.http.entity.ContentType.APPLICATION_JSON;

public class GetSummariesHandler implements RequestStreamHandler {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(GetSummariesHandler.class);
    private static final int MAX_RESULTS = 10000;

    @Override
    public void handleRequest(InputStream inputStream, OutputStream outputStream, Context context) throws IOException {

        ObjectMapper mapper = new ObjectMapper();
        JsonNode rawQueryParams = mapper.readTree(inputStream).findValue("queryStringParameters");
        ObjectNode queryParams = rawQueryParams != null ? rawQueryParams.deepCopy() : new ObjectNode(mapper.getNodeFactory());;
        if (!queryParams.has("userID")) {
        	queryParams.set("userID", new TextNode("global"));
        }
        GetSummariesRequest request = mapper.treeToValue(queryParams, GetSummariesRequest.class);

        try(RestHighLevelClient client = ESClientBuilder.getHighLevelClient()) {
            RequestSummaryDao dao = new RequestSummaryDao(client);
            List<RequestSummary> result = dao.getSummaries(request, MAX_RESULTS);
            mapper.writeValue(outputStream, new GatewayResponse<>(mapper.writeValueAsString(result), APPLICATION_JSON, SC_OK));
        }

    }
}
