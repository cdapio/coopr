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

import co.cask.coopr.client.rest.RestClientTest;
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
import java.util.List;
import javax.ws.rs.HttpMethod;

/**
 * This abstract class contains common logic for test handlers.
 *
 * @param <T> Type of requested object
 */
public abstract class AbstractAdminHandler<T> implements HttpRequestHandler {

  private static final Gson GSON = new Gson();
  private static final String COOPR_TENANT_ID_HEADER_NAME = "Coopr-TenantID";
  private static final String COOPR_USER_ID_HEADER_NAME = "Coopr-UserID";


  @Override
  public void handle(HttpRequest request, HttpResponse response, HttpContext context) throws HttpException,
    IOException {

    RequestLine requestLine = request.getRequestLine();
    String method = requestLine.getMethod();
    String url = requestLine.getUri();
    int statusCode = HttpStatus.SC_OK;

    String userId = request.getFirstHeader(COOPR_USER_ID_HEADER_NAME).getValue();
    String tenantId = request.getFirstHeader(COOPR_TENANT_ID_HEADER_NAME).getValue();
    if (userId.equals(RestClientTest.TEST_USER_ID) && tenantId.equals(RestClientTest.TEST_TENANT_ID)) {
      if (HttpMethod.GET.equals(method) && getAllURL().equals(url)) {
        response.setEntity(new StringEntity(GSON.toJson(getAll())));
      } else if (HttpMethod.GET.equals(method)) {
        response.setEntity(new StringEntity(GSON.toJson(getSingle())));
      } else if (!HttpMethod.DELETE.equals(method)) {
        statusCode = HttpStatus.SC_BAD_REQUEST;
      }
    } else {
      statusCode = getStatusCodeByTestStatusUserId(userId);
    }
    response.setStatusCode(statusCode);
  }

  private int getStatusCodeByTestStatusUserId(String value) {
    int statusCode;
    TestStatusUserId statusHeaderName = TestStatusUserId.getByValue(value);
    switch (statusHeaderName) {
      case NOT_FOUND_STATUS_USER_ID:
        statusCode = HttpStatus.SC_NOT_FOUND;
        break;
      case BAD_REQUEST_STATUS_USER_ID:
        statusCode = HttpStatus.SC_BAD_REQUEST;
        break;
      case CONFLICT_STATUS_USER_ID:
        statusCode = HttpStatus.SC_CONFLICT;
        break;
      case FORBIDDEN_STATUS_USER_ID:
        statusCode = HttpStatus.SC_FORBIDDEN;
        break;
      case METHOD_NOT_ALLOWED_STATUS_USER_ID:
        statusCode = HttpStatus.SC_METHOD_NOT_ALLOWED;
        break;
      case NOT_ACCEPTABLE_STATUS_USER_ID:
        statusCode = HttpStatus.SC_NOT_ACCEPTABLE;
        break;
      case INTERNAL_SERVER_ERROR_STATUS_USER_ID:
        statusCode = HttpStatus.SC_INTERNAL_SERVER_ERROR;
        break;
      case UNAUTHORIZED_STATUS_USER_ID:
        statusCode = HttpStatus.SC_UNAUTHORIZED;
        break;
      case NOT_IMPLEMENTED_STATUS_USER_ID:
      default:
        statusCode = HttpStatus.SC_NOT_IMPLEMENTED;
    }
    return statusCode;
  }

  public abstract List<T> getAll();
  public abstract T getSingle();
  public abstract String getAllURL();
}
