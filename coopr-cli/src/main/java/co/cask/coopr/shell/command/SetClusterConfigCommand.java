package co.cask.coopr.shell.command;

import co.cask.common.cli.Arguments;
import co.cask.common.cli.Command;
import co.cask.coopr.client.ClusterClient;
import co.cask.coopr.http.request.ClusterConfigureRequest;
import co.cask.coopr.shell.util.CliUtil;

import java.io.PrintStream;
import javax.inject.Inject;

/**
 * Sets the cluster config.
 */
public class SetClusterConfigCommand implements Command {

  private static final String CLUSTER_ID_KEY = "cluster-id";
  private static final String CLUSTER_CONFIG_KEY = "cluster-config";

  private final ClusterClient clusterClient;

  @Inject
  public SetClusterConfigCommand(ClusterClient clusterClient) {
    this.clusterClient = clusterClient;
  }

  @Override
  public void execute(Arguments arguments, PrintStream printStream) throws Exception {
    String id = arguments.get(CLUSTER_ID_KEY);
    CliUtil.checkArgument(id);
    ClusterConfigureRequest clusterConfigureRequest = CliUtil.getObjectFromJson(arguments, CLUSTER_CONFIG_KEY,
                                                                                ClusterConfigureRequest.class);
    clusterClient.setClusterConfig(id.substring(1, id.length() - 1), clusterConfigureRequest);
  }

  @Override
  public String getPattern() {
    return "set config <cluster-config> for cluster <cluster-id>";
  }

  @Override
  public String getDescription() {
    return "Sets the cluster config";
  }
}
