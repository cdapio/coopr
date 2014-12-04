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
import co.cask.coopr.common.conf.Constants;
import com.google.common.base.Objects;
import com.google.common.collect.Lists;
import com.google.gson.Gson;
import com.google.inject.Guice;
import com.google.inject.Injector;

import java.io.IOException;
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
  private String userId;
  private String tenantId;
  private List<ReconnectListener> reconnectListeners;
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
    this.userId = Objects.firstNonNull(userId, DEFAULT_USER_ID);
    this.tenantId = Objects.firstNonNull(tenantId, DEFAULT_TENANT_ID);
    this.uri = URI.create(String.format("http://%s:%d", this.host, this.port));
    this.sslPort = DEFAULT_SSL_PORT;
    RestClientManager.Builder builder = RestClientManager.builder(this.host, this.port);
    builder.ssl(DEFAULT_SSL);
    builder.userId(this.userId);
    builder.tenantId(this.tenantId);
    builder.gson(injector.getInstance(Gson.class));
    this.clientManager = builder.build();
    this.reconnectListeners = Lists.newArrayList();
  }

  public String getHost() {
    return host;
  }

  public String getUserId() {
    return userId;
  }

  public String getTenantId() {
    return tenantId;
  }

  public int getPort() {
    return port;
  }

  public int getSslPort() {
    return sslPort;
  }

  public boolean isAdmin() {
    return Constants.ADMIN_USER.equals(userId);
  }

  public boolean isSuperadmin() {
    return Constants.ADMIN_USER.equals(userId) && Constants.SUPERADMIN_TENANT.equals(tenantId);
  }

  public RestClientManager getClientManager() {
    return clientManager;
  }

  public void setConnection(String host, int port, boolean ssl, String userId, String tenantId) throws IOException {
    this.host = host;
    if (ssl) {
      this.sslPort = port;
    } else {
      this.port = port;
    }
    this.uri = URI.create(String.format("%s://%s:%d", ssl ? "https" : "http", host, port));
    RestClientManager.Builder builder = RestClientManager.builder(host, port);
    builder.ssl(DEFAULT_SSL);
    this.userId = userId;
    this.tenantId = tenantId;
    builder.userId(userId);
    builder.tenantId(tenantId);
    builder.gson(injector.getInstance(Gson.class));
    this.clientManager = builder.build();
    for (ReconnectListener listener : reconnectListeners) {
      listener.onReconnect();
    }
  }

  public void addReconnectListener(ReconnectListener listener) {
    this.reconnectListeners.add(listener);
  }

  public URI getURI() {
    return uri;
  }

  /**
   * Listener to reconnect to a Coopr instance.
   */
  public interface ReconnectListener {
    void onReconnect() throws IOException;
  }
}
