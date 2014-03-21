/*
 * Copyright 2012-2014, Continuuity, Inc.
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
package com.continuuity.loom.scheduler.task;

import com.continuuity.loom.cluster.Cluster;
import com.continuuity.loom.cluster.Node;
import com.continuuity.loom.common.queue.Element;
import com.continuuity.loom.common.queue.TrackingQueue;
import com.continuuity.loom.common.zookeeper.lib.ZKInterProcessReentrantLock;
import com.continuuity.loom.conf.Constants;
import com.continuuity.loom.management.LoomStats;
import com.continuuity.loom.scheduler.ClusterAction;
import com.continuuity.loom.store.ClusterStore;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import org.apache.twill.zookeeper.ZKClient;
import org.apache.twill.zookeeper.ZKClients;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Set;

/**
 * Service for performing operations on clusters.
 */
public class ClusterService {
  private static final Logger LOG = LoggerFactory.getLogger(ClusterService.class);

  private final ClusterStore store;
  private final TrackingQueue clusterQueue;
  private final ZKClient zkClient;
  private final LoomStats loomStats;

  @Inject
  public ClusterService(ClusterStore store, @Named("cluster.queue") TrackingQueue clusterQueue, ZKClient zkClient,
                        LoomStats loomStats) {
    this.store = store;
    this.clusterQueue = clusterQueue;
    this.zkClient = ZKClients.namespace(zkClient, Constants.LOCK_NAMESPACE);
    this.loomStats = loomStats;
  }

  /**
   * Request deletion of a given cluster that the user has permission to delete.
   *
   * @param clusterId Id of the cluster to delete.
   * @param userId Id of the owner of the cluster, or the admin user id.
   * @throws Exception
   */
  public void requestClusterDelete(String clusterId, String userId) throws Exception {
    ZKInterProcessReentrantLock lock = new ZKInterProcessReentrantLock(zkClient, "/" + clusterId);
    lock.acquire();
    try {
      Cluster cluster = getUserCluster(clusterId, userId);
      JobId deleteJobId = store.getNewJobId(clusterId);
      ClusterJob deleteJob = new ClusterJob(deleteJobId, ClusterAction.CLUSTER_DELETE);
      deleteJob.setJobStatus(ClusterJob.Status.RUNNING);
      cluster.addJob(deleteJobId.getId());
      cluster.setStatus(Cluster.Status.PENDING);

      LOG.debug("Writing cluster {} to store with delete job {}", clusterId, deleteJobId);
      store.writeCluster(cluster);
      store.writeClusterJob(deleteJob);

      loomStats.getClusterStats().incrementStat(ClusterAction.CLUSTER_DELETE);
      clusterQueue.add(new Element(clusterId, ClusterAction.CLUSTER_DELETE.name()));
    } finally {
      lock.release();
    }
  }

  /**
   * Changes expire time of a cluster.
   * The new expire time has to be greater than the cluster create time, and the increment requested should be less
   * than the step size, and new expire time should not be greater than the max duration defined in the template.
   *
   * @param clusterId cluster Id.
   * @param userId user requesting the change.
   * @param expireTime new expire time.
   * @throws Exception
   */
  public void changeExpireTime(String clusterId, String userId, long expireTime) throws Exception {
    ZKInterProcessReentrantLock lock = new ZKInterProcessReentrantLock(zkClient, "/" + clusterId);
    lock.acquire();
    try {
      Cluster cluster = getUserCluster(clusterId, userId);

      if (cluster.getStatus() == Cluster.Status.TERMINATED) {
        throw new IllegalArgumentException("Cannot change expire time of terminated cluster");
      }

      if (cluster.getStatus() == Cluster.Status.PENDING) {
        throw new IllegalArgumentException("Cannot change expire time of cluster at this time");
      }

      if (expireTime < cluster.getCreateTime()) {
        throw new IllegalArgumentException("New expire time cannot be lesser than create time");
      }

      long step = cluster.getClusterTemplate().getAdministration().getLeaseDuration().getStep();
      step = step == 0 ? Long.MAX_VALUE : step;

      long clusterMaxDuration = cluster.getClusterTemplate().getAdministration().getLeaseDuration().getMax();
      if (clusterMaxDuration != 0 && expireTime - cluster.getExpireTime() > step) {
        throw new IllegalArgumentException("New expire time cannot be greater than allowed increment");
      }

      if (clusterMaxDuration != 0 && cluster.getCreateTime() + clusterMaxDuration < expireTime) {
        throw new IllegalArgumentException("Cluster cannot be prolonged beyond its max duration");
      }

      cluster.setExpireTime(expireTime);
      LOG.debug("Prolonging lease of cluster {} by {} to {}", clusterId, expireTime, cluster.getExpireTime());
      store.writeCluster(cluster);
    } finally {
      lock.release();
    }
  }

  /**
   * Get a specific cluster that the user has permission to get, or null if no such cluster exists.
   *
   * @param clusterId Id of the cluster to get.
   * @param userId Id of the owner of the cluster, or the admin user id.
   * @return Cluster with the id given and owned by the user given, or null if no such cluster exists.
   * @throws Exception
   */
  public Cluster getUserCluster(String clusterId, String userId) throws Exception {
    Cluster cluster;
    if (userId.equals(Constants.ADMIN_USER) || userId.equals(Constants.SYSTEM_USER)) {
      cluster = store.getCluster(clusterId);
    } else {
      cluster = store.getCluster(clusterId, userId);
    }
    return cluster;
  }

  /**
   * Get all the clusters that the user has permission to get.
   *
   * @param userId Id of the user.
   * @return List of all clusters owned by the user, or list of all clusters if the user is the admin.
   * @throws Exception
   */
  public List<Cluster> getAllUserClusters(String userId) throws Exception {
    List<Cluster> clusters;
    if (userId.equals(Constants.ADMIN_USER) || userId.equals(Constants.SYSTEM_USER)) {
      clusters = store.getAllClusters();
    } else {
      clusters = store.getAllClusters(userId);
    }
    return clusters;
  }

  /**
   * Get all the nodes in a cluster that the user has permission to get.
   *
   * @param clusterId Id of the cluster.
   * @param userId Id of the user.
   * @return Set of all nodes in the cluster owned by the user or the user is the admin.
   * @throws Exception
   */
  public Set<Node> getClusterNodes(String clusterId, String userId) throws Exception {
    if (userId.equals(Constants.ADMIN_USER) || userId.equals(Constants.SYSTEM_USER)) {
      return store.getClusterNodes(clusterId);
    } else {
      return store.getClusterNodes(clusterId, userId);
    }
  }
}
