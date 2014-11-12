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
import co.cask.coopr.http.handler.TaskHandler;
import co.cask.http.HttpHandler;
import co.cask.http.SSLConfig;
import com.google.common.base.Preconditions;
import com.google.common.collect.Sets;
import com.google.inject.Inject;

import java.io.File;
import java.util.Arrays;
import java.util.HashSet;

/**
 * Netty service for running the server that manages internal API.
 */
public class InternalHandlerServer extends HandlerServer {

  @Inject
  private InternalHandlerServer(TaskHandler handler, Configuration conf) {
    super(Sets.<HttpHandler>newHashSet(Arrays.asList(handler)), conf, Constants.INTERNAL_PORT);
  }

  @Override
  SSLConfig getSSLConfig(Configuration conf) {
    String keyStoreFilePath = conf.get(Constants.SSL_KEYSTORE_PATH);
    Preconditions.checkArgument(keyStoreFilePath != null,
                                String.format("%s is not specified.", Constants.SSL_KEYSTORE_PATH));
    File keyStore = new File(keyStoreFilePath);
    SSLConfig.Builder sslConfigBuilder = SSLConfig.builder(keyStore, conf.get(Constants.SSL_KEYSTORE_PASSWORD))
      .setCertificatePassword(conf.get(Constants.SSL_KEYPASSWORD));

    String trustKeyStoreFilePath = conf.get(Constants.SSL_TRUST_KEYSTORE_PATH);
    if (trustKeyStoreFilePath != null) {
      sslConfigBuilder.setTrustKeyStore(new File(trustKeyStoreFilePath))
        .setTrustKeyStorePassword(conf.get(Constants.SSL_TRUST_KEYPASSWORD));
    }

    return sslConfigBuilder.build();
  }
}
