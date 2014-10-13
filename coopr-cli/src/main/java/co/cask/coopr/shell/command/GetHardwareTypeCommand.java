package co.cask.coopr.shell.command;

import co.cask.common.cli.Arguments;
import co.cask.common.cli.Command;
import co.cask.coopr.client.AdminClient;
import co.cask.coopr.shell.util.CliUtil;

import java.io.PrintStream;
import javax.inject.Inject;

/**
 * Gets the hardware type.
 */
public class GetHardwareTypeCommand implements Command {

  private static final String NAME_KEY = "name";

  private final AdminClient adminClient;

  @Inject
  public GetHardwareTypeCommand(AdminClient adminClient) {
    this.adminClient = adminClient;
  }

  @Override
  public void execute(Arguments arguments, PrintStream printStream) throws Exception {
    String name = CliUtil.checkArgument(arguments.get(NAME_KEY));
    printStream.print(CliUtil.getPrettyJson(adminClient.getHardwareType(name)));
  }

  @Override
  public String getPattern() {
    return "get hardware type <name>";
  }

  @Override
  public String getDescription() {
    return "Gets the hardware type";
  }
}
