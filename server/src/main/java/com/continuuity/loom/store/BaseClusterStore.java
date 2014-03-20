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
package com.continuuity.loom.store;

import com.continuuity.loom.common.zookeeper.lib.ZKInterProcessReentrantLock;
import com.continuuity.loom.scheduler.task.JobId;
import com.continuuity.loom.scheduler.task.TaskId;
import com.google.common.primitives.Longs;
import com.google.common.util.concurrent.Futures;
import org.apache.twill.zookeeper.NodeData;
import org.apache.twill.zookeeper.ZKClient;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.data.Stat;

/**
 * Store that uses Zookeeper for creating new unique ids.
 */
public abstract class BaseClusterStore implements ClusterStore {
  private static final String IDS_BASEPATH = "/ids";
  private static final String JOB_IDS_PATH = IDS_BASEPATH + "/jobs";
  private static final String TASK_IDS_PATH = IDS_BASEPATH + "/tasks";
  private static final String CLUSTER_IDS_PATH = IDS_BASEPATH + "/clusters";

  private final long startId;
  private final long incrementBy;
  private ThreadLocal<ZKInterProcessReentrantLock> idLock;

  private final ZKClient zkClient;

  BaseClusterStore(final ZKClient zkClient, long startId, long incrementBy) {
    this.zkClient = zkClient;
    this.startId = startId;
    this.incrementBy = incrementBy;
  }

  @Override
  public void initialize() {
    this.idLock = new ThreadLocal<ZKInterProcessReentrantLock>() {
      @Override
      protected ZKInterProcessReentrantLock initialValue() {
        return new ZKInterProcessReentrantLock(zkClient, IDS_BASEPATH + "/lock");
      }
    };
    idLock.get().acquire();
    try {
      initializeCounter(JOB_IDS_PATH);
      initializeCounter(TASK_IDS_PATH);
      initializeCounter(CLUSTER_IDS_PATH);
    } finally {
      idLock.get().release();
    }
  }

  @Override
  public String getNewClusterId() {
    return format(getUniqueIdFromPath(CLUSTER_IDS_PATH));
  }

  @Override
  public JobId getNewJobId(String clusterId) {
    return new JobId(clusterId, getUniqueIdFromPath(JOB_IDS_PATH));
  }

  @Override
  public TaskId getNewTaskId(JobId jobId) {
    return new TaskId(jobId, getUniqueIdFromPath(TASK_IDS_PATH));
  }

  protected String format(long num) {
    return String.format("%08d", num);
  }

  // this generally is not a noticeable amount of time compared to the time it takes to perform tasks
  // TODO: try optimistic locking before actual locking to improve performance.
  private long getUniqueIdFromPath(String path) {
    idLock.get().acquire();
    try {
      NodeData nodeData = Futures.getUnchecked(zkClient.getData(path));
      long counterVal = Longs.fromByteArray(nodeData.getData());
      Futures.getUnchecked(zkClient.setData(path, Longs.toByteArray(counterVal + incrementBy)));
      return counterVal;
    } finally {
      idLock.get().release();
    }
  }

  private void initializeCounter(String path) {
    Stat stat = Futures.getUnchecked(zkClient.exists(path));
    if (stat == null) {
      Futures.getUnchecked(zkClient.create(path, Longs.toByteArray(startId), CreateMode.PERSISTENT, true));
    }
  }
}
