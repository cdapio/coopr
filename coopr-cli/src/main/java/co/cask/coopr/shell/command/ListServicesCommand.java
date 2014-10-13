package co.cask.coopr.shell.command;

import co.cask.common.cli.Arguments;
import co.cask.common.cli.Command;
import co.cask.coopr.client.AdminClient;
import co.cask.coopr.shell.util.CliUtil;

import java.io.PrintStream;
import javax.inject.Inject;

/**
 * Lists all services.
 */
public class ListServicesCommand implements Command {

  private final AdminClient adminClient;

  @Inject
  public ListServicesCommand(AdminClient adminClient) {
    this.adminClient = adminClient;
  }

  @Override
  public void execute(Arguments arguments, PrintStream printStream) throws Exception {
    printStream.print(CliUtil.getPrettyJson(adminClient.getAllServices()));
  }

  @Override
  public String getPattern() {
    return "list services";
  }

  @Override
  public String getDescription() {
    return "Lists all services";
  }
}
