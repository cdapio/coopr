/*
 * Copyright © 2012-2014 Cask Data, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package co.cask.coopr.store.credential;

import com.google.common.base.Objects;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.util.concurrent.AbstractIdleService;

import java.util.Collections;
import java.util.Map;

/**
 * Credential store that stores sensitive data in memory in the server process. Only viable if there is a single
 * server, as data will not be available across multiple servers.
 */
public class InProcessCredentialStore extends AbstractIdleService implements CredentialStore {
  private final Cache<CredentialKey, Map<String, Object>> cache = CacheBuilder.newBuilder().build();

  @Override
  public void set(String tenantId, String clusterId, Map<String, Object> fields) {
    cache.put(new CredentialKey(tenantId, clusterId), fields);
  }

  @Override
  public Map<String, Object> get(String tenantId, String clusterId) {
    Map<String, Object> result = cache.getIfPresent(new CredentialKey(tenantId, clusterId));
    return result == null ? Collections.<String, Object>emptyMap() : result;
  }

  @Override
  public void wipe(String tenantId, String clusterId) {
    cache.invalidate(new CredentialKey(tenantId, clusterId));
  }

  @Override
  public void wipe() {
    cache.invalidateAll();
  }

  @Override
  protected void startUp() throws Exception {
    // No-op
  }

  @Override
  protected void shutDown() throws Exception {
    // No-op
  }

  private class CredentialKey {
    private final String tenantId;
    private final String clusterId;

    private CredentialKey(String tenantId, String clusterId) {
      this.tenantId = tenantId;
      this.clusterId = clusterId;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }

      CredentialKey that = (CredentialKey) o;

      return Objects.equal(tenantId, that.tenantId) &&
        Objects.equal(clusterId, that.clusterId);
    }

    @Override
    public int hashCode() {
      return Objects.hashCode(tenantId, clusterId);
    }
  }
}
