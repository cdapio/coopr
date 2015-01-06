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
import co.cask.common.cli.Command;
import co.cask.coopr.client.ClusterClient;
import co.cask.coopr.http.request.ClusterCreateRequest;
import co.cask.coopr.shell.util.CliUtil;
import com.google.inject.Inject;

import java.io.PrintStream;

import static co.cask.coopr.shell.util.Constants.NAME_KEY;
import static co.cask.coopr.shell.util.Constants.SETTINGS_KEY;
import static co.cask.coopr.shell.util.Constants.SIZE_KEY;
import static co.cask.coopr.shell.util.Constants.TEMPLATE_KEY;

/**
 * Creates a cluster.
 */
public class CreateClusterCommand implements Command {

  private final ClusterClient clusterClient;

  @Inject
  private CreateClusterCommand(ClusterClient clusterClient) {
    this.clusterClient = clusterClient;
  }

  @Override
  public void execute(Arguments arguments, PrintStream printStream) throws Exception {
    ClusterCreateRequest.Builder builder = CliUtil.getObjectFromJson(arguments, SETTINGS_KEY,
                                                                     ClusterCreateRequest.Builder.class);
    if (builder == null) {
      builder = ClusterCreateRequest.builder();
    }
    String name = arguments.get(NAME_KEY);
    String template = arguments.get(TEMPLATE_KEY);
    int size = arguments.getInt(SIZE_KEY);
    builder.setName(name);
    builder.setClusterTemplateName(template);
    builder.setNumMachines(size);
    clusterClient.createCluster(builder.build());
  }

  @Override
  public String getPattern() {
    return String.format("create cluster <%s> with template <%s> of size <%s> [using settings <%s>]",
                         NAME_KEY, TEMPLATE_KEY, SIZE_KEY, SETTINGS_KEY);
  }

  @Override
  public String getDescription() {
    return "Create a cluster";
  }
}
