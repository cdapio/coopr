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

package co.cask.coopr.client.rest.handler;

import co.cask.coopr.Entities;
import co.cask.coopr.client.rest.PluginRestTest;
import co.cask.coopr.client.rest.RestClientTest;
import co.cask.coopr.provisioner.plugin.ResourceMeta;
import co.cask.coopr.provisioner.plugin.ResourceStatus;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.gson.Gson;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.RequestLine;
import org.apache.http.entity.StringEntity;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpRequestHandler;

import java.io.IOException;
import javax.ws.rs.HttpMethod;


public class PluginTestHandler implements HttpRequestHandler {
  private static final String GET_ALL_AUTOMATOR_TYPES = "/v2/plugins/automatortypes";
  private static final String GET_AUTOMATOR_TYPE = "/v2/plugins/automatortypes/\\w+";
  private static final String GET_AUTOMATOR_TYPE_RESOURCES = "/v2/plugins/automatortypes/\\w+/\\w+/\\?status=.*";
  private static final String STAGE_AUTOMATOR_TYPE_RESOURCES =
    "/v2/plugins/automatortypes/\\w+/\\w+/\\w+/versions/\\w+/stage";
  private static final String RECALL_AUTOMATOR_TYPE_RESOURCES =
    "/v2/plugins/automatortypes/\\w+/\\w+/\\w+/versions/\\w+/recall";
  private static final String DELETE_AUTOMATOR_TYPE_RESOURCES_VERSION =
    "/v2/plugins/automatortypes/\\w+/\\w+/\\w+/versions/.*";

  private static final String GET_ALL_PROVIDER_TYPES = "/v2/plugins/providertypes";
  private static final String GET_PROVIDER_TYPE = "/v2/plugins/providertypes/\\w+";
  private static final String GET_PROVIDER_TYPE_RESOURCES = "/v2/plugins/providertypes/\\w+/\\w+/\\?status=.*";
  private static final String STAGE_PROVIDER_TYPE_RESOURCES =
    "/v2/plugins/providertypes/\\w+/\\w+/\\w+/versions/\\w+/stage";
  private static final String RECALL_PROVIDER_TYPE_RESOURCES =
    "/v2/plugins/providertypes/\\w+/\\w+/\\w+/versions/\\w+/recall";
  private static final String DELETE_PROVIDER_TYPE_RESOURCES_VERSION =
    "/v2/plugins/providertypes/\\w+/\\w+/\\w+/versions/.*";

  private static final Gson GSON = new Gson();
  private static final String COOPR_TENANT_ID_HEADER_NAME = "Coopr-TenantID";
  private static final String COOPR_USER_ID_HEADER_NAME = "Coopr-UserID";

  @Override
  public void handle(HttpRequest request, HttpResponse response, HttpContext context)
    throws HttpException, IOException {

    RequestLine requestLine = request.getRequestLine();
    String method = requestLine.getMethod();
    String url = requestLine.getUri();
    int statusCode = HttpStatus.SC_OK;
    String responseBody = "";

    String userId = request.getFirstHeader(COOPR_USER_ID_HEADER_NAME).getValue();
    String tenantId = request.getFirstHeader(COOPR_TENANT_ID_HEADER_NAME).getValue();
    if (userId.equals(RestClientTest.TEST_USER_ID) && tenantId.equals(RestClientTest.TEST_TENANT_ID)) {
      if (url.equals(GET_ALL_AUTOMATOR_TYPES) || url.equals(GET_ALL_PROVIDER_TYPES) && HttpMethod.GET.equals(method)) {
        if (url.equals(GET_ALL_AUTOMATOR_TYPES)) {
          responseBody = GSON.toJson(Lists.newArrayList(
            Entities.AutomatorTypeExample.CHEF,
            Entities.AutomatorTypeExample.PUPPET));
        } else {
          responseBody = GSON.toJson(Lists.newArrayList(
            Entities.ProviderTypeExample.JOYENT,
            Entities.ProviderTypeExample.RACKSPACE));
        }

      } else if (url.matches(GET_AUTOMATOR_TYPE) || url.matches(GET_PROVIDER_TYPE) && HttpMethod.GET.equals(method)) {
        if (url.matches(GET_AUTOMATOR_TYPE)) {
          if (url.contains(PluginRestTest.CHEF_PLUGIN)) {
            responseBody = GSON.toJson(Entities.AutomatorTypeExample.CHEF);
          } else {
            statusCode = HttpStatus.SC_NOT_FOUND;
          }
        } else {
          if (url.contains(PluginRestTest.JOYENT_PLUGIN)) {
            responseBody = GSON.toJson(Entities.ProviderTypeExample.JOYENT);
          } else {
            statusCode = HttpStatus.SC_NOT_FOUND;
          }
        }
      } else if (url.matches(GET_AUTOMATOR_TYPE_RESOURCES) || url.matches(GET_PROVIDER_TYPE_RESOURCES)
        && HttpMethod.GET.equals(method)) {
        if (url.contains(PluginRestTest.REACTOR_RESOURCE)) {
          ResourceMeta reactor1 = new ResourceMeta(PluginRestTest.REACTOR_RESOURCE, 1, ResourceStatus.ACTIVE);
          ResourceMeta reactor2 = new ResourceMeta(PluginRestTest.REACTOR_RESOURCE, 2, ResourceStatus.ACTIVE);
          responseBody = GSON.toJson(ImmutableMap.of(PluginRestTest.REACTOR_RESOURCE,
                                                     ImmutableSet.of(reactor1, reactor2)));
        } else {
          statusCode = HttpStatus.SC_NOT_FOUND;
        }

      } else if (url.matches(STAGE_AUTOMATOR_TYPE_RESOURCES) || url.matches(STAGE_PROVIDER_TYPE_RESOURCES)
        && HttpMethod.POST.equals(method)) {
        // Send 200 OK
      } else if (url.matches(RECALL_AUTOMATOR_TYPE_RESOURCES) || url.matches(RECALL_PROVIDER_TYPE_RESOURCES)
        && HttpMethod.POST.equals(method)) {
        // Send 200 OK
      } else if (url.matches(DELETE_AUTOMATOR_TYPE_RESOURCES_VERSION)
        || url.matches(DELETE_PROVIDER_TYPE_RESOURCES_VERSION) && HttpMethod.DELETE.equals(method)) {
        // Send 200 OK
      } else {
        statusCode = HandlerUtils.getStatusCodeByTestStatusUserId(userId);
      }

      response.setStatusCode(statusCode);
      if (!Strings.isNullOrEmpty(responseBody)) {
        response.setEntity(new StringEntity(responseBody));
      }

    }
  }
}
