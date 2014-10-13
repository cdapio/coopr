package co.cask.coopr.shell.command;

import co.cask.common.cli.Arguments;
import co.cask.common.cli.Command;
import co.cask.coopr.client.ClusterClient;
import co.cask.coopr.shell.util.CliUtil;

import java.io.PrintStream;
import javax.inject.Inject;

/**
 * Sets the cluster expire time.
 */
public class SetClusterExpireTimeCommand implements Command {

  private static final String CLUSTER_ID_KEY = "cluster-id";
  private static final String EXPIRE_TIME_KEY = "expire-time";

  private final ClusterClient clusterClient;

  @Inject
  public SetClusterExpireTimeCommand(ClusterClient clusterClient) {
    this.clusterClient = clusterClient;
  }

  @Override
  public void execute(Arguments arguments, PrintStream printStream) throws Exception {
    String clusterId = CliUtil.checkArgument(arguments.get(CLUSTER_ID_KEY));
    long expireTime = arguments.getLong(EXPIRE_TIME_KEY);
    clusterClient.setClusterExpireTime(clusterId, expireTime);
  }

  @Override
  public String getPattern() {
    return "set expire time <expire-time> for cluster <cluster-id>";
  }

  @Override
  public String getDescription() {
    return "Sets the cluster expire time";
  }
}
