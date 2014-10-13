package co.cask.coopr.shell.command;

import co.cask.common.cli.Arguments;
import co.cask.common.cli.Command;
import co.cask.coopr.client.ClusterClient;
import co.cask.coopr.http.request.AddServicesRequest;
import co.cask.coopr.shell.util.CliUtil;

import java.io.PrintStream;
import javax.inject.Inject;

/**
 * Adds service on cluster.
 */
public class AddServicesOnClusterCommand implements Command {

  private static final String CLUSTER_ID_KEY = "cluster-id";
  private static final String SERVICES_KEY = "services";

  private final ClusterClient clusterClient;

  @Inject
  public AddServicesOnClusterCommand(ClusterClient clusterClient) {
    this.clusterClient = clusterClient;
  }

  @Override
  public void execute(Arguments arguments, PrintStream printStream) throws Exception {
    String id = CliUtil.checkArgument(arguments.get(CLUSTER_ID_KEY));
    AddServicesRequest addServicesRequest = CliUtil.getObjectFromJson(arguments, SERVICES_KEY,
                                                                           AddServicesRequest.class);
    clusterClient.addServicesOnCluster(id, addServicesRequest);
  }

  @Override
  public String getPattern() {
    return "add services <services> on cluster <cluster-id>";
  }

  @Override
  public String getDescription() {
    return "Adds service on cluster";
  }
}
