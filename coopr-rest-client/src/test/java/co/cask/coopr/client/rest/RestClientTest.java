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

import co.cask.coopr.client.ClientManager;
import co.cask.coopr.client.rest.handler.ClusterHandler;
import co.cask.coopr.client.rest.handler.ClusterTemplateHandler;
import co.cask.coopr.client.rest.handler.HardwareTypeHandler;
import co.cask.coopr.client.rest.handler.ImageTypeHandler;
import co.cask.coopr.client.rest.handler.PartialTemplateHandler;
import co.cask.coopr.client.rest.handler.ProviderHandler;
import co.cask.coopr.client.rest.handler.ProvisionerHandler;
import co.cask.coopr.client.rest.handler.ServiceHandler;
import com.google.common.base.Suppliers;
import org.apache.http.conn.ssl.SSLContexts;
import co.cask.coopr.client.rest.handler.TenantHandler;
import org.apache.http.localserver.LocalTestServer;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;

import java.io.InputStream;
import java.net.URL;
import java.security.KeyStore;
import javax.net.ssl.SSLContext;

/**
 * Contains common fields for REST Client API unit tests.
 */
public class RestClientTest {

  public static final String TEST_USER_ID = "test";
  public static final String TEST_TENANT_ID = "supertest";

  protected ClientManager clientManager;
  protected String testServerHost;
  protected int testServerPort;
  protected boolean sslEnabled = false;

  private LocalTestServer localTestServer;

  private ClusterTemplateHandler clusterTemplatesHandler = new ClusterTemplateHandler();
  private PartialTemplateHandler partialTemplatesHandler = new PartialTemplateHandler();
  private ProviderHandler providerHandler = new ProviderHandler();
  private ServiceHandler serviceHandler = new ServiceHandler();
  private HardwareTypeHandler hardwareTypeHandler = new HardwareTypeHandler();
  private ImageTypeHandler imageTypeHandler = new ImageTypeHandler();
  private ClusterHandler clusterHandler = new ClusterHandler();
  private ProvisionerHandler provisionerHandler = new ProvisionerHandler();
  private TenantHandler tenantHandler = new TenantHandler();

  @Before
  public void setUp() throws Exception {
    if (sslEnabled) {
      URL keyStoreURL = getClass().getClassLoader().getResource("cert.jks");
      Assert.assertNotNull(keyStoreURL);
      final InputStream inStream = keyStoreURL.openStream();
      KeyStore keystore = KeyStore.getInstance("jks");
      try {
        keystore.load(inStream, "secret".toCharArray());
      } finally {
        inStream.close();
      }

      final SSLContext serverSSLContext = SSLContexts.custom()
        .useProtocol("TLS")
        .loadKeyMaterial(keystore, "secret".toCharArray())
        .build();
      localTestServer = new LocalTestServer(serverSSLContext);
    } else {
      localTestServer = new LocalTestServer(null, null);
    }

    localTestServer.register("/v2/clustertemplates*", clusterTemplatesHandler);
    localTestServer.register("/v2/partialtemplates*", partialTemplatesHandler);
    localTestServer.register("/v2/providers*", providerHandler);
    localTestServer.register("/v2/services*", serviceHandler);
    localTestServer.register("/v2/hardwaretypes*", hardwareTypeHandler);
    localTestServer.register("/v2/imagetypes*", imageTypeHandler);
    localTestServer.register("/v2/clusters*", clusterHandler);
    localTestServer.register("/v2/provisioners*", provisionerHandler);
    localTestServer.register("/v2/tenants*", tenantHandler);
    localTestServer.start();
    testServerHost = localTestServer.getServiceAddress().getHostName();
    testServerPort = localTestServer.getServiceAddress().getPort();
    clientManager = createClientManager(TEST_USER_ID);
  }

  protected ClientManager createClientManager(String userId) {
    return new RestClientManager(Suppliers.ofInstance(
      RestClientConnectionConfig.builder(testServerHost, testServerPort)
        .userId(userId)
        .tenantId(TEST_TENANT_ID)
        .ssl(sslEnabled)
        .verifySSLCert(!sslEnabled)
        .build()));
  }

  @After
  public void shutDown() throws Exception {
    clientManager.close();
    localTestServer.stop();
  }
}
