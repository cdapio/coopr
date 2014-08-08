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
package com.continuuity.loom.macro;

import com.continuuity.loom.admin.Service;
import com.continuuity.loom.cluster.Cluster;
import com.continuuity.loom.cluster.Node;
import com.continuuity.loom.common.utils.ImmutablePair;
import com.google.common.base.Objects;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

/**
 * An expression represents a single macro. It has a type and a name that are used to lookup the substitute for the
 * macro. Currently, the substitute is either the list of ip addresses or the list of host names of the nodes that the
 * named service runs on. By default, the substitute is returned as a comma-separated list. The list separator can be
 * overridden by passing a different one to the constructor. Also, optionally each element in the substitute can be
 * formatted according to a format string. The actual element is inserted by replacing the $ in the format string. The
 * $ sign can be escaped by preceding it with another $.
 */
public class Expression {

  private static final String DEFAULT_SEPARATOR = ",";
  private static final char PLACEHOLDER = '$';

  /**
   * Distinguishes the type of substitute - currently the host name or the ip address of a node with a service.
   */
  public enum Type {
    HOST_OF_SERVICE("host.service."),
    IP_OF_SERVICE("ip.service."),
    NUM_OF_SERVICE("num.service."),
    SELF_INSTANCE_OF_SERVICE("instance.self.service."),
    SELF_HOST_OF_SERVICE("host.self.service."),
    SELF_IP_OF_SERVICE("ip.self.service."),
    CLUSTER_OWNER("cluster.owner");

    private String representation;

    private Type(String repr) {
      this.representation = repr;
    }

    public String getRepresentation() {
      return representation;
    }

    public boolean isClusterType() {
      return this == CLUSTER_OWNER;
    }
  }

  private static final Set<Type> SINGLE_TYPES = ImmutableSet.of(
    Type.NUM_OF_SERVICE, Type.SELF_INSTANCE_OF_SERVICE, Type.SELF_HOST_OF_SERVICE, Type.SELF_IP_OF_SERVICE);

