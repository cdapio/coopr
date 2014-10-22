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
import co.cask.coopr.shell.CLIConfig;
import com.google.common.base.Supplier;

import java.io.PrintStream;

import static co.cask.coopr.shell.util.Constants.EV_HOST;

/**
 * Prints helper text for all commands.
 */
public class HelpCommand implements Command {

  private final Supplier<Iterable<Command>> getCommands;
  private final CLIConfig config;

  public HelpCommand(Supplier<Iterable<Command>> getCommands, CLIConfig config) {
    this.getCommands = getCommands;
    this.config = config;
  }

  @Override
  public void execute(Arguments arguments, PrintStream printStream) throws Exception {
    printStream.println(EV_HOST + "=" + config.getHost());
    printStream.println();
    printStream.println(String.format("Available commands: \n%s: %s", getPattern(), getDescription()));
    for (Command command : getCommands.get()) {
      printStream.println(String.format("%s: %s", command.getPattern(), command.getDescription()));
    }
    printStream.println();
  }

  @Override
  public String getPattern() {
    return "help";
  }

  @Override
  public String getDescription() {
    return "Prints this helper text";
  }
}
