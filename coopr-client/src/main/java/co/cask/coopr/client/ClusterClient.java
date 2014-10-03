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

import co.cask.coopr.client.model.ClusterConfigInfo;
import co.cask.coopr.client.model.ClusterInfo;
import co.cask.coopr.client.model.ClusterStatusInfo;
import co.cask.coopr.http.request.AddServicesRequest;
import co.cask.coopr.http.request.ClusterCreateRequest;

import java.util.List;
import java.util.Set;

/**
 * The client API for manage clusters.
 */
public interface ClusterClient {

  /**
   * Provides a summary of details about all clusters visible to a user.
   *
   * @return list of {@link co.cask.coopr.client.model.ClusterInfo} objects
   */
  List<ClusterInfo> getClusters();

  /**
   * Provides full details about a cluster by id.
   *
   * @param clusterId String value of a cluster id
   * @return {@link co.cask.coopr.client.model.ClusterInfo} object
   */
  ClusterInfo getCluster(String clusterId);

  /**
   * Deletes specified cluster by id.
   *
   * @param clusterId String value of a cluster id
   */
  void deleteCluster(String clusterId);

  /**
   * Creates new Cluster according to the specified parameters.
   *
   * @param cluster {@link co.cask.coopr.http.request.ClusterCreateRequest} object
   * @return new Cluster id
   */
  String createCluster(ClusterCreateRequest cluster);

  /**
   * Retrieves the status of a cluster by id.
   *
   * @param clusterId String value of a cluster id
   * @return {@link co.cask.coopr.client.model.ClusterStatusInfo} object
   */
  ClusterStatusInfo getClusterStatus(String clusterId);

  /**
   * Retrieves the configuration of a cluster by id.
   *
   * @param clusterId String value of a cluster id
   * @return {@link co.cask.coopr.client.model.ClusterConfigInfo} object
   */
  ClusterConfigInfo getClusterConfig(String clusterId);

  /**
   * Retrieves the names of services placed on the cluster by id.
   *
   * @param clusterId String value of a cluster id
   * @return Set of service names
   */
  Set<String> getClusterServices(String clusterId);

  /**
   * Sync the cluster template of the cluster to the current version of the cluster template by the cluster id.
   *
   * @param clusterId String value of a cluster id
   */
  void syncClusterTemplate(String clusterId);

  /**
   * Changes a cluster expiration timestamp.
   *
   * @param clusterId String value of a cluster id
   * @param expireTime long value of expiration time in milliseconds
   */
  void changeClusterExpireTime(String clusterId, long expireTime);

  /**
   * Starts a specific service on a cluster.
   *
   * @param clusterId String value of a cluster id
   * @param serviceId String value of a specific service id
   */
  void startServiceOnCluster(String clusterId, String serviceId);

  /**
   * Stops a specific service on a cluster.
   *
   * @param clusterId String value of a cluster id
   * @param serviceId String value of a specific service id
   */
  void stopServiceOnCluster(String clusterId, String serviceId);

  /**
   * Restarts a specific service on a cluster.
   *
   * @param clusterId String value of a cluster id
   * @param serviceId String value of a specific service id
   */
  void restartServiceOnCluster(String clusterId, String serviceId);

  /**
   * Adds specific services to a cluster.
   *
   * @param clusterId String value of a cluster id
   * @param addServicesRequest {@link co.cask.coopr.http.request.AddServicesRequest} object
   */
  void addServicesOnCluster(String clusterId, AddServicesRequest addServicesRequest);
}
