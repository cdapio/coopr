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
package co.cask.coopr.provisioner.mock;

import co.cask.coopr.spec.ProvisionerAction;
import com.google.common.base.Charsets;
import com.google.common.base.Joiner;
import com.google.common.io.CharStreams;
import com.google.common.util.concurrent.AbstractScheduledService;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.protocol.HttpContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Random;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Mock worker that periodically takes a task and finishes it without performing any actual work.
 */
public class MockWorker extends AbstractScheduledService {
  private static final Gson GSON = new Gson();
  private static final Logger LOG = LoggerFactory.getLogger(MockWorker.class);
  private static final Random RANDOM = new Random();
  private final String provisionerId;
  private final String workerId;
  private final String tenantId;
  private final CloseableHttpClient httpClient;
  private final HttpPost finishRequest;
  private final HttpPost takeRequest;
  private final long taskMs;
  private final long msBetweenTasks;
  private final int failureRate;
  private final ScheduledExecutorService executorService;
  private final HttpContext httpContext;

  public MockWorker(String provisionerId, String workerId, String tenantId, String serverUrl,
                    ScheduledExecutorService executorService, long taskMs, long msBetweenTasks,
                    int failureRate, CloseableHttpClient httpClient) {
    this.provisionerId = provisionerId;
    this.workerId = workerId;
    this.tenantId = tenantId;
    this.executorService = executorService;
    this.taskMs = taskMs;
    this.msBetweenTasks = msBetweenTasks;
    this.failureRate = failureRate;
    this.finishRequest = new HttpPost(String.format(serverUrl + "/tasks/finish"));
    this.takeRequest = new HttpPost(serverUrl + "/tasks/take");
    this.httpClient = httpClient;
    this.httpContext = HttpClientContext.create();
  }

  /**
   * Get the id of the worker.
   *
   * @return Id of the worker
   */
  public String getWorkerId() {
    return workerId;
  }

  /**
   * Get the tenant the worker is for.
   *
   * @return Tenant the worker is for
   */
  public String getTenantId() {
    return tenantId;
  }

  @Override
  protected void startUp() throws Exception {
    LOG.info("Started worker {} for tenant {}", workerId, tenantId);
  }

  @Override
  protected void shutDown() throws Exception {
    LOG.info("Stopped worker {} for tenant {}", workerId, tenantId);
  }

  @Override
  protected ScheduledExecutorService executor() {
    return executorService;
  }

  @Override
  protected void runOneIteration() throws Exception {
    JsonObject task = takeTask();
    if (task != null) {
      String taskId = task.get("taskId").getAsString();
      ProvisionerAction action = ProvisionerAction.valueOf(task.get("taskName").getAsString());
      LOG.info("got task {}", taskId);
      TimeUnit.MILLISECONDS.sleep(taskMs);
      finishTask(taskId, action);
    }
  }

  @Override
  protected Scheduler scheduler() {
    return Scheduler.newFixedRateSchedule(0, msBetweenTasks, TimeUnit.MILLISECONDS);
  }

  private JsonObject takeTask() {
    try {
      JsonObject body = new JsonObject();
      body.addProperty("provisionerId", provisionerId);
      body.addProperty("workerId", provisionerId + "." + workerId);
      body.addProperty("tenantId", tenantId);
      takeRequest.setEntity(new StringEntity(body.toString()));

      Reader reader = null;
      CloseableHttpResponse response = httpClient.execute(takeRequest, httpContext);
      try {
        int statusCode = response.getStatusLine().getStatusCode();
        if (statusCode / 100 != 2) {
          LOG.error("Error taking task. Got status code {} with message:\n{}",
                    statusCode, getResponseString(response));
          return null;
        } else if (statusCode != 200) {
          return null;
        }
        reader = new InputStreamReader(response.getEntity().getContent(), Charsets.UTF_8);
        JsonObject task = GSON.fromJson(reader, JsonObject.class);
        LOG.debug("task details: {}", task.toString());
        return task;
      } finally {
        if (reader != null) {
          reader.close();
        }
        response.close();
      }
    } catch (Exception e) {
      LOG.error("Exception making take request.", e);
      return null;
    } finally {
      takeRequest.reset();
    }
  }

  private void finishTask(String taskId, ProvisionerAction action) throws IOException {
    LOG.debug("finishing task {}, which is a {} action.", taskId, action);
    try {
      JsonObject body;
      // generate random num from 0-99
      int num = RANDOM.nextInt(100);
      if (failureRate > num) {
        body = failureBody(taskId);
      } else {
        body = successBody(taskId, action);
      }

      finishRequest.setEntity(new StringEntity(GSON.toJson(body)));
      CloseableHttpResponse response = httpClient.execute(finishRequest, httpContext);
      try {
        int statusCode = response.getStatusLine().getStatusCode();
        if (statusCode / 100 != 2) {
          LOG.error("Error finishing task {}. Got status code {} with message:\n{}",
                    taskId, statusCode, getResponseString(response));
        }
      } finally {
        response.close();
      }
    } catch (Exception e) {
      LOG.error("Exception making finish request.", e);
    } finally {
      finishRequest.reset();
    }
  }

  private JsonObject failureBody(String taskId) {
    JsonObject body = new JsonObject();
    body.addProperty("provisionerId", provisionerId);
    body.addProperty("workerId", provisionerId + "." + workerId);
    body.addProperty("taskId", taskId);
    body.addProperty("tenantId", tenantId);
    body.addProperty("status", "1");
    body.addProperty("stdout", RandomStringUtils.randomAscii(2048));
    body.addProperty("stderr", "");
    return body;
  }

  private JsonObject successBody(String taskId, ProvisionerAction action) {
    JsonObject body = new JsonObject();
    body.addProperty("provisionerId", provisionerId);
    body.addProperty("workerId", provisionerId + "." + workerId);
    body.addProperty("taskId", taskId);
    body.addProperty("tenantId", tenantId);
    body.addProperty("status", "0");
    // include some random field in the result
    JsonObject result = new JsonObject();
    result.addProperty(RandomStringUtils.randomAlphanumeric(4), RandomStringUtils.randomAlphanumeric(8));
    if (action == ProvisionerAction.CONFIRM) {
      JsonObject ips = new JsonObject();
      ips.addProperty("access_v4", randomIP());
      ips.addProperty("bind_v4", randomIP());
      body.add("ipaddresses", ips);
      LOG.debug("adding ips {}.", ips);
      body.addProperty("hostname", "host-" + randomIP() + ".local");
      JsonObject sshAuth = new JsonObject();
      sshAuth.addProperty("user", "root");
      sshAuth.addProperty("password", RandomStringUtils.randomAlphanumeric(8));
      result.add("ssh-auth", sshAuth);
    }
    body.add("result", result);
    return body;
  }

  private String getResponseString(CloseableHttpResponse response) throws IOException {
    Reader reader = new InputStreamReader(response.getEntity().getContent(), Charsets.UTF_8);
    try {
      return CharStreams.toString(reader);
    } finally {
      reader.close();
    }
  }

  private String randomIP() {
    return Joiner.on('.').join(
      RandomStringUtils.randomNumeric(3),
      RandomStringUtils.randomNumeric(3),
      RandomStringUtils.randomNumeric(3),
      RandomStringUtils.randomNumeric(3));
  }
}
