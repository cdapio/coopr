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

import java.util.HashMap;
import java.util.Map;

/**
 * This enumeration contains possible User ID values for generating appropriate responses by the handler.
 */
public enum TestStatusUserId {

  BAD_REQUEST_STATUS_USER_ID("BadRequest"),
  NOT_FOUND_STATUS_USER_ID("NotFound"),
  CONFLICT_STATUS_USER_ID("Conflict"),
  FORBIDDEN_STATUS_USER_ID("Forbidden"),
  METHOD_NOT_ALLOWED_STATUS_USER_ID("MethodNotAllowed"),
  NOT_ACCEPTABLE_STATUS_USER_ID("NotAcceptable"),
  INTERNAL_SERVER_ERROR_STATUS_USER_ID("InternalServerError"),
  UNAUTHORIZED_STATUS_USER_ID("Unauthorized"),
  NOT_IMPLEMENTED_STATUS_USER_ID("NotImplemented");

  private String value;
  private static final Map<String, TestStatusUserId> valueMap;

  static {
    valueMap = new HashMap<String, TestStatusUserId>();
    for (TestStatusUserId userId : values()) {
      valueMap.put(userId.getValue(), userId);
    }
  }

  TestStatusUserId(String value) {
    this.value = value;
  }

  public String getValue() {
    return value;
  }

  public static TestStatusUserId getByValue(String value) {
    return valueMap.get(value);
  }
}
