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
package com.continuuity.loom.conf;

import com.continuuity.loom.scheduler.ClusterAction;
import com.continuuity.loom.scheduler.callback.HttpPostClusterCallback;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableSet;

import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Constants used by Loom.
 */
public class Constants {

  /**
   * Zookeeper Constants.
   */
  public static final class Zookeeper {
    public static final String QUORUM = "zookeeper.quorum";
    public static final String CFG_SESSION_TIMEOUT_MILLIS = "zookeeper.session.timeout.millis";
    public static final int DEFAULT_SESSION_TIMEOUT_MILLIS = 40000;
    public static final String NAMESPACE = "zookeeper.namespace";
    public static final String DEFAULT_NAMESPACE = "/loom";
  }

  public static final String PORT = "loom.port";
  public static final int DEFAULT_PORT = 55054;

  public static final String HOST = "loom.host";
  public static final String DEFAULT_HOST = "localhost";

  public static final String SCHEDULER_INTERVAL_SECS = "scheduler.run.interval.seconds";
  public static final int DEFAULT_SCHEDULER_INTERVAL_SECS = 1;

  public static final String TASK_NAMESPACE = "/tasks";
  public static final String LOCK_NAMESPACE = TASK_NAMESPACE + "/lock";

  public static final String JDBC_DRIVER = "loom.jdbc.driver";
  public static final String DEFAULT_JDBC_DRIVER = "org.apache.derby.jdbc.EmbeddedDriver";
  public static final String JDBC_CONNECTION_STRING = "loom.jdbc.connection.string";
  public static final String DB_USER = "loom.db.user";
  public static final String DEFAULT_DB_USER = "loom";
  public static final String DB_PASSWORD = "loom.db.password";
  public static final String DB_VALIDATION_QUERY = "loom.jdbc.validation.query";
  public static final String DEFAULT_DB_VALIDATION_QUERY = "VALUES 1";
  public static final String DB_MAX_ACTIVE_CONNECTIONS = "loom.jdbc.max.active.connections";
  public static final int DEFAULT_DB_MAX_ACTIVE_CONNECTIONS = 100;

  public static final String USER_HEADER = "X-Loom-UserID";
  public static final String API_KEY_HEADER = "X-Loom-ApiKey";
  public static final String ADMIN_USER = "admin";
  public static final String SYSTEM_USER = "system";

  public static final String SOLVER_NUM_THREADS = "loom.solver.num.threads";
  public static final int DEFAULT_SOLVER_NUM_THREADS = 20;

  public static final String LOCAL_DATA_DIR = "loom.local.data.dir";
  public static final String DEFAULT_LOCAL_DATA_DIR = "/var/loom/data";

  public static final String TASK_TIMEOUT_SECS = "loom.task.timeout.seconds";
  public static final long DEFAULT_TASK_TIMEOUT_SECS = TimeUnit.SECONDS.convert(30, TimeUnit.MINUTES);

  public static final String CLUSTER_CLEANUP_SECS = "loom.cluster.cleanup.seconds";
  public static final long DEFAULT_CLUSTER_CLEANUP_SECS = TimeUnit.SECONDS.convert(3, TimeUnit.MINUTES);

  public static final String NETTY_EXEC_NUM_THREADS = "loom.netty.exec.num.threads";
  public static final int DEFAULT_NETTY_EXEC_NUM_THREADS = 50;

  public static final String NETTY_WORKER_NUM_THREADS = "loom.netty.worker.num.threads";
  public static final int DEFAULT_NETTY_WORKER_NUM_THREADS = 20;

  public static final String MAX_PER_NODE_LOG_LENGTH = "loom.node.max.log.length";
  public static final int DEFAULT_MAX_PER_NODE_LOG_LENGTH = 2 * 1024;

  public static final String MAX_PER_NODE_NUM_ACTIONS = "loom.node.max.num.actions";
  public static final int DEFAULT_MAX_PER_NODE_NUM_ACTIONS = 200;

  public static final String MAX_ACTION_RETRIES = "loom.max.action.retries";
  public static final int DEFAULT_MAX_ACTION_RETRIES = 3;

  public static final String MAX_CLUSTER_SIZE = "loom.max.cluster.size";
  public static final int DEFAULT_MAX_CLUSTER_SIZE = 10000;

  public static final String ID_START_NUM = "loom.ids.start.num";
  public static final long DEFAULT_ID_START_NUM = 1;

  public static final String ID_INCREMENT_BY = "loom.ids.increment.by";
  public static final long DEFAULT_ID_INCREMENT_BY = 1;

  public static final String CALLBACK_CLASS = "loom.callback.class";
  public static final String DEFAULT_CALLBACK_CLASS = HttpPostClusterCallback.class.getCanonicalName();

  /**
   * {@link HttpPostClusterCallback} config settings.
   */
  public static final class HttpCallback {
    private static final String prefix = "loom.callback.http.";

    public static final String START_URL = prefix + "start.url";
    public static final String SUCCESS_URL = prefix + "success.url";
    public static final String FAILURE_URL = prefix + "failure.url";

    public static final String START_TRIGGERS = prefix + "start.triggers";
    public static final String DEFAULT_START_TRIGGERS = Joiner.on(',').join(ClusterAction.values());

    public static final String SUCCESS_TRIGGERS = prefix + "success.triggers";
    public static final String DEFAULT_SUCCESS_TRIGGERS = Joiner.on(',').join(ClusterAction.values());

    public static final String FAILURE_TRIGGERS = prefix + "failure.triggers";
    public static final String DEFAULT_FAILURE_TRIGGERS = Joiner.on(',').join(ClusterAction.values());

    public static final String SOCKET_TIMEOUT = prefix + "socket.timeout";
    public static final int DEFAULT_SOCKET_TIMEOUT = 10000;

    public static final String MAX_CONNECTIONS = prefix + "max.connections";
    public static final int DEFAULT_MAX_CONNECTIONS = 100;
  }

  /**
   * Queue related constants.
   */
  public static final class Queue {
    public static final String PROVISIONER = "nodeprovisioner.queue";
    public static final String CLUSTER = "cluster.queue";
    public static final String SOLVER = "solver.queue";
    public static final String JOB = "internal.job.queue";
    public static final String CALLBACK = "callback.queue";
    public static final Set<String> ALL = ImmutableSet.of(PROVISIONER, CLUSTER, SOLVER, JOB, CALLBACK);
  }
}
