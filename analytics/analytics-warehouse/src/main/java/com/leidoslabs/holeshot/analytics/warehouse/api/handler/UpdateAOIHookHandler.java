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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;
import com.amazonaws.services.sns.AmazonSNS;
import com.amazonaws.services.sns.AmazonSNSClientBuilder;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.leidoslabs.holeshot.analytics.warehouse.api.model.response.GatewayResponse;

public class UpdateAOIHookHandler implements RequestStreamHandler {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(GetCacheListHandler.class);
	private static final String topicARN = System.getenv("TOPIC_ARN");

	@Override
	public void handleRequest(InputStream inputStream, OutputStream outputStream, Context context) throws IOException {
		ObjectMapper mapper = new ObjectMapper();
		JsonNode queryParams = mapper.readTree(inputStream).findValue("queryStringParameters");
		String userID = mapper.treeToValue(queryParams.get("userID"), String.class);
		publishTopic(userID);
		mapper.writeValue(outputStream, new GatewayResponse<>("Success", APPLICATION_JSON, SC_OK));
	}
	
	public void publishTopic(String userID) {
		try {
			AmazonSNS snsClient = AmazonSNSClientBuilder.defaultClient();
			snsClient.publish(topicARN, userID);

	     } catch (Exception e) {
	    	 LOGGER.error("Unable to publish to CacheListTopic");
	    	 e.printStackTrace();
	     }
	}

}
