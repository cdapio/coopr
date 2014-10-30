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
import co.cask.coopr.common.queue.internal.ElementsTrackingQueue;
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
import java.security.KeyStoreException;
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
  private static int port;
  private static String base;
  protected static HandlerServer handlerServer;
  protected static ElementsTrackingQueue balancerQueue;
  protected static QueueGroup provisionerQueues;
  protected static QueueGroup clusterQueues;
  protected static QueueGroup solverQueues;
  protected static QueueGroup jobQueues;
  protected static QueueGroup callbackQueues;
  protected static TenantProvisionerService tenantProvisionerService;


  @BeforeClass
  public static void setupServiceBase() throws Exception {
    balancerQueue = injector.getInstance(
      Key.get(ElementsTrackingQueue.class, Names.named(Constants.Queue.WORKER_BALANCE)));
    provisionerQueues = injector.getInstance(Key.get(QueueGroup.class, Names.named(Constants.Queue.PROVISIONER)));
    clusterQueues = injector.getInstance(Key.get(QueueGroup.class, Names.named(Constants.Queue.CLUSTER)));
    solverQueues = injector.getInstance(Key.get(QueueGroup.class, Names.named(Constants.Queue.SOLVER)));
    jobQueues = injector.getInstance(Key.get(QueueGroup.class, Names.named(Constants.Queue.JOB)));
    callbackQueues = injector.getInstance(Key.get(QueueGroup.class, Names.named(Constants.Queue.CALLBACK)));
    handlerServer = injector.getInstance(HandlerServer.class);
    handlerServer.startAndWait();
    port = handlerServer.getBindAddress().getPort();
    tenantProvisionerService = injector.getInstance(TenantProvisionerService.class);
    base = "http://" + HOSTNAME + ":" + port + Constants.API_BASE;
  }

  @Before
  public void setupServiceTest() throws Exception {
    tenantProvisionerService.writeProvisioner(new Provisioner(PROVISIONER_ID, "host1", 12345, 100, null, null));
    tenantStore.writeTenant(new Tenant(TENANT_ID, new TenantSpecification(TENANT, 10, 100, 1000)));
  }

  @AfterClass
  public static void cleanupServiceBase() {
    handlerServer.stopAndWait();
  }

  public static HttpResponse doGetWithoutVersion(String resource) throws Exception {
    DefaultHttpClient client = new DefaultHttpClient();
    HttpGet get = new HttpGet("http://" + HOSTNAME + ":" + port + resource);
    return client.execute(get);
  }


  public static HttpResponse doGet(String resource) throws Exception {
    return doGet(resource, null);
  }

  public static HttpResponse doGet(String resource, Header[] headers) throws Exception {
    DefaultHttpClient client = new DefaultHttpClient();
    HttpGet get = new HttpGet(base + resource);

    if (headers != null) {
      get.setHeaders(headers);
    }

    return client.execute(get);
  }

  public static HttpResponse doPut(String resource, String body) throws Exception {
    return doPut(resource, body, null);
  }

  public static HttpResponse doPut(String resource, String body, Header[] headers) throws Exception {
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

  public static HttpResponse doPost(String resource, String body) throws Exception {
    return doPost(resource, body, null);
  }

  public static HttpResponse doPost(String resource, String body, Header[] headers) throws Exception {
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

  public static HttpResponse doDelete(String resource, Header[] headers) throws Exception {
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

  public static String getBaseUrl() {
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
