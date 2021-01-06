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

package com.leidoslabs.holeshot.analytics.warehouse.storage.es.client;

import com.amazonaws.auth.AWS4Signer;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.leidoslabs.holeshot.analytics.common.client.AnalyticsURLBuilder;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequestInterceptor;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;

public class ESClientBuilder {

    // TODO remove hardcoded region
    private static final String REGION = "us-east-1";

    public static RestHighLevelClient getHighLevelClient() {
        AWS4Signer signer = new AWS4Signer();
        signer.setServiceName("es");
        signer.setRegionName(REGION);
        HttpRequestInterceptor interceptor = new AWSRequestSigningApacheInterceptor("es", signer, new DefaultAWSCredentialsProviderChain());
        return new RestHighLevelClient(
                RestClient.builder(new HttpHost(AnalyticsURLBuilder.getElasticsearchUrl(), 443, "https"))
                        .setHttpClientConfigCallback(c -> c.addInterceptorLast(interceptor)));
    }

}
