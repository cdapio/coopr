/*
 * Copyright © 2012-2014 Cask Data, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package co.cask.coopr.macro.eval;

import co.cask.coopr.macro.SyntaxException;

/**
 * Parses a macro string and returns the correct evaluator for expanding the macro.
 */
public class Evaluators {
  private static final String DEFAULT_IP_TYPE = "bind_v4";

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
      int numParts = ipMacroParts.length;
      if ("self".equals(ipStripped)) {
        // if the macro is ip.self
        return new IPSelfEvaluator(DEFAULT_IP_TYPE);
      } else if (numParts == 2 && "service".equals(ipMacroParts[0])) {
        // if the macro is ip.service.<servicename>
        return new IPServiceEvaluator(ipMacroParts[1], DEFAULT_IP_TYPE, instanceNum);
      } else if (numParts == 2 && ipMacroParts[1].equals("self")) {
        // if the macro is ip.<iptype>.self
        return new IPSelfEvaluator(ipMacroParts[0]);
      } else if (numParts == 3 && ipMacroParts[1].equals("service")) {
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
