package co.cask.coopr.store.cluster;

import co.cask.coopr.cluster.Cluster;
import co.cask.coopr.cluster.Node;
import co.cask.coopr.scheduler.task.ClusterJob;
import co.cask.coopr.scheduler.task.ClusterTask;
import co.cask.coopr.scheduler.task.JobId;
import co.cask.coopr.scheduler.task.TaskId;

import java.io.IOException;
import java.util.List;
import java.util.Set;

/**
 * A full view of the cluster store for management purposes. Used within the system for managing jobs and tasks
 * for operating on clusters.
 */
public interface ClusterStore extends ClusterStoreView {
  /**
   * Get a cluster job by its id.
   * @param jobId Id of the cluster job to get.
   * @return The cluster job with the given id, or null if none exists.
   * @throws java.io.IOException if there was a problem getting the cluster job.
   */
  ClusterJob getClusterJob(JobId jobId) throws IOException;

  /**
   * Write a cluster job to the store.
   * @param clusterJob The cluster job to write.
   * @throws IOException if there was a problem writing the cluster job.
   */
  void writeClusterJob(ClusterJob clusterJob) throws IOException;

  /**
   * Deletes the cluster job that has the given id.
   * @param jobId Id of the cluster job to delete.
   * @throws IOException if there was a problem deleting the cluster job.
   */
  void deleteClusterJob(JobId jobId) throws IOException;

  /**
   * Get a cluster task by its id.
   * @param taskId Id of the cluster task to get.
   * @return The cluster task with the given id, or null if none exists.
   * @throws IOException if there was a problem getting the cluster task.
   */
  ClusterTask getClusterTask(TaskId taskId) throws IOException;

  /**
   * Retrieves tasks according to the {@code filter} filters.
   *
   * @param filter the object wrapper around filters
   * @return list of cluster tasks according to the {@code filter} filters
   * @throws IOException if there was a problem getting the cluster task.
   */
  List<ClusterTask> getClusterTasks(ClusterTaskFilter filter) throws IOException;

  /**
   * Write a cluster task to the store using its id.
   * @param clusterTask The cluster task to write.
   * @throws IOException if there was a problem writing the cluster task.
   */
  void writeClusterTask(ClusterTask clusterTask) throws IOException;

  /**
   * Deletes the cluster task that has the given id.
   * @param taskId Id of the cluster task to delete.
   * @throws IOException if there was a problem deleting the cluster task.
   */
  void deleteClusterTask(TaskId taskId) throws IOException;

  /**
   * Get the node with the given id.
   * @param nodeId Id of the node to get.
   * @return The node with the given id, or null if none exists.
   * @throws IOException if there was a problem getting the node.
   */
  Node getNode(String nodeId) throws IOException;

  /**
   * Write the given node to the store.
   * @param node The node to write.
   * @throws IOException if there was a problem writing the node.
   */
  void writeNode(Node node) throws IOException;

  /**
   * Delete the node with the given id.
   * @param nodeId Id of the node to delete.
   * @throws IOException if there was a problem deleting the node.
   */
  void deleteNode(String nodeId) throws IOException;

  /**
   * Returns an immutable set of all IN_PROGRESS tasks that were submitted before timestamp.
   * @param timestamp timestamp in milliseconds.
   * @return set of tasks.
   * @throws IOException
   */
  Set<ClusterTask> getRunningTasks(long timestamp) throws IOException;

  /**
   * Returns an immutable set of all ACTIVE or INCOMPLETE clusters that expire before timestamp.
   * @param timestamp timestamp in milliseconds.
   * @return set of clusters.
   * @throws IOException
   */
  Set<Cluster> getExpiringClusters(long timestamp) throws IOException;
}
