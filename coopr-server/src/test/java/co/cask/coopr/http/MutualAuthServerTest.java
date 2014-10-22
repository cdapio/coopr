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

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.BasicHttpClientConnectionManager;
import org.apache.http.util.EntityUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Test the HttpsServer with mutual authentication.
 */
public class MutualAuthServerTest extends ServiceTestBase {

  private static File serverKeyStore;
  private static File clientKeyStore;
  private static final String SERVER_KEY_STORE_PASSWORD = "secret";
  private static final String CLIENT_KEY_STORE_PASSWORD = "password";

  @BeforeClass
  public static void setup() {
    conf.setBoolean("server.ssl.enabled", true);
    String serverCertificate = HttpsServerTest.class.getClassLoader().getResource("cert.jks").getPath();
    conf.set("server.ssl.keystore.path", serverCertificate);
    conf.set("server.ssl.keystore.password", SERVER_KEY_STORE_PASSWORD);
    conf.set("server.ssl.cert.password", "secret");
    serverKeyStore = new File(serverCertificate);

    String clientCertificate = HttpsServerTest.class.getClassLoader().getResource("client.jks").getFile();
    conf.set("server.ssl.trust.keystore.path", clientCertificate);
    conf.set("server.ssl.trust.keystore.password", CLIENT_KEY_STORE_PASSWORD);
    clientKeyStore = new File(clientCertificate);
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
    future.get(5000, TimeUnit.MILLISECONDS);
  }

  public static HttpResponse doGet(String url) throws Exception {
    HttpClient client = HttpClients.custom()
      .setConnectionManager(new BasicHttpClientConnectionManager(getRegistry(clientKeyStore, CLIENT_KEY_STORE_PASSWORD,
                                                                             serverKeyStore,
                                                                             SERVER_KEY_STORE_PASSWORD))).build();
    HttpGet get = new HttpGet(url);
    return client.execute(get);
  }


}
