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
import com.google.common.base.Objects;
import com.google.common.collect.Lists;

import java.net.URI;
import java.util.List;

/**
 * Configuration for the Coopr CLI.
 */
public class CLIConfig {

  private static final int DEFAULT_PORT = 55054;
  private static final int DEFAULT_SSL_PORT = 55054;
  private static final boolean DEFAULT_SSL = false;

  private RestClientManager clientManager;
  private String host;
  private List<HostnameChangeListener> hostnameChangeListeners;
  private int port;
  private int sslPort;
  private URI uri;

  /**
   *
   * @param host the host of the Coopr server to interact with (e.g. "example.com")
   * @param userId the user id
   * @param tenantId the admin id
   */
  public CLIConfig(String host, String userId, String tenantId) {
    this.host = Objects.firstNonNull(host, "localhost");
    this.port = DEFAULT_PORT;
    this.uri = URI.create(String.format("http://%s:%d", host, port));
    this.sslPort = DEFAULT_SSL_PORT;
    RestClientManager.Builder builder = RestClientManager.builder(host, port);
    builder.ssl(DEFAULT_SSL);
    builder.userId(userId);
    builder.tenantId(tenantId);
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
