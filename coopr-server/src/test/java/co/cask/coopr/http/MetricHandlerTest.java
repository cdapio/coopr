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

import co.cask.coopr.account.Account;
import co.cask.coopr.http.handler.MetricHandler;
import co.cask.coopr.http.util.Interval;
import co.cask.coopr.scheduler.ClusterAction;
import co.cask.coopr.scheduler.task.ClusterTask;
import co.cask.coopr.scheduler.task.TaskId;
import co.cask.coopr.spec.ProvisionerAction;
import co.cask.coopr.store.cluster.ClusterStore;
import com.google.common.base.Charsets;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import org.apache.http.HttpResponse;
import org.jboss.netty.handler.codec.http.HttpResponseStatus;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

/**
 *
 */
public class MetricHandlerTest extends ServiceTestBase {

  private static final ProvisionerAction P_CREATE = ProvisionerAction.CREATE;
  private static final ProvisionerAction P_DELETE = ProvisionerAction.DELETE;
  private static final ClusterAction C_ACTION = ClusterAction.CLUSTER_CREATE;
  private static final Account ACCOUNT1 = new Account("user1", "tenant1");
  private static final Account ACCOUNT2 = new Account("user2", "tenant2");

  private static final ClusterTask CLUSTER_TASK1 = new ClusterTask(P_CREATE, TaskId.fromString("1-1-1"), "node1",
                                                                   "service1", C_ACTION, "template1", ACCOUNT1);
  private static final ClusterTask CLUSTER_TASK2 = new ClusterTask(P_CREATE, TaskId.fromString("1-1-2"), "node2",
                                                                   "service1", C_ACTION, "template1", ACCOUNT1);
  private static final ClusterTask CLUSTER_TASK3 = new ClusterTask(P_CREATE, TaskId.fromString("1-1-3"), "node3",
                                                                   "service1", C_ACTION, "template1", ACCOUNT1);
  private static final ClusterTask CLUSTER_TASK4 = new ClusterTask(P_DELETE, TaskId.fromString("1-1-4"), "node3",
                                                                   "service1", C_ACTION, "template1", ACCOUNT1);
  private static final ClusterTask CLUSTER_TASK5 = new ClusterTask(P_CREATE, TaskId.fromString("2-2-1"), "node1",
                                                                   "service1", C_ACTION, "template1", ACCOUNT1);
  private static final ClusterTask CLUSTER_TASK6 = new ClusterTask(P_CREATE, TaskId.fromString("2-2-2"), "node2",
                                                                   "service1", C_ACTION, "template1", ACCOUNT2);
  private static final ClusterTask CLUSTER_TASK7 = new ClusterTask(P_CREATE, TaskId.fromString("2-2-3"), "node3",
                                                                   "service1", C_ACTION, "template1", ACCOUNT1);

  private static ClusterStore clusterStore;

  @BeforeClass
  public static void init() throws NoSuchFieldException, IllegalAccessException {
    MetricHandler metricHandler = injector.getInstance(MetricHandler.class);
    Field field = metricHandler.getClass().getDeclaredField("clusterStore");
    field.setAccessible(true);
    clusterStore = (ClusterStore) field.get(metricHandler);
  }

  @Before
  public void before() throws IOException {
    clusterStore.deleteClusterTask(TaskId.fromString("1-1-1"));
    clusterStore.deleteClusterTask(TaskId.fromString("1-1-2"));
    clusterStore.deleteClusterTask(TaskId.fromString("1-1-3"));
    clusterStore.deleteClusterTask(TaskId.fromString("1-1-4"));
    clusterStore.deleteClusterTask(TaskId.fromString("2-2-1"));
    clusterStore.deleteClusterTask(TaskId.fromString("2-2-2"));
    clusterStore.deleteClusterTask(TaskId.fromString("2-2-3"));
  }

