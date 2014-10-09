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

package co.cask.coopr.client.rest;

import co.cask.coopr.client.ClusterClient;
import co.cask.coopr.client.rest.request.HttpDeleteWithContent;
import co.cask.coopr.cluster.ClusterSummary;
import co.cask.coopr.http.request.AddServicesRequest;
import co.cask.coopr.http.request.ClusterConfigureRequest;
import co.cask.coopr.http.request.ClusterCreateRequest;
import co.cask.coopr.http.request.ClusterOperationRequest;
import co.cask.coopr.http.request.ClusterStatusResponse;
import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableMap;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.ws.rs.core.MediaType;

/**
 * The {@link co.cask.coopr.client.ClusterClient} interface implementation based on the Rest requests to
 * the Coopr Rest API.
 */
public class ClusterRestClient extends RestClient implements ClusterClient {

  private static final Logger LOG = LoggerFactory.getLogger(ClusterRestClient.class);

  private static final String CLUSTER_ID_ATTRIBUTE_NAME = "id";
  private static final String EXPIRE_TIME_ATTRIBUTE_NAME = "expireTime";
  private static final String CLUSTERS_URL_SUFFIX = "clusters";

  public ClusterRestClient(RestClientConnectionConfig config, CloseableHttpClient httpClient) {
    super(config, httpClient);
  }

  @Override
  public List<ClusterSummary> getClusters() throws IOException {
    return getAll(CLUSTERS_URL_SUFFIX);
  }

  @Override
  public JsonObject getCluster(String clusterId) throws IOException {
    HttpGet getRequest = new HttpGet(getBaseURL().resolve(String.format("/%s/clusters/%s", getVersion(), clusterId)));
    CloseableHttpResponse httpResponse = execute(getRequest);
    JsonObject clusterDetails;
    try {
      RestClient.responseCodeAnalysis(httpResponse);
      clusterDetails =
        new JsonParser().parse(EntityUtils.toString(httpResponse.getEntity(), Charsets.UTF_8)).getAsJsonObject();
    } finally {
      httpResponse.close();
    }
    return clusterDetails;
  }

  @Override
  public void deleteCluster(String clusterId) throws IOException {
    deleteCluster(clusterId, null);
  }

  @Override
  public void deleteCluster(String clusterId, ClusterOperationRequest clusterOperationRequest) throws IOException {
    HttpDeleteWithContent deleteRequest =
      new HttpDeleteWithContent(getBaseURL().resolve(String.format("/%s/clusters/%s", getVersion(), clusterId)));
    if (clusterOperationRequest != null) {
      StringEntity stringEntity = new StringEntity(GSON.toJson(clusterOperationRequest), Charsets.UTF_8);
      deleteRequest.setEntity(stringEntity);
      LOG.debug("Added optional provider fields to the request body: {}.", stringEntity);
    }
    CloseableHttpResponse httpResponse = execute(deleteRequest);
    try {
      RestClient.responseCodeAnalysis(httpResponse);
    } finally {
      httpResponse.close();
    }
  }

  @Override
  public String createCluster(ClusterCreateRequest clusterCreateRequest) throws IOException {
    HttpPost postRequest = new HttpPost(getBaseURL().resolve(String.format("/%s/clusters", getVersion())));
    if (clusterCreateRequest != null) {
      StringEntity stringEntity = new StringEntity(GSON.toJson(clusterCreateRequest), Charsets.UTF_8);
      postRequest.setEntity(stringEntity);
      LOG.debug("Create cluster request with parameters {}.", clusterCreateRequest);
    }
    CloseableHttpResponse httpResponse = execute(postRequest);
    String newClusterId;
    try {
      RestClient.responseCodeAnalysis(httpResponse);
      JsonObject jsonContent =
        new JsonParser().parse(EntityUtils.toString(httpResponse.getEntity(), Charsets.UTF_8)).getAsJsonObject();
      newClusterId = jsonContent.get(CLUSTER_ID_ATTRIBUTE_NAME).getAsString();
    } finally {
      httpResponse.close();
    }
    return newClusterId;
  }

  @Override
  public ClusterStatusResponse getClusterStatus(String clusterId) throws IOException {
    HttpGet getRequest = new HttpGet(getBaseURL().resolve(
      String.format("/%s/clusters/%s/status", getVersion(), clusterId)));
    CloseableHttpResponse httpResponse = execute(getRequest);
    ClusterStatusResponse clusterStatusResponse;
    try {
      RestClient.responseCodeAnalysis(httpResponse);
      clusterStatusResponse = GSON.fromJson(EntityUtils.toString(httpResponse.getEntity(), Charsets.UTF_8),
                                            new TypeToken<ClusterStatusResponse>() { }.getType());
    } finally {
      httpResponse.close();
    }
    return clusterStatusResponse;
  }

  @Override
  public JsonObject getClusterConfig(String clusterId) throws IOException {
    HttpGet getRequest = new HttpGet(getBaseURL().resolve(
      String.format("/%s/clusters/%s/config", getVersion(), clusterId)));
    CloseableHttpResponse httpResponse = execute(getRequest);
    JsonObject clusterDetails;
    try {
      RestClient.responseCodeAnalysis(httpResponse);
      clusterDetails =
        new JsonParser().parse(EntityUtils.toString(httpResponse.getEntity(), Charsets.UTF_8)).getAsJsonObject();
    } finally {
      httpResponse.close();
    }
    return clusterDetails;
  }

