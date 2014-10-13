package co.cask.coopr.shell.command;

import co.cask.common.cli.Arguments;
import co.cask.common.cli.Command;
import co.cask.coopr.client.AdminClient;
import co.cask.coopr.shell.util.CliUtil;

import java.io.PrintStream;
import javax.inject.Inject;

/**
 * Lists all providers.
 */
public class ListProvidersCommand implements Command {

  private final AdminClient adminClient;

  @Inject
  public ListProvidersCommand(AdminClient adminClient) {
    this.adminClient = adminClient;
  }

  @Override
  public void execute(Arguments arguments, PrintStream printStream) throws Exception {
    printStream.print(CliUtil.getPrettyJson(adminClient.getAllProviders()));
  }

  @Override
  public String getPattern() {
    return "list providers";
  }

  @Override
  public String getDescription() {
    return "Lists all providers";
  }
}
