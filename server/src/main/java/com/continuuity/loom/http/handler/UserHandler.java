/*
 * Copyright 2012-2014, Continuuity, Inc.
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
package com.continuuity.loom.http.handler;

import com.continuuity.http.HttpResponder;
import com.continuuity.loom.account.Account;
import com.continuuity.loom.common.conf.Constants;
import com.continuuity.loom.http.HttpHelper;
import com.continuuity.loom.http.request.TenantWriteRequest;
import com.continuuity.loom.provisioner.CapacityException;
import com.continuuity.loom.provisioner.Provisioner;
import com.continuuity.loom.provisioner.QuotaException;
import com.continuuity.loom.provisioner.TenantProvisionerService;
import com.continuuity.loom.spec.TenantSpecification;
import com.continuuity.loom.spec.plugin.AutomatorType;
import com.continuuity.loom.spec.plugin.ProviderType;
import com.continuuity.loom.store.entity.EntityStoreService;
import com.continuuity.loom.store.tenant.TenantStore;
import com.continuuity.loom.store.user.UserStore;
import com.google.common.base.Charsets;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.google.inject.Inject;
import org.jboss.netty.buffer.ChannelBufferInputStream;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.jboss.netty.handler.codec.http.HttpResponseStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.Type;
import java.util.Map;

/**
 * Handler for managing users. Beta api, may undergo changes.
 */
@Path(Constants.API_BASE)
public class UserHandler extends AbstractAuthHandler {
  private static final Logger LOG  = LoggerFactory.getLogger(UserHandler.class);

  private final Gson gson;
  private final UserStore userStore;

  @Inject
  private UserHandler(TenantStore tenantStore, UserStore userStore, Gson gson) {
    super(tenantStore);
    this.gson = gson;
    this.userStore = userStore;
  }

  /**
   * Get the profile for an account.
   *
   * @param request Request for account profile.
   * @param responder Responder for sending the response.
   */
  @GET
  @Path("/profile")
  public void getProfile(HttpRequest request, HttpResponder responder) {
    Account account = getAndAuthenticateAccount(request, responder);
    if (account == null) {
      return;
    }

    try {
      Map<String, Object> profile = userStore.getProfile(account);
      if (profile == null) {
        responder.sendStatus(HttpResponseStatus.NOT_FOUND);
        return;
      }
      responder.sendJson(HttpResponseStatus.OK, profile);
    } catch (IOException e) {
      LOG.error("Error getting profile for account {}.", account, e);
      responder.sendError(HttpResponseStatus.INTERNAL_SERVER_ERROR,
                          "Error getting profile for user " + account.getUserId());
    }
  }

  /**
   * Write an account profile.
   *
   * @param request Request for a tenant.
   * @param responder Responder for sending the response.
   * TODO: should not write profile if the user does not already exist. punting until user management is flushed out
   */
  @PUT
  @Path("/profile")
  public void writeProfile(HttpRequest request, HttpResponder responder) {
    Account account = getAndAuthenticateAccount(request, responder);
    if (account == null) {
      return;
    }

    Map<String, Object> profile = HttpHelper.decodeRequestBody(
      request, responder, new TypeToken<Map<String, Object>>() {}.getType(), gson);
    if (profile == null) {
      return;
    }

    try {
      userStore.writeProfile(account, profile);
      responder.sendStatus(HttpResponseStatus.OK);
    } catch (IOException e) {
      LOG.error("Exception writing profile for account {}.", account, e);
      responder.sendError(HttpResponseStatus.INTERNAL_SERVER_ERROR,
                          "Error writing profile for user " + account.getUserId());
    }
  }

  /**
   * Delete the profile for an account.
   *
   * @param request Request to delete a profile.
   * @param responder Responder for sending the response.
   */
  @DELETE
  @Path("/profile")
  public void deleteProfile(HttpRequest request, HttpResponder responder) {
    Account account = getAndAuthenticateAccount(request, responder);
    if (account == null) {
      return;
    }

    try {
      userStore.deleteProfile(account);
      responder.sendStatus(HttpResponseStatus.OK);
    } catch (IOException e) {
      LOG.error("Exception deleting profile for account {}.", account, e);
      responder.sendError(HttpResponseStatus.INTERNAL_SERVER_ERROR,
                          "Error deleting profile for user " + account.getUserId());
    }
  }
}
