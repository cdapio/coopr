package co.cask.coopr.shell.command;

import co.cask.common.cli.Arguments;
import co.cask.common.cli.Command;
import co.cask.coopr.client.AdminClient;
import co.cask.coopr.shell.util.CliUtil;

import java.io.PrintStream;
import javax.inject.Inject;

/**
 * Gets the image type.
 */
public class GetImageTypeCommand implements Command {

  private static final String NAME_KEY = "name";

  private final AdminClient adminClient;

  @Inject
  public GetImageTypeCommand(AdminClient adminClient) {
    this.adminClient = adminClient;
  }

  @Override
  public void execute(Arguments arguments, PrintStream printStream) throws Exception {
    String name = arguments.get(NAME_KEY);
    CliUtil.checkArgument(name);
    printStream.print(CliUtil.getPrettyJson(adminClient.getImageType(name.substring(1, name.length() - 1))));
  }

  @Override
  public String getPattern() {
    return "get image type <name>";
  }

  @Override
  public String getDescription() {
    return "Gets the image type";
  }
}
