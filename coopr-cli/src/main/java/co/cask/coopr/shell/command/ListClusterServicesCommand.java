package co.cask.coopr.shell.command;

import co.cask.common.cli.Arguments;
import co.cask.common.cli.Command;
import co.cask.coopr.client.ClusterClient;
import co.cask.coopr.shell.util.CliUtil;

import java.io.PrintStream;
import javax.inject.Inject;

/**
 * Lists all clusters services.
 */
public class ListClusterServicesCommand implements Command {

  private static final String CLUSTER_ID_KEY = "cluster-id";

  private final ClusterClient clusterClient;

  @Inject
  public ListClusterServicesCommand(ClusterClient clusterClient) {
    this.clusterClient = clusterClient;
  }

  @Override
  public void execute(Arguments arguments, PrintStream printStream) throws Exception {
    String id = CliUtil.checkArgument(arguments.get(CLUSTER_ID_KEY));
    printStream.print(CliUtil.getPrettyJson(clusterClient.getClusterServices(id)));
  }

  @Override
  public String getPattern() {
    return "list cluster services <cluster-id>";
  }

  @Override
  public String getDescription() {
    return "Lists all cluster services";
  }
}
