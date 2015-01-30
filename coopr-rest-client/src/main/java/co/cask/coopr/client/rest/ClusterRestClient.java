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
import co.cask.coopr.cluster.ClusterDetails;
import co.cask.coopr.cluster.ClusterSummary;
import co.cask.coopr.http.request.AddServicesRequest;
import co.cask.coopr.http.request.ClusterConfigureRequest;
import co.cask.coopr.http.request.ClusterCreateRequest;
import co.cask.coopr.http.request.ClusterOperationRequest;
import co.cask.coopr.http.request.ClusterStatusResponse;
import com.google.common.base.Charsets;
import com.google.common.base.Preconditions;
import com.google.common.base.Supplier;
import com.google.common.collect.ImmutableMap;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.util.List;
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

  public ClusterRestClient(Supplier<RestClientConnectionConfig> config, CloseableHttpClient httpClient) {
    super(config, httpClient);
  }

  public ClusterRestClient(Supplier<RestClientConnectionConfig> config, CloseableHttpClient httpClient, Gson gson) {
    super(config, httpClient, gson);
  }

  @Override
  public List<ClusterSummary> getClusters() throws IOException {
    return getAll(CLUSTERS_URL_SUFFIX, new TypeToken<List<ClusterSummary>>() { }.getType());
  }

  @Override
  public ClusterDetails getCluster(String clusterId) throws IOException {
    return getSingle(CLUSTERS_URL_SUFFIX, clusterId, ClusterDetails.class);
  }

  @Override
  public void deleteCluster(String clusterId) throws IOException {
    deleteCluster(clusterId, null);
  }

  @Override
  public void deleteCluster(String clusterId, ClusterOperationRequest clusterOperationRequest) throws IOException {
    HttpDeleteWithContent deleteRequest =
      new HttpDeleteWithContent(resolveURL(String.format("/clusters/%s", clusterId)));
    addRequestBody(deleteRequest, clusterOperationRequest);
    CloseableHttpResponse httpResponse = execute(deleteRequest);
    try {
      RestClient.analyzeResponseCode(httpResponse);
    } finally {
      httpResponse.close();
    }
  }

  @Override
  public String createCluster(ClusterCreateRequest clusterCreateRequest) throws IOException {
    Preconditions.checkArgument(clusterCreateRequest != null, "ClusterCreateRequest object couldn't be null.");
    HttpPost postRequest = new HttpPost(resolveURL("/clusters"));
    addRequestBody(postRequest, clusterCreateRequest);
    CloseableHttpResponse httpResponse = execute(postRequest);
    String newClusterId;
    try {
      RestClient.analyzeResponseCode(httpResponse);
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
    return getSingle(resolveURL(String.format("/clusters/%s/status", clusterId)), ClusterStatusResponse.class);
  }

  @Override
  public JsonObject getClusterConfig(String clusterId) throws IOException {
    return getSingle(resolveURL(String.format("/clusters/%s/config", clusterId)), JsonObject.class);
  }

  @Override
  public void setClusterConfig(String clusterId, ClusterConfigureRequest clusterConfigureRequest) throws IOException {
    Preconditions.checkArgument(clusterConfigureRequest != null);
    HttpPut putRequest = new HttpPut(resolveURL(String.format("/clusters/%s/config", clusterId)));
    addRequestBody(putRequest, clusterConfigureRequest);
    CloseableHttpResponse httpResponse = execute(putRequest);
    try {
      RestClient.analyzeResponseCode(httpResponse);
    } finally {
      httpResponse.close();
    }
  }

  @Override
  public List<String> getClusterServices(String clusterId) throws IOException {
    return getAll(resolveURL(String.format("/clusters/%s/services", clusterId)),
                  new TypeToken<List<String>>() { }.getType());
  }

  @Override
  public void syncClusterTemplate(String clusterId) throws IOException {
    HttpPost postRequest = new HttpPost(resolveURL(String.format("/clusters/%s/clustertemplate/sync", clusterId)));
    CloseableHttpResponse httpResponse = execute(postRequest);
    try {
      RestClient.analyzeResponseCode(httpResponse);
    } finally {
      httpResponse.close();
    }
  }

  @Override
  public void setClusterExpireTime(String clusterId, long expireTime) throws IOException {
    HttpPost postRequest = new HttpPost(resolveURL(String.format("/clusters/%s", clusterId)));
    StringEntity entity = new StringEntity(getGson().toJson(ImmutableMap.of(EXPIRE_TIME_ATTRIBUTE_NAME, expireTime)));
    entity.setContentType(MediaType.APPLICATION_JSON);
    postRequest.setEntity(entity);
    LOG.debug("Set a cluster expiration timestamp to the new value {}.", expireTime);
    CloseableHttpResponse httpResponse = execute(postRequest);
    try {
      RestClient.analyzeResponseCode(httpResponse);
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
    URI startURI = resolveURL(String.format("/clusters/%s/services/%s/start", clusterId, serviceId));
    serviceActionRequest(startURI, clusterOperationRequest);
  }

  @Override
  public void stopServiceOnCluster(String clusterId, String serviceId) throws IOException {
    stopServiceOnCluster(clusterId, serviceId, null);
  }

  @Override
  public void stopServiceOnCluster(String clusterId, String serviceId, ClusterOperationRequest clusterOperationRequest)
    throws IOException {
    URI stopURL = resolveURL(String.format("/clusters/%s/services/%s/stop", clusterId, serviceId));
    serviceActionRequest(stopURL, clusterOperationRequest);
  }

  @Override
  public void restartServiceOnCluster(String clusterId, String serviceId) throws IOException {
    restartServiceOnCluster(clusterId, serviceId, null);
  }

  @Override
  public void restartServiceOnCluster(String clusterId, String serviceId,
                                      ClusterOperationRequest clusterOperationRequest) throws IOException {
    URI restartURL = resolveURL(String.format("/clusters/%s/services/%s/restart", clusterId, serviceId));
    serviceActionRequest(restartURL, clusterOperationRequest);
  }

  private void serviceActionRequest(URI url, ClusterOperationRequest clusterOperationRequest)
    throws IOException {
    HttpPost postRequest = new HttpPost(url);
    addRequestBody(postRequest, clusterOperationRequest);
    CloseableHttpResponse httpResponse = execute(postRequest);
    try {
      RestClient.analyzeResponseCode(httpResponse);
    } finally {
      httpResponse.close();
    }
  }

  @Override
  public void addServicesOnCluster(String clusterId, AddServicesRequest addServicesRequest) throws IOException {
    HttpPost postRequest = new HttpPost(resolveURL(String.format("/clusters/%s/services", clusterId)));
    addRequestBody(postRequest, addServicesRequest);
    CloseableHttpResponse httpResponse = execute(postRequest);
    try {
      RestClient.analyzeResponseCode(httpResponse);
    } finally {
      httpResponse.close();
    }
  }
}
