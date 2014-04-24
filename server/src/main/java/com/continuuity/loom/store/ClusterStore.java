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
package com.continuuity.loom.store;

import com.continuuity.loom.cluster.Cluster;
import com.continuuity.loom.cluster.Node;
import com.continuuity.loom.scheduler.task.ClusterJob;
import com.continuuity.loom.scheduler.task.ClusterTask;
import com.continuuity.loom.scheduler.task.JobId;
import com.continuuity.loom.scheduler.task.TaskException;
import com.continuuity.loom.scheduler.task.TaskId;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Persistent store for clusters, jobs, tasks, and nodes.
 */
public interface ClusterStore {

  /**
   * Initialize the store.
   */
  void initialize();

  /**
   * Get a unique id that can be used for a new cluster.
   *
   * @return Unique id that can be used for a new cluster.
   */
  String getNewClusterId();

  /**
   * Get a unique job id that can be used for new {@link ClusterJob}s.
   *
   * @param clusterId Id of the cluster the job is for.
   * @return Unique job id.
   */
  JobId getNewJobId(String clusterId);

  /**
   * Get a unique task id that can be used for new {@link ClusterTask}s.
   *
   * @param jobId Id of the job the task is a part of.
   * @return Unique task id.
   */
  TaskId getNewTaskId(JobId jobId);

  /**
   * Get an immutable list of all clusters in the store.
   * @return All clusters in the store.
   * @throws Exception if there was a problem getting the clusters.
   */
  List<Cluster> getAllClusters() throws Exception;

  /**
   * Get an immutable list of all clusters owned by the given user.
   * @param ownerId Id of the user that owns the clusters.
   * @return List of all clusters owned by the user.
   * @throws Exception if there was a problem getting the clusters.
   */
  List<Cluster> getAllClusters(String ownerId) throws Exception;

  /**
   * Get a specific cluster by id.
   * @param clusterId Id of the cluster to find.
   * @return The cluster matching the id, or null if no cluster exists.
   * @throws Exception if there was a problem getting the cluster.
   */
  Cluster getCluster(String clusterId) throws Exception;

  /**
   * Get a specific cluster by id if it is owned by the given user id.
   * @param clusterId Id of the cluster to find.
   * @param ownerId Id of the user that owns the cluster.
   * @return The cluster matching the cluster and user ids, or null if no cluster exists.
   * @throws Exception if there was a problem getting the cluster.
   */
  Cluster getCluster(String clusterId, String ownerId) throws Exception;

  /**
   * Return whether or not the cluster with the given id exists or not, where existence is determined by whether or not
   * the cluster is in the store, and not by whether or not there is an active cluster with the given id.
   *
   * @param clusterId Id of the cluster.
   * @return True if the cluster exists, False if not.
   * @throws Exception
   */
  boolean clusterExists(String clusterId) throws Exception;

  /**
   * Return whether or not a cluster with the given id owned by the given user exists or not, where existence is
   * determined by whether or not the cluster is in the store, and not by whether or not there is an active cluster
   * with the given id owned by the given user.
   *
   * @param clusterId Id of the cluster.
   * @param ownerId Id of the owner of the cluster.
   * @return True if the cluster exists and is owned by the user, False if not.
   * @throws Exception
   */
  boolean clusterExists(String clusterId, String ownerId) throws Exception;

  /**
   * Write a cluster to the store using its id.
   * @param cluster Cluster to write.
   * @throws Exception if there was a problem writing the cluster.
   */
  void writeCluster(Cluster cluster) throws Exception;

  /**
   * Delete the cluster that has the given id.
   * @param clusterId Id of the cluster to delete.
   * @throws Exception if there was a problem deleting the cluster.
   */
  void deleteCluster(String clusterId) throws Exception;

  /**
   * Get a cluster job by its id.
   * @param jobId Id of the cluster job to get.
   * @return The cluster job with the given id, or null if none exists.
   * @throws TaskException if there was a problem getting the cluster job.
   */
  ClusterJob getClusterJob(JobId jobId) throws TaskException;

  /**
   * Get a map of jobid to cluster job for that job id.
   * @param jobIds Set of jobs ids to get.
   * @return Map of job id to cluster job. Job ids without a cluster job are not included.
   * @throws TaskException if there was a problem getting the cluster jobs.
   */
  Map<JobId, ClusterJob> getClusterJobs(Set<JobId> jobIds) throws TaskException;

