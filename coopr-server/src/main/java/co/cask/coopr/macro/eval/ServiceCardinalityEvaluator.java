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
package co.cask.coopr.macro.eval;

import co.cask.coopr.cluster.Cluster;
import co.cask.coopr.cluster.Node;
import co.cask.coopr.macro.IncompleteClusterException;
import co.cask.coopr.spec.service.Service;
import com.google.common.base.Objects;
import com.google.common.collect.ImmutableList;

import java.util.List;
import java.util.Set;

/**
 * Evaluates a macro that expands to be the number of nodes in the cluster that contain a given service.
 */
public class ServiceCardinalityEvaluator implements Evaluator {
  private final String serviceName;

  public ServiceCardinalityEvaluator(String serviceName) {
    this.serviceName = serviceName;
  }

  @Override
  public List<String> evaluate(Cluster cluster, Set<Node> clusterNodes, Node node) throws IncompleteClusterException {
    int count = 0;
    for (Node clusterNode : clusterNodes) {
      for (Service service : clusterNode.getServices()) {
        if (serviceName.equals(service.getName())) {
          count++;
        }
      }
    }
    return ImmutableList.of(String.valueOf(count));
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    ServiceCardinalityEvaluator that = (ServiceCardinalityEvaluator) o;

    return Objects.equal(serviceName, that.serviceName);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(serviceName);
  }
}
