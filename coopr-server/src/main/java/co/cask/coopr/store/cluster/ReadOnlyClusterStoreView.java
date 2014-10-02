/*
 * Copyright © 2014 Cask Data, Inc.
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

package co.cask.coopr.store.cluster;

import co.cask.coopr.cluster.Cluster;
import co.cask.coopr.cluster.ClusterSummary;
import co.cask.coopr.cluster.Node;
import co.cask.coopr.scheduler.task.ClusterJob;

import java.io.IOException;
import java.util.List;
import java.util.Set;

/**
 * A read-only view of the cluster store as seen by a given account.
 */
public interface ReadOnlyClusterStoreView {

  /**
   * Get an immutable list of all clusters in the store.
   *
   * @return All clusters in the store.
   * @throws java.io.IOException if there was a problem getting the clusters.
   */
  List<Cluster> getAllClusters() throws IOException;

  /**
   * Get an immutable list of summaries of all clusters in the store.
   *
   * @return Immutable list of summaries of all clusters.
   * @throws IOException if there was a problem getting the cluster summaries.
   */
  List<ClusterSummary> getAllClusterSummaries() throws IOException;

  /**
   * Get an immutable list of summaries of all clusters in the store that are in one of the given cluster states.
   *
   * @param states returned clusters must be in one of these states.
   * @return Immutable list of summaries of all clusters in the store that are in one of the given cluster states.
   * @throws IOException if there was a problem getting the cluster summaries.
   */
  List<ClusterSummary> getAllClusterSummaries(Set<Cluster.Status> states) throws IOException;

  /**
   * Get an immutable list of all clusters in the store that are not in the terminated state.
   *
   * @return All clusters in the store.
   * @throws IOException if there was a problem getting the clusters.
   */
  List<Cluster> getNonTerminatedClusters() throws IOException;

  /**
   * Get a specific cluster by id.
   *
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
   * Get all jobs performed or being performed on the given cluster owned by the given user.
   *
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
   *
   * @param clusterId Id of the cluster whose nodes will be fetched.
   * @return Set of all nodes belonging to specified cluster. Empty if no cluster or cluster nodes exist.
   * @throws IOException if there was a problem getting the cluster nodes.
   */
  Set<Node> getClusterNodes(String clusterId) throws IOException;
}