  /**
   * Get all jobs performed or being performed on the given cluster.
   * @param clusterId Id of the cluster for which to get jobs.
   * @param limit Max number of jobs to return. If there are more, the most recent jobs will be returned. A
   *              negative number is interpreted as no limit.
   * @return List of all jobs performed or being performed on the given cluster, up to limit amount of jobs. If no jobs
   *         exist for the cluster, an empty list is returned.
   * @throws TaskException if there was a problem getting the cluster jobs.
   */
  List<ClusterJob> getClusterJobs(String clusterId, int limit) throws TaskException;

  /**
   * Get all jobs performed or being performed on the given cluster owned by the given user.
   * @param clusterId Id of the cluster for which to get jobs.
   * @param ownerId Id of the owner of the cluster.
   * @param limit Max number of jobs to return. If there are more, the most recent jobs will be returned. A
   *              negative number is interpreted as no limit.
   * @return List of all jobs performed or being performed on the given cluster owned by the user, up to limit amount
   *         of jobs. If no jobs exist for the cluster owned by the user, an empty list is returned.
   * @throws TaskException if there was a problem getting the cluster jobs.
   */
  List<ClusterJob> getClusterJobs(String clusterId, String ownerId, int limit) throws TaskException;

  /**
   * Write a cluster job to the store.
   * @param clusterJob The cluster job to write.
   * @throws TaskException if there was a problem writing the cluster job.
   */
  void writeClusterJob(ClusterJob clusterJob) throws TaskException;

  /**
   * Deletes the cluster job that has the given id.
   * @param jobId Id of the cluster job to delete.
   * @throws TaskException if there was a problem deleting the cluster job.
   */
  void deleteClusterJob(JobId jobId) throws TaskException;

  /**
   * Get a cluster task by its id.
   * @param taskId Id of the cluster task to get.
   * @return The cluster task with the given id, or null if none exists.
   * @throws TaskException if there was a problem getting the cluster task.
   */
  ClusterTask getClusterTask(TaskId taskId) throws TaskException;

  /**
   * Write a cluster task to the store using its id.
   * @param clusterTask The cluster task to write.
   * @throws TaskException if there was a problem writing the cluster task.
   */
  void writeClusterTask(ClusterTask clusterTask) throws TaskException;

  /**
   * Deletes the cluster task that has the given id.
   * @param taskId Id of the cluster task to delete.
   * @throws TaskException if there was a problem deleting the cluster task.
   */
  void deleteClusterTask(TaskId taskId) throws TaskException;

  /**
   * Get the node with the given id.
   * @param nodeId Id of the node to get.
   * @return The node with the given id, or null if none exists.
   * @throws Exception if there was a problem getting the node.
   */
  Node getNode(String nodeId) throws Exception;

  /**
   * Write the given node to the store.
   * @param node The node to write.
   * @throws Exception if there was a problem writing the node.
   */
  void writeNode(Node node) throws Exception;

  /**
   * Delete the node with the given id.
   * @param nodeId Id of the node to delete.
   * @throws Exception if there was a problem deleting the node.
   */
  void deleteNode(String nodeId) throws Exception;

  /**
   * Get an immutable set of all nodes belonging to a specific cluster.
   * @param clusterId Id of the cluster whose nodes will be fetched.
   * @return Set of all nodes belonging to specified cluster. Empty if no cluster or cluster nodes exist.
   * @throws Exception if there was a problem getting the cluster nodes.
   */
  Set<Node> getClusterNodes(String clusterId) throws Exception;

  /**
   * Get an immutable set of all nodes belonging to a specific cluster owned by a specific user.
   * @param clusterId Id of the cluster whose nodes will be fetched.
   * @param userId Id of the owner of the cluster.
   * @return Set of all nodes belonging to specified cluster owned by the specified user.
   *         Empty if no cluster or cluster nodes exist.
   * @throws Exception if there was a problem getting the cluster nodes.
   */
  Set<Node> getClusterNodes(String clusterId, String userId) throws Exception;

  /**
   * Returns an immutable set of all IN_PROGRESS tasks that were submitted before timestamp.
   * @param timestamp timestamp in milliseconds.
   * @return set of tasks.
   * @throws Exception
   */
  Set<ClusterTask> getRunningTasks(long timestamp) throws Exception;

  /**
   * Returns an immutable set of all ACTIVE or INCOMPLETE clusters that expire before timestamp.
   * @param timestamp timestamp in milliseconds.
   * @return set of clusters.
   * @throws Exception
   */
  Set<Cluster> getExpiringClusters(long timestamp) throws Exception;
}
