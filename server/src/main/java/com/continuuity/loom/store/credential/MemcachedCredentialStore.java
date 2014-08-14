package com.continuuity.loom.store.credential;

import com.continuuity.loom.common.conf.Configuration;
import com.continuuity.loom.common.conf.Constants;
import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import net.spy.memcached.AddrUtil;
import net.spy.memcached.MemcachedClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Memcached backed implementation of {@link CredentialStore}. Will store values
 */
public class MemcachedCredentialStore implements CredentialStore {
  private static final Logger LOG  = LoggerFactory.getLogger(MemcachedCredentialStore.class);
  private final MemcachedClient client;
  private final int ttlSeconds;
  private final int timeoutSeconds;

  @Inject
  private MemcachedCredentialStore(Configuration conf) throws IOException {
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
  public void set(String tenantId, String clusterId, Map<String, String> fields) throws IOException {
    client.set(getKey(tenantId, clusterId), ttlSeconds, fields);
  }

  @Override
  public Map<String, String> get(String tenantId, String clusterId) throws IOException {
    // to prevent continuous retries if the memcache server is down
    // Try to get a value, for up to 5 seconds, and cancel if it doesn't return
    Object result = null;
    Future<Object> f = client.asyncGet(getKey(tenantId, clusterId));
    try {
      result = f.get(timeoutSeconds, TimeUnit.SECONDS);
    } catch (TimeoutException e) {
      LOG.error("Timed out after {} seconds getting credentials for tenant {} and cluster {} from memcache.",
                timeoutSeconds, tenantId, clusterId, e);
      // Since we don't need this, go ahead and cancel the operation.  This
      // is not strictly necessary, but it'll save some work on the server.
      f.cancel(false);
      // Do other timeout related stuff
    } catch (InterruptedException e) {
      LOG.error("Interruped while getting credentials for tenant {} and cluster {} from memcache.",
                tenantId, clusterId, e);
    } catch (ExecutionException e) {
      LOG.error("Execution exception while getting credentials for tenant {} and cluster {} from memcache.",
                tenantId, clusterId, e);
    }

    return (Map<String, String>) result;
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
}
