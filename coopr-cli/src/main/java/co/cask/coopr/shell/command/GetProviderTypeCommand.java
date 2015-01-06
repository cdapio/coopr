/*
 * Copyright © 2012-2014 Cask Data, Inc.
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

import static co.cask.coopr.shell.util.Constants.PROVIDER_TYPE_ID;

/**
 * Get provider type.
 */
public class GetProviderTypeCommand extends AbstractAuthCommand {

  private final PluginClient pluginClient;

  @Inject
  public GetProviderTypeCommand(PluginClient pluginClient, CLIConfig cliConfig) {
    super(cliConfig);
    this.pluginClient = pluginClient;
  }

  @Override
  public void perform(Arguments arguments, PrintStream printStream) throws Exception {
    String id = arguments.get(PROVIDER_TYPE_ID);
    printStream.print(CliUtil.getPrettyJson(pluginClient.getProviderType(id)));
  }

  @Override
  public String getPattern() {
    return String.format("get providertype <%s>", PROVIDER_TYPE_ID);
  }

  @Override
  public String getDescription() {
    return "Get provider type";
  }
}
