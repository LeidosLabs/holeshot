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
package com.leidoslabs.holeshot.tileserver.session;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.commons.lang3.tuple.Pair;
import org.eclipse.jetty.server.session.AbstractSessionDataStore;
import org.eclipse.jetty.server.session.SessionData;
import org.eclipse.jetty.server.session.UnreadableSessionDataException;
import org.eclipse.jetty.util.ClassLoadingObjectInputStream;
import org.ehcache.impl.internal.util.ByteBufferInputStream;
import org.slf4j.ext.XLogger;
import org.slf4j.ext.XLoggerFactory;
import com.leidoslabs.holeshot.tileserver.cache.ByteBufferCodec;
import com.leidoslabs.holeshot.tileserver.cache.RedisCache;
import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;

/**
 * Redis session datastore
 *
 */
public class RedisSessionDataStore extends AbstractSessionDataStore {
  private static final XLogger logger = XLoggerFactory.getXLogger(RedisSessionDataStore.class);
  private static final String KEY_PREFIX="jettySession:";

  @Override
  public boolean isPassivating() {
    return true;
  }

  /**
   * Checks if id is in datastore
   */
  @Override
  public boolean exists(String id) throws Exception {
    boolean exists = false;
    if (RedisCache.getInstance().isAvailable()) {
      RedisClient client = RedisCache.getInstance().getRedis();
      try (StatefulRedisConnection<String, ByteBuffer> connection = client.connect(new ByteBufferCodec())) {
        RedisCommands<String, ByteBuffer> commands = connection.sync();
        exists = (commands.exists(getRedisKey(id)) > 0);
      }
    }
    return exists;
  }

  
  /**
   * Load from id
   */
  @Override
  public SessionData load(String id) throws Exception {
    SessionData result = null;

    if (RedisCache.getInstance().isAvailable()) {
      RedisClient client = RedisCache.getInstance().getRedis();
      try (StatefulRedisConnection<String, ByteBuffer> connection = client.connect(new ByteBufferCodec())) {
        RedisCommands<String, ByteBuffer> commands = connection.sync();
        ByteBuffer sessionBytes = commands.get(getRedisKey(id));

        if (sessionBytes != null) {
          try (ByteBufferInputStream bis = new ByteBufferInputStream(sessionBytes)) {
            result = load(bis, id);
          }
        }
      }
    }

    return result;
  }

  @Override
  public boolean delete(String id) throws Exception {
    boolean success = false;
    if (RedisCache.getInstance().isAvailable()) {
      RedisClient client = RedisCache.getInstance().getRedis();
      try (StatefulRedisConnection<String, ByteBuffer> connection = client.connect(new ByteBufferCodec())) {
        RedisCommands<String, ByteBuffer> commands = connection.sync();
        success = (commands.del(getRedisKey(id)) > 0);
      }
    }
    return success;
  }

  @Override
  public void doStore(String id, SessionData data, long lastSaveTime) throws Exception {
    if (RedisCache.getInstance().isAvailable()) {
      RedisClient client = RedisCache.getInstance().getRedis();
      try (StatefulRedisConnection<String, ByteBuffer> connection = client.connect(new ByteBufferCodec())) {
        RedisCommands<String, ByteBuffer> commands = connection.sync();
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
          this.save(bos, id, data);
          commands.set(getRedisKey(id), ByteBuffer.wrap(bos.toByteArray()));
        }
      }
    }
  }

  @Override
  public Set<String> doGetExpired(Set<String> candidates) {
    final long now = System.currentTimeMillis();
    Set<String> expired = candidates.stream()
        .map(c-> {
          Pair<String, SessionData> result = null;
          try {
            result = Pair.of(c, this.load(c));
          } catch (Exception e) {
            logger.error(e.getMessage(), e);
          }
          return result;
        })
        .filter(c->c.getRight()==null || c.getRight().getExpiry() < now)
        .map(c->c.getLeft())
        .collect(Collectors.toSet());
    return expired;
  }

  private String getRedisKey(String key) {
    return KEY_PREFIX + key;
  }

  private void save(OutputStream os, String id, SessionData data)  throws IOException
  {    
    DataOutputStream out = new DataOutputStream(os);
    out.writeUTF(id);
    out.writeUTF(data.getContextPath());
    out.writeUTF(data.getVhost());
    out.writeUTF(data.getLastNode());
    out.writeLong(data.getCreated());
    out.writeLong(data.getAccessed());
    out.writeLong(data.getLastAccessed());
    out.writeLong(data.getCookieSet());
    out.writeLong(data.getExpiry());
    out.writeLong(data.getMaxInactiveMs());

    List<String> keys = new ArrayList<String>(data.getKeys());
    out.writeInt(keys.size());
    ObjectOutputStream oos = new ObjectOutputStream(out);
    for (String name:keys)
    {
      oos.writeUTF(name);
      oos.writeObject(data.getAttribute(name));
    }
  }
  /**
   * @param is inputstream containing session data
   * @param expectedId the id we've been told to load
   * @return the session data
   * @throws Exception
   */
  private SessionData load (InputStream is, String expectedId)
      throws Exception
  {
    String id = null; //the actual id from inside the file

    try (DataInputStream di = new DataInputStream(is))
    {
      SessionData data = null;
      id = di.readUTF();
      String contextPath = di.readUTF();
      String vhost = di.readUTF();
      String lastNode = di.readUTF();
      long created = di.readLong();
      long accessed = di.readLong();
      long lastAccessed = di.readLong();
      long cookieSet = di.readLong();
      long expiry = di.readLong();
      long maxIdle = di.readLong();

      data = newSessionData(id, created, accessed, lastAccessed, maxIdle); 
      data.setContextPath(contextPath);
      data.setVhost(vhost);
      data.setLastNode(lastNode);
      data.setCookieSet(cookieSet);
      data.setExpiry(expiry);
      data.setMaxInactiveMs(maxIdle);

      // Attributes
      restoreAttributes(di, di.readInt(), data);

      return data;        
    }
    catch (Exception e)
    {
      throw new UnreadableSessionDataException(expectedId, _context, e);
    }
  }

  /**
   * @param is inputstream containing session data
   * @param size number of attributes
   * @param data the data to restore to
   * @throws Exception
   */
  private void restoreAttributes (InputStream is, int size, SessionData data)
      throws Exception
  {
    if (size>0)
    {
      // input stream should not be closed here
      try (ClassLoadingObjectInputStream ois =  new ClassLoadingObjectInputStream(is)) {
        Map<String,Object> attributes = new HashMap<String,Object>();
        for (int i=0; i<size;i++)
        {
          String key = ois.readUTF();
          Object value = ois.readObject();
          attributes.put(key,value);
        }
        data.putAllAttributes(attributes);
      }
    }
  }
}
