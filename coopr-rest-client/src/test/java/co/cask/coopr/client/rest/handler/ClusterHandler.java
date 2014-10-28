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

package co.cask.coopr.client.rest.handler;

import co.cask.coopr.Entities;
import co.cask.coopr.client.rest.ClusterRestClientTest;
import co.cask.coopr.client.rest.RestClientTest;
import co.cask.coopr.cluster.ClusterDetails;
import co.cask.coopr.cluster.ClusterSummary;
import co.cask.coopr.http.request.AddServicesRequest;
import co.cask.coopr.http.request.ClusterConfigureRequest;
import co.cask.coopr.http.request.ClusterCreateRequest;
import co.cask.coopr.http.request.ClusterStatusResponse;
import co.cask.coopr.scheduler.ClusterAction;
import co.cask.coopr.scheduler.task.ClusterJob;
import co.cask.coopr.scheduler.task.JobId;
import com.google.common.base.Charsets;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.RequestLine;
import org.apache.http.entity.StringEntity;
import org.apache.http.message.BasicHttpEntityEnclosingRequest;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpRequestHandler;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.util.List;
import javax.ws.rs.HttpMethod;

public class ClusterHandler implements HttpRequestHandler {

  private static final String GET_ALL_URL = "/v2/clusters";
  private static final String SINGLE_URL_REGEX = "/v2/clusters/\\w+";
  private static final String STATUS_URL_REGEX = "/v2/clusters/\\w+/status$";
  private static final String CONFIG_URL_REGEX = "/v2/clusters/\\w+/config$";
  private static final String SERVICES_URL_REGEX = "/v2/clusters/\\w+/services$";
  private static final String SYNC_TEMPLATE_URL_REGEX = "/v2/clusters/\\w+/clustertemplate/sync$";
  private static final String SERVICES_START_URL_REGEX = "/v2/clusters/\\w+/services/\\w+/start$";
  private static final String SERVICES_STOP_URL_REGEX = "/v2/clusters/\\w+/services/\\w+/stop$";
  private static final String SERVICES_RESTART_URL_REGEX = "/v2/clusters/\\w+/services/\\w+/restart$";
  private static final Gson GSON = new Gson();
  private static final String COOPR_TENANT_ID_HEADER_NAME = "Coopr-TenantID";
  private static final String COOPR_USER_ID_HEADER_NAME = "Coopr-UserID";

