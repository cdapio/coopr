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
package com.continuuity.loom.scheduler;

import com.continuuity.loom.common.conf.Configuration;
import com.continuuity.loom.common.conf.Constants;
import com.continuuity.loom.provisioner.TenantProvisionerService;
import com.google.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

/**
 * Time out provisioners and perform cleanup.
 */
public class ProvisionerCleanup implements Runnable {
  private static final Logger LOG = LoggerFactory.getLogger(ProvisionerCleanup.class);
  private final TenantProvisionerService tenantProvisionerService;
  private final long provisionerTimeoutSecs;

  @Inject
  private ProvisionerCleanup(TenantProvisionerService tenantProvisionerService,
                             Configuration conf) {
    this.tenantProvisionerService = tenantProvisionerService;
    this.provisionerTimeoutSecs = conf.getLong(Constants.PROVISIONER_TIMEOUT_SECS);
  }

  @Override
  public void run() {
    try {
      // time out provisioners that have not sent a heartbeat in a while
      long staleTime = System.currentTimeMillis() -
        TimeUnit.MILLISECONDS.convert(provisionerTimeoutSecs, TimeUnit.SECONDS);
      tenantProvisionerService.timeoutProvisioners(staleTime);
    } catch (Throwable e) {
      LOG.error("Got exception: ", e);
    }
  }
}
