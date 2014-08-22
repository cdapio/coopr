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
package com.continuuity.loom.scheduler;

import com.continuuity.loom.common.conf.Constants;
import com.continuuity.loom.common.queue.Element;
import com.continuuity.loom.common.queue.TrackingQueue;
import com.continuuity.loom.provisioner.CapacityException;
import com.continuuity.loom.provisioner.TenantProvisionerService;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * Balances number of workers to place on each provisioner.
 */
public class WorkerBalanceScheduler implements Runnable {

  private static final Logger LOG = LoggerFactory.getLogger(WorkerBalanceScheduler.class);

  private final String id;
  private final TrackingQueue balanceQueue;
  private final TenantProvisionerService tenantProvisionerService;

  @Inject
  private WorkerBalanceScheduler(@Named("scheduler.id") String id,
                                 @Named(Constants.Queue.WORKER_BALANCE) TrackingQueue balanceQueue,
                                 TenantProvisionerService tenantProvisionerService) {
    this.id = id;
    this.balanceQueue = balanceQueue;
    this.tenantProvisionerService = tenantProvisionerService;
  }

  @Override
  public void run() {
    while (true) {
      Element element = balanceQueue.take(id);
      if (element == null) {
        return;
      }

      try {
        tenantProvisionerService.rebalanceTenantWorkers(element.getValue());
        balanceQueue.recordProgress(id, element.getId(),
                                    TrackingQueue.ConsumingStatus.FINISHED_SUCCESSFULLY, "balanced");
      } catch (CapacityException e) {
        LOG.error("Not enough capacity trying to balance workers for tenant {}", element.getValue(), e);
        // a failed status puts the element back in the queue, we don't want to consume this element again.
        // when another provisioner comes online, workers will get rebalanced once again.
        balanceQueue.recordProgress(id, element.getId(),
                                    TrackingQueue.ConsumingStatus.FINISHED_SUCCESSFULLY, "not enough capacity");
      } catch (IOException e) {
        LOG.error("Exception trying to balance workers for tenant {}", element.getValue(), e);
        balanceQueue.recordProgress(id, element.getId(),
                                    TrackingQueue.ConsumingStatus.FINISHED_SUCCESSFULLY, "failed");
      }
    }
  }
}
