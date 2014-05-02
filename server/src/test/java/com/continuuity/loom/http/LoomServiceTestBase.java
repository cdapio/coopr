/*
 * Copyright 2012-2014, Continuuity, Inc.
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
package com.continuuity.loom.http;

import com.continuuity.loom.BaseTest;
import com.continuuity.loom.common.queue.internal.TimeoutTrackingQueue;
import com.continuuity.loom.conf.Constants;
import com.continuuity.loom.scheduler.JobScheduler;
import com.continuuity.loom.scheduler.Scheduler;
import com.continuuity.loom.store.ClusterStore;
import com.google.inject.Key;
import com.google.inject.name.Names;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicHeader;
import org.jboss.netty.handler.codec.http.HttpResponseStatus;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;

/**
 *
 */
public class LoomServiceTestBase extends BaseTest {
  protected static final String USER1 = "user1";
  protected static final String USER2 = "user2";
  protected static final String ADMIN_USERID = "admin";
  protected static final String API_KEY = "apikey";
  protected static final Header[] USER1_HEADERS =
    { new BasicHeader(Constants.USER_HEADER, USER1), new BasicHeader(Constants.API_KEY_HEADER, API_KEY) };
  protected static final Header[] USER2_HEADERS =
    { new BasicHeader(Constants.USER_HEADER, USER2), new BasicHeader(Constants.API_KEY_HEADER, API_KEY) };
  protected static final Header[] ADMIN_HEADERS =
    { new BasicHeader(Constants.USER_HEADER, ADMIN_USERID), new BasicHeader(Constants.API_KEY_HEADER, API_KEY) };
  private static int port;
  protected static LoomService loomService;
  protected static TimeoutTrackingQueue nodeProvisionTaskQueue;
  protected static TimeoutTrackingQueue clusterQueue;
  protected static TimeoutTrackingQueue solverQueue;
  protected static TimeoutTrackingQueue jobQueue;
  protected static TimeoutTrackingQueue callbackQueue;
  protected static ClusterStore clusterStore;
  protected static Scheduler scheduler;
  protected static JobScheduler jobScheduler;


  @BeforeClass
  public static void setupServiceBase() throws Exception {
    nodeProvisionTaskQueue = injector.getInstance(
      Key.get(TimeoutTrackingQueue.class, Names.named(Constants.Queue.PROVISIONER)));
    nodeProvisionTaskQueue.start();
    clusterQueue = injector.getInstance(
      Key.get(TimeoutTrackingQueue.class, Names.named(Constants.Queue.CLUSTER)));
    clusterQueue.start();
    solverQueue = injector.getInstance(
      Key.get(TimeoutTrackingQueue.class, Names.named(Constants.Queue.SOLVER)));
    solverQueue.start();
    jobQueue = injector.getInstance(
      Key.get(TimeoutTrackingQueue.class, Names.named(Constants.Queue.JOB)));
    jobQueue.start();
    callbackQueue = injector.getInstance(
      Key.get(TimeoutTrackingQueue.class, Names.named(Constants.Queue.CALLBACK)));
    callbackQueue.start();
    loomService = injector.getInstance(LoomService.class);
    loomService.startAndWait();
    port = loomService.getBindAddress().getPort();
    clusterStore = injector.getInstance(ClusterStore.class);
    clusterStore.initialize();
    scheduler = injector.getInstance(Scheduler.class);
    scheduler.startAndWait();
    jobScheduler = injector.getInstance(JobScheduler.class);
  }

  @AfterClass
  public static void cleanupServiceBase() {
    loomService.stopAndWait();
    clusterQueue.stop();
    nodeProvisionTaskQueue.stop();
    scheduler.stopAndWait();
  }

  public static HttpResponse doGet(String resource) throws Exception {
    return doGet(resource, null);
  }

  public static HttpResponse doGet(String resource, Header[] headers) throws Exception {
    DefaultHttpClient client = new DefaultHttpClient();
    HttpGet get = new HttpGet("http://" + HOSTNAME + ":" + port + resource);

    if (headers != null) {
      get.setHeaders(headers);
    }

    return client.execute(get);
  }

  public static HttpResponse doPut(String resource, String body, Header[] headers) throws Exception {
    DefaultHttpClient client = new DefaultHttpClient();
    HttpPut put = new HttpPut("http://" + HOSTNAME + ":" + port + resource);

    if (headers != null) {
      put.setHeaders(headers);
    }
    if (body != null) {
      put.setEntity(new StringEntity(body));
    }
    return client.execute(put);
  }

  public static HttpResponse doPost(String resource, String body) throws Exception {
    return doPost(resource, body, null);
  }

  public static HttpResponse doPost(String resource, String body, Header[] headers) throws Exception {
    DefaultHttpClient client = new DefaultHttpClient();
    HttpPost post = new HttpPost("http://" + HOSTNAME + ":" + port + resource);

    if (headers != null) {
      post.setHeaders(headers);
    }
    if (body != null) {
      post.setEntity(new StringEntity(body));
    }

    return client.execute(post);
  }

  public static HttpResponse doDelete(String resource, Header[] headers) throws Exception {
    DefaultHttpClient client = new DefaultHttpClient();
    HttpDelete delete = new HttpDelete("http://" + HOSTNAME + ":" + port + resource);
    if (headers != null) {
      delete.setHeaders(headers);
    }
    return client.execute(delete);
  }

  public static void assertResponseStatus(HttpResponse response, HttpResponseStatus expected) {
    Assert.assertEquals(response.getStatusLine().getReasonPhrase(),
                        expected.getCode(), response.getStatusLine().getStatusCode());
  }

  public static String getBaseUrl() {
    return String.format("http://%s:%d", HOSTNAME, port);
  }
}
