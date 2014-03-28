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
package com.continuuity.loom.scheduler.task;

import com.continuuity.loom.cluster.Node;
import com.continuuity.loom.conf.Constants;
import com.continuuity.loom.store.ClusterStore;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Service for performing actions on {@link Node}s.
 */
public class NodeService {
  private static final Logger LOG = LoggerFactory.getLogger(NodeService.class);

  private final ClusterStore clusterStore;
  private final int maxActions;
  private final int maxLogLength;

  @Inject
  public NodeService(ClusterStore clusterStore,
                     @Named(Constants.MAX_PER_NODE_NUM_ACTIONS) int maxActions,
                     @Named(Constants.MAX_PER_NODE_LOG_LENGTH) int maxLogLength) {
    this.clusterStore = clusterStore;
    this.maxActions = maxActions;
    this.maxLogLength = maxLogLength;
  }

  /**
   * Start an action on a node. Updates the node in the persistent store.
   *
   * @param node Node the action will take place on.
   * @param taskId Id of the task associated with the action.
   * @param service Service the action is for or empty if its not a service action.
   * @param action Action to execute on the node.
   * @throws Exception
   */
  public void startAction(Node node, String taskId, String service, String action) throws Exception {
    if (node.getActions().size() >= maxActions) {
      Node.Action removed = node.removeFirstAction();
      LOG.debug("Removing action {} from node {} since num actions is more than {}",
                removed, node.getId(), maxActions);
    }
    node.addAction(new Node.Action(taskId, service, action));
    clusterStore.writeNode(node);
  }

  /**
   * Complete an action on a node. Updates the node in the persistent store.
   *
   * @param node Node the completed action took place on.
   * @throws Exception
   */
  public void completeAction(Node node) throws Exception {
    Node.Action action = validateAndGetAction(node);
    action.setStatus(Node.Status.COMPLETE);
    action.setStatusTime(System.currentTimeMillis());
    clusterStore.writeNode(node);
  }

  /**
   * Fail an action on a node with optional logs on what went wrong. Updates the node in the persistent store.
   *
   * @param node Node the action failed on.
   * @param stdout Stdout of failed action.
   * @param stderr Stderr of failed action.
   * @throws Exception
   */
  public void failAction(Node node, String stdout, String stderr) throws Exception {
    Node.Action action = validateAndGetAction(node);
    action.setStatus(Node.Status.FAILED);
    action.setStatusTime(System.currentTimeMillis());
    action.setStdout(truncateLog(stdout, maxLogLength));
    action.setStderr(truncateLog(stderr, maxLogLength));
    clusterStore.writeNode(node);
  }

  private Node.Action validateAndGetAction(Node node) {
    List<Node.Action> actions = node.getActions();
    if (actions.isEmpty()) {
      String errMsg = "Trying to close action when there are no actions for node " + node.getId();
      LOG.error(errMsg);
      throw new IllegalStateException(errMsg);
    }

    Node.Action action = actions.get(actions.size() - 1);
    if (action.getStatus() != Node.Status.IN_PROGRESS) {
      String errMsg = "Trying to close action when action is already closed " + node.getId();
      LOG.error(errMsg);
      throw new IllegalStateException(errMsg);
    }

    return action;
  }

  static String truncateLog(String log, int maxLength) {
    if (log == null || log.length() <= maxLength) {
      return log;
    }

    return "[snipped]" + log.substring(log.length() - maxLength);
  }


  /**
   * Creates a unique hostname from the cluster name, cluster id, and node number. Hostnames are of the format
   * <clustername><clusterid>-<nodenum>.<dnsSuffix>, with underscores and dots replaced by dashes, and with whole
   * hostname trimmed to 255 characters if it would otherwise have been too long.
   *
   * @param clusterName Name of the cluster the node is a part of.
   * @param clusterId Id of the cluster the node is a part of.
   * @param nodeNum Number of the node in the cluster. Must be unique across the cluster.
   * @param dnsSuffix DNS suffix to use.
   * @return Unique hostname (at least across Loom managed nodes).
   */
  public static String createHostname(String clusterName, String clusterId, int nodeNum, String dnsSuffix) {
    String actualSuffix = (dnsSuffix == null || dnsSuffix.isEmpty()) ? "local" : dnsSuffix;
    if (actualSuffix.startsWith(".")) {
      actualSuffix = actualSuffix.substring(1);
    }

    long clusterIdNum = Long.parseLong(clusterId);
    String cleaned = clusterName.replace("_", "-");
    cleaned = cleaned.replace(".", "-");
    StringBuilder postfixBuilder = new StringBuilder();
    postfixBuilder.append(clusterIdNum);
    postfixBuilder.append("-");
    postfixBuilder.append(nodeNum);
    postfixBuilder.append(".");
    postfixBuilder.append(actualSuffix);
    String postfix = postfixBuilder.toString();
    if (cleaned.length() + postfix.length() > 255) {
      cleaned = cleaned.substring(0, 255 - postfix.length());
    }
    return cleaned + postfix;
  }
}
