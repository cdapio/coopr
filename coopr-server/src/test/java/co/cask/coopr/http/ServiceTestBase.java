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

import co.cask.coopr.BaseTest;
import co.cask.coopr.account.Account;
import co.cask.coopr.common.conf.Constants;
import co.cask.coopr.common.queue.QueueGroup;
import co.cask.coopr.common.queue.QueueType;
import co.cask.coopr.common.queue.TrackingQueue;
import co.cask.coopr.provisioner.Provisioner;
import co.cask.coopr.provisioner.TenantProvisionerService;
import co.cask.coopr.spec.Tenant;
import co.cask.coopr.spec.TenantSpecification;
import com.google.common.base.Strings;
import com.google.common.io.Closeables;
import com.google.inject.Key;
import com.google.inject.name.Names;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicHeader;
import org.jboss.netty.handler.codec.http.HttpResponseStatus;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

/**
 *
 */
public class ServiceTestBase extends BaseTest {
  protected static final String USER1 = "user1";
  protected static final String USER2 = "user2";
  protected static final String API_KEY = "apikey";
  protected static final String TENANT_ID = "tenant1";
  protected static final String PROVISIONER_ID = "provisioner1";
  protected static final String TENANT = "tenant1";
  protected static final Account USER1_ACCOUNT = new Account(USER1, TENANT_ID);
  protected static final Account ADMIN_ACCOUNT = new Account(Constants.ADMIN_USER, TENANT_ID);
  protected static final Account SUPERADMIN_ACCOUNT = new Account(Constants.ADMIN_USER, Constants.SUPERADMIN_TENANT);
  protected static final Header[] USER1_HEADERS = {
    new BasicHeader(Constants.USER_HEADER, USER1),
    new BasicHeader(Constants.API_KEY_HEADER, API_KEY),
    new BasicHeader(Constants.TENANT_HEADER, TENANT_ID)
  };
  protected static final Header[] USER2_HEADERS = {
    new BasicHeader(Constants.USER_HEADER, USER2),
    new BasicHeader(Constants.API_KEY_HEADER, API_KEY),
    new BasicHeader(Constants.TENANT_HEADER, TENANT_ID)
  };
  protected static final Header[] ADMIN_HEADERS = {
    new BasicHeader(Constants.USER_HEADER, Constants.ADMIN_USER),
    new BasicHeader(Constants.API_KEY_HEADER, API_KEY),
    new BasicHeader(Constants.TENANT_HEADER, TENANT_ID)
  };
  protected static final Header[] SUPERADMIN_HEADERS = {
    new BasicHeader(Constants.USER_HEADER, Constants.ADMIN_USER),
    new BasicHeader(Constants.API_KEY_HEADER, API_KEY),
    new BasicHeader(Constants.TENANT_HEADER, Constants.SUPERADMIN_TENANT)
  };

  protected static TrackingQueue balancerQueue;
  private static int externalPort;
  private static int internalPort;
  private static String externalBase;
  private static String internalBase;
  protected static ExternalHandlerServer externalHandlerServer;
  protected static InternalHandlerServer internalHandlerServer;
  protected static QueueGroup provisionerQueues;
  protected static QueueGroup clusterQueues;
  protected static QueueGroup solverQueues;
  protected static QueueGroup jobQueues;
  protected static QueueGroup callbackQueues;
  protected static TenantProvisionerService tenantProvisionerService;

