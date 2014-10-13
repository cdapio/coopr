package co.cask.coopr.shell.command;

import co.cask.common.cli.Arguments;
import co.cask.common.cli.Command;
import co.cask.coopr.client.AdminClient;
import co.cask.coopr.shell.util.CliUtil;

import java.io.PrintStream;
import javax.inject.Inject;

/**
 * Deletes a hardware type.
 */
public class DeleteHardwareTypeCommand implements Command {

  private static final String NAME_KEY = "name";

  private final AdminClient adminClient;

  @Inject
  public DeleteHardwareTypeCommand(AdminClient adminClient) {
    this.adminClient = adminClient;
  }

  @Override
  public void execute(Arguments arguments, PrintStream printStream) throws Exception {
    String name = CliUtil.checkArgument(arguments.get(NAME_KEY));
    adminClient.deleteHardwareType(name);
  }

  @Override
  public String getPattern() {
    return "delete hardware type <name>";
  }

  @Override
  public String getDescription() {
    return "Deletes a hardware type";
  }
}
