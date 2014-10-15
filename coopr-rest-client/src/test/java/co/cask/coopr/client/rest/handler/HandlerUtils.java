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

import org.apache.http.HttpStatus;

public class HandlerUtils {

  private HandlerUtils() {
  }

  public static int getStatusCodeByTestStatusUserId(String value) {
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
}
