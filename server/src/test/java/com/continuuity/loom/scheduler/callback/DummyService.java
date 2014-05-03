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
package com.continuuity.loom.scheduler.callback;

import com.continuuity.http.HttpHandler;
import com.continuuity.http.NettyHttpService;
import com.google.common.collect.ImmutableSet;
import com.google.common.util.concurrent.AbstractIdleService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;

/**
 *
 */
public class DummyService extends AbstractIdleService {
  private static final Logger LOG  = LoggerFactory.getLogger(DummyService.class);
  private final NettyHttpService httpService;

  public DummyService(int port, HttpHandler handler) {
    NettyHttpService.Builder builder = NettyHttpService.builder();
    builder.addHttpHandlers(ImmutableSet.of(handler));

    builder.setHost("localhost");
    builder.setPort(port);

    builder.setConnectionBacklog(20000);
    builder.setExecThreadPoolSize(10);
    builder.setBossThreadPoolSize(1);
    builder.setWorkerThreadPoolSize(1);

    this.httpService = builder.build();
  }

  @Override
  protected void startUp() throws Exception {
    httpService.startAndWait();
    LOG.info("Dummy service started successfully on {}", httpService.getBindAddress());
  }

  @Override
  protected void shutDown() throws Exception {
    httpService.stopAndWait();
  }

  /**
   * Get the address the service has bound to.
   *
   * @return Address the service has bound to.
   */
  public InetSocketAddress getBindAddress() {
    return httpService.getBindAddress();
  }
}
