package co.cask.coopr.shell.command;

import co.cask.common.cli.Arguments;
import co.cask.common.cli.Command;
import co.cask.coopr.client.ClusterClient;
import co.cask.coopr.http.request.ClusterCreateRequest;
import co.cask.coopr.shell.util.CliUtil;

import java.io.PrintStream;
import java.util.Arrays;
import javax.inject.Inject;

/**
 * Creates the cluster.
 */
public class CreateClusterCommand implements Command {

  private static final String NAME_KEY = "name";
  private static final String TEMPLATE_KEY = "template";
  private static final String SIZE_KEY = "size";
  private static final String SETTINGS_KEY = "settings";

  private final ClusterClient clusterClient;

  @Inject
  public CreateClusterCommand(ClusterClient clusterClient) {
    this.clusterClient = clusterClient;
  }

  @Override
  public void execute(Arguments arguments, PrintStream printStream) throws Exception {
    ClusterCreateRequest.Builder builder = CliUtil.getObjectFromJson(arguments, SETTINGS_KEY,
                                                                     ClusterCreateRequest.Builder.class);
    if (builder == null) {
      builder = ClusterCreateRequest.builder();
    }
    String name = arguments.get(NAME_KEY);
    String template = arguments.get(TEMPLATE_KEY);
    int size = arguments.getInt(SIZE_KEY);
    CliUtil.checkArguments(Arrays.asList(name, template));
    builder.setName(name.substring(1, name.length() - 1));
    builder.setClusterTemplateName(template.substring(1, template.length() - 1));
    builder.setNumMachines(size);
    clusterClient.createCluster(builder.build());
  }

  @Override
  public String getPattern() {
    return "create cluster <name> with template <template> of size <size>[ using settings <settings>]";
  }

  @Override
  public String getDescription() {
    return "Creates the cluster";
  }
}
