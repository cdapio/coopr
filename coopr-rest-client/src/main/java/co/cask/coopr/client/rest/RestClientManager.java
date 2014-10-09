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

package co.cask.coopr.client.rest;

import co.cask.coopr.client.AdminClient;
import co.cask.coopr.client.ClientManager;
import co.cask.coopr.client.ClusterClient;
import co.cask.coopr.client.PluginClient;
import co.cask.coopr.client.ProvisionerClient;
import co.cask.coopr.client.TenantClient;
import org.apache.http.config.Registry;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;

/**
 * The {@link co.cask.coopr.client.ClientManager} implementation for Rest based clients.
 */
public class RestClientManager implements ClientManager {

  private static final Logger LOG = LoggerFactory.getLogger(RestClientManager.class);

  private static final String DEFAULT_VERSION = "v2";
  private static final boolean DEFAULT_SSL = false;
  private static final boolean DEFAULT_VERIFY_SSL_CERT = true;

  private AdminClient adminClient;
  private ClusterClient clusterClient;
  private PluginClient pluginClient;
  private ProvisionerClient provisionerClient;
  private TenantClient tenantClient;
  private final CloseableHttpClient httpClient;
  private final RestClientConnectionConfig connectionConfig;
  private Registry<ConnectionSocketFactory> connectionRegistry;

  private RestClientManager(Builder builder) {
    this.connectionConfig =
      new RestClientConnectionConfig(builder.host, builder.port, builder.apiKey, builder.ssl, builder.version,
                                     builder.userId, builder.tenantId, builder.verifySSLCert);
    if (!builder.verifySSLCert) {
      try {
        connectionRegistry = RestUtil.getRegistryWithDisabledCertCheck();
      } catch (KeyManagementException e) {
        LOG.error("Failed to init SSL context: {}", e);
      } catch (NoSuchAlgorithmException e) {
        LOG.error("Failed to get instance of SSL context: {}", e);
      }
    }
    this.httpClient = HttpClients.custom().setConnectionManager(createConnectionManager()).build();
  }

  /**
   * Create builder for build RestClientManager instance.
   *
   * @param host coopr server host
   * @param port coopr server port
   * @return {@link Builder} Builder instance
   */
  public static Builder builder(String host, int port) {
    return new Builder(host, port);
  }

  @Override
  public AdminClient getAdminClient() {
    if (adminClient == null) {
      adminClient = new AdminRestClient(connectionConfig, httpClient);
    }
    return adminClient;
  }

  @Override
  public ClusterClient getClusterClient() {
    if (clusterClient == null) {
      clusterClient = new ClusterRestClient(connectionConfig, httpClient);
    }
    return clusterClient;
  }

  @Override
  public PluginClient getPluginClient() {
    if (pluginClient == null) {
      pluginClient = new PluginRestClient(connectionConfig, httpClient);
    }
    return pluginClient;
  }

  @Override
  public ProvisionerClient getProvisionerClient() {
    if (provisionerClient == null) {
      provisionerClient = new ProvisionerRestClient(connectionConfig, httpClient);
    }
    return provisionerClient;
  }

  @Override
  public TenantClient getTenantClient() {
    if (tenantClient == null) {
      tenantClient = new TenantRestClient(connectionConfig, httpClient);
    }
    return tenantClient;
  }

  @Override
  public void close() throws IOException {
    httpClient.close();
  }

  private PoolingHttpClientConnectionManager createConnectionManager() {
    if (connectionRegistry != null) {
      return new PoolingHttpClientConnectionManager(connectionRegistry);
    } else {
      return new PoolingHttpClientConnectionManager();
    }
  }

  /**
   * Class Builder for create RestClientManager instance.
   */
  public static class Builder {
    //mandatory
    private final int port;
    private final String host;

    //optional
    private String apiKey;
    private boolean ssl = DEFAULT_SSL;
    private boolean verifySSLCert = DEFAULT_VERIFY_SSL_CERT;
    private String version = DEFAULT_VERSION;
    private String userId;
    private String tenantId;

    public Builder(String host, int port) {
      this.host = host;
      this.port = port;
    }

    public Builder ssl(boolean ssl) {
      this.ssl = ssl;
      return this;
    }

    public Builder verifySSLCert(boolean verifySSLCert) {
      this.verifySSLCert = verifySSLCert;
      return this;
    }

    public Builder apiKey(String apiKey) {
      this.apiKey = apiKey;
      return this;
    }

    public Builder version(String version) {
      this.version = version;
      return this;
    }

     public Builder userId(String userId) {
      this.userId = userId;
      return this;
    }

     public Builder tenantId(String tenantId) {
      this.tenantId = tenantId;
      return this;
    }

    public RestClientManager build() {
      return new RestClientManager(this);
    }
  }
}
