package co.cask.coopr.shell.command;

import co.cask.common.cli.Arguments;
import co.cask.common.cli.Command;
import co.cask.coopr.client.ClusterClient;
import co.cask.coopr.shell.util.CliUtil;

import java.io.PrintStream;
import javax.inject.Inject;

/**
 * Gets the cluster status.
 */
public class GetClusterStatusCommand implements Command {

  private static final String CLUSTER_ID_KEY = "cluster-id";

  private final ClusterClient clusterClient;

  @Inject
  public GetClusterStatusCommand(ClusterClient clusterClient) {
    this.clusterClient = clusterClient;
  }

  @Override
  public void execute(Arguments arguments, PrintStream printStream) throws Exception {
    String id = arguments.get(CLUSTER_ID_KEY);
    CliUtil.checkArgument(id);
    printStream.print(CliUtil.getPrettyJson(clusterClient.getClusterStatus(id.substring(1, id.length() - 1))));
  }

  @Override
  public String getPattern() {
    return "get cluster status <cluster-id>";
  }

  @Override
  public String getDescription() {
    return "Gets the cluster status";
  }
}
