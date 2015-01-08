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
import co.cask.coopr.shell.CLIConfig;
import co.cask.coopr.shell.util.CliUtil;
import com.google.inject.Inject;

import java.io.IOException;
import java.io.PrintStream;

import static co.cask.coopr.shell.util.Constants.HOST_KEY;
import static co.cask.coopr.shell.util.Constants.PORT_KEY;
import static co.cask.coopr.shell.util.Constants.SSL_KEY;
import static co.cask.coopr.shell.util.Constants.TENANT_ID_KEY;
import static co.cask.coopr.shell.util.Constants.USER_ID_KEY;

/**
 * Connects to a Coopr instance.
 */
public class ConnectCommand implements Command {

  private final CLIConfig cliConfig;

  @Inject
  private ConnectCommand(CLIConfig cliConfig) {
    this.cliConfig = cliConfig;
  }

  @Override
  public void execute(Arguments arguments, PrintStream printStream) throws Exception {
    String host = arguments.get(HOST_KEY);
    boolean ssl = ssl(arguments);
    int port;
    if (arguments.hasArgument(PORT_KEY)) {
      port = arguments.getInt(PORT_KEY);
    } else if (ssl) {
      port = cliConfig.getSslPort();
    } else {
      port = cliConfig.getPort();
    }
    if (!CliUtil.isAvailable(host, port)) {
      throw new IOException(String.format("Host %s on port %d could not be reached", host, port));
    }
    String userId = arguments.get(USER_ID_KEY);
    String tenantId = arguments.get(TENANT_ID_KEY);

    cliConfig.setConnection(host, port, ssl, userId, tenantId);
  }

  private boolean ssl(Arguments arguments) {
    return arguments.hasArgument(SSL_KEY) && arguments.get(SSL_KEY).equalsIgnoreCase("enabled");
  }

  @Override
  public String getPattern() {
    return String.format("connect to <%s> as <%s> <%s> [using port <%s>] [with ssl <%s>]",
                         HOST_KEY, USER_ID_KEY, TENANT_ID_KEY, PORT_KEY, SSL_KEY);
  }

  @Override
  public String getDescription() {
    return "Connect to a Coopr instance";
  }
}
