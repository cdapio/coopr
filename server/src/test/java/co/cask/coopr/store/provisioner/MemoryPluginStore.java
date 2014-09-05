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
package co.cask.coopr.store.provisioner;

import co.cask.coopr.account.Account;
import co.cask.coopr.common.conf.Configuration;
import co.cask.coopr.provisioner.plugin.ResourceType;
import com.google.common.base.Objects;
import com.google.common.collect.Maps;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.ConcurrentMap;

/**
 * Plugin store that keeps plugin modules in memory. Used for testing since nothing is persisted to disk.
 */
public class MemoryPluginStore implements PluginStore {
  private final ConcurrentMap<Key, ByteArrayOutputStream> store;

  public MemoryPluginStore() {
    this.store = Maps.newConcurrentMap();
  }

  // for unit tests
  public void clearData() {
    store.clear();
  }

  @Override
  public void initialize(Configuration conf) {
    // No-op
  }

  @Override
  public OutputStream getResourceOutputStream(Account account, ResourceType type, String name, int version)
    throws IOException {
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    store.put(new Key(account, type, name, version), outputStream);
    return outputStream;
  }

  @Override
  public InputStream getResourceInputStream(Account account, ResourceType type, String name, int version)
    throws IOException {
    Key key = new Key(account, type, name, version);
    if (!store.containsKey(key)) {
      return null;
    }
    return new ByteArrayInputStream(store.get(key).toByteArray());
  }

  @Override
  public void deleteResource(Account account, ResourceType type,  String name, int version) throws IOException {
    store.remove(new Key(account, type, name, version));
  }

  private class Key {
    private final Account account;
    private final ResourceType type;
    private final String name;
    private final int version;

    private Key(Account account, ResourceType type, String name, int version) {
      this.account = account;
      this.type = type;
      this.name = name;
      this.version = version;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (!(o instanceof Key)) {
        return false;
      }

      Key that = (Key) o;

      return Objects.equal(account, that.account) &&
        Objects.equal(type, that.type) &&
        Objects.equal(name, that.name) &&
        Objects.equal(version, that.version);
    }

    @Override
    public int hashCode() {
      return Objects.hashCode(account, type, name, version);
    }
  }
}
