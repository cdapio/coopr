package co.cask.coopr.shell.command;

import co.cask.common.cli.Arguments;
import co.cask.common.cli.Command;

import java.io.PrintStream;
import javax.inject.Inject;

/**
 * Exits the cli.
 */
public class ExitCommand implements Command {

  @Inject
  public ExitCommand() {}

  @Override
  public void execute(Arguments arguments, PrintStream printStream) throws Exception {
    System.exit(0);
  }

  @Override
  public String getPattern() {
    return "exit";
  }

  @Override
  public String getDescription() {
    return "Exits the cli";
  }
}
