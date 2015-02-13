package co.cask.coopr.provisioner;

import co.cask.coopr.common.conf.Configuration;
import co.cask.coopr.common.conf.Constants;
import co.cask.coopr.provisioner.plugin.ResourceCollection;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableMap;
import com.google.gson.Gson;
import com.google.inject.Inject;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.config.SocketConfig;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.NoConnectionReuseStrategy;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Service that makes http calls with retries to provisioners for different provisioner operations, such as
 * deleting a tenant or putting tenant information.
 */
public class HttpProvisionerRequestService implements ProvisionerRequestService {
  private static final Logger LOG  = LoggerFactory.getLogger(HttpProvisionerRequestService.class);
  private static final String BASE_TENANT_PATH = Constants.API_BASE + "/tenants/";
  private final int maxRetries;
  private final long msBetweenRetries;
  private final CloseableHttpClient httpClient;
  private final Gson gson;

  @Inject
  private HttpProvisionerRequestService(Configuration conf, Gson gson) {
    this.maxRetries = conf.getInt(Constants.PROVISIONER_REQUEST_MAX_RETRIES);
    this.msBetweenRetries = conf.getLong(Constants.PROVISIONER_REQUEST_MS_BETWEEN_RETRIES);
    int socketTimeout = conf.getInt(Constants.PROVISIONER_REQUEST_SOCKET_TIMEOUT_MS);
    int connectTimeout = conf.getInt(Constants.PROVISIONER_REQUEST_CONNECT_TIMEOUT_MS);

    this.httpClient = HttpClients.custom()
      .setConnectionReuseStrategy(NoConnectionReuseStrategy.INSTANCE)
      .setDefaultSocketConfig(SocketConfig.custom().setSoTimeout(socketTimeout).build())
      .setDefaultRequestConfig(
        RequestConfig.custom().setSocketTimeout(socketTimeout).setConnectTimeout(connectTimeout).build())
      .build();
    this.gson = gson;
  }

  @Override
  public boolean deleteTenant(Provisioner provisioner, String tenantId) {
    HttpDelete delete = new HttpDelete(getTenantURL(provisioner, tenantId));
    return makeRequestWithRetries(delete);
  }

  @Override
  public boolean putTenant(Provisioner provisioner, String tenantId, ResourceCollection resourceCollection) {
    HttpPut put = new HttpPut(getTenantURL(provisioner, tenantId));
    int workers = provisioner.getAssignedWorkers(tenantId);
    try {
      String body = gson.toJson(new ProvisionerTenant(workers, resourceCollection));
      put.setEntity(new StringEntity(body));
    } catch (UnsupportedEncodingException e) {
      // should never happen
      LOG.error("Unsupported encoding when writing tenant {} to provisioner {}",
                tenantId, provisioner.getId());
      Throwables.propagate(e);
    }
    return makeRequestWithRetries(put);
  }

  @Override
  public boolean putTenantWorkers(Provisioner provisioner, String tenantId) {
    HttpPut put = new HttpPut(getTenantURL(provisioner, tenantId) + "/workers");
    Map<String, Integer> body = ImmutableMap.of("workers", provisioner.getAssignedWorkers(tenantId));
    try {
      put.setEntity(new StringEntity(gson.toJson(body)));
    } catch (UnsupportedEncodingException e) {
      // should never happen
      LOG.error("Unsupported encoding when writing workers for tenant {} to provisioner {}",
                tenantId, provisioner.getId());
      Throwables.propagate(e);
    }
    return makeRequestWithRetries(put);
  }

  @Override
  public boolean putTenantResources(Provisioner provisioner, String tenantId, ResourceCollection resourceCollection) {
    HttpPut put = new HttpPut(getTenantURL(provisioner, tenantId) + "/resources");
    Map<String, ResourceCollection> body = ImmutableMap.of("resources", resourceCollection);
    try {
      put.setEntity(new StringEntity(gson.toJson(body)));
    } catch (UnsupportedEncodingException e) {
      // should never happen
      LOG.error("Unsupported encoding when writing resources for tenant {} to provisioner {}",
                tenantId, provisioner.getId());
      Throwables.propagate(e);
    }
    return makeRequestWithRetries(put);
  }

  private boolean makeRequestWithRetries(HttpRequestBase request) {
    int numRetried = 0;
    while (numRetried < maxRetries) {
      try {
        int statusCode = makeRequest(request);
        if (statusCode / 100 == 2) {
          return true;
        }
        LOG.error("{} request to {} failed with status code {}",
                  request.getMethod(), request.getURI().toString(), statusCode);
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

  private int makeRequest(HttpRequestBase request) throws IOException {
    CloseableHttpResponse response = httpClient.execute(request);
    try {
      return response.getStatusLine().getStatusCode();
    } finally {
      response.close();
    }
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
