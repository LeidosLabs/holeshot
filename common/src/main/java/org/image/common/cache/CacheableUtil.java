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
package org.image.common.cache;

import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import org.apache.commons.collections4.map.ListOrderedMap;
import org.image.common.cache.util.ListOrderedMapBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.locationtech.jts.geom.Geometry;


/**
 * Utility class for caching, provides interface for retrieving size of a datatype to cache
 * 
 */
public class CacheableUtil {
  private static final Logger LOGGER = LoggerFactory.getLogger(CacheableUtil.class);

  
  private final ListOrderedMap<Class<?>, Function<Object, Long>> DEFAULT_SIZE_HANDLERS =
      new ListOrderedMapBuilder<Class<?>, Function<Object, Long>>()
          .put(Cacheable.class, o -> ((Cacheable) o).getSizeInBytes())
          .put(String.class, o -> (long) ((String) o).length())
          .put(Map.class, o -> getSizeInBytesForObject(((Map<?, ?>) o).entrySet()))
          .put(Collection.class,
              o -> ((Collection<?>) o).stream().mapToLong(x -> getSizeInBytesForObject(x)).sum())
          .put(Object[].class,
              o -> Arrays.stream((Object[]) o).mapToLong(x -> getSizeInBytesForObject(x)).sum())
          .put(Byte.class, o -> (long) Byte.BYTES).put(Short.class, o -> (long) Short.BYTES)
          .put(Integer.class, o -> (long) Integer.BYTES).put(Double.class, o -> (long) Double.BYTES)
          .put(Float.class, o -> (long) Float.BYTES).put(Date.class, o -> (long) Long.BYTES * 2)
          .put(Geometry.class, o -> (long) Double.BYTES * ((Geometry) o).getNumPoints()).build();
  private final ListOrderedMap<Class<?>, Function<Object, Long>> handlers;

  private static CacheableUtil defaultInstance = null;

  public synchronized static CacheableUtil getDefault() {
    if (defaultInstance == null) {
      defaultInstance = new CacheableUtil();
    }
    return defaultInstance;
  }

  /**
   * Initialize defaults 
   */
  public CacheableUtil() {
    handlers = new ListOrderedMap<Class<?>, Function<Object, Long>>();
    handlers.putAll(DEFAULT_SIZE_HANDLERS);
  }

  /**
   * @param object object to query size of
   * @return Size in bytes of object, if handler has been added by default or with registerAdditionalHanderl
   * Otherwise Logs an error.
   */
  public long getSizeInBytesForObject(Object object) {
    long result = 0;
    if (object != null) {
      Optional<Function<Object, Long>> handler = handlers.entrySet().stream()
          .filter(e -> e.getKey().isInstance(object)).map(t -> t.getValue()).findFirst();
      if (handler.isPresent()) {
        result = handler.get().apply(object);
      } else {
        LOGGER.error(String.format("Don't know how to compute the size of a '%s'.  Ignoring.", object.getClass().getName()));
      }
    }
    return result;
  }

  /**
   * Register an additional handler
   * @param clazz data type to handle
   * @param handler function that maps instance of clazz to its size in bytes
   */
  public void registerAdditionalHandler(Class<?> clazz, Function<Object, Long> handler) {
    handlers.put(clazz, handler);
  }

  public long getSizeInBytesForObjects(Object... object) {
    return getSizeInBytesForObject(object);
  }

}
