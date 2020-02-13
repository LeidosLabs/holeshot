/*
 * Licensed to The Leidos Corporation under one or more contributor license agreements.
 * See the NOTICE file distributed with this work for additional information regarding copyright ownership.
 * The Leidos Corporation licenses this file to You under the Apache License, Version 2.0
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
package com.leidoslabs.holeshot.ingest;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.S3Event;
import com.amazonaws.services.s3.event.S3EventNotification;

/**
 * Lambda Container to handle S3 Events (image uploads) then processing the tiling and metadata publishing
 */
public class S3IngestRequestHandler extends AWSTilePyramidListener implements RequestHandler<S3Event, String> {

    /**
     * Initialize using lambda environment variables to define the bucket to write tiles to and SNS topic
     * to publish metadata to
     * @throws Exception
     */
  public S3IngestRequestHandler() throws Exception {
    super(System.getenv("OUTPUT_S3_BUCKET"),System.getenv("OUTPUT_METADATA_TOPIC"));
  }

    /**
     * Handle S3 Event by passing it to AWSTilePyramidListener handler
     * @param s3Event Any s3 event, we are concerned with new images
     * @param context
     * @return The processing log string generated when handling the S3 Event
     */
  @Override
  public String handleRequest(S3Event s3Event, Context context) {

    StringBuilder result = new StringBuilder();
    for (S3EventNotification.S3EventNotificationRecord record : s3Event.getRecords()) {
      result.append(this.handleS3EventRecord(record));
    }
    return result.toString();
  }


}
