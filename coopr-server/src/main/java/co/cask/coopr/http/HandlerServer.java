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
package co.cask.coopr.http;

import co.cask.coopr.common.conf.Configuration;
import co.cask.coopr.common.conf.Constants;
import co.cask.http.HttpHandler;
import co.cask.http.NettyHttpService;
import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.AbstractIdleService;
import com.google.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.InetSocketAddress;
import java.util.Set;

/**
 * Netty service for running the server.
 */
public class HandlerServer extends AbstractIdleService {

  private static final Logger LOG = LoggerFactory.getLogger(HandlerServer.class);
  private final NettyHttpService httpService;

  @Inject
  private HandlerServer(Set<HttpHandler> handlers, Configuration conf) {
    String host = conf.get(Constants.HOST);
    int port = conf.getInt(Constants.PORT);
    int numExecThreads = conf.getInt(Constants.NETTY_EXEC_NUM_THREADS);
    int numWorkerThreads = conf.getInt(Constants.NETTY_WORKER_NUM_THREADS);
    boolean enableSSL = conf.getBoolean(Constants.ENABLE_SSL);

    NettyHttpService.Builder builder = NettyHttpService.builder();
    builder.addHttpHandlers(handlers);

    builder.setHost(host);
    builder.setPort(port);

    builder.setConnectionBacklog(20000);
    builder.setExecThreadPoolSize(numExecThreads);
    builder.setBossThreadPoolSize(1);
    builder.setWorkerThreadPoolSize(numWorkerThreads);

    if (enableSSL) {
      String keyStoreFilePath = conf.get(Constants.SSL_KEYSTORE_PATH);
      Preconditions.checkArgument(keyStoreFilePath != null,
                                  String.format("%s is not specified.", Constants.SSL_KEYSTORE_PATH));
      File keyStore = new File(keyStoreFilePath);
      builder.enableSSL(keyStore, conf.get(Constants.SSL_KEYSTORE_PASSWORD), conf.get(Constants.SSL_KEYPASSWORD));
    }
    this.httpService = builder.build();
  }

  @Override
  protected void startUp() throws Exception {
    httpService.startAndWait();
    LOG.info("Started successfully on {}", httpService.getBindAddress());
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
