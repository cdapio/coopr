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
package co.cask.coopr.scheduler;

import co.cask.coopr.cluster.Cluster;
import co.cask.coopr.cluster.ClusterService;
import co.cask.coopr.cluster.Node;
import co.cask.coopr.common.conf.Configuration;
import co.cask.coopr.common.conf.Constants;
import co.cask.coopr.common.queue.Element;
import co.cask.coopr.common.queue.QueueGroup;
import co.cask.coopr.common.queue.QueueService;
import co.cask.coopr.common.queue.QueueType;
import co.cask.coopr.common.queue.QueuedElement;
import co.cask.coopr.http.request.ClusterOperationRequest;
import co.cask.coopr.scheduler.task.ClusterTask;
import co.cask.coopr.scheduler.task.NodeService;
import co.cask.coopr.scheduler.task.TaskId;
import co.cask.coopr.scheduler.task.TaskService;
import co.cask.coopr.store.cluster.ClusterStore;
import co.cask.coopr.store.cluster.ClusterStoreService;
import com.google.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Run cleanup tasks.  Leader election is performed between servers sharing a zookeeper quorum so this only happens
 * on the leader server.  If an id increment is specified, will only clean up clusters that the server could have
 * created to prevent conflicts with other servers.
 */
public class ClusterCleanup implements Runnable {
  private static final Logger LOG = LoggerFactory.getLogger(ClusterCleanup.class);

  private final ClusterService clusterService;
  private final ClusterStore clusterStore;
  private final NodeService nodeService;
  private final TaskService taskService;
  private final QueueGroup jobQueues;
  private final QueueGroup provisionerQueues;
  private final long taskTimeout;
  private final long myMod;
  private final long incrementBy;

  @Inject
  private ClusterCleanup(ClusterStoreService clusterStoreService,
                         ClusterService clusterService,
                         NodeService nodeService,
                         TaskService taskService,
                         QueueService queueService,
                         Configuration conf) {
    this(clusterStoreService.getSystemView(), clusterService, nodeService, taskService,
         queueService.getQueueGroup(QueueType.JOB),
         queueService.getQueueGroup(QueueType.PROVISIONER),
         conf.getLong(Constants.TASK_TIMEOUT_SECS),
         conf.getLong(Constants.ID_START_NUM),
         conf.getLong(Constants.ID_INCREMENT_BY));
  }

  // for unit tests
  ClusterCleanup(ClusterStore clusterStore,
                 ClusterService clusterService,
                 NodeService nodeService,
                 TaskService taskService,
                 QueueGroup jobQueues,
                 QueueGroup provisionerQueues,
                 long taskTimeout, long startId, long incrementBy) {
    this.clusterStore = clusterStore;
    this.clusterService = clusterService;
    this.nodeService = nodeService;
    this.taskService = taskService;
    this.jobQueues = jobQueues;
    this.provisionerQueues = provisionerQueues;
    this.taskTimeout = taskTimeout;
    this.incrementBy = incrementBy;
    this.myMod = startId % incrementBy;
    LOG.info("Task timeout in seconds = {}", this.taskTimeout);
  }

  @Override
  public void run() {
    try {
      long currentTime = System.currentTimeMillis();

      for (String queueName : provisionerQueues.getQueueNames()) {
        timeoutTasks(queueName, currentTime);
      }

      expireClusters(currentTime);

    } catch (Throwable e) {
      LOG.error("Got exception: ", e);
    }
  }

  private void timeoutTasks(String queueName, long currentTime) {
    try {
      long taskFailTime = currentTime - TimeUnit.MILLISECONDS.convert(taskTimeout, TimeUnit.SECONDS);
      LOG.debug("Task fail time = {}", taskFailTime);

      Iterator<QueuedElement> beingConsumed = provisionerQueues.getBeingConsumed(queueName);

      while (beingConsumed.hasNext()) {
        QueuedElement queuedElement = beingConsumed.next();

        if (queuedElement.getStatusTime() > taskFailTime) {
          LOG.trace("Task {} with queue time {} has not timed out yet", queuedElement.getElement().getId(),
                    queuedElement.getStatusTime());
          continue;
        }

        String taskId = queuedElement.getElement().getId();
        ClusterTask task = clusterStore.getClusterTask(TaskId.fromString(taskId));

        if (task == null) {
          LOG.warn("provisioner queue contains task {} which is not in the cluster store, removing it from the queue.",
                   taskId);
          provisionerQueues.remove(queueName, taskId);
          continue;
        }

        if (provisionerQueues.remove(queueName, task.getTaskId())) {
          LOG.debug("Timing out task {} whose queue time is {}", task.getTaskId(), queuedElement.getStatusTime());

          // Fail the task
          String statusMessage = String.format("Timed out by after %d secs", taskTimeout);
          task.setStatusMessage(statusMessage);
          taskService.failTask(task, -1);

          // Update node status
          Node node = clusterStore.getNode(task.getNodeId());
          nodeService.failAction(node, "", statusMessage);

          // Schedule the job
          jobQueues.add(queueName, new Element(task.getJobId()));
        }
      }
    } catch (Throwable e) {
      LOG.error("Got exception: ", e);
    }
  }

  private void expireClusters(long currentTime) {
    try {
      LOG.debug("Expiring clusters older than {}", currentTime);

      Set<Cluster> clusters = clusterStore.getExpiringClusters(currentTime);

      if (clusters.isEmpty()) {
        LOG.debug("Got 0 clusters to be expired for time {}", currentTime);
        return;
      }

      LOG.debug("Got {} possible clusters to expire for time {}", clusters.size(), currentTime);

      for (Cluster cluster : clusters) {
        // mod check done here instead of db to avoid full table scan.
        if (Long.valueOf(cluster.getId()) % incrementBy == myMod) {
          LOG.debug("Deleting cluster {} with expire time {}", cluster.getId(), cluster.getExpireTime());
          clusterService.requestClusterDelete(cluster.getId(), cluster.getAccount(), new ClusterOperationRequest(null));
        }
      }
    } catch (Throwable e) {
      LOG.error("Got exception: ", e);
    }
  }
}
