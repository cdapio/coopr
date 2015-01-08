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
import co.cask.coopr.shell.CLIConfig;
import com.google.inject.Inject;

import java.io.PrintStream;

import static co.cask.coopr.shell.util.Constants.CLUSTER_ID_KEY;
import static co.cask.coopr.shell.util.Constants.EXPIRE_TIME_KEY;

/**
 * Sets the cluster expire time.
 */
public class SetClusterExpireTimeCommand extends AbstractAuthCommand {

  private final ClusterClient clusterClient;

  @Inject
  private SetClusterExpireTimeCommand(ClusterClient clusterClient, CLIConfig cliConfig) {
    super(cliConfig);
    this.clusterClient = clusterClient;
  }

  @Override
  public void perform(Arguments arguments, PrintStream printStream) throws Exception {
    String clusterId = arguments.get(CLUSTER_ID_KEY);
    long expireTime = arguments.getLong(EXPIRE_TIME_KEY);
    clusterClient.setClusterExpireTime(clusterId, expireTime);
  }

  @Override
  public String getPattern() {
    return String.format("set expire time <%s> for cluster <%s>", EXPIRE_TIME_KEY, CLUSTER_ID_KEY);
  }

  @Override
  public String getDescription() {
    return "Set the cluster expire time";
  }
}
