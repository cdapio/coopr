package co.cask.coopr.shell.command;

import co.cask.common.cli.Arguments;
import co.cask.common.cli.Command;
import co.cask.coopr.client.AdminClient;
import co.cask.coopr.shell.util.CliUtil;

import java.io.PrintStream;
import javax.inject.Inject;

/**
 * Deletes a provider.
 */
public class DeleteProviderCommand implements Command {

  private static final String NAME_KEY = "name";

  private final AdminClient adminClient;

  @Inject
  public DeleteProviderCommand(AdminClient adminClient) {
    this.adminClient = adminClient;
  }

  @Override
  public void execute(Arguments arguments, PrintStream printStream) throws Exception {
    String name = arguments.get(NAME_KEY);
    CliUtil.checkArgument(name);
    adminClient.deleteProvider(name.substring(1, name.length() - 1));
  }

  @Override
  public String getPattern() {
    return "delete provider <name>";
  }

  @Override
  public String getDescription() {
    return "Deletes a provider";
  }
}