  @Override
  public void handle(HttpRequest request, HttpResponse response, HttpContext context) throws HttpException,
    IOException {

    RequestLine requestLine = request.getRequestLine();
    String method = requestLine.getMethod();
    String url = requestLine.getUri();
    int statusCode = HttpStatus.SC_OK;
    String responseBody = "";

    String userId = request.getFirstHeader(COOPR_USER_ID_HEADER_NAME).getValue();
    String tenantId = request.getFirstHeader(COOPR_TENANT_ID_HEADER_NAME).getValue();
    if (userId.equals(RestClientTest.TEST_USER_ID) && tenantId.equals(RestClientTest.TEST_TENANT_ID)) {
      if (GET_ALL_URL.equals(url) && HttpMethod.GET.equals(method)) {
        responseBody = GSON.toJson(createClusterSummaries());
      } else if (GET_ALL_URL.equals(url) && HttpMethod.POST.equals(method)) {
        BasicHttpEntityEnclosingRequest httpRequest = (BasicHttpEntityEnclosingRequest) request;
        ClusterCreateRequest createRequest
          = GSON.fromJson(EntityUtils.toString(httpRequest.getEntity(), Charsets.UTF_8), ClusterCreateRequest.class);
        if (createRequest != null && createRequest.getName().equals(ClusterRestClientTest.EXPECTED_NEW_CLUSTER_NAME)) {
          responseBody = "{'id' : 'testCluster'}";
        } else {
          statusCode = HttpStatus.SC_BAD_REQUEST;
        }
      } else if (url.matches(SINGLE_URL_REGEX) && HttpMethod.GET.equals(method)) {
        responseBody = GSON.toJson(createClusterDetails());
      } else if (url.matches(SINGLE_URL_REGEX) && HttpMethod.DELETE.equals(method)) {
        // Send 200 OK
      } else if (url.matches(SINGLE_URL_REGEX) && HttpMethod.POST.equals(method)) {
        BasicHttpEntityEnclosingRequest httpRequest = (BasicHttpEntityEnclosingRequest) request;
        JsonObject jsonContent =
          new JsonParser().parse(EntityUtils.toString(httpRequest.getEntity(), Charsets.UTF_8)).getAsJsonObject();
        long expireTime = jsonContent.get("expireTime").getAsLong();
        if (ClusterRestClientTest.EXPECTED_TEST_EXPIRE_TIME != expireTime) {
          statusCode = HttpStatus.SC_BAD_REQUEST;
        }
      } else if (url.matches(STATUS_URL_REGEX) && HttpMethod.GET.equals(method)) {
        responseBody = GSON.toJson(createClusterStatusResponse());
      } else if (url.matches(CONFIG_URL_REGEX) && HttpMethod.GET.equals(method)) {
        responseBody = GSON.toJson(createConfig());
      } else if (url.matches(CONFIG_URL_REGEX) && HttpMethod.PUT.equals(method)) {
        BasicHttpEntityEnclosingRequest httpRequest = (BasicHttpEntityEnclosingRequest) request;
        ClusterConfigureRequest clusterConfigureRequest =
          GSON.fromJson(EntityUtils.toString(httpRequest.getEntity(), Charsets.UTF_8), ClusterConfigureRequest.class);
        if (clusterConfigureRequest == null || clusterConfigureRequest.getConfig() == null ||
          !clusterConfigureRequest.getConfig().get("name").getAsString()
            .equals(ClusterRestClientTest.UPDATED_CONFIG_CLUSTER_NAME)) {
          statusCode = HttpStatus.SC_BAD_REQUEST;
        }
      } else if (url.matches(SERVICES_URL_REGEX) && HttpMethod.GET.equals(method)) {
        responseBody = GSON.toJson(Entities.ClusterExample.createCluster().getServices());
      } else if (url.matches(SERVICES_URL_REGEX) && HttpMethod.POST.equals(method)) {
        BasicHttpEntityEnclosingRequest httpRequest = (BasicHttpEntityEnclosingRequest) request;
        AddServicesRequest addServicesRequest
          = GSON.fromJson(EntityUtils.toString(httpRequest.getEntity(), Charsets.UTF_8), AddServicesRequest.class);
        if (addServicesRequest == null || addServicesRequest.getServices() == null ||
          addServicesRequest.getServices().isEmpty()) {
          statusCode = HttpStatus.SC_BAD_REQUEST;
        }
      } else if (url.matches(SYNC_TEMPLATE_URL_REGEX) && HttpMethod.POST.equals(method)) {
        // Send 200 OK
      } else if (url.matches(SERVICES_START_URL_REGEX) && HttpMethod.POST.equals(method)) {
        // Send 200 OK
      } else if (url.matches(SERVICES_STOP_URL_REGEX) && HttpMethod.POST.equals(method)) {
        // Send 200 OK
      } else if (url.matches(SERVICES_RESTART_URL_REGEX) && HttpMethod.POST.equals(method)) {
        // Send 200 OK
      } else {
        statusCode = HttpStatus.SC_NOT_IMPLEMENTED;
      }
    } else {
      statusCode = HandlerUtils.getStatusCodeByTestStatusUserId(userId);
    }

    response.setStatusCode(statusCode);
    if (!Strings.isNullOrEmpty(responseBody)) {
      response.setEntity(new StringEntity(responseBody));
    }
  }

  private ClusterDetails createClusterDetails() {
    return new ClusterDetails(Entities.ClusterExample.createCluster(),
                              Sets.newHashSet(Entities.ClusterExample.NODE1, Entities.ClusterExample.NODE2),
                              new ClusterJob(JobId.fromString("job-1"), ClusterAction.CLUSTER_CREATE));
  }

  private List<ClusterSummary> createClusterSummaries() {
    return Lists.newArrayList(
      new ClusterSummary(Entities.ClusterExample.createCluster(),
                         new ClusterJob(JobId.fromString("job-2"), ClusterAction.CLUSTER_CREATE)));
  }

  private ClusterStatusResponse createClusterStatusResponse() {
    return new ClusterStatusResponse(Entities.ClusterExample.createCluster(),
                                     new ClusterJob(JobId.fromString("job-3"), ClusterAction.SOLVE_LAYOUT));
  }

  private JsonObject createConfig() {
    JsonObject testConfig = new JsonObject();
    testConfig.addProperty("name", "test");
    testConfig.addProperty("expireTime", 10000);
    testConfig.addProperty("nodes", 5);
    return testConfig;
  }
}
