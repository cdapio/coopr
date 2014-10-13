package co.cask.coopr.shell.command;

import co.cask.common.cli.Arguments;
import co.cask.common.cli.Command;
import co.cask.coopr.client.AdminClient;
import co.cask.coopr.shell.util.CliUtil;

import java.io.PrintStream;
import javax.inject.Inject;

/**
 * Deletes a cluster template.
 */
public class DeleteClusterTemplateCommand implements Command {

  private static final String NAME_KEY = "name";

  private final AdminClient adminClient;

  @Inject
  public DeleteClusterTemplateCommand(AdminClient adminClient) {
    this.adminClient = adminClient;
  }

  @Override
  public void execute(Arguments arguments, PrintStream printStream) throws Exception {
    String name = arguments.get(NAME_KEY);
    CliUtil.checkArgument(name);
    adminClient.deleteClusterTemplate(name.substring(1, name.length() - 1));
  }

  @Override
  public String getPattern() {
    return "delete cluster template <name>";
  }

  @Override
  public String getDescription() {
    return "Deletes a cluster template";
  }
}
