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
import co.cask.coopr.cluster.Node;
import co.cask.coopr.common.queue.Element;
import co.cask.coopr.common.queue.GroupElement;
import co.cask.coopr.common.queue.QueueGroup;
import co.cask.coopr.common.queue.QueueService;
import co.cask.coopr.common.queue.QueueType;
import co.cask.coopr.common.queue.TrackingQueue;
import co.cask.coopr.common.zookeeper.IdService;
import co.cask.coopr.scheduler.dag.TaskNode;
import co.cask.coopr.scheduler.task.ClusterJob;
import co.cask.coopr.scheduler.task.ClusterTask;
import co.cask.coopr.scheduler.task.JobId;
import co.cask.coopr.scheduler.task.TaskId;
import co.cask.coopr.scheduler.task.TaskService;
import co.cask.coopr.spec.ProvisionerAction;
import co.cask.coopr.spec.service.Service;
import co.cask.coopr.store.cluster.ClusterStore;
import co.cask.coopr.store.cluster.ClusterStoreService;
import com.google.common.base.Function;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Schedule clusters to be provisioned. Polls a queue that contains elements specifying what cluster action needs to
 * be performed on what cluster. The scheduler determines a plan for how to perform the cluster action and stores
 * all plan information into a cluster job, then writes to a queue to tell the {@link JobScheduler} to start
 * scheduling tasks to perform the cluster action.
 */
public class ClusterScheduler implements Runnable {

  private static final Logger LOG = LoggerFactory.getLogger(ClusterScheduler.class);

  private final String id;
  private final ClusterStore clusterStore;
  private final TaskService taskService;
  private final IdService idService;
  private final QueueGroup clusterQueues;

  private final Actions actions = Actions.getInstance();

  @Inject
  private ClusterScheduler(@Named("scheduler.id") String id,
                           ClusterStoreService clusterStoreService,
                           TaskService taskService,
                           IdService idService,
                           QueueService queueService) {
    this.id = id;
    this.clusterStore = clusterStoreService.getSystemView();
    this.taskService = taskService;
    this.idService = idService;
    this.clusterQueues = queueService.getQueueGroup(QueueType.CLUSTER);
  }

