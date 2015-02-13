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
package co.cask.coopr.common.zookeeper;

import co.cask.coopr.common.conf.Configuration;
import co.cask.coopr.common.conf.Constants;
import co.cask.coopr.common.zookeeper.lib.ReentrantDistributedLock;
import co.cask.coopr.scheduler.task.JobId;
import co.cask.coopr.scheduler.task.TaskId;
import com.google.common.primitives.Longs;
import com.google.common.util.concurrent.AbstractIdleService;
import com.google.common.util.concurrent.Futures;
import com.google.inject.Inject;
import org.apache.twill.zookeeper.NodeData;
import org.apache.twill.zookeeper.ZKClient;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.data.Stat;

import java.util.concurrent.locks.Lock;

/**
 * Uses Zookeeper for creating new unique ids.
 */
public final class IdService extends AbstractIdleService {
  private static final String IDS_BASEPATH = "/ids";

  private final long startId;
  private final long incrementBy;
  private ThreadLocal<Lock> idLock;

  private final ZKClient zkClient;

  /**
   * Different types of objects that require Ids.
   */
  public enum Type {
    JOB(IDS_BASEPATH + "/jobs"),
    TASK(IDS_BASEPATH + "/tasks"),
    CLUSTER(IDS_BASEPATH + "/clusters"),
    PLUGIN_RESOURCE(IDS_BASEPATH + "/plugins");
    private final String path;

    private Type(String path) {
      this.path = path;
    }
  }

  @Inject
  private IdService(final ZKClient zkClient, Configuration conf)  {
    this.zkClient = zkClient;
    this.startId = conf.getInt(Constants.ID_START_NUM);
    this.incrementBy = conf.getInt(Constants.ID_INCREMENT_BY);
  }

  // for unit testing
  IdService(final ZKClient zkClient, int startId, int incrementBy) {
    this.zkClient = zkClient;
    this.startId = startId;
    this.incrementBy = incrementBy;
  }

  @Override
  protected void startUp() {
    this.idLock = new ThreadLocal<Lock>() {
      @Override
      protected Lock initialValue() {
        return new ReentrantDistributedLock(zkClient, IDS_BASEPATH + "/lock");
      }
    };
    idLock.get().lock();
    try {
      for (Type type : Type.values()) {
        initializeCounter(type);
      }
    } finally {
      idLock.get().unlock();
    }
  }

  @Override
  protected void shutDown() throws Exception {
    // No-op
  }

  /**
   * Get a unique id that can be used for a new cluster.
   *
   * @return Unique id that can be used for a new cluster.
   */
  public String getNewClusterId() {
    return String.format("%08d", generateId(Type.CLUSTER));
  }

  /**
   * Get a unique job id that can be used for new {@link co.cask.coopr.scheduler.task.ClusterJob}s.
   *
   * @param clusterId Id of the cluster the job is for.
   * @return Unique job id.
   */
  public JobId getNewJobId(String clusterId) {
    return new JobId(clusterId, generateId(Type.JOB));
  }

  /**
   * Get a unique task id that can be used for new {@link co.cask.coopr.scheduler.task.ClusterTask}s.
   *
   * @param jobId Id of the job the task is a part of.
   * @return Unique task id.
   */
  public TaskId getNewTaskId(JobId jobId) {
    return new TaskId(jobId, generateId(Type.TASK));
  }

  // This generally should not be a noticeable amount of time compared to the time it takes to perform tasks
  // TODO: try optimistic locking before actual locking to improve performance.
  private long generateId(Type type) {
    idLock.get().lock();
    try {
      NodeData nodeData = Futures.getUnchecked(zkClient.getData(type.path));
      long counterVal = Longs.fromByteArray(nodeData.getData());
      Futures.getUnchecked(zkClient.setData(type.path, Longs.toByteArray(counterVal + incrementBy)));
      return counterVal;
    } finally {
      idLock.get().unlock();
    }
  }

  private void initializeCounter(Type type) {
    Stat stat = Futures.getUnchecked(zkClient.exists(type.path));
    if (stat == null) {
      Futures.getUnchecked(zkClient.create(type.path, Longs.toByteArray(startId), CreateMode.PERSISTENT, true));
    }
  }
}
