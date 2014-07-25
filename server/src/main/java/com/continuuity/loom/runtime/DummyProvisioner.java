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
package com.continuuity.loom.runtime;

import com.google.common.base.Charsets;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Loom Provisioner that takes and finishes tasks without actually doing any work for testing purposes.
 */
public final class DummyProvisioner {
  private static final Logger LOG = LoggerFactory.getLogger(DummyProvisioner.class);
  private static final Gson GSON = new Gson();
  private static final Random random = new Random();
  private static int concurrency;
  private static int failurePercent;
  private static boolean runOnce;
  private static long taskMs;
  private static long sleepMs;
  private static String host;
  private static int port;
  private static int numTasks;
  private static String tenant;
  private static ExecutorService pool;

  public static void main(final String[] args) throws Exception {
    Options options = new Options();
    options.addOption("h", "host", true, "Loom server to connect to");
    options.addOption("p", "port", true, "Loom server port to connect to");
    options.addOption("c", "concurrency", true, "default concurrent threads");
    options.addOption("f", "failurePercent", true, "% of the time a provisioner should fail its task");
    options.addOption("o", "once", false, "whether or not only one task should be taken before exiting");
    options.addOption("d", "taskDuration", true, "number of milliseconds it should take to finish a task");
    options.addOption("s", "sleepMs", true, "number of milliseconds a thread will sleep before taking another task");
    options.addOption("n", "numTasks", true, "number of tasks to try and take from the queue.  Default is infinite.");
    options.addOption("t", "tenant", true, "tenant id to use.");

    try {
      CommandLineParser parser = new BasicParser();
      CommandLine cmd = parser.parse(options, args);
      host = cmd.hasOption('h') ? cmd.getOptionValue('h') : "localhost";
      port = cmd.hasOption('p') ? Integer.valueOf(cmd.getOptionValue('p')) : 55054;
      concurrency = cmd.hasOption('c') ? Integer.valueOf(cmd.getOptionValue('c')) : 5;
      failurePercent = cmd.hasOption('f') ? Integer.valueOf(cmd.getOptionValue('f')) : 0;
      runOnce = cmd.hasOption('o');
      taskMs = cmd.hasOption('d') ? Long.valueOf(cmd.getOptionValue('d')) : 1000;
      sleepMs = cmd.hasOption('s') ? Long.valueOf(cmd.getOptionValue('s')) : 1000;
      numTasks = cmd.hasOption('n') ? Integer.valueOf(cmd.getOptionValue('n')) : -1;
      tenant = cmd.hasOption('t') ? cmd.getOptionValue('t') : "loom";
    } catch (ParseException e) {
      LOG.error("exception parsing input arguments.", e);
      return;
    }
    if (concurrency < 1) {
      LOG.error("invalid concurrency level {}.", concurrency);
      return;
    }

    if (runOnce) {
      new Provisioner("dummy-0", tenant, host, port, failurePercent, taskMs, sleepMs, 1).runOnce();
    } else {
      LOG.info(String.format("running with %d threads, connecting to %s:%d using tenant %s, with a failure rate of" +
                               "%d percent, task time of %d ms, and sleep time of %d ms between fetches",
                             concurrency, host, port, tenant, failurePercent, taskMs, sleepMs));
      pool = Executors.newFixedThreadPool(concurrency);

      try {
        int tasksPerProvisioner = numTasks >= 0 ? numTasks / concurrency : -1;
        int extra = numTasks < 0 ? 0 : numTasks % concurrency;
        pool.execute(
          new Provisioner("dummy-0", tenant, host, port, failurePercent, taskMs, sleepMs, tasksPerProvisioner + extra));
        for (int i = 1; i < concurrency; i++) {
          pool.execute(new Provisioner("dummy-" + i, tenant, host, port,
                                       failurePercent, taskMs, sleepMs, tasksPerProvisioner));
        }
      } catch (Exception e) {
        LOG.error("Caught exception, shutting down now.", e);
        pool.shutdownNow();
      }
      pool.shutdown();
    }
  }

  /**
   * {@link Runnable} that takes tasks from the server and posts back success or failure.
   */
  public static class Provisioner implements Runnable {
    private final String id;
    private final String tenant;
    private final String host;
    private final int port;
    private final int failureRate;
    private final int numTasks;
    private final long taskMs;
    private final long sleepMs;
    private int tasksTaken;

    public Provisioner(String id, String tenant, String host, int port, int failureRate,
                       long taskMs, long sleepMs, int numTasks) {
      this.id = id;
      this.tenant = tenant;
      this.host = host;
      this.port = port;
      this.failureRate = failureRate;
      this.taskMs = taskMs;
      this.sleepMs = sleepMs;
      this.numTasks = numTasks;
      this.tasksTaken = 0;
    }

    @Override
    public void run() {
      while (numTasks < 0 || tasksTaken < numTasks) {
        try {
          runOnce();
        } catch (Exception e) {
          LOG.error("exception taking task.", e);
          return;
        }
      }
    }

    public void runOnce() throws InterruptedException, IOException {
      String taskId = takeTask();
      if (taskId != null) {
        LOG.info("got task {}", taskId);
        TimeUnit.MILLISECONDS.sleep(taskMs);
        finishTask(taskId);
        tasksTaken++;
      }
      TimeUnit.MILLISECONDS.sleep(sleepMs);
    }

    private String takeTask() throws IOException {
      DefaultHttpClient client = new DefaultHttpClient();
      HttpPost post = new HttpPost(String.format("http://%s:%d/v1/loom/tasks/take", host, port));
      JsonObject requestBody = new JsonObject();
      requestBody.addProperty("workerId", id);
      requestBody.addProperty("tenantId", tenant);
      post.setEntity(new StringEntity(requestBody.toString()));
      HttpResponse response = client.execute(post);
      if (response.getStatusLine().getStatusCode() != 200) {
        return null;
      }
      Reader reader = new InputStreamReader(response.getEntity().getContent(), Charsets.UTF_8);
      JsonObject task = GSON.fromJson(reader, JsonObject.class);
      LOG.debug("task details: {}", task.toString());
      return task.get("taskId").getAsString();
    }

    private void finishTask(String taskId) throws IOException {
      DefaultHttpClient client = new DefaultHttpClient();
      JsonObject finishResponse = new JsonObject();
      finishResponse.addProperty("workerId", id);
      finishResponse.addProperty("tenantId", tenant);
      finishResponse.addProperty("taskId", taskId);

      int randInt = random.nextInt(100);
      String status = "0";
      if (randInt < failureRate) {
        status = "1";
        JsonObject result = new JsonObject();
        result.addProperty("stdout", RandomStringUtils.randomAlphanumeric(random.nextInt(2048)));
        result.addProperty("stderr", RandomStringUtils.randomAlphanumeric(random.nextInt(2048)));
        finishResponse.add("result", result);
      }
      LOG.info("finishing task {} with a status of {}.", taskId, status);
      finishResponse.addProperty("status", status);

      HttpPost post = new HttpPost(String.format("http://%s:%d/v1/loom/tasks/finish", host, port));
      post.setEntity(new StringEntity(GSON.toJson(finishResponse)));
      client.execute(post);
    }
  }
}
