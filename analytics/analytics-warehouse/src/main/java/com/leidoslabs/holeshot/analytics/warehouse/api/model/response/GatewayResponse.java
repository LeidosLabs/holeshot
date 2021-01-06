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

package com.leidoslabs.holeshot.analytics.warehouse.api.model.response;

import org.apache.http.entity.ContentType;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Response object for API Gateway.
 */
public class GatewayResponse<T> {

    private final T body;
    private final Map<String, String> headers;
    private final int statusCode;

    /**
     * Creates a GatewayResponse object.
     * @param body body of the response
     * @param headers headers of the response
     * @param statusCode status code of the response
     */
    public GatewayResponse(final T body, final Map<String, String> headers, final int statusCode) {
        this.statusCode = statusCode;
        this.body = body;
        this.headers = Collections.unmodifiableMap(new HashMap<>(headers));
    }

    /**
     * Creates a GatewayResponse object.
     * @param body body of the response
     * @param contentType headers of the response
     * @param statusCode status code of the response
     */
    public GatewayResponse(final T body, final ContentType contentType, final int statusCode) {
        this(body, Collections.singletonMap("Content-Type", contentType.getMimeType()), statusCode);
    }

    public T getBody() {
        return body;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public int getStatusCode() {
        return statusCode;
    }
}