  @Test
  public void sevenTaskNoGroupByTest() throws Exception {
    long submitTimeTask1 = 1;
    long statusTimeTask1 = 2;
    long submitTimeTask2 = 13;
    long statusTimeTask2 = 17;
    long submitTimeTask3 = 7;
    long statusTimeTask3 = 9;
    long submitTimeTask4 = 15;
    long statusTimeTask4 = 20;
    long submitTimeTask5 = 19;
    long statusTimeTask5 = 25;
    long submitTimeTask6 = 7;
    long statusTimeTask6 = 11;
    long submitTimeTask7 = 3;
    long statusTimeTask7 = 4;

    clusterStore.writeClusterTask(CLUSTER_TASK1);
    clusterStore.writeClusterTask(CLUSTER_TASK2);
    clusterStore.writeClusterTask(CLUSTER_TASK3);
    clusterStore.writeClusterTask(CLUSTER_TASK4);
    clusterStore.writeClusterTask(CLUSTER_TASK5);
    clusterStore.writeClusterTask(CLUSTER_TASK6);
    clusterStore.writeClusterTask(CLUSTER_TASK7);

    CLUSTER_TASK1.setStatus(ClusterTask.Status.COMPLETE);
    CLUSTER_TASK1.setSubmitTime(submitTimeTask1);
    CLUSTER_TASK1.setStatusTime(statusTimeTask1);

    CLUSTER_TASK2.setStatus(ClusterTask.Status.COMPLETE);
    CLUSTER_TASK2.setSubmitTime(submitTimeTask2);
    CLUSTER_TASK2.setStatusTime(statusTimeTask2);

    CLUSTER_TASK3.setStatus(ClusterTask.Status.COMPLETE);
    CLUSTER_TASK3.setSubmitTime(submitTimeTask3);
    CLUSTER_TASK3.setStatusTime(statusTimeTask3);

    CLUSTER_TASK4.setStatus(ClusterTask.Status.COMPLETE);
    CLUSTER_TASK4.setSubmitTime(submitTimeTask4);
    CLUSTER_TASK4.setStatusTime(statusTimeTask4);

    CLUSTER_TASK5.setStatus(ClusterTask.Status.COMPLETE);
    CLUSTER_TASK5.setSubmitTime(submitTimeTask5);
    CLUSTER_TASK5.setStatusTime(statusTimeTask5);

    CLUSTER_TASK6.setStatus(ClusterTask.Status.COMPLETE);
    CLUSTER_TASK6.setSubmitTime(submitTimeTask6);
    CLUSTER_TASK6.setStatusTime(statusTimeTask6);

    CLUSTER_TASK7.setStatus(ClusterTask.Status.FAILED);
    CLUSTER_TASK7.setSubmitTime(submitTimeTask7);
    CLUSTER_TASK7.setStatusTime(statusTimeTask7);

    clusterStore.writeClusterTask(CLUSTER_TASK1);
    clusterStore.writeClusterTask(CLUSTER_TASK2);
    clusterStore.writeClusterTask(CLUSTER_TASK3);
    clusterStore.writeClusterTask(CLUSTER_TASK4);
    clusterStore.writeClusterTask(CLUSTER_TASK5);
    clusterStore.writeClusterTask(CLUSTER_TASK6);
    clusterStore.writeClusterTask(CLUSTER_TASK7);

    HttpResponse response = doGet("/metrics/nodes/usage?start=5&end=22", USER1_HEADERS);
    assertResponseStatus(response, HttpResponseStatus.OK);

    List<Interval> actual = getJsonFromResponse(response);

    List<Interval> expected = new ArrayList<Interval>(1);
    Interval interval = new Interval(5, 22);
    interval.increaseSeconds(17);
    interval.increaseSeconds(16);
    expected.add(interval);

    check(expected, actual);
  }

  @Test
  public void noPermissionsTest() throws Exception {
    long submitTimeTask6 = 1;
    long statusTimeTask6 = 2;

    clusterStore.writeClusterTask(CLUSTER_TASK6);

    CLUSTER_TASK6.setStatus(ClusterTask.Status.COMPLETE);
    CLUSTER_TASK6.setSubmitTime(submitTimeTask6);
    CLUSTER_TASK6.setStatusTime(statusTimeTask6);

    clusterStore.writeClusterTask(CLUSTER_TASK6);

    HttpResponse response = doGet("/metrics/nodes/usage?start=5&end=22&tenant=tenant2", USER1_HEADERS);

    assertResponseStatus(response, HttpResponseStatus.METHOD_NOT_ALLOWED);
  }

