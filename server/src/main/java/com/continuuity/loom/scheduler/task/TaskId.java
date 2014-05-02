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
package com.continuuity.loom.scheduler.task;

import com.google.common.base.Preconditions;

/**
 * Task ids are of the format clusterid-jobnum-tasknum, where all ids and numbers are numeric.
 */
public class TaskId {
  private final JobId jobId;
  private final long taskNum;
  private final String id;

  /**
   * Returns a task id from a string, validating that the string is a valid id. The format is clusterid-jobnum-tasknum.
   * An IllegalArgumentException will be thrown if the string is invalid.
   *
   * @param taskIdStr String of the task id to parse.
   * @return Task id of the string.
   */
  public static TaskId fromString(String taskIdStr) {
    int index1 = taskIdStr.indexOf("-");
    Preconditions.checkArgument(index1 > 0, "invalid task id string " + taskIdStr);
    String clusterId = taskIdStr.substring(0, index1);

    int index2 = taskIdStr.indexOf("-", index1 + 1);
    Preconditions.checkArgument(index2 > 0, "invalid task id string " + taskIdStr);
    long jobNum = Long.valueOf(taskIdStr.substring(index1 + 1, index2));

    Preconditions.checkArgument(taskIdStr.indexOf("-", index2 + 1) < 0, "invalid task id string " + taskIdStr);
    long taskNum = Long.valueOf(taskIdStr.substring(index2 + 1));

    return new TaskId(new JobId(clusterId, jobNum), taskNum);
  }

  public TaskId(JobId jobId, long taskNum) {
    this.jobId = jobId;
    this.taskNum = taskNum;
    this.id = String.format("%s-%03d", jobId.getId(), taskNum);
  }

  public JobId getJobId() {
    return jobId;
  }

  public String getClusterId() {
    return jobId.getClusterId();
  }

  public long getJobNum() {
    return jobId.getJobNum();
  }

  public long getTaskNum() {
    return taskNum;
  }

  public String getId() {
    return id;
  }

  @Override
  public String toString() {
    return id;
  }
}
