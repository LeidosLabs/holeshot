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
import com.fasterxml.jackson.databind.ObjectMapper;
import com.leidoslabs.holeshot.analytics.warehouse.api.model.IndexSuccessEvent;
import com.leidoslabs.holeshot.analytics.warehouse.api.model.request.PostSummariesRequest;
import com.leidoslabs.holeshot.analytics.warehouse.api.model.response.GatewayResponse;
import com.leidoslabs.holeshot.analytics.warehouse.storage.es.client.ESClientBuilder;
import com.leidoslabs.holeshot.analytics.warehouse.storage.es.dao.RequestSummaryDao;
import org.elasticsearch.client.RestHighLevelClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import static org.apache.http.HttpStatus.SC_OK;
import static org.apache.http.entity.ContentType.APPLICATION_JSON;

public class PostSummariesHandler implements RequestStreamHandler {

	private static final Logger LOGGER = LoggerFactory.getLogger(PostSummariesHandler.class);
	
	@Override
    public void handleRequest(InputStream inputStream, OutputStream outputStream, Context context) throws IOException {

        ObjectMapper mapper = new ObjectMapper();
        String requestBody = mapper.readTree(inputStream).findValue("body").asText();
        context.getLogger().log(requestBody);
        PostSummariesRequest request = mapper.readValue(requestBody, PostSummariesRequest.class);
        
        try(RestHighLevelClient client = ESClientBuilder.getHighLevelClient()) {
        	String resultStr = "";
			PostSummariesRequest userRequest = new PostSummariesRequest();
			userRequest.setSummaries(request.getData());
			RequestSummaryDao rusd = new RequestSummaryDao(client);
			IndexSuccessEvent result = rusd.indexItems(userRequest);
			resultStr += mapper.writeValueAsString(result) + "\n";
            mapper.writeValue(outputStream, new GatewayResponse<>(resultStr, APPLICATION_JSON, SC_OK));
        }

    }
}
