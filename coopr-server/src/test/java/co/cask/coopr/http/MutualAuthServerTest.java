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

package co.cask.coopr.http;

import co.cask.coopr.Entities;
import co.cask.coopr.cluster.NodeProperties;
import co.cask.coopr.common.conf.Constants;
import co.cask.coopr.common.queue.Element;
import co.cask.coopr.http.request.TakeTaskRequest;
import co.cask.coopr.scheduler.ClusterAction;
import co.cask.coopr.scheduler.task.ClusterJob;
import co.cask.coopr.scheduler.task.ClusterTask;
import co.cask.coopr.scheduler.task.JobId;
import co.cask.coopr.scheduler.task.SchedulableTask;
import co.cask.coopr.scheduler.task.TaskConfig;
import co.cask.coopr.scheduler.task.TaskId;
import co.cask.coopr.scheduler.task.TaskServiceAction;
import co.cask.coopr.spec.ProvisionerAction;
import co.cask.coopr.spec.service.ServiceAction;
import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonObject;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.BasicHttpClientConnectionManager;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Test the HttpsServer with mutual authentication.
 */
public class MutualAuthServerTest extends ServiceTestBase {

  private static File serverKeyStore;
  private static File clientKeyStore;
  private static final String SERVER_KEY_STORE_PASSWORD = "secret";
  private static final String CLIENT_KEY_STORE_PASSWORD = "password";

  @BeforeClass
  public static void setup() {
    conf.setBoolean("server.tasks.ssl.enabled", true);
    String serverCertificate = HttpsServerTest.class.getClassLoader().getResource("cert.jks").getPath();
    conf.set("server.tasks.ssl.keystore.path", serverCertificate);
    conf.set("server.tasks.ssl.keystore.password", SERVER_KEY_STORE_PASSWORD);
    conf.set("server.tasks.ssl.cert.password", "secret");
    serverKeyStore = new File(serverCertificate);

    String clientCertificate = HttpsServerTest.class.getClassLoader().getResource("client.jks").getFile();
    conf.set("server.tasks.ssl.trust.keystore.path", clientCertificate);
    conf.set("server.tasks.ssl.trust.keystore.password", CLIENT_KEY_STORE_PASSWORD);
    clientKeyStore = new File(clientCertificate);
  }

  @Before
  public void startServer() {
    internalHandlerServer = injector.getInstance(InternalHandlerServer.class);
    internalHandlerServer.startAndWait();
  }

  @After
  public void stopServer() {
    internalHandlerServer.stopAndWait();
  }

  @Test
  public void testTasksTake() throws Exception {
    HttpResponse response = doSecurePost(String.format("https://%s:%d%s/tasks/take", HOSTNAME,
                                                       internalHandlerServer.getBindAddress().getPort(),
                                                       Constants.API_BASE), gson.toJson(getRequest()));
    Assert.assertEquals(200, response.getStatusLine().getStatusCode());
  }

  @Test(expected = TimeoutException.class)
  public void testTasksTakeFail() throws Exception {
    Future<HttpResponse> future = Executors.newSingleThreadExecutor().submit(new Callable<HttpResponse>() {
      public HttpResponse call() throws Exception {
        return doSecurePost(String.format("http://%s:%d%s/tasks/finish", HOSTNAME,
                                          internalHandlerServer.getBindAddress().getPort(),
                                          Constants.API_BASE), gson.toJson(getRequest()));
      }
    });
    future.get(5000, TimeUnit.MILLISECONDS);
  }

  private TakeTaskRequest getRequest() throws IOException {
    String tenantId = USER1_ACCOUNT.getTenantId();
    ClusterTask clusterTask = new ClusterTask(
      ProvisionerAction.CREATE, TaskId.fromString("1-1-1"), "node_id", "service", ClusterAction.CLUSTER_CREATE,
      "test", USER1_ACCOUNT);
    clusterStore.writeClusterTask(clusterTask);
    ClusterJob clusterJob = new ClusterJob(JobId.fromString("1-1"), ClusterAction.CLUSTER_CREATE);
    clusterStore.writeClusterJob(clusterJob);
    TaskConfig taskConfig = new TaskConfig(
      NodeProperties.builder().build(),
      Entities.ProviderExample.JOYENT,
      ImmutableMap.<String, NodeProperties>of(),
      new TaskServiceAction("svcA", new ServiceAction("shell", ImmutableMap.<String, String>of())),
      new JsonObject(),
      new JsonObject()
    );
    SchedulableTask schedulableTask= new SchedulableTask(clusterTask, taskConfig);
    provisionerQueues.add(tenantId, new Element(clusterTask.getTaskId(), gson.toJson(schedulableTask)));

    return new TakeTaskRequest("worker1", PROVISIONER_ID, TENANT_ID);
  }

  private static HttpResponse doSecurePost(String url, String body) throws Exception {
    HttpClient client = HttpClients.custom()
      .setConnectionManager(new BasicHttpClientConnectionManager(getRegistry(clientKeyStore, CLIENT_KEY_STORE_PASSWORD,
                                                                             serverKeyStore,
                                                                             SERVER_KEY_STORE_PASSWORD))).build();
    HttpPost post = new HttpPost(url);
    post.setEntity(new StringEntity(body));
    return client.execute(post);
  }
}
