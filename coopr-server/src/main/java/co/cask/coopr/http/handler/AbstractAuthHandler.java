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
import co.cask.coopr.spec.Tenant;
import co.cask.coopr.store.tenant.TenantStore;
import co.cask.http.AbstractHttpHandler;
import co.cask.http.HttpResponder;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.jboss.netty.handler.codec.http.HttpResponseStatus;

import java.io.IOException;

/**
 * Abstract handler that provides some base methods for authenticating and authorizing requests.
 */
public abstract class AbstractAuthHandler extends AbstractHttpHandler {
  private final TenantStore tenantStore;

  protected AbstractAuthHandler(TenantStore tenantStore) {
    this.tenantStore = tenantStore;
  }

  /**
   * Gets the user and tenant from the request and authenticates, returning null and writing an error message to the
   * responder if there was an error getting or authenticating the user and tenant.
   *
   * @param request Request containing the user id.
   * @param responder Responder to use when there is an issue getting or authenticating the user.
   * @return Account id if it exists and authentication passes.
   */
  protected Account getAndAuthenticateAccount(HttpRequest request, HttpResponder responder) {
    // TODO: proper authentication/authorization
    String user = request.getHeader(Constants.USER_HEADER);
    String tenantName = request.getHeader(Constants.TENANT_HEADER);
    String apiKey = request.getHeader(Constants.API_KEY_HEADER);
    if (user == null) {
      responder.sendError(HttpResponseStatus.UNAUTHORIZED, Constants.USER_HEADER + " not found in request headers.");
      return null;
    }
    if (tenantName == null) {
      responder.sendError(HttpResponseStatus.UNAUTHORIZED, Constants.TENANT_HEADER + " not found in request headers.");
      return null;
    }
    try {
      Tenant tenant = tenantStore.getTenantByName(tenantName);
      if (tenant == null) {
        responder.sendError(HttpResponseStatus.NOT_FOUND, "Tenant does not exist.");
        return null;
      }
      return new Account(user, tenant.getId());
    } catch (IOException e) {
      responder.sendError(HttpResponseStatus.INTERNAL_SERVER_ERROR, "Error authenticating tenant");
      return null;
    }
  }
}
