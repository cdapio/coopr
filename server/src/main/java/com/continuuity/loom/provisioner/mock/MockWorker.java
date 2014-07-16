package com.continuuity.loom.provisioner.mock;

import com.google.common.base.Charsets;
import com.google.common.util.concurrent.AbstractScheduledService;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.protocol.HttpContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Mock worker that periodically takes a task and finishes it without performing any actual work.
 */
public class MockWorker extends AbstractScheduledService {
  private static final Gson GSON = new Gson();
  private static final Logger LOG = LoggerFactory.getLogger(MockWorker.class);
  private final String provisionerId;
  private final String workerId;
  private final String tenantId;
  private final CloseableHttpClient httpClient;
  private final HttpPost finishRequest;
  private final HttpPost takeRequest;
  private final long taskMs;
  private final long msBetweenTasks;
  private final ScheduledExecutorService executorService;
  private final HttpContext httpContext;

  public MockWorker(String provisionerId, String workerId, String tenantId, String serverUrl,
                    ScheduledExecutorService executorService, long taskMs, long msBetweenTasks,
                    CloseableHttpClient httpClient) {
    this.provisionerId = provisionerId;
    this.workerId = workerId;
    this.tenantId = tenantId;
    this.executorService = executorService;
    this.taskMs = taskMs;
    this.msBetweenTasks = msBetweenTasks;
    this.finishRequest = new HttpPost(String.format(serverUrl + "/v1/loom/tasks/finish"));
    this.takeRequest = new HttpPost(serverUrl + "/v1/loom/tasks/take");
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
    String taskId = takeTask();
    if (taskId != null) {
      LOG.info("got task {}", taskId);
      TimeUnit.MILLISECONDS.sleep(taskMs);
      finishTask(taskId);
    }
  }

  @Override
  protected Scheduler scheduler() {
    return Scheduler.newFixedRateSchedule(0, msBetweenTasks, TimeUnit.MILLISECONDS);
  }

  private String takeTask() {
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
        return task.get("taskId").getAsString();
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

  private void finishTask(String taskId) throws IOException {
    try {
      JsonObject body = new JsonObject();
      body.addProperty("provisionerId", provisionerId);
      body.addProperty("workerId", provisionerId + "." + workerId);
      body.addProperty("taskId", taskId);
      body.addProperty("tenantId", tenantId);
      body.addProperty("status", "0");

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

  private String getResponseString(CloseableHttpResponse response) throws IOException {
    BufferedReader reader =
      new BufferedReader(new InputStreamReader(response.getEntity().getContent(), Charsets.UTF_8));
    try {
      String line;
      StringBuilder output = new StringBuilder();
      while ((line = reader.readLine()) != null) {
        output.append(line);
      }
      return output.toString();
    } finally {
      reader.close();
    }
  }
}
