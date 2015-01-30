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
import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
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
  private static final Gson GSON = new Gson();

  private final AdminClient adminClient;
  private final ClusterClient clusterClient;
  private final PluginClient pluginClient;
  private final ProvisionerClient provisionerClient;
  private final TenantClient tenantClient;
  private final CloseableHttpClient httpClient;
  private Registry<ConnectionSocketFactory> connectionRegistry;

  public RestClientManager(Supplier<RestClientConnectionConfig> connectionConfig) {
    if (!connectionConfig.get().isVerifySSLCert()) {
      try {
        connectionRegistry = RestUtil.getRegistryWithDisabledCertCheck();
      } catch (KeyManagementException e) {
        LOG.error("Failed to init SSL context: {}", e);
      } catch (NoSuchAlgorithmException e) {
        LOG.error("Failed to get instance of SSL context: {}", e);
      }
    }
    this.httpClient = HttpClients.custom().setConnectionManager(createConnectionManager()).build();
    this.adminClient = new AdminRestClient(connectionConfig, httpClient, GSON);
    this.clusterClient = new ClusterRestClient(connectionConfig, httpClient, GSON);
    this.pluginClient = new PluginRestClient(connectionConfig, httpClient, GSON);
    this.provisionerClient = new ProvisionerRestClient(connectionConfig, httpClient, GSON);
    this.tenantClient = new TenantRestClient(connectionConfig, httpClient, GSON);
  }

  public RestClientManager(RestClientConnectionConfig connectionConfig) {
    this(Suppliers.ofInstance(connectionConfig));
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

}
