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

package co.cask.coopr.shell;

import co.cask.common.cli.CLI;
import co.cask.common.cli.Command;
import co.cask.common.cli.CommandSet;
import co.cask.coopr.client.AdminClient;
import co.cask.coopr.client.ClusterClient;
import co.cask.coopr.client.PluginClient;
import co.cask.coopr.client.ProvisionerClient;
import co.cask.coopr.client.TenantClient;
import co.cask.coopr.shell.command.HelpCommand;
import co.cask.coopr.shell.command.set.CooprCommandSets;
import com.google.common.base.Joiner;
import com.google.common.base.Supplier;
import com.google.common.collect.ImmutableList;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import jline.console.completer.Completer;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Collections;

import static co.cask.coopr.shell.util.Constants.EV_HOST;
import static co.cask.coopr.shell.util.Constants.EV_PORT;
import static co.cask.coopr.shell.util.Constants.EV_TENANT_ID;
import static co.cask.coopr.shell.util.Constants.EV_USER_ID;

/**
 * Main class for the Coopr CLI.
 */
public class CLIMain {

  private CLI<Command> cli;

  public CLIMain(final CLIConfig cliConfig) throws URISyntaxException, IOException {
    final Injector injector = Guice.createInjector(
      new AbstractModule() {
        @Override
        protected void configure() {
          bind(CLIConfig.class).toInstance(cliConfig);
          bind(AdminClient.class).toInstance(cliConfig.getClientManager().getAdminClient());
          bind(ClusterClient.class).toInstance(cliConfig.getClientManager().getClusterClient());
          bind(TenantClient.class).toInstance(cliConfig.getClientManager().getTenantClient());
          bind(ProvisionerClient.class).toInstance(cliConfig.getClientManager().getProvisionerClient());
          bind(PluginClient.class).toInstance(cliConfig.getClientManager().getPluginClient());
        }
      }
    );

    cli = new CLI<Command>(getCommandSet(cliConfig, injector), Collections.<String, Completer>emptyMap());
    cli.getReader().setPrompt("coopr (" + cliConfig.getURI() + ")> ");

    cliConfig.addReconnectListener(new CLIConfig.ReconnectListener() {
      @Override
      public void onReconnect() throws IOException {
        cli.setCommands(getCommandSet(cliConfig, injector));
        cli.getReader().setPrompt("coopr (" + cliConfig.getURI() + ")> ");
      }
    });
  }

  private CommandSet<Command> getCommandSet(CLIConfig cliConfig, Injector injector) {
    final CommandSet<Command> commandSet;
    if (cliConfig.isSuperadmin()) {
      commandSet = CooprCommandSets.getCommandSetForSuperadmin(injector);
    } else if (cliConfig.isAdmin()) {
      commandSet = CooprCommandSets.getCommandSetForAdmin(injector);
    } else {
      commandSet = CooprCommandSets.getCommandSetForNonAdminUser(injector);
    }
    HelpCommand helpCommand = new HelpCommand(new Supplier<Iterable<Command>>() {
      @Override
      public Iterable<Command> get() {
        return commandSet.getCommands();
      }
    }, cliConfig);
    return new CommandSet<Command>(ImmutableList.of((Command) helpCommand), ImmutableList.of(commandSet));
  }

  public static void main(String[] args) throws Exception {
    String host = System.getenv(EV_HOST);
    String stringPort = System.getenv(EV_PORT);
    String userId = System.getenv(EV_USER_ID);
    String tenantId = System.getenv(EV_TENANT_ID);
    Integer port = null;
    try {
      port = Integer.parseInt(stringPort);
    } catch (NumberFormatException ignored) {
    }

    CLIConfig config = new CLIConfig(host, port, userId, tenantId);
    CLIMain shell = new CLIMain(config);

    if (args.length == 0) {
      shell.cli.startInteractiveMode(System.out);
    } else {
      shell.cli.execute(Joiner.on(" ").join(args), System.out);
    }
  }
}
