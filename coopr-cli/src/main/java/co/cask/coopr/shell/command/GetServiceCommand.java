package co.cask.coopr.shell.command;

import co.cask.common.cli.Arguments;
import co.cask.common.cli.Command;
import co.cask.coopr.client.AdminClient;
import co.cask.coopr.shell.util.CliUtil;

import java.io.PrintStream;
import javax.inject.Inject;

/**
 * Gets the service.
 */
public class GetServiceCommand implements Command {

  private static final String NAME_KEY = "name";

  private final AdminClient adminClient;

  @Inject
  public GetServiceCommand(AdminClient adminClient) {
    this.adminClient = adminClient;
  }

  @Override
  public void execute(Arguments arguments, PrintStream printStream) throws Exception {
    String name = CliUtil.checkArgument(arguments.get(NAME_KEY));
    printStream.print(CliUtil.getPrettyJson(adminClient.getService(name)));
  }

  @Override
  public String getPattern() {
    return "get service <name>";
  }

  @Override
  public String getDescription() {
    return "Gets the service";
  }
}
