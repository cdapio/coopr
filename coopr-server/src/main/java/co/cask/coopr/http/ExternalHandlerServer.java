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
import co.cask.http.SSLConfig;
import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import org.apache.twill.discovery.DiscoveryServiceClient;

import java.io.File;
import java.util.Set;

/**
 * Netty service for running the server that manages external API.
 */
public class ExternalHandlerServer extends HandlerServer {

  @Inject
  private ExternalHandlerServer(Set<HttpHandler> handlers, Configuration conf,
                                CConfiguration cConf, TokenValidator tokenValidator,
                                AccessTokenTransformer accessTokenTransformer,
                                DiscoveryServiceClient discoveryServiceClient) {
    super(handlers, conf, Constants.EXTERNAL_PORT,
          cConf, tokenValidator, accessTokenTransformer, discoveryServiceClient);
  }

  @Override
  SSLConfig getSSLConfig(Configuration conf) {
    return getSSLConfigBuilderWithKeyStore(conf).build();
  }
}
