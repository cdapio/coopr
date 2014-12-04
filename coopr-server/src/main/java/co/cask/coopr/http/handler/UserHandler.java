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
package co.cask.coopr.http.handler;

import co.cask.coopr.account.Account;
import co.cask.coopr.common.conf.Constants;
import co.cask.coopr.http.HttpHelper;
import co.cask.coopr.store.tenant.TenantStore;
import co.cask.coopr.store.user.UserStore;
import co.cask.http.HttpResponder;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.google.inject.Inject;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.jboss.netty.handler.codec.http.HttpResponseStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Map;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;

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
      request, responder, new TypeToken<Map<String, Object>>() { }.getType(), gson);
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
