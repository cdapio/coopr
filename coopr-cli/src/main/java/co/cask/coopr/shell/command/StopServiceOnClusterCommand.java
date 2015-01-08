/*
 * Copyright © 2012-2015 Cask Data, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package co.cask.coopr.shell.command;

import co.cask.common.cli.Arguments;
import co.cask.coopr.client.ClusterClient;
import co.cask.coopr.http.request.ClusterOperationRequest;
import co.cask.coopr.shell.CLIConfig;
import co.cask.coopr.shell.util.CliUtil;
import com.google.inject.Inject;

import java.io.PrintStream;

import static co.cask.coopr.shell.util.Constants.CLUSTER_ID_KEY;
import static co.cask.coopr.shell.util.Constants.PROVIDER_FIELDS_KEY;
import static co.cask.coopr.shell.util.Constants.SERVICE_ID_KEY;

/**
 * Stops service on cluster.
 */
public class StopServiceOnClusterCommand extends AbstractAuthCommand {

  private final ClusterClient clusterClient;

  @Inject
  private StopServiceOnClusterCommand(ClusterClient clusterClient, CLIConfig cliConfig) {
    super(cliConfig);
    this.clusterClient = clusterClient;
  }

  @Override
  public void perform(Arguments arguments, PrintStream printStream) throws Exception {
    String clusterId = arguments.get(CLUSTER_ID_KEY);
    String serviceId = arguments.get(SERVICE_ID_KEY);
    ClusterOperationRequest clusterOperationRequest = CliUtil.getObjectFromJson(arguments, PROVIDER_FIELDS_KEY,
                                                                                ClusterOperationRequest.class);
    if (clusterOperationRequest == null) {
      clusterClient.stopServiceOnCluster(clusterId, serviceId);
    } else {
      clusterClient.stopServiceOnCluster(clusterId, serviceId, clusterOperationRequest);
    }
  }

  @Override
  public String getPattern() {
    return String.format("stop service <%s> on cluster <%s> [with provider fields <%s>]",
                         SERVICE_ID_KEY, CLUSTER_ID_KEY, PROVIDER_FIELDS_KEY);
  }

  @Override
  public String getDescription() {
    return "Stop a service on a cluster";
  }
}
