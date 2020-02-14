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

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.Vector;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import org.eclipse.jetty.server.session.AbstractSessionDataStore;
import org.eclipse.jetty.server.session.SessionContext;
import org.eclipse.jetty.server.session.SessionData;
import org.eclipse.jetty.server.session.SessionDataStore;
import org.slf4j.ext.XLogger;
import org.slf4j.ext.XLoggerFactory;

/**
 * Datastore for a tiered system
 */
public class TieredSessionDataStore extends AbstractSessionDataStore {
  private static final XLogger logger = XLoggerFactory.getXLogger(TieredSessionDataStore.class);

  private List<SessionDataStore> datastores;

  public TieredSessionDataStore(SessionDataStore[] datastores) {
    this.datastores = Arrays.asList(datastores);
  }

  @Override
  public void initialize(SessionContext context) throws Exception {
    super.initialize(context);
    
    for (SessionDataStore datastore: datastores) {
      datastore.initialize(context);
    }
  }  

  @Override
  public boolean isPassivating() {
    return true;
  }

  @Override
  public boolean exists(String id) throws Exception {
    return datastores.stream().anyMatch(d->{
      boolean exists = false;
      try {
        exists = d.exists(id);
      } catch (Exception e) {
        logger.error(e.getMessage(), e);
      }
      return exists;
    });
  }

  @Override
  public SessionData doLoad(String id) throws Exception {
    SessionData result = null;

    final List<SessionDataStore> notFoundInStores = new Vector<SessionDataStore>();

    final Optional<SessionData> optResult = datastores.stream().map(d->{
      SessionData sessionData = null;
      try {
        sessionData = d.load(id);
        if (sessionData == null) {
          notFoundInStores.add(d);
        }
      } catch (Exception e) {
        logger.error(e.getMessage(), e);
      }
      return sessionData;
    }).filter(s->s != null).findFirst();

    if (optResult.isPresent() ) {
      result = optResult.get();
      updateStores(notFoundInStores, id, result);
    }

    return result;
  }

  private void updateStores(List<SessionDataStore> storesToUpdate, String id, SessionData data) {
    storesToUpdate.stream().forEach(d->{
      try {
        d.store(id, data);
      } catch (Exception e) {
        logger.error(e.getMessage(), e);
      }
    });
  }

  @Override
  public boolean delete(String id) throws Exception {
    long deleted = datastores.stream().filter(d->{
      boolean result = false;
      try {
        result = d.delete(id);
      } catch (Exception e) {
        logger.error(e.getMessage(), e);
      }
      return result;
    }).count();

    return deleted > 0;
  }

  @Override
  public void doStore(String id, SessionData data, long lastSaveTime) throws Exception {
    updateStores(datastores, id, data);
  }

  @Override
  public Set<String> doGetExpired(Set<String> candidates) {
    Set<String> expired = 
        datastores.stream().flatMap(d->d.getExpired(candidates).stream()).collect(Collectors.toSet());
    return expired;
  }
}
