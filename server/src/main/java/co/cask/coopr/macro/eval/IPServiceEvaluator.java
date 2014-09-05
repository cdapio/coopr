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
import com.google.common.collect.Lists;

import java.util.List;
import java.util.Set;

/**
 * Evaluates a macro that expands to be a list of the ip addresses of nodes in the cluster that contain a given
 * service.
 */
public class IPServiceEvaluator extends ServiceEvaluator {
  private final String ipType;
  private final Integer instanceNum;

  public IPServiceEvaluator(String serviceName, String ipType, Integer instanceNum) {
    super(serviceName);
    this.ipType = ipType;
    this.instanceNum = instanceNum;
  }

  @Override
  public List<String> evaluate(Cluster cluster, Set<Node> clusterNodes, Node node) throws IncompleteClusterException {
    List<String> output = Lists.newArrayList();
    if (instanceNum != null) {
      Node instanceNode = getNthServiceNode(clusterNodes, instanceNum);
      output.add(instanceNode.getProperties().getIPAddress(ipType));
    } else {
      // go through all nodes, looking for nodes with the service on it.
      for (Node clusterNode : clusterNodes) {
        for (Service service : clusterNode.getServices()) {
          // if the node has the service on it, add the relevant node property.
          if (serviceName.equals(service.getName())) {
            String ip = clusterNode.getProperties().getIPAddress(ipType);
            if (ip == null) {
              throw new IncompleteClusterException("node " + clusterNode.getId() + " has no ip for macro expansion.");
            }
            output.add(ip);
          }
        }
      }
    }
    return output.isEmpty() ? null : output;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    IPServiceEvaluator that = (IPServiceEvaluator) o;

    return Objects.equal(ipType, that.ipType) &&
      Objects.equal(instanceNum, that.instanceNum);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(ipType, instanceNum);
  }
}
