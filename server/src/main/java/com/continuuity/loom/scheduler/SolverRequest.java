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
package com.continuuity.loom.scheduler;

import com.google.common.base.Objects;

/**
 * Wrapper around different types of cluster requests that require solver interaction. Used so that we can pass
 * different types of solver tasks on the same queue.
 */
public class SolverRequest {
  private final Type type;
  private final String jsonRequest;

  /**
   * Type of solver request.
   */
  public enum Type {
    CREATE_CLUSTER,
    ADD_SERVICES;
  }

  /**
   * Create a request of the given type, with corresponding request details serialized as a json string.
   *
   * @param type Type of solver request.
   * @param jsonRequest Serialized request details.
   */
  public SolverRequest(Type type, String jsonRequest) {
    this.type = type;
    this.jsonRequest = jsonRequest;
  }

  /**
   * Get type of solver request, which indicates both what the solver should do, and how to deserialize the request
   * details.
   *
   * @return Type of solver request.
   */
  public Type getType() {
    return type;
  }

  /**
   * Get the serialized request details.
   *
   * @return Serialized request details.
   */
  public String getJsonRequest() {
    return jsonRequest;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof SolverRequest)) {
      return false;
    }

    SolverRequest that = (SolverRequest) o;

    return Objects.equal(type, that.type) && Objects.equal(jsonRequest, that.jsonRequest);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(type, jsonRequest);
  }
}
