package com.continuuity.loom.provisioner;

import com.continuuity.loom.common.conf.Configuration;
import com.continuuity.loom.common.conf.Constants;
import com.google.common.base.Throwables;
import com.google.gson.Gson;
import com.google.inject.Inject;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.config.SocketConfig;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.concurrent.TimeUnit;

/**
 * Service that makes http calls with retries to provisioners for different provisioner operations, such as
 * deleting a tenant or putting tenant information.
 */
public class HttpProvisionerRequestService implements ProvisionerRequestService {
  private static final Gson GSON = new Gson();
  private static final Logger LOG  = LoggerFactory.getLogger(HttpProvisionerRequestService.class);
  private static final String BASE_TENANT_PATH = "/v1/tenants/";
  private final int maxRetries;
  private final long msBetweenRetries;
  private final HttpClient httpClient;

  @Inject
  private HttpProvisionerRequestService(Configuration conf) {
    this.maxRetries = conf.getInt(Constants.PROVISIONER_REQUEST_MAX_RETRIES);
    this.msBetweenRetries = conf.getLong(Constants.PROVISIONER_REQUEST_MS_BETWEEN_RETRIES);
    int maxConnections = conf.getInt(Constants.PROVISIONER_REQUEST_MAX_CONNECTIONS);
    int socketTimeout = conf.getInt(Constants.PROVISIONER_REQUEST_SOCKET_TIMEOUT_MS);

    PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager();
    connectionManager.setDefaultMaxPerRoute(maxConnections);
    connectionManager.setMaxTotal(maxConnections);
    SocketConfig socketConfig = SocketConfig.custom().setSoTimeout(socketTimeout).build();
    connectionManager.setDefaultSocketConfig(socketConfig);
    this.httpClient = HttpClientBuilder.create().setConnectionManager(connectionManager).build();
  }

  @Override
  public boolean deleteTenant(Provisioner provisioner, String tenantId) {
    HttpDelete delete = new HttpDelete(getTenantURL(provisioner, tenantId));
    return makeRequestWithRetries(delete);
  }

  @Override
  public boolean putTenant(Provisioner provisioner, String tenantId) {
    HttpPut put = new HttpPut(getTenantURL(provisioner, tenantId));
    ProvisionerTenant provisionerTenant = new ProvisionerTenant(provisioner.getAssignedWorkers(tenantId));
    try {
      put.setEntity(new StringEntity(GSON.toJson(provisionerTenant)));
    } catch (UnsupportedEncodingException e) {
      // should never happen
      LOG.error("Unsupported encoding when putting tenant {} to provisioner {}", tenantId, provisioner.getId());
      Throwables.propagate(e);
    }
    return makeRequestWithRetries(put);
  }

  private boolean makeRequestWithRetries(HttpRequestBase request) {
    int numRetried = 0;
    while (numRetried < maxRetries) {
      try {
        HttpResponse response = httpClient.execute(request);
        if (response.getStatusLine().getStatusCode() / 100 == 2) {
          return true;
        }
      } catch (IOException e) {
        LOG.error("Exception making {} request to {} on attempt #{}",
                  request.getMethod(), request.getURI().toString(), numRetried + 1, e);
      } finally {
        request.releaseConnection();
      }
      numRetried++;
      try {
        TimeUnit.MILLISECONDS.sleep(msBetweenRetries);
      } catch (InterruptedException e) {
        LOG.error("Sleep between retries interrupted.", e);
        Throwables.propagate(e);
      }
    }

    return false;
  }

  private String getTenantURL(Provisioner provisioner, String tenantId) {
    return new StringBuilder()
      .append("http://")
      .append(provisioner.getHost())
      .append(":")
      .append(provisioner.getPort())
      .append(BASE_TENANT_PATH)
      .append(tenantId)
      .toString();
  }
}
