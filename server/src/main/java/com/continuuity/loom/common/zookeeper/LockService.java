package com.continuuity.loom.common.zookeeper;

import com.continuuity.loom.common.conf.Constants;
import com.continuuity.loom.common.zookeeper.lib.ZKInterProcessReentrantLock;
import com.google.common.base.Joiner;
import com.google.inject.Inject;
import org.apache.twill.zookeeper.ZKClient;

/**
 * Service for getting locks that makes sure different types of locks have different zookeeper namespaces.
 */
public class LockService {
  private final ZKClient zkClient;

  @Inject
  private LockService(ZKClient zkClient) {
    this.zkClient = zkClient;
  }

  public ZKInterProcessReentrantLock getClusterCreateLock(String tenantId) {
    String path = Joiner.on('/').join(Constants.Lock.CLUSTER_NAMESPACE, "create", tenantId);
    return new ZKInterProcessReentrantLock(zkClient, path);
  }

  public ZKInterProcessReentrantLock getClusterLock(String tenantId, String clusterId) {
    String path = Joiner.on('/').join(Constants.Lock.CLUSTER_NAMESPACE, "clusters", tenantId, clusterId);
    return new ZKInterProcessReentrantLock(zkClient, path);
  }

  public ZKInterProcessReentrantLock getResourceLock(String tenantId, String pluginType, String pluginName,
                                                     String typeName, String resourceName) {
    String path = Joiner.on('/')
      .join(Constants.Lock.PLUGIN_NAMESPACE,
            "resources",
            tenantId,
            pluginType,
            pluginName,
            typeName,
            resourceName);
    return new ZKInterProcessReentrantLock(zkClient, path);
  }

  public ZKInterProcessReentrantLock getResourceSyncLock(String tenantId) {
    String path = Joiner.on('/').join(Constants.Lock.PLUGIN_NAMESPACE, "sync", tenantId);
    return new ZKInterProcessReentrantLock(zkClient, path);
  }

  public ZKInterProcessReentrantLock getJobLock(String tenantId, String clusterId) {
    String path = Joiner.on('/').join(Constants.Lock.TASK_NAMESPACE, "jobs", tenantId, clusterId);
    return new ZKInterProcessReentrantLock(zkClient, path);
  }

  public ZKInterProcessReentrantLock getTenantProvisionerLock() {
    return new ZKInterProcessReentrantLock(zkClient, Constants.Lock.TENANT_NAMESPACE);
  }
}
