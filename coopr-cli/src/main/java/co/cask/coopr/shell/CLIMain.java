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
import co.cask.coopr.client.AdminClient;
import co.cask.coopr.client.ClusterClient;
import co.cask.coopr.shell.command.HelpCommand;
import co.cask.coopr.shell.command.set.CommandSet;
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

  private final CLI<Command> cli;

  public CLIMain(final CLIConfig cliConfig) throws URISyntaxException, IOException {
    Injector injector = Guice.createInjector(
      new AbstractModule() {
        @Override
        protected void configure() {
          bind(CLIConfig.class).toInstance(cliConfig);
          bind(AdminClient.class).toInstance(cliConfig.getClientManager().getAdminClient());
          bind(ClusterClient.class).toInstance(cliConfig.getClientManager().getClusterClient());
        }
      }
    );

    final co.cask.common.cli.CommandSet<Command> commandSetWithoutHelp = CommandSet.getCliCommandSet(injector);
    HelpCommand helpCommand = new HelpCommand(new Supplier<Iterable<Command>>() {
      @Override
      public Iterable<Command> get() {
        return commandSetWithoutHelp.getCommands();
      }
    }, cliConfig);
    co.cask.common.cli.CommandSet<Command> commandSet = new co.cask.common.cli.CommandSet<Command>(
      ImmutableList.of((Command) helpCommand), ImmutableList.of(commandSetWithoutHelp));
    this.cli = new CLI<Command>(commandSet, Collections.<String, Completer>emptyMap());
    cliConfig.addHostnameChangeListener(new CLIConfig.HostnameChangeListener() {
      @Override
      public void onHostnameChanged(String newHostname) {
        cli.getReader().setPrompt("coopr (" + cliConfig.getURI() + ")> ");
      }
    });
  }

  private static String toString(String[] array) {
    StringBuilder builder = new StringBuilder();
    for (String element : array) {
      builder.append(element).append(" ");
    }
    return builder.toString();
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
    shell.cli.getReader().setPrompt("coopr (" + config.getURI() + ")> ");

    if (args.length == 0) {
      shell.cli.startInteractiveMode(System.out);
    } else {
      shell.cli.execute(toString(args), System.out);
    }
  }
}
