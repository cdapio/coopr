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
package co.cask.coopr.scheduler.task;

import co.cask.coopr.cluster.Node;
import co.cask.coopr.common.conf.Configuration;
import co.cask.coopr.common.conf.Constants;
import co.cask.coopr.common.utils.StringUtils;
import co.cask.coopr.store.cluster.ClusterStore;
import co.cask.coopr.store.cluster.ClusterStoreService;
import com.google.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
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
  private NodeService(ClusterStoreService clusterStoreService, Configuration conf) {
    this.clusterStore = clusterStoreService.getSystemView();
    this.maxActions = conf.getInt(Constants.MAX_PER_NODE_NUM_ACTIONS);
    this.maxLogLength = conf.getInt(Constants.MAX_PER_NODE_LOG_LENGTH);
  }

  /**
   * Start an action on a node. Updates the node in the persistent store.
   *
   * @param node Node the action will take place on.
   * @param taskId Id of the task associated with the action.
   * @param service Service the action is for or empty if its not a service action.
   * @param action Action to execute on the node.
   * @throws IOException
   */
  public void startAction(Node node, String taskId, String service, String action) throws IOException {
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
   * @throws IOException
   */
  public void completeAction(Node node) throws IOException {
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
   * @throws IOException
   */
  public void failAction(Node node, String stdout, String stderr) throws IOException {
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
   * hostname trimmed to 255 characters if it would otherwise have been too long, and the first label trimmed to
   * 63 characters if it would otherwise have been too long.
   *
   * @param clusterName Name of the cluster the node is a part of.
   * @param clusterId Id of the cluster the node is a part of.
   * @param nodeNum Number of the node in the cluster. Must be unique across the cluster.
   * @param dnsSuffix DNS suffix to use.
   * @return Unique hostname (at least across system managed nodes).
   */
  public static String createHostname(String clusterName, String clusterId, int nodeNum, String dnsSuffix) {
    String actualSuffix = (dnsSuffix == null || dnsSuffix.isEmpty()) ? "local" : dnsSuffix;
    if (actualSuffix.startsWith(".")) {
      actualSuffix = actualSuffix.substring(1);
    }

    long clusterIdNum = Long.parseLong(clusterId);
    // this is something like 53-1000 is it's cluster 53 and node 1000. We want to append this to the cluster name
    // to get the first label of the hostname.
    // ex: name53-1000
    String labelSuffix = clusterIdNum + "-" + nodeNum;
    // it is technically valid to have a hostname that has digits in the front, but it used to not be valid
    // stripping leading digits to play nice with older code which may check for leading digits.
    String labelPrefix = StringUtils.stripLeadingDigits(clusterName);
    // the label must be from 1 to 63 (inclusive) characters in length. If the name is too long, chop off the end
    if (labelPrefix.length() + labelSuffix.length() > 63) {
      labelPrefix = labelPrefix.substring(0, 63 - labelSuffix.length());
    }
    // cannot have . or _ in the first label of the hostname
    labelPrefix = labelPrefix.replace("_", "-");
    labelPrefix = labelPrefix.replace(".", "-");
    // hostnames are case insensitive, so lets just lowercase for consistency since that is what is normal
    labelPrefix = labelPrefix.toLowerCase();
    return labelPrefix + labelSuffix + "." + actualSuffix;
  }
}
