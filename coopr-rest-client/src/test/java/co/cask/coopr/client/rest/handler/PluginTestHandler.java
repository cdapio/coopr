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

import co.cask.coopr.provisioner.plugin.ResourceMeta;
import co.cask.coopr.spec.plugin.AutomatorType;
import co.cask.http.AbstractHttpHandler;
import co.cask.http.HttpResponder;
import com.google.common.reflect.TypeToken;
import org.jboss.netty.handler.codec.http.HttpResponseStatus;

import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;


public class PluginTestHandler extends AbstractHttpHandler {

  private static final String GET_ALL_AUTOMATOR_TYPES = "v2/plugins/automatortypes";
  private static final String GET_AUTOMATOR_TYPE = "v2/plugins/automatortypes/\\w+";
  private static final String GET_AUTOMATOR_TYPE_RESOURCES = "v2/plugins/automatortypes/\\w+/\\w+";
  private static final String GET_NOT_EXIST_AUTOMATOR_TYPE =
                              "v2/plugins/automatortypes/" + PluginTestConstants.NOT_EXISISTING_PLUGIN;
  private static final String GET_NOT_EXIST_PROVIDER_TYPE =
                              "v2/plugins/providertypes/" + PluginTestConstants.NOT_EXISISTING_PLUGIN;
  private static final String GET_NOT_EXIST_PROVIDER_RESOURCE =
                              "v2/plugins/providertypes/"+ PluginTestConstants.JOYENT_PLUGIN + "/" +
                                PluginTestConstants.NOT_EXISISTING_RESOURCE;
  private static final String GET_NOT_EXIST_AUTOMATOR_RESOURCE =
                              "v2/plugins/automatortypes/" + PluginTestConstants.CHEF_PLUGIN + "/" +
                                PluginTestConstants.NOT_EXISISTING_RESOURCE;
  private static final String STAGE_AUTOMATOR_TYPE_RESOURCES =
                              "v2/plugins/automatortypes/\\w+/\\w+/\\w+/versions/\\w+/stage";
  private static final String RECALL_AUTOMATOR_TYPE_RESOURCES =
                              "v2/plugins/automatortypes/\\w+/\\w+/\\w+/versions/\\w+/recall";
  private static final String DELETE_AUTOMATOR_TYPE_RESOURCES_VERSION =
                              "v2/plugins/automatortypes/\\w+/\\w+/\\w+/versions/\\d*";

  private static final String GET_ALL_PROVIDER_TYPES = "/v2/plugins/providertypes";
  private static final String GET_PROVIDER_TYPE = "/v2/plugins/providertypes/\\w+";
  private static final String GET_PROVIDER_TYPE_RESOURCES = "/v2/plugins/providertypes/\\w+/\\w+/";
  private static final String STAGE_PROVIDER_TYPE_RESOURCES =
                              "/v2/plugins/providertypes/\\w+/\\w+/\\w+/versions/\\w+/stage";
  private static final String RECALL_PROVIDER_TYPE_RESOURCES =
                              "/v2/plugins/providertypes/\\w+/\\w+/\\w+/versions/\\w+/recall";
  private static final String DELETE_PROVIDER_TYPE_RESOURCES_VERSION =
                              "/v2/plugins/providertypes/\\w+/\\w+/\\w+/versions/.*";

  @GET
  @Path(GET_ALL_AUTOMATOR_TYPES)
  public void getAllAutomatorTypes(org.jboss.netty.handler.codec.http.HttpRequest request, HttpResponder responder)
    throws Exception {
    responder.sendJson(HttpResponseStatus.OK, PluginTestConstants.AUTOMATOR_LISTS, new TypeToken<List<AutomatorType>>()
    { }.getType());
  }

  @GET
  @Path(GET_AUTOMATOR_TYPE)
  public void getAutomatorType(org.jboss.netty.handler.codec.http.HttpRequest request, HttpResponder responder)
    throws Exception {
    responder.sendJson(HttpResponseStatus.OK, PluginTestConstants.AUTOMATOR_TYPE, new TypeToken<AutomatorType>() {
    }.getType());
  }

  @GET
  @Path(GET_NOT_EXIST_AUTOMATOR_TYPE)
  public void getNotExistAutomatorType(org.jboss.netty.handler.codec.http.HttpRequest request, HttpResponder responder)
    throws Exception {
    responder.sendStatus(HttpResponseStatus.NOT_FOUND);
  }

