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
import co.cask.coopr.client.ProvisionerClient;
import co.cask.coopr.shell.CLIConfig;
import co.cask.coopr.shell.util.CliUtil;
import com.google.inject.Inject;

import java.io.PrintStream;

import static co.cask.coopr.shell.util.Constants.PROVISIONER_ID;

/**
 * Gets the provisioner.
 */
public class GetProvisionerCommand extends AbstractAuthCommand {

  private final ProvisionerClient provisionerClient;

  @Inject
  private GetProvisionerCommand(ProvisionerClient provisionerClient, CLIConfig cliConfig) {
    super(cliConfig);
    this.provisionerClient = provisionerClient;
  }

  @Override
  public void perform(Arguments arguments, PrintStream printStream) throws Exception {
    String name = arguments.get(PROVISIONER_ID);
    printStream.print(CliUtil.getPrettyJson(provisionerClient.getProvisioner(name)));
  }

  @Override
  public String getPattern() {
    return String.format("get provisioner <%s>", PROVISIONER_ID);
  }

  @Override
  public String getDescription() {
    return "Get the provisioner by id";
  }
}
