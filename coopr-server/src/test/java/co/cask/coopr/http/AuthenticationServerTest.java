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

import co.cask.cdap.common.conf.Constants;
import co.cask.cdap.security.server.ExternalAuthenticationServer;
import co.cask.coopr.TestHelper;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.apache.http.Header;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicHeader;
import org.apache.http.util.EntityUtils;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.net.URL;
import java.util.Map;

/**
 * Test the External Authentication service integration
 */
public class AuthenticationServerTest extends ServiceTestBase {
  private static final String TEST_REALM_PROPERTIES = "test-realm.properties";
  private static final int SLEEP_TIME_IN_SEC = 10;

  private static ExternalAuthenticationServer externalAuthenticationServer;
  private static String authURL;
  private static int testServerPort;

  @BeforeClass
  public static void setup() throws IOException, InterruptedException {
    initAuthTestProps();
    externalHandlerServer = injector.getInstance(ExternalHandlerServer.class);
    externalHandlerServer.startAndWait();
    externalAuthenticationServer = injector.getInstance(ExternalAuthenticationServer.class);
    externalAuthenticationServer.startAndWait();
    // Wait *SLEEP_TIME_IN_SEC* for the complete service start
    Thread.sleep(1000 * SLEEP_TIME_IN_SEC);
    testServerPort = externalHandlerServer.getBindAddress().getPort();
    authURL = String.format("http://%s:%d/token",
                            externalAuthenticationServer.getSocketAddress().getHostName(),
                            externalAuthenticationServer.getSocketAddress().getPort());
  }

  private static void initAuthTestProps() throws IOException {
    conf.setBoolean(Constants.Security.ENABLED, true);
    conf.set(Constants.Security.AUTH_SERVER_ADDRESS, "127.0.0.1");
    conf.setInt(Constants.Security.AUTH_SERVER_PORT, TestHelper.getFreePort());
    conf.set(Constants.Security.AUTH_HANDLER_CLASS, "co.cask.cdap.security.server.BasicAuthenticationHandler");
    URL realmTestFile = AuthenticationServerTest.class.getClassLoader().getResource(TEST_REALM_PROPERTIES);
    if (realmTestFile != null) {
      conf.set(Constants.Security.BASIC_REALM_FILE, realmTestFile.getFile());
    }
    for (Map.Entry<String, String> props : conf) {
      cConfiguration.set(props.getKey(), props.getValue());
    }
  }

  @AfterClass
  public static void shutDown() {
    externalHandlerServer.stopAndWait();
    externalAuthenticationServer.stopAndWait();
  }

  @Test
  public void testStatusUnauthorized() throws Exception {
    HttpResponse response = doGet(String.format("http://%s:%d/status", HOSTNAME, testServerPort), null, null);
    Assert.assertEquals(401, response.getStatusLine().getStatusCode());
    JsonObject entity = gson.fromJson(EntityUtils.toString(response.getEntity()), JsonObject.class);
    JsonArray authURIs = entity.getAsJsonArray("auth_uri");
    Assert.assertNotNull(authURIs);
    Assert.assertEquals(authURL, authURIs.get(0).getAsString());
  }

  @Test
  public void testGetAccessToken() throws IOException {
    HttpResponse response = doGet(authURL, null, new UsernamePasswordCredentials("admin", "password"));
    Assert.assertEquals(200, response.getStatusLine().getStatusCode());
    JsonObject authEntity = gson.fromJson(EntityUtils.toString(response.getEntity()), JsonObject.class);
    Assert.assertNotNull(authEntity);
    String tokenType = authEntity.get("token_type").getAsString();
    Assert.assertEquals("Bearer", tokenType);
  }

  @Test
  public void testGetAccessTokenUnauthorized() throws IOException {
    HttpResponse response = doGet(authURL, null, new UsernamePasswordCredentials("test", "test"));
    Assert.assertEquals(401, response.getStatusLine().getStatusCode());
  }

  @Test
  public void testStatusWithAuth() throws Exception {
    String statusURL = String.format("http://%s:%d/status", HOSTNAME, testServerPort);
    // Send request without access token
    HttpResponse response = doGet(statusURL, null, null);
    // Receive 401 response with authentication service URI in the response body
    Assert.assertEquals(401, response.getStatusLine().getStatusCode());
    JsonObject entity = gson.fromJson(EntityUtils.toString(response.getEntity()), JsonObject.class);
    JsonArray authURIs = entity.getAsJsonArray("auth_uri");
    Assert.assertNotNull(authURIs);
    // Get authentication service URI
    String authURI = authURIs.get(0).getAsString();

    // Send authentication request with credentials to the external authentication service
    response = doGet(authURI, null, new UsernamePasswordCredentials("admin", "password"));
    // Get success response with access token and access token type
    Assert.assertEquals(200, response.getStatusLine().getStatusCode());
    JsonObject authEntity = gson.fromJson(EntityUtils.toString(response.getEntity()), JsonObject.class);
    String accessToken = authEntity.get("access_token").getAsString();
    String tokenType = authEntity.get("token_type").getAsString();

    // Set access token to authentication header
    Header[] headers = {new BasicHeader(HttpHeaders.AUTHORIZATION, String.format("%s %s", tokenType, accessToken))};
    // Send the same request as in the start of test, but with authentication header
    response = doGet(statusURL, headers, null);
    // Receive success response
    Assert.assertEquals(200, response.getStatusLine().getStatusCode());
    Assert.assertEquals("OK\n", EntityUtils.toString(response.getEntity()));
  }

  private static HttpResponse doGet(String url, Header[] headers, UsernamePasswordCredentials credentials)
    throws IOException {

    HttpClientBuilder clientBuilder = HttpClientBuilder.create();
    if (credentials != null) {
      CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
      credentialsProvider.setCredentials(AuthScope.ANY, credentials);
      clientBuilder.setDefaultCredentialsProvider(credentialsProvider);
    }
    HttpClient httpClient = clientBuilder.build();
    HttpGet get = new HttpGet(url);
    if (headers != null) {
      get.setHeaders(headers);
    }
    return httpClient.execute(get);
  }
}
