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
import co.cask.coopr.client.PluginClient;
import co.cask.coopr.shell.CLIConfig;
import co.cask.coopr.shell.util.CliUtil;
import com.google.inject.Inject;

import java.io.PrintStream;

/**
 * Lists all provider types.
 */
public class ListAllProviderTypesCommand extends AbstractAuthCommand {

  private final PluginClient pluginClient;

  @Inject
  public ListAllProviderTypesCommand(PluginClient pluginClient, CLIConfig cliConfig) {
    super(cliConfig);
    this.pluginClient = pluginClient;
  }

  @Override
  public void perform(Arguments arguments, PrintStream printStream) throws Exception {
    printStream.print(CliUtil.getPrettyJson(pluginClient.getAllProviderTypes()));
  }

  @Override
  public String getPattern() {
    return String.format("list provider-types");
  }

  @Override
  public String getDescription() {
    return "List all provider types";
  }

}
