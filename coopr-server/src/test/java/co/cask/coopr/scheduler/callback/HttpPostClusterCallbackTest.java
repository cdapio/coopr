package co.cask.coopr.scheduler.callback;

import co.cask.coopr.BaseTest;
import co.cask.coopr.Entities;
import co.cask.coopr.cluster.Cluster;
import co.cask.coopr.common.conf.Configuration;
import co.cask.coopr.common.conf.Constants;
import co.cask.coopr.scheduler.ClusterAction;
import co.cask.coopr.scheduler.task.ClusterJob;
import co.cask.coopr.scheduler.task.JobId;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 *
 */
public class HttpPostClusterCallbackTest extends BaseTest {
  private static Cluster cluster = Entities.ClusterExample.createCluster();
  private static DummyService service;
  private static DummyHandler handler;
  private static String host;
  private static int port;

  @Test
  public void testCalls() {
    HttpPostClusterCallback callback = new HttpPostClusterCallback();

    String base = "http://" + host + ":" + port;
    conf = Configuration.create();
    conf.set(Constants.HttpCallback.START_URL, base + "/start/endpoint");
    conf.set(Constants.HttpCallback.SUCCESS_URL, base + "/success/endpoint");
    conf.set(Constants.HttpCallback.FAILURE_URL, base + "/failure/endpoint");

    callback.initialize(conf);
    ClusterJob job = new ClusterJob(new JobId(cluster.getId(), 1), ClusterAction.CLUSTER_CREATE);
    CallbackData data = new CallbackData(CallbackData.Type.START, cluster, job);
    CallbackContext context = new CallbackContext(clusterStoreService, userStore, cluster.getAccount());

    callback.onStart(data, context);
    callback.onSuccess(data, context);
    callback.onSuccess(data, context);
    callback.onFailure(data, context);
    Assert.assertEquals(handler.getStartCount(), 1);
    Assert.assertEquals(handler.getFailureCount(), 1);
    Assert.assertEquals(handler.getSuccessCount(), 2);
  }

  @Test
  public void testTriggers() {
    HttpPostClusterCallback callback = new HttpPostClusterCallback();

    String base = "http://" + host + ":" + port;
    conf = Configuration.create();
    conf.set(Constants.HttpCallback.START_URL, base + "/start/endpoint");
    conf.set(Constants.HttpCallback.START_TRIGGERS, ClusterAction.CLUSTER_CONFIGURE.name());

    callback.initialize(conf);

    // should not get triggered
    ClusterJob job = new ClusterJob(new JobId(cluster.getId(), 1), ClusterAction.CLUSTER_CREATE);
    CallbackData data = new CallbackData(CallbackData.Type.START, cluster, job);
    CallbackContext context = new CallbackContext(clusterStoreService, userStore, cluster.getAccount());
    callback.onStart(data, context);
    Assert.assertEquals(0, handler.getStartCount());

    // should get triggered
    job = new ClusterJob(new JobId(cluster.getId(), 1), ClusterAction.CLUSTER_CONFIGURE);
    data = new CallbackData(CallbackData.Type.START, cluster, job);
    callback.onStart(data, context);
    Assert.assertEquals(1, handler.getStartCount());
  }

  @Test
  public void testOnStartIsTrueWithBadURL() {
    HttpPostClusterCallback callback = new HttpPostClusterCallback();

    conf = Configuration.create();
    conf.set(Constants.HttpCallback.START_URL, "malformed-url");

    callback.initialize(conf);
    ClusterJob job = new ClusterJob(new JobId(cluster.getId(), 1), ClusterAction.CLUSTER_CREATE);
    CallbackData data = new CallbackData(CallbackData.Type.START, cluster, job);
    CallbackContext context = new CallbackContext(clusterStoreService, userStore, cluster.getAccount());

    Assert.assertTrue(callback.onStart(data, context));
  }

  @Before
  public void setupTest() {
    handler.clear();
  }

  @BeforeClass
  public static void setupTestClass() throws Exception {
    handler = new DummyHandler();
    service = new DummyService(0, handler);
    service.startAndWait();
    port = service.getBindAddress().getPort();
    host = service.getBindAddress().getHostName();

    clusterStoreService.getView(cluster.getAccount()).writeCluster(cluster);
    clusterStoreService.getSystemView().writeNode(Entities.ClusterExample.NODE1);
    clusterStoreService.getSystemView().writeNode(Entities.ClusterExample.NODE2);
  }

  @AfterClass
  public static void cleanupTestClass() {
    service.stopAndWait();
  }
}
