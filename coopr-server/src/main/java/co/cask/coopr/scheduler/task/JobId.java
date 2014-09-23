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
package co.cask.coopr.scheduler.task;

import com.google.common.base.Preconditions;

/**
 * Job ids are of the form clusterid-jobid, where ids are numeric.
 */
public class JobId {
  private final String clusterId;
  private final long jobNum;
  private final String id;

  /**
   * Convert a string representation of a job id into an object while validating that the string is correctly
   * formatted. Throws an IllegalArgumentException if the string is invalid.
   *
   * @param jobIdStr String representation of a job id.
   * @return converted JobId based on the input string.
   */
  public static JobId fromString(String jobIdStr) {
    int index = jobIdStr.indexOf("-");
    Preconditions.checkArgument(index > 0, "invalid job id string " + jobIdStr);
    String clusterId = jobIdStr.substring(0, index);

    Preconditions.checkArgument(jobIdStr.indexOf("-", index + 1) < 0, "invalid job id string " + jobIdStr);
    long jobNum = Long.valueOf(jobIdStr.substring(index + 1));

    return new JobId(clusterId, jobNum);
  }

  public JobId(String clusterId, long jobNum) {
    this.clusterId = clusterId;
    this.jobNum = jobNum;
    this.id = String.format("%s-%03d", clusterId, jobNum);
  }

  /**
   * Get the id of the cluster the job is for.
   *
   * @return Id of the cluster the job is for.
   */
  public String getClusterId() {
    return clusterId;
  }

  /**
   * Get the job number part of the id.
   *
   * @return Job number part of the id.
   */
  public long getJobNum() {
    return jobNum;
  }

  /**
   * Get the id as a string. Format is clusterid-jobid.
   *
   * @return Id as a string.
   */
  public String getId() {
    return id;
  }

  @Override
  public String toString() {
    return id;
  }
}