  @Test
  public void sevenTaskGroupByTest() throws Exception {
    long millisPerHour = 3600000;
    long submitTimeTask1 = 1 * millisPerHour;
    long statusTimeTask1 = 2 * millisPerHour;
    long submitTimeTask2 = 13 * millisPerHour;
    long statusTimeTask2 = 17 * millisPerHour;
    long submitTimeTask3 = 7 * millisPerHour;
    long statusTimeTask3 = 9 * millisPerHour;
    long submitTimeTask4 = 15 * millisPerHour;
    long statusTimeTask4 = 20 * millisPerHour;
    long submitTimeTask5 = 19 * millisPerHour;
    long statusTimeTask5 = 25 * millisPerHour;
    long submitTimeTask6 = 7 * millisPerHour;
    long statusTimeTask6 = 11 * millisPerHour;
    long submitTimeTask7 = 3 * millisPerHour;
    long statusTimeTask7 = 4 * millisPerHour;

    clusterStore.writeClusterTask(CLUSTER_TASK1);
    clusterStore.writeClusterTask(CLUSTER_TASK2);
    clusterStore.writeClusterTask(CLUSTER_TASK3);
    clusterStore.writeClusterTask(CLUSTER_TASK4);
    clusterStore.writeClusterTask(CLUSTER_TASK5);
    clusterStore.writeClusterTask(CLUSTER_TASK6);
    clusterStore.writeClusterTask(CLUSTER_TASK7);

    CLUSTER_TASK1.setStatus(ClusterTask.Status.COMPLETE);
    CLUSTER_TASK1.setSubmitTime(submitTimeTask1);
    CLUSTER_TASK1.setStatusTime(statusTimeTask1);

    CLUSTER_TASK2.setStatus(ClusterTask.Status.COMPLETE);
    CLUSTER_TASK2.setSubmitTime(submitTimeTask2);
    CLUSTER_TASK2.setStatusTime(statusTimeTask2);

    CLUSTER_TASK3.setStatus(ClusterTask.Status.COMPLETE);
    CLUSTER_TASK3.setSubmitTime(submitTimeTask3);
    CLUSTER_TASK3.setStatusTime(statusTimeTask3);

    CLUSTER_TASK4.setStatus(ClusterTask.Status.COMPLETE);
    CLUSTER_TASK4.setSubmitTime(submitTimeTask4);
    CLUSTER_TASK4.setStatusTime(statusTimeTask4);

    CLUSTER_TASK5.setStatus(ClusterTask.Status.COMPLETE);
    CLUSTER_TASK5.setSubmitTime(submitTimeTask5);
    CLUSTER_TASK5.setStatusTime(statusTimeTask5);

    CLUSTER_TASK6.setStatus(ClusterTask.Status.COMPLETE);
    CLUSTER_TASK6.setSubmitTime(submitTimeTask6);
    CLUSTER_TASK6.setStatusTime(statusTimeTask6);

    CLUSTER_TASK7.setStatus(ClusterTask.Status.FAILED);
    CLUSTER_TASK7.setSubmitTime(submitTimeTask7);
    CLUSTER_TASK7.setStatusTime(statusTimeTask7);

    clusterStore.writeClusterTask(CLUSTER_TASK1);
    clusterStore.writeClusterTask(CLUSTER_TASK2);
    clusterStore.writeClusterTask(CLUSTER_TASK3);
    clusterStore.writeClusterTask(CLUSTER_TASK4);
    clusterStore.writeClusterTask(CLUSTER_TASK5);
    clusterStore.writeClusterTask(CLUSTER_TASK6);
    clusterStore.writeClusterTask(CLUSTER_TASK7);

    HttpResponse response = doGet("/metrics/nodes/usage?start=" + (5 * millisPerHour) +
                                    "&end=" + (22 * millisPerHour) + "&groupby=hour", USER1_HEADERS);
    assertResponseStatus(response, HttpResponseStatus.OK);

    List<Interval> actual = getJsonFromResponse(response);

    List<Interval> expected = new ArrayList<Interval>(17);

    //from 5 * millisPerHour to 9 * millisPerHour
    for (int i = 5; i < 9; i++) {
      addInterval(expected, i * millisPerHour, (i + 1) * millisPerHour - 1, millisPerHour);
    }
    //from 9 * millisPerHour to 17 * millisPerHour
    for (int i = 9; i < 17; i++) {
      addInterval(expected, i * millisPerHour, (i + 1) * millisPerHour - 1, 2 * millisPerHour);
    }
    //from 17 * millisPerHour to 20 * millisPerHour
    for (int i = 17; i < 20; i++) {
      addInterval(expected, i * millisPerHour, (i + 1) * millisPerHour - 1, 3 * millisPerHour);
    }
    //from 20 * millisPerHour to 21 * millisPerHour
    for (int i = 20; i < 21; i++) {
      addInterval(expected, i * millisPerHour, (i + 1) * millisPerHour - 1, 2 * millisPerHour);
    }
    //from 21 * millisPerHour to 22 * millisPerHour
    for (int i = 21; i < 22; i++) {
      addInterval(expected, i * millisPerHour, (i + 1) * millisPerHour, 2 * millisPerHour);
    }

    check(expected, actual);
  }

  private void check(List<Interval> expected, List<Interval> actual) {
    Assert.assertEquals(expected.size(), actual.size());
    int index = 0;
    for (Interval expectedInterval : expected) {
      Interval actualInterval = actual.get(index++);
      Assert.assertEquals(expectedInterval.getStart(), actualInterval.getStart());
      Assert.assertEquals(expectedInterval.getEnd(), actualInterval.getEnd());
      Assert.assertEquals(expectedInterval.getSeconds(), actualInterval.getSeconds());
    }
  }

  private void addInterval(List<Interval> intervals, long start, long end, long seconds) {
    Interval interval = new Interval(start, end);
    interval.increaseSeconds(seconds);
    intervals.add(interval);
  }

  private List<Interval> getJsonFromResponse(HttpResponse response) throws IOException {
    Reader reader = new InputStreamReader(response.getEntity().getContent(), Charsets.UTF_8);
    JsonObject jsonObject = gson.fromJson(reader, JsonObject.class);
    return gson.fromJson(jsonObject.getAsJsonArray("usage"), new TypeToken<List<Interval>>() {}.getType());
  }
}
