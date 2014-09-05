/**
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
package com.continuuity.test.input;

import com.google.common.base.Objects;

/**
 *
 */
public class TestCluster {
  private final String name;
  private final String clusterId;
  private final long timestamp;
  private final String template;
  private final int nodeNumber;

  public TestCluster(String name, String clusterId, long timestamp, String template, int nodeNumber) {
    this.name = name;
    this.clusterId = clusterId;
    this.timestamp = timestamp;
    this.template = template;
    this.nodeNumber = nodeNumber;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof TestCluster)) return false;

    TestCluster other = (TestCluster) o;

    return Objects.equal(name, other.name) &&
      Objects.equal(clusterId, other.clusterId) &&
      Objects.equal(timestamp, other.timestamp) &&
      Objects.equal(template, other.template) &&
      Objects.equal(nodeNumber, other.nodeNumber);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(name, clusterId, timestamp, template, nodeNumber);
  }

  @Override
  public String toString() {
    return Objects.toStringHelper(this)
      .add("name", name)
      .add("clusterId", clusterId)
      .add("timestamp", timestamp)
      .add("template", template)
      .add("nodeNumber", nodeNumber)
      .toString();
  }
}
