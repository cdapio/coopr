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
import co.cask.http.HttpHandler;
import co.cask.http.NettyHttpService;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import org.apache.twill.discovery.DiscoveryServiceClient;

import java.util.Set;

/**
 * Netty service for running the server that manages external API.
 */
public class ExternalHandlerServer extends HandlerServer {

  @Inject
  private ExternalHandlerServer(@Named(Constants.HandlersNames.EXTERNAL) Set<HttpHandler> handlers, Configuration conf,
                                final CConfiguration cConf,
                                final TokenValidator tokenValidator,
                                final AccessTokenTransformer accessTokenTransformer,
                                final DiscoveryServiceClient discoveryServiceClient) {
    super(handlers, conf, Constants.EXTERNAL_PORT,
          cConf, tokenValidator, accessTokenTransformer, discoveryServiceClient);
  }

  @Override
  void addSSLConfig(NettyHttpService.Builder builder, Configuration conf) {
    boolean enableSSL = conf.getBoolean(Constants.EXTERNAL_ENABLE_SSL);
    if (enableSSL) {
      builder.enableSSL(getSSLConfig(conf, Constants.EXTERNAL_SSL_KEYSTORE_PATH,
                                     Constants.EXTERNAL_SSL_KEYSTORE_PASSWORD,
                                     Constants.EXTERNAL_SSL_KEYPASSWORD,
                                     Constants.EXTERNAL_SSL_TRUST_KEYSTORE_PATH,
                                     Constants.EXTERNAL_SSL_TRUST_KEYPASSWORD));
    }
  }
}
