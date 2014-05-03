package com.continuuity.loom.scheduler.callback;

import com.continuuity.loom.BaseTest;
import com.continuuity.loom.Entities;
import com.continuuity.loom.cluster.Cluster;
import com.continuuity.loom.conf.Configuration;
import com.continuuity.loom.conf.Constants;
import com.continuuity.loom.guice.LoomModules;
import com.continuuity.loom.scheduler.ClusterAction;
import com.continuuity.loom.scheduler.task.ClusterJob;
import com.continuuity.loom.scheduler.task.JobId;
import com.continuuity.loom.store.ClusterStore;
import com.continuuity.loom.store.SQLClusterStore;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.inject.Guice;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 *
 */
public class HttpPostClusterCallbackTest extends BaseTest {
  private static Cluster cluster = Entities.ClusterExample.CLUSTER;
  private static DummyService service;
  private static DummyHandler handler;
  private static ClusterStore clusterStore;
  private static String host;
  private static int port;

  @Test
  public void testCalls() {
    HttpPostClusterCallback callback = new HttpPostClusterCallback();

    String base = "http://" + host + ":" + port;
    conf = new Configuration();
    conf.set(Constants.HttpCallback.START_URL, base + "/start/endpoint");
    conf.set(Constants.HttpCallback.SUCCESS_URL, base + "/success/endpoint");
    conf.set(Constants.HttpCallback.FAILURE_URL, base + "/failure/endpoint");

    callback.initialize(conf, clusterStore);
    ClusterJob job = new ClusterJob(new JobId(cluster.getId(), 1), ClusterAction.CLUSTER_CREATE);
    CallbackData data = new CallbackData(CallbackData.Type.START, cluster, job);

    callback.onStart(data);
    callback.onSuccess(data);
    callback.onSuccess(data);
    callback.onFailure(data);
    Assert.assertEquals(handler.getStartCount(), 1);
    Assert.assertEquals(handler.getFailureCount(), 1);
    Assert.assertEquals(handler.getSuccessCount(), 2);
  }

  @Test
  public void testTriggers() {
    HttpPostClusterCallback callback = new HttpPostClusterCallback();

    String base = "http://" + host + ":" + port;
    conf = new Configuration();
    conf.set(Constants.HttpCallback.START_URL, base + "/start/endpoint");
    conf.set(Constants.HttpCallback.START_TRIGGERS, ClusterAction.CLUSTER_CONFIGURE.name());

    callback.initialize(conf, clusterStore);

    // should not get triggered
    ClusterJob job = new ClusterJob(new JobId(cluster.getId(), 1), ClusterAction.CLUSTER_CREATE);
    CallbackData data = new CallbackData(CallbackData.Type.START, cluster, job);
    callback.onStart(data);
    Assert.assertEquals(0, handler.getStartCount());

    // should get triggered
    job = new ClusterJob(new JobId(cluster.getId(), 1), ClusterAction.CLUSTER_CONFIGURE);
    data = new CallbackData(CallbackData.Type.START, cluster, job);
    callback.onStart(data);
    Assert.assertEquals(1, handler.getStartCount());
  }

  @Test
  public void testOnStartIsTrueWithBadURL() {
    HttpPostClusterCallback callback = new HttpPostClusterCallback();

    conf = new Configuration();
    conf.set(Constants.HttpCallback.START_URL, "malformed-url");

    callback.initialize(conf, clusterStore);
    ClusterJob job = new ClusterJob(new JobId(cluster.getId(), 1), ClusterAction.CLUSTER_CREATE);
    CallbackData data = new CallbackData(CallbackData.Type.START, cluster, job);

    Assert.assertTrue(callback.onStart(data));
  }

  @Before
  public void setupTest() {
    handler.clear();
  }

  @BeforeClass
  public static void setupTestClass() throws Exception {
    Configuration conf = new Configuration();
    conf.set(Constants.JDBC_DRIVER, "org.apache.derby.jdbc.EmbeddedDriver");
    conf.set(Constants.JDBC_CONNECTION_STRING, "jdbc:derby:memory:loom;create=true");
    injector = Guice.createInjector(LoomModules.createModule(
      zkClientService, MoreExecutors.sameThreadExecutor(), conf));
    SQLClusterStore sqlClusterStore = injector.getInstance(SQLClusterStore.class);
    sqlClusterStore.initialize();
    clusterStore = sqlClusterStore;
    handler = new DummyHandler();
    service = new DummyService(0, handler);
    service.startAndWait();
    port = service.getBindAddress().getPort();
    host = service.getBindAddress().getHostName();

    clusterStore.writeCluster(cluster);
    clusterStore.writeNode(Entities.ClusterExample.NODE1);
    clusterStore.writeNode(Entities.ClusterExample.NODE2);
  }

  @AfterClass
  public static void cleanupTestClass() {
    service.stopAndWait();
  }
}
