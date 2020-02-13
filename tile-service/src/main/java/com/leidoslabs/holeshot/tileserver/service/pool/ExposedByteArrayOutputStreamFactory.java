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

import org.apache.commons.pool2.BasePooledObjectFactory;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.DefaultPooledObject;
import com.leidoslabs.holeshot.tileserver.service.io.ExposedByteArrayOutputStream;
/**
 * Pooled Factory for ExposedByteArrayOutputStreams
 *
 */
public class ExposedByteArrayOutputStreamFactory extends BasePooledObjectFactory<ExposedByteArrayOutputStream> {
  public ExposedByteArrayOutputStreamFactory() {
  }

  @Override
  public ExposedByteArrayOutputStream create() throws Exception {
    return new ExposedByteArrayOutputStream();
  }

  @Override
  public PooledObject<ExposedByteArrayOutputStream> wrap(ExposedByteArrayOutputStream bos) {
    return new DefaultPooledObject<ExposedByteArrayOutputStream>(bos);
  }
  
  @Override
  public void passivateObject(PooledObject<ExposedByteArrayOutputStream> p) throws Exception {
     p.getObject().reset();
  }  

}
