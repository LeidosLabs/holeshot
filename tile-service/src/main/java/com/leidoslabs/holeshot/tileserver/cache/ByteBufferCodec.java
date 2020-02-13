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
package com.leidoslabs.holeshot.tileserver.cache;

import java.nio.ByteBuffer;
import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.codec.Utf8StringCodec;

/*
 * Implements Codec to transcode keys to Redis
 */
public class ByteBufferCodec implements RedisCodec<String, ByteBuffer> {
  
  private Utf8StringCodec keyCodec;
  
  public ByteBufferCodec() {
    keyCodec = new Utf8StringCodec();
  }

  @Override
  public String decodeKey(ByteBuffer bytes) {
     return keyCodec.decodeKey(bytes);
  }

  @Override
  public ByteBuffer decodeValue(ByteBuffer bytes) {
    return bytes;
  }

  @Override
  public ByteBuffer encodeKey(String key) {
      return keyCodec.encodeKey(key);
  }

  @Override
  public ByteBuffer encodeValue(ByteBuffer value) {
    return value;
  }
}
