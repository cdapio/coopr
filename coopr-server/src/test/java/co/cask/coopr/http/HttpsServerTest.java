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

import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 *
 */
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
    externalHandlerServer = injector.getInstance(ExternalHandlerServer.class);
    externalHandlerServer.startAndWait();
  }

  @After
  public void stopServer() {
    externalHandlerServer.stopAndWait();
  }

  @Test
  public void testStatus() throws Exception {
    HttpResponse response = doSecureGet(String.format("https://%s:%d/status", HOSTNAME,
                                                externalHandlerServer.getBindAddress().getPort()));
    Assert.assertEquals(200, response.getStatusLine().getStatusCode());
    Assert.assertEquals("OK\n", EntityUtils.toString(response.getEntity()));
  }

  @Test(expected = TimeoutException.class)
  public void testStatusFail() throws Exception {
    Future<HttpResponse> future = Executors.newSingleThreadExecutor().submit(new Callable<HttpResponse>() {
      public HttpResponse call() throws Exception {
        return doSecureGet(String.format("http://%s:%d/status", HOSTNAME,
                                         externalHandlerServer.getBindAddress().getPort()));
      }
    });
    future.get(5000, TimeUnit.MILLISECONDS);
  }

  private static HttpResponse doSecureGet(String url) throws Exception {
    HttpClient client = HttpClients.custom()
      .setConnectionManager(new BasicHttpClientConnectionManager(getSimpleRegistry())).build();
    HttpGet get = new HttpGet(url);
    return client.execute(get);
  }
}
