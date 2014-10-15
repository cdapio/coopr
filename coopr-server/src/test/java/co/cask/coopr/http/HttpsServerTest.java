package co.cask.coopr.http;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.BasicHttpClientConnectionManager;
import org.apache.http.util.EntityUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

public class HttpsServerTest extends ServiceTestBase {



  @BeforeClass
  public static void setup() {
    conf.setBoolean("server.ssl.enabled", true);
    conf.set("server.ssl.keystore.path", HttpsServerTest.class.getClassLoader().getResource("cert.jks").getPath());
    conf.set("server.ssl.keystore.password", "secret");
    conf.set("server.ssl.cert.password", "secret");
  }

  @Before
  public void startServer() {
    handlerServer = injector.getInstance(HandlerServer.class);
    handlerServer.startAndWait();
  }

  @After
  public void stopServer() {
    handlerServer.stopAndWait();
  }

  @Test
  public void testStatus() throws Exception {
    HttpResponse response = doGet(String.format("https://%s:%d/status", HOSTNAME,
                                                handlerServer.getBindAddress().getPort()));
    Assert.assertEquals(200, response.getStatusLine().getStatusCode());
    Assert.assertEquals("OK\n", EntityUtils.toString(response.getEntity()));
  }

  @Test(expected = TimeoutException.class)
  public void testStatusFail() throws Exception {
    Future<HttpResponse> future = Executors.newSingleThreadExecutor().submit(new Callable<HttpResponse>() {
      public HttpResponse call() throws Exception {
        return doGet(String.format("http://%s:%d/status", HOSTNAME,
                                     handlerServer.getBindAddress().getPort()));
      }
    });
    HttpResponse response = future.get(5000, TimeUnit.MILLISECONDS);
    Assert.assertNull(response);
  }

  public static HttpResponse doGet(String url) throws Exception {
    HttpClient client = HttpClients.custom()
      .setConnectionManager(new BasicHttpClientConnectionManager(getRegistryWithDisabledCertCheck())).build();
    HttpGet get = new HttpGet(url);
    return client.execute(get);
  }

  private static Registry<ConnectionSocketFactory> getRegistryWithDisabledCertCheck()
    throws KeyManagementException, NoSuchAlgorithmException {
    SSLContext sslContext = SSLContext.getInstance("TLS");
    sslContext.init(null, new TrustManager[]{new X509TrustManager() {
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
    }}, new SecureRandom());
    SSLConnectionSocketFactory sf =
      new SSLConnectionSocketFactory(sslContext, SSLConnectionSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);
    return RegistryBuilder
      .<ConnectionSocketFactory>create().register("https", sf)
      .register("http", PlainConnectionSocketFactory.getSocketFactory())
      .build();
  }
}
