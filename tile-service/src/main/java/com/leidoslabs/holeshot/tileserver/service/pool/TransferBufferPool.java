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
package com.leidoslabs.holeshot.tileserver.service.pool;

import org.apache.commons.pool2.impl.GenericObjectPool;
import org.slf4j.ext.XLogger;
import org.slf4j.ext.XLoggerFactory;


/**
 * ObjectPool for byte arrays used for transfer buffers
 *
 */
public class TransferBufferPool extends GenericObjectPool<byte[]> {
    private static final XLogger logger = XLoggerFactory.getXLogger(TransferBufferPool.class);

    public TransferBufferPool(int maxNumberOfConcurrentRequests, int buffersPerRequest, int bufferSizeInBytes) {
       super(new ByteArrayFactory(bufferSizeInBytes), new TileserverObjectPoolConfig(maxNumberOfConcurrentRequests, buffersPerRequest));
       logger.entry(maxNumberOfConcurrentRequests, buffersPerRequest, bufferSizeInBytes);
       logger.exit();
    }
  }
