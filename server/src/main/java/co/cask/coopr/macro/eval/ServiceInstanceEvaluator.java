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
import com.google.common.collect.ImmutableList;

import java.util.List;
import java.util.Set;

/**
 * Evaluates a macro that expands to be the instance number of the given node that contains the given service. For
 * example, if there are 3 zookeeper nodes in the cluster, %instance.self.service.zookeeper% will evaluate to 1
 * for the first node with zookeeper, 2 for the second node with zookeeper, and 3 for the last node with zookeeper.
 */
public class ServiceInstanceEvaluator extends ServiceEvaluator {

  public ServiceInstanceEvaluator(String serviceName) {
    super(serviceName);
  }

  @Override
  public List<String> evaluate(Cluster cluster, Set<Node> clusterNodes, Node node) throws IncompleteClusterException {
    int instanceNum = getServiceInstanceNum(node, sortServiceNodes(clusterNodes));
    if (instanceNum < 0) {
      return null;
    }
    return ImmutableList.of(String.valueOf(instanceNum));
  }
}