  @BeforeClass
  public static void setupServiceBase() throws Exception {
    balancerQueue = injector.getInstance(Key.get(TrackingQueue.class, Names.named(Constants.Queue.WORKER_BALANCE)));
    provisionerQueues = queueService.getQueueGroup(QueueType.PROVISIONER);
    clusterQueues = queueService.getQueueGroup(QueueType.CLUSTER);
    solverQueues = queueService.getQueueGroup(QueueType.SOLVER);
    jobQueues = queueService.getQueueGroup(QueueType.JOB);
    callbackQueues = queueService.getQueueGroup(QueueType.CALLBACK);
    internalHandlerServer = injector.getInstance(InternalHandlerServer.class);
    internalHandlerServer.startAndWait();
    internalPort = internalHandlerServer.getBindAddress().getPort();
    externalHandlerServer = injector.getInstance(ExternalHandlerServer.class);
    externalHandlerServer.startAndWait();
    externalPort = externalHandlerServer.getBindAddress().getPort();
    tenantProvisionerService = injector.getInstance(TenantProvisionerService.class);
    internalBase = "http://" + HOSTNAME + ":" + internalPort + Constants.API_BASE;
    externalBase = "http://" + HOSTNAME + ":" + externalPort + Constants.API_BASE;
  }

  @Before
  public void setupServiceTest() throws Exception {
    tenantProvisionerService.writeProvisioner(new Provisioner(PROVISIONER_ID, "host1", 12345, 100, null, null));
    tenantStore.writeTenant(new Tenant(TENANT_ID, new TenantSpecification(TENANT, 10, 100, 1000)));
  }

  @AfterClass
  public static void cleanupServiceBase() {
    internalHandlerServer.stopAndWait();
    externalHandlerServer.stopAndWait();
  }

  public static HttpResponse doGetWithoutVersionExternalAPI(String resource) throws Exception {
    return doGetWithoutVersion(resource, externalPort);
  }

  private static HttpResponse doGetWithoutVersion(String resource, int port) throws Exception {
    DefaultHttpClient client = new DefaultHttpClient();
    HttpGet get = new HttpGet("http://" + HOSTNAME + ":" + port + resource);
    return client.execute(get);
  }

  public static HttpResponse doGetInternalAPI(String resource) throws Exception {
    return doGet(resource, null, internalBase);
  }

  public static HttpResponse doGetExternalAPI(String resource, Header[] headers) throws Exception {
    return doGet(resource, headers, externalBase);
  }

  private static HttpResponse doGet(String resource, Header[] headers, String base) throws Exception {
    DefaultHttpClient client = new DefaultHttpClient();
    HttpGet get = new HttpGet(base + resource);

    if (headers != null) {
      get.setHeaders(headers);
    }

    return client.execute(get);
  }

  public static HttpResponse doPutInternalAPI(String resource, String body) throws Exception {
    return doPut(resource, body, null, internalBase);
  }

  public static HttpResponse doPutExternalAPI(String resource, String body, Header[] headers) throws Exception {
    return doPut(resource, body, headers, externalBase);
  }

  public static HttpResponse doPutInternalAPI(String resource, String body, Header[] headers) throws Exception {
    return doPut(resource, body, headers, internalBase);
  }

  private static HttpResponse doPut(String resource, String body, Header[] headers, String base) throws Exception {
    DefaultHttpClient client = new DefaultHttpClient();
    HttpPut put = new HttpPut(base + resource);

    if (headers != null) {
      put.setHeaders(headers);
    }
    if (body != null) {
      put.setEntity(new StringEntity(body));
    }
    return client.execute(put);
  }

  public static HttpResponse doPostInternalAPI(String resource, String body) throws Exception {
    return doPost(resource, body, null, internalBase);
  }

  public static HttpResponse doPostExternalAPI(String resource, String body, Header[] headers) throws Exception {
    return doPost(resource, body, headers, externalBase);
  }

  private static HttpResponse doPost(String resource, String body, Header[] headers, String base) throws Exception {
    DefaultHttpClient client = new DefaultHttpClient();
    HttpPost post = new HttpPost(base + resource);

    if (headers != null) {
      post.setHeaders(headers);
    }
    if (body != null) {
      post.setEntity(new StringEntity(body));
    }

    return client.execute(post);
  }

  public static HttpResponse doDeleteExternalAPI(String resource, Header[] headers) throws Exception {
    return doDelete(resource, headers, externalBase);
  }

