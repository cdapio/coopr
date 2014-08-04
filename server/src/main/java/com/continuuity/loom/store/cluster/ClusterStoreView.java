package com.continuuity.loom.store.cluster;

import com.continuuity.loom.cluster.Cluster;
import com.continuuity.loom.cluster.Node;
import com.continuuity.loom.scheduler.task.ClusterJob;

import java.io.IOException;
import java.util.List;
import java.util.Set;

/**
 * A view of the cluster store as seen by a given account.
 */
public interface ClusterStoreView {
  /**
   * Get an immutable list of all clusters in the store.
   * @return All clusters in the store.
   * @throws IOException if there was a problem getting the clusters.
   */
  List<Cluster> getAllClusters() throws IOException;

  /**
   * Get an immutable list of all clusters in the store that are not in the terminated state.
   * @return All clusters in the store.
   * @throws IOException if there was a problem getting the clusters.
   */
  List<Cluster> getNonTerminatedClusters() throws IOException;

  /**
   * Get a specific cluster by id.
   * @param clusterId Id of the cluster to find.
   * @return The cluster matching the id, or null if no cluster exists.
   * @throws IOException if there was a problem getting the cluster.
   */
  Cluster getCluster(String clusterId) throws IOException;

  /**
   * Return whether or not the cluster with the given id exists or not, where existence is determined by whether or not
   * the cluster is in the store, and not by whether or not there is an active cluster with the given id.
   *
   * @param clusterId Id of the cluster.
   * @return True if the cluster exists, False if not.
   * @throws IOException
   */
  boolean clusterExists(String clusterId) throws IOException;

  /**
   * Write a cluster to the store using its id.
   * @param cluster Cluster to write.
   * @throws IOException if there was a problem writing the cluster.
   */
  void writeCluster(Cluster cluster) throws IllegalAccessException, IOException;

  /**
   * Delete the cluster that has the given id.
   * @param clusterId Id of the cluster to delete.
   * @throws IOException if there was a problem deleting the cluster.
   */
  void deleteCluster(String clusterId) throws IOException;

  /**
   * Get all jobs performed or being performed on the given cluster owned by the given user.
   * @param clusterId Id of the cluster for which to get jobs.
   * @param limit Max number of jobs to return. If there are more, the most recent jobs will be returned. A
   *              negative number is interpreted as no limit.
   * @return List of all jobs performed or being performed on the given cluster owned by the user, up to limit amount
   *         of jobs. If no jobs exist for the cluster owned by the user, an empty list is returned.
   * @throws IOException if there was a problem getting the cluster jobs.
   */
  List<ClusterJob> getClusterJobs(String clusterId, int limit) throws IOException;

  /**
   * Get an immutable set of all nodes belonging to a specific cluster.
   * @param clusterId Id of the cluster whose nodes will be fetched.
   * @return Set of all nodes belonging to specified cluster. Empty if no cluster or cluster nodes exist.
   * @throws IOException if there was a problem getting the cluster nodes.
   */
  Set<Node> getClusterNodes(String clusterId) throws IOException;
}
