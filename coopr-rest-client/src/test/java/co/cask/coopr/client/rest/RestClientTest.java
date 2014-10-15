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
import co.cask.coopr.client.rest.handler.ProviderHandler;
import co.cask.coopr.client.rest.handler.ServiceHandler;
import org.apache.http.localserver.LocalTestServer;
import org.junit.After;
import org.junit.Before;

/**
 * Contains common fields for REST Client API unit tests.
 */
public class RestClientTest {

  public static final String TEST_USER_ID = "test";
  public static final String TEST_TENANT_ID = "supertest";

  protected ClientManager clientManager;
  protected String testServerHost;
  protected int testServerPort;

  private LocalTestServer localTestServer;

  private ClusterTemplateHandler clusterTemplatesHandler = new ClusterTemplateHandler();
  private ProviderHandler providerHandler = new ProviderHandler();
  private ServiceHandler serviceHandler = new ServiceHandler();
  private HardwareTypeHandler hardwareTypeHandler = new HardwareTypeHandler();
  private ImageTypeHandler imageTypeHandler = new ImageTypeHandler();
  private ClusterHandler clusterHandler = new ClusterHandler();

  @Before
  public void setUp() throws Exception {
    localTestServer = new LocalTestServer(null, null);
    localTestServer.register("/v2/clustertemplates*", clusterTemplatesHandler);
    localTestServer.register("/v2/providers*", providerHandler);
    localTestServer.register("/v2/services*", serviceHandler);
    localTestServer.register("/v2/hardwaretypes*", hardwareTypeHandler);
    localTestServer.register("/v2/imagetypes*", imageTypeHandler);
    localTestServer.register("/v2/clusters*", clusterHandler);
    localTestServer.start();
    testServerHost = localTestServer.getServiceAddress().getHostName();
    testServerPort = localTestServer.getServiceAddress().getPort();
    clientManager = RestClientManager.builder(testServerHost, testServerPort)
      .userId(TEST_USER_ID)
      .tenantId(TEST_TENANT_ID)
      .build();
  }

  @After
  public void shutDown() throws Exception {
    clientManager.close();
    localTestServer.stop();
  }
}
