/**
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
package com.continuuity.test.input;

import com.continuuity.loom.admin.ProvisionerAction;
import com.google.common.base.Objects;

import java.util.Set;

/**
 *
 */
public class TestNode {
  private final String hostname;
  private final String id;
  private final Set<Action> actions;
  private final String ip;
  private final String username;
  private final String password;

  public TestNode(String hostname, String id, Set<Action> actions, String ip, String username, String password) {
    this.hostname = hostname;
    this.id = id;
    this.actions = actions;
    this.ip = ip;
    this.username = username;
    this.password = password;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof TestNode)) return false;

    TestNode other = (TestNode) o;
    return Objects.equal(hostname, other.hostname) &&
      Objects.equal(id, other.id) &&
      Objects.equal(actions, other.actions) &&
      Objects.equal(ip, other.ip) &&
      Objects.equal(username, other.username) &&
      Objects.equal(password, other.password);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(hostname, id, actions, ip, username, password);
  }

  @Override
  public String toString() {
    return Objects.toStringHelper(this)
      .add("hostname", hostname)
      .add("id", id)
      .add("actions", actions)
      .add("ip", ip)
      .add("username", username)
      .add("password", password)
      .toString();
  }

  public static class Action {
    private ProvisionerAction action;
    private String service;
    private long submitTime;
    private long duration;
    private String status;

    public Action(ProvisionerAction action, String service, long submitTime, long duration, String status) {
      this.action = action;
      this.service = service;
      this.submitTime = submitTime;
      this.duration = duration;
      this.status = status;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (!(o instanceof Action)) return false;

      Action other = (Action) o;

      return Objects.equal(action, other.action) &&
        Objects.equal(service, other.service) &&
        Objects.equal(submitTime, other.submitTime) &&
        Objects.equal(duration, other.duration) &&
        Objects.equal(status, other.status);
    }

    @Override
    public int hashCode() {
      return Objects.hashCode(action, service, submitTime, duration, status);
    }

    @Override
    public String toString() {
      return Objects.toStringHelper(this)
        .add("action", action)
        .add("service", service)
        .add("submitTime", submitTime)
        .add("duration", duration)
        .add("status", status)
        .toString();
    }
  }

}