  @GET
  @Path(GET_AUTOMATOR_TYPE_RESOURCES)
  public void getAutomatorTypeResources(org.jboss.netty.handler.codec.http.HttpRequest request, HttpResponder responder)
    throws Exception {
    responder.sendJson(HttpResponseStatus.OK, PluginTestConstants.TYPE_RESOURCES,
                       new TypeToken<Map<String, Set<ResourceMeta>>>() { }.getType());
  }

  @GET
  @Path(GET_NOT_EXIST_AUTOMATOR_RESOURCE)
  public void getNotExistAutomatorTypeResources(org.jboss.netty.handler.codec.http.HttpRequest request,
                                                HttpResponder responder) throws Exception {
    responder.sendStatus(HttpResponseStatus.NOT_FOUND);
  }

  @POST
  @Path(STAGE_AUTOMATOR_TYPE_RESOURCES)
  public void stageAutomatorTypeResources(org.jboss.netty.handler.codec.http.HttpRequest request,
                                          HttpResponder responder) throws Exception {
    responder.sendStatus(HttpResponseStatus.OK);
  }

  @POST
  @Path(RECALL_AUTOMATOR_TYPE_RESOURCES)
  public void recallAutomatorTypeResources(org.jboss.netty.handler.codec.http.HttpRequest request,
                                           HttpResponder responder) throws Exception {
    responder.sendStatus(HttpResponseStatus.OK);
  }

  @DELETE
  @Path(DELETE_AUTOMATOR_TYPE_RESOURCES_VERSION)
  public void deleteAutomatorTypeResourcesVersion(org.jboss.netty.handler.codec.http.HttpRequest request,
                                                  HttpResponder responder) throws Exception {
    responder.sendStatus(HttpResponseStatus.OK);
  }

  @GET
  @Path(GET_ALL_PROVIDER_TYPES)
  public void getAllProviderTypes(org.jboss.netty.handler.codec.http.HttpRequest request,
                                  HttpResponder responder) throws Exception {
    responder.sendJson(HttpResponseStatus.OK, PluginTestConstants.PROVIDER_LISTS, new TypeToken<List<AutomatorType>>() {
    }.getType());
  }

  @GET
  @Path(GET_PROVIDER_TYPE)
  public void getProviderType(org.jboss.netty.handler.codec.http.HttpRequest request,
                              HttpResponder responder) throws Exception {
    responder.sendJson(HttpResponseStatus.OK, PluginTestConstants.PROVIDER_TYPE, new TypeToken<AutomatorType>() {
    }.getType());
  }

  @GET
  @Path(GET_PROVIDER_TYPE_RESOURCES)
  public void getProviderTypeResources(org.jboss.netty.handler.codec.http.HttpRequest request,
                                       HttpResponder responder) throws Exception {
    responder.sendJson(HttpResponseStatus.OK, PluginTestConstants.TYPE_RESOURCES,
                       new TypeToken<Map<String, Set<ResourceMeta>>>() {
    }.getType());
  }
  @GET
  @Path(GET_NOT_EXIST_PROVIDER_RESOURCE)
  public void getNotExistProviderTypeResources(org.jboss.netty.handler.codec.http.HttpRequest request,
                                               HttpResponder responder) throws Exception {
    responder.sendStatus(HttpResponseStatus.NOT_FOUND);
  }

  @GET
  @Path(GET_NOT_EXIST_PROVIDER_TYPE)
  public void getNotExistproviderType(org.jboss.netty.handler.codec.http.HttpRequest request,
                                      HttpResponder responder) throws Exception {
    responder.sendStatus(HttpResponseStatus.NOT_FOUND);
  }

  @POST
  @Path(STAGE_PROVIDER_TYPE_RESOURCES)
  public void stageProviderTypeResources(org.jboss.netty.handler.codec.http.HttpRequest request,
                                         HttpResponder responder) throws Exception {
    responder.sendStatus(HttpResponseStatus.OK);
  }

  @POST
  @Path(RECALL_PROVIDER_TYPE_RESOURCES)
  public void recallProviderTypeResources(org.jboss.netty.handler.codec.http.HttpRequest request,
                                          HttpResponder responder) throws Exception {
    responder.sendStatus(HttpResponseStatus.OK);
  }

  @DELETE
  @Path(DELETE_PROVIDER_TYPE_RESOURCES_VERSION)
  public void deleteProviderTypeResourcesVersion(org.jboss.netty.handler.codec.http.HttpRequest request,
                                                 HttpResponder responder) throws Exception {
    responder.sendStatus(HttpResponseStatus.OK);
  }
}
