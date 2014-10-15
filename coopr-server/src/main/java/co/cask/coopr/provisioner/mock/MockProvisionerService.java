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

import co.cask.http.NettyHttpService;
import com.google.common.base.Charsets;
import com.google.common.collect.Lists;
import com.google.common.io.CharStreams;
import com.google.common.util.concurrent.AbstractScheduledService;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.twill.common.Threads;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Mock provisioner that will register itself, periodically heartbeat, and deregister itself on shutdown. Also starts
 * up services for handling http requests for editing tenants and a mock service for managing tenant workers.
 */
public class MockProvisionerService extends AbstractScheduledService {
  private static final Logger LOG = LoggerFactory.getLogger(MockProvisionerService.class);
  private static final Gson GSON = new Gson();
  private final NettyHttpService httpService;
  private final MockProvisionerTenantStore provisionerTenantStore = MockProvisionerTenantStore.getInstance();
  private final CloseableHttpClient httpClient;
  private final String id;
  private final int totalCapacity;
  private final HttpPost heartbeatRequest;
  private final HttpPut registerRequest;
  private final HttpDelete deregisterRequest;
  private final MockProvisionerWorkerService workerService;

  public MockProvisionerService(String id, String serverUrl, int totalCapacity, long taskMs,
                                long msBetweenTasks, int failureRate) {
    this.id = id;
    this.totalCapacity = totalCapacity;

    NettyHttpService.Builder builder = NettyHttpService.builder();
    builder.addHttpHandlers(Lists.newArrayList(new MockProvisionerHandler()));
    builder.setHost("localhost");
    builder.setPort(0);
    builder.setConnectionBacklog(20000);
    builder.setExecThreadPoolSize(5);
    builder.setBossThreadPoolSize(1);
    builder.setWorkerThreadPoolSize(10);
    this.httpService = builder.build();

    this.httpClient = HttpClients.createDefault();
    this.heartbeatRequest = new HttpPost(serverUrl + "/provisioners/" + id + "/heartbeat");
    this.registerRequest = new HttpPut(serverUrl + "/provisioners/" + id);
    this.deregisterRequest = new HttpDelete(serverUrl + "/provisioners/" + id);
    this.workerService =
      new MockProvisionerWorkerService(id, serverUrl, totalCapacity, taskMs, msBetweenTasks, failureRate);
  }

  @Override
  protected void runOneIteration() throws Exception {
    heartbeat();
  }

  private void heartbeat() throws Exception {
    JsonObject heartbeat = new JsonObject();
    heartbeat.add("usage", GSON.toJsonTree(provisionerTenantStore.getUsage()));
    heartbeatRequest.setEntity(new StringEntity(GSON.toJson(heartbeat)));
    try {
      LOG.debug("sending heartbeat {}...", heartbeat.toString());
      CloseableHttpResponse response = httpClient.execute(heartbeatRequest);
      try {
        int statusCode = response.getStatusLine().getStatusCode();
        if (statusCode / 100 == 2) {
          LOG.debug("heartbeat successfull.");
        } else if (response.getStatusLine().getStatusCode() == 404) {
          // if we try and heartbeat and get back a 404, register again.
          LOG.debug("heartbeat returned a 404, will try to register.");
          register();
        } else {
          LOG.info("heartbeat for provisioner {} failed. Got a {} status with message:\n {}.",
                   id, getResponseString(response), statusCode);
        }
      } finally {
        response.close();
      }
    } catch (Exception e) {
      LOG.error("Exception while sending heartbeat.", e);
    } finally {
      heartbeatRequest.reset();
    }
  }

  private void register() {
    try {
      JsonObject provisioner = new JsonObject();
      provisioner.addProperty("id", id);
      provisioner.addProperty("host", "localhost");
      provisioner.addProperty("port", myPort());
      provisioner.addProperty("capacityTotal", totalCapacity);
      registerRequest.setEntity(new StringEntity(GSON.toJson(provisioner)));
      LOG.debug("registering provisioner {}...", id);
      CloseableHttpResponse response = httpClient.execute(registerRequest);
      try {
        int statusCode = response.getStatusLine().getStatusCode();
        if (statusCode == 200) {
          LOG.info("provisioner {} successfully registered.", id);
        } else {
          LOG.info("provisioner {} registration failed. Got a {} status with message:\n {}." +
                     "\n Will retry at a later point.", id, statusCode, getResponseString(response));
        }
      } finally {
        response.close();
      }
    } catch (Exception e) {
      LOG.error("Exception while making register call.", e);
    } finally {
      registerRequest.releaseConnection();
    }
  }

  private void deregister() {
    try {
      httpClient.execute(deregisterRequest).close();
    } catch (Exception e) {
      LOG.error("Exception deregistering provisioner.", e);
    }
  }

  private String getResponseString(CloseableHttpResponse response) throws IOException {
    Reader reader = new InputStreamReader(response.getEntity().getContent(), Charsets.UTF_8);
    try {
      return CharStreams.toString(reader);
    } finally {
      reader.close();
    }
  }

  @Override
  protected void startUp() throws Exception {
    workerService.startAndWait();
    httpService.startAndWait();
    register();
    LOG.info("Mock Provisioner started successfully on {}", httpService.getBindAddress());
  }

  @Override
  protected void shutDown() throws Exception {
    deregister();
    httpClient.close();
    httpService.stopAndWait();
    workerService.stopAndWait();
    LOG.info("Services stopped.");
  }

  @Override
  protected ScheduledExecutorService executor() {
    return Executors.newSingleThreadScheduledExecutor(Threads.createDaemonThreadFactory("mock-provisioner-service"));
  }

  @Override
  protected Scheduler scheduler() {
    return Scheduler.newFixedRateSchedule(1, 10, TimeUnit.SECONDS);
  }

  private int myPort() {
    return httpService.getBindAddress().getPort();
  }
}
