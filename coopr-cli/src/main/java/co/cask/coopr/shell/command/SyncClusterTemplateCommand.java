package co.cask.coopr.shell.command;

import co.cask.common.cli.Arguments;
import co.cask.common.cli.Command;
import co.cask.coopr.client.ClusterClient;
import co.cask.coopr.shell.util.CliUtil;

import java.io.PrintStream;
import javax.inject.Inject;

/**
 * Synchronized the cluster template.
 */
public class SyncClusterTemplateCommand implements Command {

  private static final String CLUSTER_ID_KEY = "cluster-id";

  private final ClusterClient clusterClient;

  @Inject
  public SyncClusterTemplateCommand(ClusterClient clusterClient) {
    this.clusterClient = clusterClient;
  }

  @Override
  public void execute(Arguments arguments, PrintStream printStream) throws Exception {
    String clusterId = CliUtil.checkArgument(arguments.get(CLUSTER_ID_KEY));
    clusterClient.syncClusterTemplate(clusterId);
  }

  @Override
  public String getPattern() {
    return "sync cluster template <cluster-id>";
  }

  @Override
  public String getDescription() {
    return "Synchronized the cluster template";
  }
}