  @Override
  public void setClusterConfig(String clusterId, ClusterConfigureRequest clusterConfigureRequest) throws IOException {
    HttpPut putRequest = new HttpPut(getBaseURL().resolve(
      String.format("/%s/clusters/%s/config", getVersion(), clusterId)));
    if (clusterConfigureRequest != null) {
      StringEntity stringEntity = new StringEntity(GSON.toJson(clusterConfigureRequest), Charsets.UTF_8);
      putRequest.setEntity(stringEntity);
      LOG.debug("Set cluster config request with additional options {}.", clusterConfigureRequest);
    }
    CloseableHttpResponse httpResponse = execute(putRequest);
    try {
      RestClient.responseCodeAnalysis(httpResponse);
    } finally {
      httpResponse.close();
    }
  }

  @Override
  public Set<String> getClusterServices(String clusterId) throws IOException {
    HttpGet getRequest = new HttpGet(getBaseURL().resolve(
      String.format("/%s/clusters/%s/services", getVersion(), clusterId)));
    CloseableHttpResponse httpResponse = execute(getRequest);
    Set<String> clusterServices;
    try {
      RestClient.responseCodeAnalysis(httpResponse);
      clusterServices = GSON.fromJson(EntityUtils.toString(httpResponse.getEntity(), Charsets.UTF_8),
                                            new TypeToken<Set<String>>() { }.getType());
    } finally {
      httpResponse.close();
    }
    return clusterServices != null ? clusterServices : new HashSet<String>();
  }

  @Override
  public void syncClusterTemplate(String clusterId) throws IOException {
    HttpPost postRequest =
      new HttpPost(getBaseURL().resolve(String.format("/%s/clusters/%s/clustertemplate/sync", getVersion(),
                                                      clusterId)));
    CloseableHttpResponse httpResponse = execute(postRequest);
    try {
      RestClient.responseCodeAnalysis(httpResponse);
    } finally {
      httpResponse.close();
    }
  }

  @Override
  public void setClusterExpireTime(String clusterId, long expireTime) throws IOException {
    HttpPost postRequest = new HttpPost(getBaseURL().resolve(String.format("/%s/clusters/%s", getVersion(),
                                                                           clusterId)));
    StringEntity entity = new StringEntity(GSON.toJson(ImmutableMap.of(EXPIRE_TIME_ATTRIBUTE_NAME, expireTime)));
    entity.setContentType(MediaType.APPLICATION_JSON);
    postRequest.setEntity(entity);
    LOG.debug("Set a cluster expiration timestamp to the new value {}.", expireTime);
    CloseableHttpResponse httpResponse = execute(postRequest);
    try {
      RestClient.responseCodeAnalysis(httpResponse);
    } finally {
      httpResponse.close();
    }
  }

  @Override
  public void startServiceOnCluster(String clusterId, String serviceId) throws IOException {
    startServiceOnCluster(clusterId, serviceId, null);
  }

  @Override
  public void startServiceOnCluster(String clusterId, String serviceId,
                                    ClusterOperationRequest clusterOperationRequest) throws IOException {
    URI startURI = getBaseURL().resolve(String.format("/%s/clusters/%s/services/%s/start", getVersion(), clusterId,
                                                      serviceId));
    serviceActionRequest(startURI, clusterOperationRequest);
  }

  @Override
  public void stopServiceOnCluster(String clusterId, String serviceId) throws IOException {
    stopServiceOnCluster(clusterId, serviceId, null);
  }

  @Override
  public void stopServiceOnCluster(String clusterId, String serviceId, ClusterOperationRequest clusterOperationRequest)
    throws IOException {
    URI stopURL = getBaseURL().resolve(String.format("/%s/clusters/%s/services/%s/stop", getVersion(), clusterId,
                                                     serviceId));
    serviceActionRequest(stopURL, clusterOperationRequest);
  }

  @Override
  public void restartServiceOnCluster(String clusterId, String serviceId) throws IOException {
    restartServiceOnCluster(clusterId, serviceId, null);
  }

  @Override
  public void restartServiceOnCluster(String clusterId, String serviceId,
                                      ClusterOperationRequest clusterOperationRequest) throws IOException {
    URI restartURL = getBaseURL().resolve(String.format("/%s/clusters/%s/services/%s/restart", getVersion(), clusterId,
                                                        serviceId));
    serviceActionRequest(restartURL, clusterOperationRequest);
  }

  private void serviceActionRequest(URI url, ClusterOperationRequest clusterOperationRequest)
    throws IOException {
    HttpPost postRequest = new HttpPost(url);
    if (clusterOperationRequest != null) {
      StringEntity stringEntity = new StringEntity(GSON.toJson(clusterOperationRequest), Charsets.UTF_8);
      postRequest.setEntity(stringEntity);
      LOG.debug("Added optional provider fields to the request body: {}.", stringEntity);
    }
    CloseableHttpResponse httpResponse = execute(postRequest);
    try {
      RestClient.responseCodeAnalysis(httpResponse);
    } finally {
      httpResponse.close();
    }
  }

  @Override
  public void addServicesOnCluster(String clusterId, AddServicesRequest addServicesRequest) throws IOException {
    HttpPost postRequest = new HttpPost(getBaseURL().resolve(
      String.format("/%s/clusters/%s/services", getVersion(), clusterId)));
    if (addServicesRequest != null) {
      StringEntity stringEntity = new StringEntity(GSON.toJson(addServicesRequest), Charsets.UTF_8);
      postRequest.setEntity(stringEntity);
      LOG.debug("Add services on the cluster request {}.", addServicesRequest);
    }
    CloseableHttpResponse httpResponse = execute(postRequest);
    try {
      RestClient.responseCodeAnalysis(httpResponse);
    } finally {
      httpResponse.close();
    }
  }
}
