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
package co.cask.coopr.provisioner.mock;

import co.cask.coopr.common.conf.Constants;
import co.cask.http.AbstractHttpHandler;
import co.cask.http.HttpResponder;
import com.google.common.base.Charsets;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.jboss.netty.buffer.ChannelBufferInputStream;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.jboss.netty.handler.codec.http.HttpResponseStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStreamReader;
import java.io.Reader;
import javax.ws.rs.DELETE;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;

/**
 * Mock Provisioner REST APIs.
 */
@Path(Constants.API_BASE + "/tenants")
public class MockProvisionerHandler extends AbstractHttpHandler {
  private static final Logger LOG = LoggerFactory.getLogger(MockProvisionerHandler.class);
  private final MockProvisionerTenantStore provisionerTenantStore = MockProvisionerTenantStore.getInstance();
  private final Gson gson = new Gson();

  @PUT
  @Path("/{tenant-id}")
  public void writeTenant(HttpRequest request, HttpResponder responder, @PathParam("tenant-id") String tenantId) {
    LOG.debug("Received request to put tenant {}", tenantId);
    Reader reader = new InputStreamReader(new ChannelBufferInputStream(request.getContent()), Charsets.UTF_8);
    JsonObject body = gson.fromJson(reader, JsonObject.class);
    LOG.debug("Request body = {}", body);
    Integer numWorkers = body.get("workers").getAsInt();
    if (numWorkers != provisionerTenantStore.getAssignedWorkers(tenantId)) {
      provisionerTenantStore.setAssignedWorkers(tenantId, numWorkers);
    }
    responder.sendStatus(HttpResponseStatus.OK);
  }

  @DELETE
  @Path("/{tenant-id}")
  public void deleteTenant(HttpRequest request, HttpResponder responder, @PathParam("tenant-id") String tenantId) {
    LOG.debug("Received request to delete tenant {}", tenantId);
    provisionerTenantStore.deleteTenant(tenantId);
    responder.sendStatus(HttpResponseStatus.OK);
  }
}