  private static HttpResponse doDelete(String resource, Header[] headers, String base) throws Exception {
    DefaultHttpClient client = new DefaultHttpClient();
    HttpDelete delete = new HttpDelete(base + resource);
    if (headers != null) {
      delete.setHeaders(headers);
    }
    return client.execute(delete);
  }

  public static void assertResponseStatus(HttpResponse response, HttpResponseStatus expected) {
    Assert.assertEquals(response.getStatusLine().getReasonPhrase(),
                        expected.getCode(), response.getStatusLine().getStatusCode());
  }

  public static String getBaseUrlInternalAPI() {
    return getBaseUrl(internalPort);
  }

  private static String getBaseUrl(int port) {
    return String.format("http://%s:%d%s", HOSTNAME, port, Constants.API_BASE);
  }

  public static Registry<ConnectionSocketFactory> getSimpleRegistry()
    throws NoSuchAlgorithmException, KeyManagementException {
    return getRegistry(null, null, null, null);
  }

  public static Registry<ConnectionSocketFactory> getRegistry(File keyStore, String keyStorePassword,
                                                              File trustKeyStore, String trustKeyStorePassword)
    throws KeyManagementException, NoSuchAlgorithmException {
    SSLContext sslContext = getContext(keyStore, keyStorePassword, trustKeyStore, trustKeyStorePassword);
    SSLConnectionSocketFactory sf =
      new SSLConnectionSocketFactory(sslContext, SSLConnectionSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);
    return RegistryBuilder
      .<ConnectionSocketFactory>create().register("https", sf)
      .register("http", PlainConnectionSocketFactory.getSocketFactory())
      .build();
  }

  private static SSLContext getContext(File keyStore, String keyStorePassword,
                                       File trustKeyStore, String trustKeyStorePassword)
    throws NoSuchAlgorithmException {
    SSLContext sslContext = SSLContext.getInstance("TLS");
    try {
      KeyManager[] keyManagers = null;
      if (keyStore != null && !Strings.isNullOrEmpty(keyStorePassword)) {
        KeyStore ks = getKeyStore(keyStore, keyStorePassword);
        KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
        kmf.init(ks, keyStorePassword.toCharArray());
        keyManagers = kmf.getKeyManagers();
      }

      TrustManager[] trustManagers = getTrustAllManager();
      if (trustKeyStore != null && !Strings.isNullOrEmpty(trustKeyStorePassword)) {
        KeyStore tks = getKeyStore(trustKeyStore, trustKeyStorePassword);
        TrustManagerFactory tmf = TrustManagerFactory.getInstance("SunX509");
        tmf.init(tks);
        trustManagers = tmf.getTrustManagers();
      }
      sslContext.init(keyManagers, trustManagers, null);
    } catch (Exception e) {
      throw new IllegalArgumentException("Failed to initialize the client-side SSLContext", e);
    }
    return sslContext;
  }

  private static TrustManager[] getTrustAllManager() {
    return new TrustManager[]{new X509TrustManager() {
      @Override
      public java.security.cert.X509Certificate[] getAcceptedIssuers() {
        return null;
      }

      @Override
      public void checkClientTrusted(java.security.cert.X509Certificate[] x509Certificates, String s)
        throws CertificateException {
      }

      @Override
      public void checkServerTrusted(java.security.cert.X509Certificate[] x509Certificates, String s)
        throws CertificateException {
      }
    }};
  }

  private static KeyStore getKeyStore(File keyStore, String keyStorePassword) throws IOException {
    KeyStore ks = null;
    InputStream is = new FileInputStream(keyStore);
    try {
      ks = KeyStore.getInstance("JKS");
      ks.load(is, keyStorePassword.toCharArray());
    } catch (RuntimeException ex) {
        throw ex;
    } catch (Exception ex) {
      throw new IOException(ex);
    } finally {
      Closeables.closeQuietly(is);
    }
    return ks;
  }
}
