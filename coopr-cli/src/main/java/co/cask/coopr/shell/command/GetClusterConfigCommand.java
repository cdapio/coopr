package co.cask.coopr.shell.command;

import co.cask.common.cli.Arguments;
import co.cask.common.cli.Command;
import co.cask.coopr.client.ClusterClient;
import co.cask.coopr.shell.util.CliUtil;

import java.io.PrintStream;
import javax.inject.Inject;

/**
 * Gets the cluster config.
 */
public class GetClusterConfigCommand implements Command {

  private static final String CLUSTER_ID_KEY = "cluster-id";

  private final ClusterClient clusterClient;

  @Inject
  public GetClusterConfigCommand(ClusterClient clusterClient) {
    this.clusterClient = clusterClient;
  }

  @Override
  public void execute(Arguments arguments, PrintStream printStream) throws Exception {
    String id = CliUtil.checkArgument(arguments.get(CLUSTER_ID_KEY));
    printStream.print(CliUtil.getPrettyJson(clusterClient.getClusterConfig(id)));
  }

  @Override
  public String getPattern() {
    return "get cluster config <cluster-id>";
  }

  @Override
  public String getDescription() {
    return "Gets the cluster config";
  }
}
