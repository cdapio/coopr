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
import co.cask.common.cli.Command;
import co.cask.coopr.client.PluginClient;
import com.google.inject.Inject;

import java.io.PrintStream;

import static co.cask.coopr.shell.util.Constants.AUTOMATOR_TYPE_ID;
import static co.cask.coopr.shell.util.Constants.RESOURCE_NAME;
import static co.cask.coopr.shell.util.Constants.RESOURCE_TYPE;
import static co.cask.coopr.shell.util.Constants.RESOURCE_VERSION;

/**
 * Delete automator type resource version.
 */
public class DeleteAutomatorTypeResourcesCommand implements Command {

  private final PluginClient pluginClient;

  @Inject
  public DeleteAutomatorTypeResourcesCommand(PluginClient pluginClient) {
    this.pluginClient = pluginClient;
  }

  @Override
  public void execute(Arguments arguments, PrintStream printStream) throws Exception {
    String automatorTypeId = arguments.get(AUTOMATOR_TYPE_ID);
    String resourceType = arguments.get(RESOURCE_TYPE);
    String resourceName = arguments.get(RESOURCE_NAME);
    String version = arguments.get(RESOURCE_VERSION);
    pluginClient.deleteAutomatorTypeResourceVersion(automatorTypeId, resourceType, resourceName, version);
  }

  @Override
  public String getPattern() {
    return String.format("delete resource-type <%s> with name <%s> version <%s> from automator <%s>",
                         RESOURCE_TYPE, RESOURCE_NAME, RESOURCE_VERSION, AUTOMATOR_TYPE_ID);
  }

  @Override
  public String getDescription() {
    return "Delete automator type resource version.";
  }
}
