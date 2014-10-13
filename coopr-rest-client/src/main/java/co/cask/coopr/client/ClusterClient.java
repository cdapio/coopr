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

package co.cask.coopr.client;

import co.cask.coopr.cluster.ClusterDetails;
import co.cask.coopr.cluster.ClusterSummary;
import co.cask.coopr.http.request.AddServicesRequest;
import co.cask.coopr.http.request.ClusterConfigureRequest;
import co.cask.coopr.http.request.ClusterCreateRequest;
import co.cask.coopr.http.request.ClusterOperationRequest;
import co.cask.coopr.http.request.ClusterStatusResponse;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.util.List;

/**
 * The client API for manage clusters.
 */
public interface ClusterClient {

  /**
   * Provides a summary of details about all clusters visible to a user.
   * If there are no visible clusters, returns empty list.
   *
   * @return list of {@link co.cask.coopr.cluster.ClusterSummary} objects
   * @throws IOException in case of a problem or the connection was aborted
   */
  List<ClusterSummary> getClusters() throws IOException;

  /**
   * Provides full details about a cluster by id.
   *
   * @param clusterId String value of a cluster id
   * @return {@link co.cask.coopr.cluster.ClusterDetails} object
   * @throws IOException in case of a problem or the connection was aborted
   */
  ClusterDetails getCluster(String clusterId) throws IOException;

  /**
   * Deletes specified cluster by id.
   *
   * @param clusterId String value of a cluster id
   * @throws IOException in case of a problem or the connection was aborted
   */
  void deleteCluster(String clusterId) throws IOException;

  /**
   * Deletes specified cluster by id with optional provider fields.
   *
   * @param clusterId String value of a cluster id
   * @param clusterOperationRequest Request to delete the cluster, containing optional provider fields
   * @throws IOException in case of a problem or the connection was aborted
   */
  void deleteCluster(String clusterId, ClusterOperationRequest clusterOperationRequest) throws IOException;

  /**
   * Creates new Cluster according to the specified parameters.
   *
   * @param clusterCreateRequest {@link co.cask.coopr.http.request.ClusterCreateRequest} object
   * @return new Cluster id
   * @throws IOException in case of a problem or the connection was aborted
   */
  String createCluster(ClusterCreateRequest clusterCreateRequest) throws IOException;

  /**
   * Retrieves the status of a cluster by id.
   *
   * @param clusterId String value of a cluster id
   * @return {@link co.cask.coopr.http.request.ClusterStatusResponse} object
   * @throws IOException in case of a problem or the connection was aborted
   */
  ClusterStatusResponse getClusterStatus(String clusterId) throws IOException;

  /**
   * Retrieves the configuration of a cluster by id.
   *
   * @param clusterId String value of a cluster id
   * @return {@link com.google.gson.JsonObject} object
   * @throws IOException in case of a problem or the connection was aborted
   */
  JsonObject getClusterConfig(String clusterId) throws IOException;

  /**
   * Sets the config used by an active cluster.
   *
   * @param clusterId String value of a cluster id
   * @param clusterConfigureRequest Request to configure the cluster, containing the new config and additional options
   * @throws IOException in case of a problem or the connection was aborted
   */
  void setClusterConfig(String clusterId, ClusterConfigureRequest clusterConfigureRequest) throws IOException;

  /**
   * Retrieves the names of services placed on the cluster by id.
   *
   * @param clusterId String value of a cluster id
   * @return Set of service names
   * @throws IOException in case of a problem or the connection was aborted
   */
  List<String> getClusterServices(String clusterId) throws IOException;

  /**
   * Sync the cluster template of the cluster to the current version of the cluster template by the cluster id.
   *
   * @param clusterId String value of a cluster id
   * @throws IOException in case of a problem or the connection was aborted
   */
  void syncClusterTemplate(String clusterId) throws IOException;

  /**
   * Sets a cluster expiration timestamp.
   *
   * @param clusterId String value of a cluster id
   * @param expireTime timestamp in milliseconds to set the expire time to
   * @throws IOException in case of a problem or the connection was aborted
   */
  void setClusterExpireTime(String clusterId, long expireTime) throws IOException;

  /**
   * Starts a specific service on a cluster.
   *
   * @param clusterId String value of a cluster id
   * @param serviceId String value of a specific service id
   * @throws IOException in case of a problem or the connection was aborted
   */
  void startServiceOnCluster(String clusterId, String serviceId) throws IOException;

  /**
   * Starts a specific service on a cluster with an additional provider fields.
   *
   * @param clusterId String value of a cluster id
   * @param serviceId String value of a specific service id
   * @param clusterOperationRequest Service action request, containing optional provider fields
   * @throws IOException in case of a problem or the connection was aborted
   */
  void startServiceOnCluster(String clusterId, String serviceId, ClusterOperationRequest clusterOperationRequest)
    throws IOException;

  /**
   * Stops a specific service on a cluster.
   *
   * @param clusterId String value of a cluster id
   * @param serviceId String value of a specific service id
   * @throws IOException in case of a problem or the connection was aborted
   */
  void stopServiceOnCluster(String clusterId, String serviceId) throws IOException;

  /**
   * Stops a specific service on a cluster with an additional provider fields.
   *
   * @param clusterId String value of a cluster id
   * @param serviceId String value of a specific service id
   * @param clusterOperationRequest Service action request, containing optional provider fields
   * @throws IOException in case of a problem or the connection was aborted
   */
  void stopServiceOnCluster(String clusterId, String serviceId, ClusterOperationRequest clusterOperationRequest)
    throws IOException;

  /**
   * Restarts a specific service on a cluster.
   *
   * @param clusterId String value of a cluster id
   * @param serviceId String value of a specific service id
   * @throws IOException in case of a problem or the connection was aborted
   */
  void restartServiceOnCluster(String clusterId, String serviceId) throws IOException;

  /**
   * Restarts a specific service on a cluster with an additional provider fields.
   *
   * @param clusterId String value of a cluster id
   * @param serviceId String value of a specific service id
   * @param clusterOperationRequest Service action request, containing optional provider fields
   * @throws IOException in case of a problem or the connection was aborted
   */
  void restartServiceOnCluster(String clusterId, String serviceId, ClusterOperationRequest clusterOperationRequest)
    throws IOException;

  /**
   * Adds specific services to a cluster.
   *
   * @param clusterId String value of a cluster id
   * @param addServicesRequest {@link co.cask.coopr.http.request.AddServicesRequest} object
   * @throws IOException in case of a problem or the connection was aborted
   */
  void addServicesOnCluster(String clusterId, AddServicesRequest addServicesRequest) throws IOException;
}
