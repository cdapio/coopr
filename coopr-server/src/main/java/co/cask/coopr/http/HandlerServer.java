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

import co.cask.cdap.common.conf.CConfiguration;
import co.cask.cdap.security.auth.AccessTokenTransformer;
import co.cask.cdap.security.auth.TokenValidator;
import co.cask.coopr.common.conf.Configuration;
import co.cask.coopr.common.conf.Constants;
import co.cask.coopr.http.handler.auth.SecurityAuthenticationHttpHandler;
import co.cask.http.HttpHandler;
import co.cask.http.NettyHttpService;
import co.cask.http.SSLConfig;
import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.AbstractIdleService;
import org.apache.twill.discovery.DiscoveryServiceClient;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.Channels;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.InetSocketAddress;
import java.util.Set;
import javax.annotation.Nullable;

/**
 * Netty service for running the server.
 */
abstract class HandlerServer extends AbstractIdleService {

  private static final Logger LOG = LoggerFactory.getLogger(HandlerServer.class);

  private static final String DECODER_CHANNEL_HANDLER_NAME = "decoder";
  private static final String AUTHENTICATION_CHANNEL_HANDLER_NAME = "access-token-authenticator";

  private final NettyHttpService httpService;

  HandlerServer(Set<HttpHandler> handlers, Configuration conf, String portKey,
                final CConfiguration cConf,
                final TokenValidator tokenValidator,
                final AccessTokenTransformer accessTokenTransformer,
                final DiscoveryServiceClient discoveryServiceClient) {
    String host = conf.get(Constants.HOST);
    int port = conf.getInt(portKey);
    int numExecThreads = conf.getInt(Constants.NETTY_EXEC_NUM_THREADS);
    int numWorkerThreads = conf.getInt(Constants.NETTY_WORKER_NUM_THREADS);
    final boolean securityEnabled = conf.getBoolean(co.cask.cdap.common.conf.Constants.Security.ENABLED);
    final String realm = conf.get(co.cask.cdap.common.conf.Constants.Security.CFG_REALM);

    NettyHttpService.Builder builder = NettyHttpService.builder();
    builder.addHttpHandlers(handlers);

    builder.setHost(host);
    builder.setPort(port);

    builder.setConnectionBacklog(20000);
    builder.setExecThreadPoolSize(numExecThreads);
    builder.setBossThreadPoolSize(1);
    builder.setWorkerThreadPoolSize(numWorkerThreads);
    if (securityEnabled) {
      builder.modifyChannelPipeline(new Function<ChannelPipeline, ChannelPipeline>() {
        @Nullable
        @Override
        public ChannelPipeline apply(@Nullable ChannelPipeline input) {
          if (input == null) {
            input = Channels.pipeline();
          }
          input.addAfter(DECODER_CHANNEL_HANDLER_NAME, AUTHENTICATION_CHANNEL_HANDLER_NAME,
                         new SecurityAuthenticationHttpHandler(realm, tokenValidator, cConf, accessTokenTransformer,
                                                               discoveryServiceClient));
          return input;
        }
      });
    }

    addSSLConfig(builder, conf);
    this.httpService = builder.build();
  }

  abstract void addSSLConfig(NettyHttpService.Builder builder, Configuration conf);

  SSLConfig getSSLConfig(Configuration conf, String keyStorePathKey,
                                                      String keyStorePasswordKey, String keyPasswordKey,
                                                      String trustKeyStorePathKey, String trustKeyPasswordKey) {
    String keyStoreFilePath = conf.get(keyStorePathKey);
    Preconditions.checkArgument(keyStoreFilePath != null,
                                String.format("%s is not specified.", keyStorePathKey));
    File keyStore = new File(keyStoreFilePath);

    SSLConfig.Builder builder = SSLConfig.builder(keyStore, conf.get(keyStorePasswordKey))
      .setCertificatePassword(conf.get(keyPasswordKey));

    String trustKeyStoreFilePath = conf.get(trustKeyStorePathKey);
    if (trustKeyStoreFilePath == null) {
      return builder.build();
    }
    return builder.setTrustKeyStore(new File(trustKeyStoreFilePath))
      .setTrustKeyStorePassword(conf.get(trustKeyPasswordKey)).build();
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
