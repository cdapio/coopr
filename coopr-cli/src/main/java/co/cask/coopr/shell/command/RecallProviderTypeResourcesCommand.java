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
import com.google.inject.Inject;

import java.io.PrintStream;

import static co.cask.coopr.shell.util.Constants.PROVIDER_TYPE_ID;
import static co.cask.coopr.shell.util.Constants.RESOURCE_NAME;
import static co.cask.coopr.shell.util.Constants.RESOURCE_TYPE;
import static co.cask.coopr.shell.util.Constants.RESOURCE_VERSION;

/**
 * Stage provider type resource version.
 */
public class RecallProviderTypeResourcesCommand extends AbstractAuthCommand {

  private final PluginClient pluginClient;

  @Inject
  public RecallProviderTypeResourcesCommand(PluginClient pluginClient, CLIConfig cliConfig) {
    super(cliConfig);
    this.pluginClient = pluginClient;
  }

  @Override
  public void perform(Arguments arguments, PrintStream printStream) throws Exception {
    String providerTypeId = arguments.get(PROVIDER_TYPE_ID);
    String resourceType = arguments.get(RESOURCE_TYPE);
    String resourceName = arguments.get(RESOURCE_NAME);
    String version = arguments.get(RESOURCE_VERSION);
    pluginClient.recallProviderTypeResource(providerTypeId, resourceType, resourceName, version);
  }

  @Override
  public String getPattern() {
    return String.format("recall resource from provider <%s> of type <%s> and name <%s> and version <%s>",
                         PROVIDER_TYPE_ID, RESOURCE_TYPE, RESOURCE_NAME, RESOURCE_VERSION);
  }

  @Override
  public String getDescription() {
    return "Recall a specific version of a provider type resource";
  }
}
