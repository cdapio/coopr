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
package com.continuuity.loom.http;

import com.continuuity.http.AbstractHttpHandler;
import com.continuuity.http.HttpResponder;
import com.continuuity.loom.conf.Constants;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.jboss.netty.handler.codec.http.HttpResponseStatus;

/**
 * Abstract handler that provides some base methods for authenticating and authorizing requests.
 */
public abstract class LoomAuthHandler extends AbstractHttpHandler {

  /**
   * Gets the user from the request and authenticates the user, returning null and writing an error message to the
   * responder if there was an error getting or authenticating the user.
   *
   * @param request Request containing the user id.
   * @param responder Responder to use when there is an issue getting or authenticating the user.
   * @return User id if it exists and authentication passes.
   */
  protected String getAndAuthenticateUser(HttpRequest request, HttpResponder responder) {
    // TODO: proper authentication/authorization
    String user = request.getHeader(Constants.USER_HEADER);
    String apiKey = request.getHeader(Constants.API_KEY_HEADER);
    if (user == null) {
      responder.sendError(HttpResponseStatus.UNAUTHORIZED, Constants.USER_HEADER + " not found in the request header.");
      return null;
    }
    return user;
  }

  /**
   * Returns whether or not the request is a request from the super administrator.
   *
   * @param request Request containing the user id.
   * @return true if the request came from the admin and false if not.
   */
  protected boolean isAdminRequest(HttpRequest request) {
    // TODO: proper authentication/authorization
    String user = request.getHeader(Constants.USER_HEADER);
    String apiKey = request.getHeader(Constants.API_KEY_HEADER);

    return user != null && user.equals(Constants.ADMIN_USER);
  }
}
