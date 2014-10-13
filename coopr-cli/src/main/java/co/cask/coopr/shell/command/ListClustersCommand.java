package co.cask.coopr.shell.command;

import co.cask.common.cli.Arguments;
import co.cask.common.cli.Command;
import co.cask.coopr.client.ClusterClient;
import co.cask.coopr.shell.util.CliUtil;

import java.io.PrintStream;
import javax.inject.Inject;

/**
 * Lists all clusters.
 */
public class ListClustersCommand implements Command {

  private final ClusterClient clusterClient;

  @Inject
  public ListClustersCommand(ClusterClient clusterClient) {
    this.clusterClient = clusterClient;
  }

  @Override
  public void execute(Arguments arguments, PrintStream printStream) throws Exception {
    printStream.print(CliUtil.getPrettyJson(clusterClient.getClusters()));
  }

  @Override
  public String getPattern() {
    return "list clusters";
  }

  @Override
  public String getDescription() {
    return "Lists all clusters";
  }
}
