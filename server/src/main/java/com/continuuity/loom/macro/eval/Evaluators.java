package com.continuuity.loom.macro.eval;

import com.continuuity.loom.macro.SyntaxException;

/**
 *
 */
public class Evaluators {

  /**
   * Distinguishes the type of substitute - currently the host name or the ip address of a node with a service.
   */
  private enum Type {
    CLUSTER_OWNER("cluster.owner"),
    HOST_SELF("host.self"),
    HOST_OF_SERVICE("host.service."),
    NUM_OF_NODES_WITH_SERVICE("num.service."),
    INSTANCE_OF_SERVICE("instance.self.service."),
    IP("ip.");

    private String prefix;

    private Type(String prefix) {
      this.prefix = prefix;
    }

    private String stripPrefix(String macro) {
      return macro.substring(prefix.length());
    }
  }

  public static Evaluator evaluatorFor(String macroName, Integer instanceNum) throws SyntaxException {
    if (macroName.equals(Type.CLUSTER_OWNER.prefix)) {
      return new ClusterOwnerEvaluator();
    } else if (macroName.equals(Type.HOST_SELF.prefix)) {
      return new HostSelfEvaluator();
    } else if (macroName.startsWith(Type.HOST_OF_SERVICE.prefix)) {
      String service = Type.HOST_OF_SERVICE.stripPrefix(macroName);
      checkService(service);
      return new HostServiceEvaluator(service, instanceNum);
    } else if (macroName.startsWith(Type.NUM_OF_NODES_WITH_SERVICE.prefix)) {
      String service = Type.NUM_OF_NODES_WITH_SERVICE.stripPrefix(macroName);
      checkService(service);
      return new ServiceCardinalityEvaluator(service);
    } else if (macroName.startsWith(Type.INSTANCE_OF_SERVICE.prefix)) {
      String service = Type.INSTANCE_OF_SERVICE.stripPrefix(macroName);
      checkService(service);
      return new ServiceInstanceEvaluator(service);
    } else if (macroName.startsWith(Type.IP.prefix)) {
      String ipStripped = Type.IP.stripPrefix(macroName);
      // not as efficient, but easier to understand
      String[] ipMacroParts = ipStripped.split("\\.", 3);
      // if the macro is ip.<iptype>.self
      if (ipMacroParts.length == 2 && ipMacroParts[1].equals("self")) {
        return new IPSelfEvaluator(ipMacroParts[0]);
      } else if (ipMacroParts.length == 3 && ipMacroParts[1].equals("service")) {
        // if the macro is ip.<iptype>.service.<servicename>
        String service = ipMacroParts[2];
        return new IPServiceEvaluator(service, ipMacroParts[0], instanceNum);
      }
    }

    throw new SyntaxException("'" + macroName + "' is not a valid macro name");
  }

  private static void checkService(String service) throws SyntaxException {
    if (service == null || service.isEmpty()) {
      throw new SyntaxException("macro is missing a service name");
    }
  }
}
