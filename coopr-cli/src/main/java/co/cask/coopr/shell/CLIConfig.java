/*
 * Copyright © 2012-2014 Cask Data, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package co.cask.coopr.shell;

import co.cask.coopr.client.rest.RestClientManager;
import co.cask.coopr.codec.json.guice.CodecModules;
import com.google.common.base.Objects;
import com.google.common.collect.Lists;
import com.google.gson.Gson;
import com.google.inject.Guice;
import com.google.inject.Injector;

import java.net.URI;
import java.util.List;

import static co.cask.coopr.shell.util.Constants.DEFAULT_PORT;
import static co.cask.coopr.shell.util.Constants.DEFAULT_SSL;
import static co.cask.coopr.shell.util.Constants.DEFAULT_SSL_PORT;
import static co.cask.coopr.shell.util.Constants.DEFAULT_TENANT_ID;
import static co.cask.coopr.shell.util.Constants.DEFAULT_USER_ID;

/**
 * Configuration for the Coopr CLI.
 */
public class CLIConfig {

  private static final Injector injector = Guice.createInjector(
    new CodecModules().getModule()
  );

  private RestClientManager clientManager;
  private String host;
  private List<HostnameChangeListener> hostnameChangeListeners;
  private int port;
  private int sslPort;
  private URI uri;

  /**
   *
   * @param host the host of the Coopr server to interact with (e.g. "example.com")
   * @param port the port for the Coopr server to interact with
   * @param userId the user id
   * @param tenantId the admin id
   */
  public CLIConfig(String host, Integer port, String userId, String tenantId) {
    this.host = Objects.firstNonNull(host, "localhost");
    this.port = Objects.firstNonNull(port, DEFAULT_PORT);
    this.uri = URI.create(String.format("http://%s:%d", this.host, this.port));
    this.sslPort = DEFAULT_SSL_PORT;
    RestClientManager.Builder builder = RestClientManager.builder(this.host, this.port);
    builder.ssl(DEFAULT_SSL);
    builder.userId(Objects.firstNonNull(userId, DEFAULT_USER_ID));
    builder.tenantId(Objects.firstNonNull(tenantId, DEFAULT_TENANT_ID));
    builder.gson(injector.getInstance(Gson.class));
    this.clientManager = builder.build();
    this.hostnameChangeListeners = Lists.newArrayList();
  }

  public String getHost() {
    return host;
  }

  public int getPort() {
    return port;
  }

  public int getSslPort() {
    return sslPort;
  }

  public RestClientManager getClientManager() {
    return clientManager;
  }

  public void setConnection(String host, int port, boolean ssl, String userId, String tenantId) {
    this.host = host;
    if (ssl) {
      this.sslPort = port;
    } else {
      this.port = port;
    }
    this.uri = URI.create(String.format("%s://%s:%d", ssl ? "https" : "http", host, port));
    RestClientManager.Builder builder = RestClientManager.builder(host, port);
    builder.ssl(DEFAULT_SSL);
    builder.userId(userId);
    builder.tenantId(tenantId);
    builder.gson(injector.getInstance(Gson.class));
    this.clientManager = builder.build();
    for (HostnameChangeListener listener : hostnameChangeListeners) {
      listener.onHostnameChanged(host);
    }
  }

  public void addHostnameChangeListener(HostnameChangeListener listener) {
    this.hostnameChangeListeners.add(listener);
  }

  public URI getURI() {
    return uri;
  }

  /**
   * Listener for hostname changes.
   */
  public interface HostnameChangeListener {
    void onHostnameChanged(String newHost);
  }
}
