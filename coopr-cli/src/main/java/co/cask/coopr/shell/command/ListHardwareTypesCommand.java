package co.cask.coopr.shell.command;

import co.cask.common.cli.Arguments;
import co.cask.common.cli.Command;
import co.cask.coopr.client.AdminClient;
import co.cask.coopr.shell.util.CliUtil;

import java.io.PrintStream;
import javax.inject.Inject;

/**
 * Lists all hardware types.
 */
public class ListHardwareTypesCommand implements Command {

  private final AdminClient adminClient;

  @Inject
  public ListHardwareTypesCommand(AdminClient adminClient) {
    this.adminClient = adminClient;
  }

  @Override
  public void execute(Arguments arguments, PrintStream printStream) throws Exception {
    printStream.print(CliUtil.getPrettyJson(adminClient.getAllHardwareTypes()));
  }

  @Override
  public String getPattern() {
    return "list hardware types";
  }

  @Override
  public String getDescription() {
    return "Lists all cluster hardware types";
  }
}