  @Override
  public void run() {
    try {
      Iterator<GroupElement> clusterIter = clusterQueues.takeIterator(id);
      while (clusterIter.hasNext()) {
        GroupElement gElement = clusterIter.next();
        Element clusterElement = gElement.getElement();
        Cluster cluster = clusterStore.getCluster(clusterElement.getId());
        ClusterJob job = clusterStore.getClusterJob(JobId.fromString(cluster.getLatestJobId()));
        ClusterAction clusterAction = ClusterAction.valueOf(clusterElement.getValue());
        LOG.debug("Got cluster {} with action {}", cluster.getName(), clusterAction);
        try {

          List<ProvisionerAction> actionOrder = actions.getActionOrder().get(clusterAction);
          if (actionOrder == null) {
            LOG.error("Cluster action {} does not have any provisioner actions defined", clusterAction);
            clusterQueues.recordProgress(id, gElement.getQueueName(), clusterElement.getId(),
                                        TrackingQueue.ConsumingStatus.FINISHED_SUCCESSFULLY, "No actions defined");
            continue;
          }

          Set<Node> clusterNodes = clusterStore.getClusterNodes(cluster.getId());
          if (clusterNodes == null || clusterNodes.isEmpty()) {
            LOG.error("Cluster {} has no nodes defined", cluster.getId());
            clusterQueues.recordProgress(id, gElement.getQueueName(), clusterElement.getId(),
                                        TrackingQueue.ConsumingStatus.FINISHED_SUCCESSFULLY, "No nodes defined");
            continue;
          }

          LOG.trace("Cluster {}", cluster);
          JobPlanner jobPlanner = new JobPlanner(job, clusterNodes);
          List<Set<TaskNode>> linearizedTasks = jobPlanner.linearizeDependentTasks();

          // Create cluster tasks.
          List<Set<ClusterTask>> clusterTasks = createClusterTasks(linearizedTasks, cluster, job,
                                                                   jobPlanner.getServiceMap(),
                                                                   clusterAction, jobPlanner.getNodeMap());

          // Make sure multiple actions on a same node do not happen simultaneously.
          clusterTasks = JobPlanner.deDupNodePerStage(clusterTasks);

          for (Set<ClusterTask> stageTasks : clusterTasks) {
            job.addStage(Sets.newHashSet(Iterables.transform(stageTasks, CLUSTER_TASK_STRING_FUNCTION)));
          }
          taskService.startJob(job, cluster);

          clusterQueues.recordProgress(id, gElement.getQueueName(), clusterElement.getId(),
                                      TrackingQueue.ConsumingStatus.FINISHED_SUCCESSFULLY, "Scheduled");
        } catch (Throwable e) {
          LOG.error("Got exception while scheduling. Failing the job: ", e);

          // Clear staged tasks, and fail task
          job.clearTasks();
          switch (clusterAction) {
            case CLUSTER_CREATE:
              taskService.failJobAndTerminateCluster(job, cluster, "Failed to schedule the action");
              break;
            default:
              // failed to plan means the job should fail, but state has already been changed so the cluster
              // state in the db is inconsistent with reality.
              // TODO: Should revert it here but need versioning or cluster history or something to that effect.
              taskService.failJobAndSetClusterStatus(job, cluster, Cluster.Status.INCONSISTENT,
                                                     "Failed to schedule the " + clusterAction + " operation.");
              break;
          }

          clusterQueues.recordProgress(id, gElement.getQueueName(), clusterElement.getId(),
                                      TrackingQueue.ConsumingStatus.FINISHED_SUCCESSFULLY,
                                      "Exception during scheduling");
        }
      }
    } catch (Throwable e) {
      LOG.error("Got exception:", e);
    }
  }

  List<Set<ClusterTask>> createClusterTasks(List<Set<TaskNode>> tasks, Cluster cluster, ClusterJob job,
                                            Map<String, Service> serviceMap, ClusterAction clusterAction,
                                            Map<String, Node> nodeMap)
    throws Exception {
    List<Set<ClusterTask>> runnableTasks = Lists.newArrayListWithExpectedSize(tasks.size());
    for (Set<TaskNode> taskNodes : tasks) {
      // Create tasks for a stage
      Set<ClusterTask> stageTasks = Sets.newHashSet();
      for (TaskNode taskNode : taskNodes) {
        // Get the config for the task
        Service service = serviceMap.get(taskNode.getService());
        // TODO: why is this a string in the taskNode instead of a ProvisionerAction
        ProvisionerAction action = ProvisionerAction.valueOf(taskNode.getTaskName());
        // if this is a service action task, but the service action is not defined, skip it.
        if (service != null && !service.getProvisionerActions().containsKey(action)) {
          LOG.debug("Not scheduling {} for job {} since the service has no {} action.",
                    taskNode, job.getJobId(), action);
          continue;
        }

        TaskId taskId = idService.getNewTaskId(JobId.fromString(job.getJobId()));
        ClusterTask task = new ClusterTask(action, taskId, taskNode.getHostId(), taskNode.getService(), clusterAction,
                                           cluster.getClusterTemplate().getName(), cluster.getAccount());
        clusterStore.writeClusterTask(task);
        stageTasks.add(task);
      }
      if (!stageTasks.isEmpty()) {
        runnableTasks.add(stageTasks);
      }
    }
    return runnableTasks;
  }

  private static final Function<ClusterTask, String> CLUSTER_TASK_STRING_FUNCTION =
    new Function<ClusterTask, String>() {
      @Override
      public String apply(ClusterTask clusterTask) {
        return clusterTask.getTaskId();
      }
    };
}
