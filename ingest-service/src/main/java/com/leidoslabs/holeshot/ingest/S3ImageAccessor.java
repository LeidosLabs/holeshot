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

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.S3Object;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;

/**
 * An implementation of a multi-part image accessor reading objects from S3.
 */
public class S3ImageAccessor implements MultiPartImageAccessor {

  private static final Logger LOGGER = LoggerFactory.getLogger(S3ImageAccessor.class);
  //private final AmazonS3 s3Client = AmazonS3ClientBuilder.standard().withRegion("us-west-2").build();
  private final AmazonS3 s3Client = AmazonS3ClientBuilder.defaultClient();

  @Override
  public InputStream getPart(String name) {
    if (! name.startsWith("s3://")) {
      LOGGER.error("S3 Accessor requires parts in the s3://<bucket>/<key> format.");
      return null;
    }

    String bucketAndKey = name.substring(5);
    int slashIdx = bucketAndKey.indexOf("/");
    String bucket = bucketAndKey.substring(0,slashIdx);
    String key = bucketAndKey.substring(slashIdx +1);

    LOGGER.info("Accessing Object on S3 {} {}",bucket,key);
    //S3Object object = s3Client.getObject(new GetObjectRequest(bucket,key,true));
    S3Object object = s3Client.getObject(bucket,key);
    if (object != null) {
      return object.getObjectContent();
    }
    return null;
  }
}
