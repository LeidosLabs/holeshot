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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

import org.apache.http.HttpStatus;
import org.apache.http.entity.ContentType;
import org.elasticsearch.client.RestHighLevelClient;
import org.image.common.geojson.GeoJsonModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.leidoslabs.holeshot.analytics.common.model.aoi.UserAOI;
import com.leidoslabs.holeshot.analytics.warehouse.api.model.request.GetAOIRequest;
import com.leidoslabs.holeshot.analytics.warehouse.api.model.response.GatewayResponse;
import com.leidoslabs.holeshot.analytics.warehouse.storage.es.client.ESClientBuilder;
import com.leidoslabs.holeshot.analytics.warehouse.storage.es.dao.UserAOIDao;

public class GetAOIHandler implements RequestStreamHandler {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(GetAOIHandler.class);
	private static final int MAX_RESULTS = 1000;

	@Override
	public void handleRequest(InputStream inputStream, OutputStream outputStream, Context context) throws IOException {
		ObjectMapper mapper = new ObjectMapper();
		GeoJsonModule gjsonMod = new GeoJsonModule();
		mapper.registerModules(gjsonMod);
		JsonNode params = mapper.readTree(inputStream).findValue("queryStringParameters");
		GetAOIRequest request = mapper.treeToValue(params, GetAOIRequest.class);
		
		try(RestHighLevelClient client = ESClientBuilder.getHighLevelClient()){
			UserAOIDao dao = new UserAOIDao(client);
			List<UserAOI> aois = dao.getUserAOIs(request, MAX_RESULTS);
			mapper.writeValue(outputStream, new GatewayResponse<>(mapper.writeValueAsString(aois), ContentType.APPLICATION_JSON, HttpStatus.SC_OK));		
		}
	}

}
