package co.cask.coopr.common.zookeeper;

import co.cask.coopr.common.conf.Constants;
import co.cask.coopr.common.zookeeper.lib.ReentrantDistributedLock;
import com.google.common.base.Joiner;
import com.google.inject.Inject;
import org.apache.twill.zookeeper.ZKClient;

import java.util.concurrent.locks.Lock;

/**
 * Service for getting locks that makes sure different types of locks have different zookeeper namespaces.
 */
public class LockService {
  private final ZKClient zkClient;

  @Inject
  private LockService(ZKClient zkClient) {
    this.zkClient = zkClient;
  }

  public Lock getClusterCreateLock(String tenantId) {
    String path = Joiner.on('/').join(Constants.Lock.CLUSTER_NAMESPACE, "create", tenantId);
    return new ReentrantDistributedLock(zkClient, path);
  }

  public Lock getClusterLock(String tenantId, String clusterId) {
    String path = Joiner.on('/').join(Constants.Lock.CLUSTER_NAMESPACE, "clusters", tenantId, clusterId);
    return new ReentrantDistributedLock(zkClient, path);
  }

  public Lock getResourceLock(String tenantId, String pluginType, String pluginName,
                                         String typeName, String resourceName) {
    String path = Joiner.on('/')
      .join(Constants.Lock.PLUGIN_NAMESPACE,
            "resources",
            tenantId,
            pluginType,
            pluginName,
            typeName,
            resourceName);
    return new ReentrantDistributedLock(zkClient, path);
  }

  public Lock getJobLock(String tenantId, String clusterId) {
    String path = Joiner.on('/').join(Constants.Lock.TASK_NAMESPACE, "jobs", tenantId, clusterId);
    return new ReentrantDistributedLock(zkClient, path);
  }

  public Lock getTenantProvisionerLock() {
    return new ReentrantDistributedLock(zkClient, Constants.Lock.TENANT_NAMESPACE);
  }
}