  private final Type type;
  private final String name;
  private final String format;
  private final String separator;
  private final Integer instanceNum;

  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj == null || obj.getClass() != Expression.class) {
      return false;
    }
    Expression other = (Expression) obj;
    return type.equals(other.type) && name.equals(other.name) &&
      Objects.equal(format, other.format) && Objects.equal(separator, other.separator);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(type, name, format, separator);
  }

  /**
   * Extract the type and service name from a macro name.
   */
  public static ImmutablePair<Type, String> typeAndNameOf(String macroName) throws SyntaxException {
    for (Type type : Type.values()) {
      String prefix = type.getRepresentation();
      if (type.isClusterType() && macroName.equals(type.getRepresentation())) {
        return ImmutablePair.of(type, null);
      }
      if (!macroName.startsWith(prefix)) {
        continue;
      }
      if (macroName.length() < prefix.length() + 1) {
        continue; // the prefix must be followed at least one character
      }
      return ImmutablePair.of(type, macroName.substring(prefix.length()));
    }
    throw new SyntaxException("'" + macroName + "' is not a valid macro name");
  }

  /**
   * Constructor with all argument.
   * @param type the type of substitute, for example host or ip address.
   * @param name the name of the service.
   * @param format the format string.
   * @param separator the list separator.
   * @param instanceNum the instance number of the service.
   */
  public Expression(Type type, @Nullable String name, @Nullable String format,
                    @Nullable String separator, @Nullable Integer instanceNum) {
    this.type = type;
    this.name = name;
    this.format = format;
    this.separator = separator;
    this.instanceNum = instanceNum;
  }

  /**
   * Evaluate the expression for a given cluster. Looks up the service name in the cluster to find all nodes that run
   * the service, then formats and joins all results into a string.
   * @param cluster the cluster to evaluate for.
   * @param clusterNodes the nodes of the cluster to evaluate for.
   * @param node the node of the cluster to evaluate the expression for.
   * @return the replacement string for the expression, or null if the service required for replacement is not in
   *         the cluster.
   * @throws IncompleteClusterException if a node is missing the property that is required for the lookup type.
   */
  public String evaluate(Cluster cluster, Set<Node> clusterNodes, Node node) throws IncompleteClusterException {
    if (name == null) {
      return evaluateClusterProperty(cluster);
    }
    List<Node> serviceNodes = nodesForService(clusterNodes, name);
    if (serviceNodes.isEmpty()) {
      return null;
    }
    if (instanceNum != null || SINGLE_TYPES.contains(type)) {
      return evaluateSingle(serviceNodes, node);
    }
    StringBuilder builder = new StringBuilder();
    boolean first = true;
    for (Node serviceNode : serviceNodes) {
      String property = getNodeProperty(serviceNode);
      if (property == null) {
        throw new IncompleteClusterException(
          "Property '" + type + "' not defined for node '" + node.getId() + "'.");
      }
      if (!first) {
        builder.append(separator == null ? DEFAULT_SEPARATOR : separator);
      } else {
        first = false;
      }
      format(property, builder);
    }
    return builder.toString();
  }

  private String getNodeProperty(Node node) {
    switch (type) {
      case HOST_OF_SERVICE:
      case SELF_HOST_OF_SERVICE:
        return node.getProperties().getHostname();
      case IP_OF_SERVICE:
      case SELF_IP_OF_SERVICE:
        return node.getProperties().getIpaddress();
      default:
        return null;
    }
  }

  private String evaluateClusterProperty(Cluster cluster) {
    switch (type) {
      case CLUSTER_OWNER:
        return cluster.getAccount().getUserId();
      default:
        // shouldn't ever happen
        return type.getRepresentation();
    }
  }

  private String evaluateSingle(List<Node> serviceNodes, Node node) throws IncompleteClusterException {
    Collections.sort(serviceNodes, new NodeNumComparator());
    if (type == Type.NUM_OF_SERVICE) {
      return String.valueOf(serviceNodes.size());
    } else if (type == Type.SELF_INSTANCE_OF_SERVICE) {
      int nodeNum = getNodeNum(node);
      int selfInstance = getServiceInstanceNum(nodeNum, serviceNodes);
      // if this node does not have the service, this macro is not for us and we can leave it unexpanded.
      if (selfInstance < 0) {
        return null;
      }
      return String.valueOf(selfInstance);
    } else {
      Node serviceNode = node;
      if (type == Type.HOST_OF_SERVICE || type == Type.IP_OF_SERVICE) {
        if (instanceNum >= serviceNodes.size()) {
          throw new IncompleteClusterException("Unable to expand macro, there are not " + instanceNum +
                                                 " nodes with the service.");
        }
        serviceNode = serviceNodes.get(instanceNum);
      }
      String property = getNodeProperty(serviceNode);
      if (property == null) {
        throw new IncompleteClusterException(
          "Property '" + type + "' not defined for node '" + node.getId() + "'.");
      }
      return property;
    }
  }

  /**
   * Apply the format string to a substitute string and append it to a string builder.
   * @param raw the original substitute.
   * @param builder the string builder to append.
   */
  void format(String raw, StringBuilder builder) {
    if (format == null) {
      builder.append(raw);
      return;
    }
    for (int i = 0; i < format.length(); i++) {
      if (PLACEHOLDER == format.charAt(i)) {
        if (i + 1 < format.length() && PLACEHOLDER == format.charAt(i + 1)) {
          // escaped
          builder.append(PLACEHOLDER);
          i++; // move past second placeholder
        } else {
          builder.append(raw);
        }
      } else {
        builder.append(format.charAt(i));
      }
    }
  }

  /**
   * Find all nodes of a cluster that have a given service.
   */
  List<Node> nodesForService(Set<Node> nodes, String serviceName) {
    List<Node> nodesFound = Lists.newArrayList();
    for (Node node : nodes) {
      for (Service service : node.getServices()) {
        if (service.getName().equals(serviceName)) {
          nodesFound.add(node);
          break; // done with this node
        }
      }
    }
    return nodesFound;
  }

  int getServiceInstanceNum(int nodenum, List<Node> sortedNodeList) {
    int index = 1;
    for (Node node : sortedNodeList) {
      if (getNodeNum(node) == nodenum) {
        return index;
      }
      index++;
    }
    return -1;
  }

  private class NodeNumComparator implements Comparator<Node> {

    @Override
    public int compare(Node node, Node node2) {
      Integer nodenum1 = getNodeNum(node);
      Integer nodenum2 = getNodeNum(node2);
      return nodenum1.compareTo(nodenum2);
    }
  }

  private int getNodeNum(Node node) {
    return node.getProperties().getNodenum();
  }
}
