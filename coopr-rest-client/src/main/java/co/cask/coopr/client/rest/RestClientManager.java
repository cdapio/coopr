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

import co.cask.cdap.security.authentication.client.AccessToken;
import co.cask.coopr.client.AdminClient;
import co.cask.coopr.client.ClientManager;
import co.cask.coopr.client.ClusterClient;
import co.cask.coopr.client.PluginClient;
import co.cask.coopr.client.ProvisionerClient;
import co.cask.coopr.client.TenantClient;
import com.google.common.base.Supplier;
import com.google.gson.Gson;
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
  private static final Gson DEFAULT_GSON_INSTANCE = new Gson();

  private final AdminClient adminClient;
  private final ClusterClient clusterClient;
  private final PluginClient pluginClient;
  private final ProvisionerClient provisionerClient;
  private final TenantClient tenantClient;
  private final CloseableHttpClient httpClient;
  private Registry<ConnectionSocketFactory> connectionRegistry;

  private RestClientManager(Builder builder) {
    RestClientConnectionConfig connectionConfig =
      new RestClientConnectionConfig(builder.host, builder.port, builder.apiKey, builder.ssl, builder.version,
                                     builder.userId, builder.tenantId, builder.verifySSLCert,
                                     builder.accessTokenSupplier);
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

    this.adminClient = new AdminRestClient(connectionConfig, httpClient, builder.gson);
    this.clusterClient = new ClusterRestClient(connectionConfig, httpClient, builder.gson);
    this.pluginClient = new PluginRestClient(connectionConfig, httpClient, builder.gson);
    this.provisionerClient = new ProvisionerRestClient(connectionConfig, httpClient, builder.gson);
    this.tenantClient = new TenantRestClient(connectionConfig, httpClient, builder.gson);
  }

  /**
   * Create builder for building a RestClientManager instance.
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
    return adminClient;
  }

  @Override
  public ClusterClient getClusterClient() {
    return clusterClient;
  }

  @Override
  public PluginClient getPluginClient() {
    return pluginClient;
  }

  @Override
  public ProvisionerClient getProvisionerClient() {
    return provisionerClient;
  }

  @Override
  public TenantClient getTenantClient() {
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
    private Gson gson = DEFAULT_GSON_INSTANCE;
    private Supplier<AccessToken> accessTokenSupplier;

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

    public Builder accessToken(Supplier<AccessToken> accessTokenSupplier) {
      this.accessTokenSupplier = accessTokenSupplier;
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

    public Builder gson(Gson gson) {
      this.gson = gson;
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
