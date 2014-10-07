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

import co.cask.coopr.common.conf.Configuration;
import co.cask.coopr.common.conf.Constants;
import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import net.spy.memcached.AddrUtil;
import net.spy.memcached.MemcachedClient;
import net.spy.memcached.internal.OperationFuture;
import org.apache.commons.codec.DecoderException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Memcached backed implementation of {@link CredentialStore}. Will store values in memcache.
 */
public class MemcachedCredentialStore extends EncryptedCredentialStore {
  private static final Logger LOG  = LoggerFactory.getLogger(MemcachedCredentialStore.class);
  private final MemcachedClient client;
  private final int ttlSeconds;
  private final int timeoutSeconds;

  @Inject
  private MemcachedCredentialStore(Configuration conf) throws IOException, GeneralSecurityException,
    DecoderException {
    super(conf);
    String addresses = conf.get(Constants.MemcachedCredentialStore.ADDRESSES);
    Preconditions.checkArgument(addresses != null && !addresses.isEmpty(),
                                Constants.MemcachedCredentialStore.ADDRESSES + " must be specified.");
    this.client = new MemcachedClient(AddrUtil.getAddresses(addresses));
    int ttlSeconds = conf.getInt(Constants.MemcachedCredentialStore.TTL,
                                 Constants.MemcachedCredentialStore.DEFAULT_TTL);
    // if it's longer than 30 days, memcache treats it like a timestamp, which does not make sense to set as a
    // config setting.
    if (ttlSeconds > 60 * 60 * 24 * 30) {
      throw new IllegalArgumentException("Invalid TTL " + ttlSeconds + ". Must be under 30 days.");
    }
    this.ttlSeconds = ttlSeconds;
    this.timeoutSeconds = conf.getInt(Constants.MemcachedCredentialStore.TIMEOUT,
                                      Constants.MemcachedCredentialStore.DEFAULT_TIMEOUT);
  }

  @Override
  void setValue(String tenantId, String clusterId, byte[] val) throws IOException {
    try {
      OperationFuture<Boolean> future = client.set(getKey(tenantId, clusterId), ttlSeconds, val);
      if (!future.get(timeoutSeconds, TimeUnit.SECONDS)) {
        throw new IOException("Unable to set credentials");
      }
    } catch (Exception e) {
      LOG.error("Exception while setting credentials for tenant {} and cluster {} to memcache.",
                tenantId, clusterId, e);
      throw new IOException("Unable to set credentials", e);
    }
  }

  @Override
  byte[] getValue(String tenantId, String clusterId) throws IOException {
    // to prevent continuous retries if the memcache server is down
    // Try to get a value, for up to 5 seconds, and cancel if it doesn't return
    Future<Object> f = client.asyncGet(getKey(tenantId, clusterId));
    try {
      return (byte[]) f.get(timeoutSeconds, TimeUnit.SECONDS);
    } catch (TimeoutException e) {
      LOG.error("Timed out after {} seconds getting credentials for tenant {} and cluster {} from memcache.",
                timeoutSeconds, tenantId, clusterId, e);
      // Since we don't need this, go ahead and cancel the operation.  This
      // is not strictly necessary, but it'll save some work on the server.
      f.cancel(false);
      throw new IOException(e);
    } catch (Exception e) {
      LOG.error("Exception getting credentials for tenant {} and cluster {} from memcache.", e);
      throw new IOException(e);
    }
  }

  @Override
  public void wipe(String tenantId, String clusterId) throws IOException {
    client.delete(getKey(tenantId, clusterId));
  }

  @Override
  public void wipe() throws IOException {
    client.flush();
  }

  private String getKey(String tenantId, String clusterId) {
    // tenant is a UUID so concatenating like this is guaranteed to be unique.
    return tenantId + "." + clusterId;
  }

  @Override
  protected void startUp() throws Exception {
    // No-op
  }

  @Override
  protected void shutDown() throws Exception {
    // No-op
  }
}
