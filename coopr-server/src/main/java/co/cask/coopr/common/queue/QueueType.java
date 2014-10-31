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

package co.cask.coopr.common.queue;

import com.google.common.collect.ImmutableSet;

import java.util.Set;

/**
 * Types of Queues.
 */
public enum QueueType {
  // solver queues hold jobids for solving cluster layouts
  SOLVER("/clustermanager/solver"),
  // cluster queues hold cluster ids for creating the plan for performing a cluster operation and kicking off the job
  CLUSTER("/clustermanager/clustercreate"),
  // job queues hold job ids and coordinate task progress and moving to the next stage of a job
  JOB("/clustermanager/jobscheduler"),
  // callback queues hold cluster and job objects for an operation and
  // are read to perform callbacks before/after cluster operations
  CALLBACK("/clustermanager/callback"),
  // provisioner queues hold tasks for provisioners to complete
  PROVISIONER("/clustermanager/nodeprovision"),
  // balancer queue holds tenants to rebalance workers for
  BALANCER("/clustermanager/balancer");
  public static final Set<QueueType> GROUP_TYPES = ImmutableSet.of(SOLVER, CLUSTER, JOB, CALLBACK, PROVISIONER);
  private final String path;

  private QueueType(String path) {
    this.path = path;
  }

  public String getPath() {
    return path;
  }
}
