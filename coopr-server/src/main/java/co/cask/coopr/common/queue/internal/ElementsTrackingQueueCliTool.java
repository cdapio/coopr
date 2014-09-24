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
package co.cask.coopr.common.queue.internal;

import co.cask.coopr.common.queue.QueuedElement;
import com.google.common.base.Preconditions;
import com.google.common.collect.Iterators;
import com.google.common.collect.Sets;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.twill.zookeeper.RetryStrategies;
import org.apache.twill.zookeeper.ZKClient;
import org.apache.twill.zookeeper.ZKClientService;
import org.apache.twill.zookeeper.ZKClients;

import java.io.PrintStream;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Command-line tool for manipulating ElementsTrackingQueue.
 */
public class ElementsTrackingQueueCliTool {
  public static final String CMD_LIST = "list";
  public static final String CMD_REMOVE = "remove";
  public static final String CMD_REMOVE_ALL = "remove_all";
  public static final String CMD_PROMOTE = "promote";
  public static final String CMD_HELP = "help";

  private static final Set<String> AVAILABLE_COMMANDS =
    Sets.newHashSet(CMD_LIST, CMD_REMOVE, CMD_REMOVE_ALL, CMD_PROMOTE, CMD_HELP);

  private static final String ARG_OPT_ELEMENT = "element";
  private static final String ARG_OPT_ZK_CONNECTION = "zk-connection";
  private static final String ARG_OPT_QUEUE_NAME = "queue-name";

  private String command = null;
  private String elementId = null;
  private String zkConnectionString = null;
  private String queueName = null;

  public static void main(String[] args) throws Exception {
    ElementsTrackingQueueCliTool client = null;
    try {
      client = new ElementsTrackingQueueCliTool();
      client.configure(args);
    } catch (ParseException e) {
      printHelp();
      return;
    } catch (Throwable e) {
      printHelp();
      // to divide usage from stacktrace output. It's lame, yes.
      Thread.sleep(10);
      System.out.println("ERROR");
      e.printStackTrace();
      return;
    }
    client.execute();
  }

  public String configure(String args[]) throws ParseException {
    Preconditions.checkArgument(args.length >= 1, "Not enough arguments");
    boolean knownCommand = AVAILABLE_COMMANDS.contains(args[0]);
    Preconditions.checkArgument(knownCommand, "Unknown Command specified: " + args[0]);

    String sentCommand = args[0];

    CommandLineParser commandLineParser = new GnuParser();

    Options options = new Options();

    options.addOption("e", ARG_OPT_ELEMENT, true, "Element Id.");
    options.addOption("z", ARG_OPT_ZK_CONNECTION, true, "ZK Connection String.");
    options.addOption("q", ARG_OPT_QUEUE_NAME, true, "Queue Name.");

    CommandLine commandLine = null;

    commandLine = commandLineParser.parse(options, Arrays.copyOfRange(args, 1, args.length));

    //Check if the appropriate args are passed in for each of the commands
    if (CMD_HELP.equals(sentCommand)) {
      // NO params needed
      printHelp();
    }
    if (CMD_LIST.equals(sentCommand)) {
      // NO params needed
    }
    if (CMD_REMOVE_ALL.equals(sentCommand)) {
      // NO params needed
    }
    if (CMD_REMOVE.equals(sentCommand)) {
      Preconditions.checkArgument(commandLine.hasOption(ARG_OPT_ELEMENT), CMD_REMOVE + " command should have " +
        "element argument");
      this.elementId = commandLine.getOptionValue(ARG_OPT_ELEMENT);
    }
    if (CMD_PROMOTE.equals(sentCommand)) {
      Preconditions.checkArgument(commandLine.hasOption(ARG_OPT_ELEMENT), CMD_PROMOTE + " command should have " +
        "element argument");
      elementId = commandLine.getOptionValue(ARG_OPT_ELEMENT);
    }

    Preconditions.checkArgument(commandLine.hasOption(ARG_OPT_ZK_CONNECTION),
                                ARG_OPT_ZK_CONNECTION + " must be specified");
    Preconditions.checkArgument(commandLine.hasOption(ARG_OPT_QUEUE_NAME),
                                ARG_OPT_QUEUE_NAME + " must be specified");
    zkConnectionString = commandLine.getOptionValue(ARG_OPT_ZK_CONNECTION);
    queueName = commandLine.getOptionValue(ARG_OPT_QUEUE_NAME);

    command = sentCommand;

    return command;
  }

  private static void printHelp() {
    PrintStream out = System.out;

    out.println("Usage: ");
    String command = "ElementsTrackingQueueCliTool";
    out.println("  " + command + " list [queue options]");
    out.println("  " + command + " promote --element <element_id> [queue options]");
    out.println("  " + command + " remove --element <element_id> [queue options]");
    out.println("  " + command + " remove_all [queue options]");
    out.println("  " + command + " help");
    out.println(" queue options:");
    out.println("  --zk-connection <zk_connection_string> --queue-name <queue_name>");
  }

  /**
   * Execute the configured operation.
   */
  public void execute() throws Exception {
    if (CMD_HELP.equals(command)) {
      // NO action needed: we wrote usage info during configure()
      return;
    }

    ZKClientService zkClientService = ZKClientService.Builder.of(zkConnectionString).build();
    ZKClient zkClient =
      ZKClients.reWatchOnExpire(
        ZKClients.retryOnFailure(zkClientService,
                                 RetryStrategies.limit(5, RetryStrategies.fixDelay(5, TimeUnit.SECONDS))));

    zkClientService.start();
    try {
      ElementsTrackingQueue queue = new ElementsTrackingQueue(new ZKElementsTracking(zkClient, queueName));
      try {
        if (CMD_LIST.equals(command)) {
          printQueueElements(queue);
        }
        if (CMD_REMOVE.equals(command)) {
          if (queue.remove(elementId)) {
            System.out.println("element " + elementId + " was removed successfully");
          } else {
            System.out.println("removing of element " + elementId + " failed.");
          }
        }
        if (CMD_PROMOTE.equals(command)) {
          if (queue.toHighestPriority(elementId)) {
            System.out.println("element " + elementId + " was promoted to the top of the queue successfully");
          } else {
            System.out.println("promoting of element " + elementId + " to the top of the queue failed.");
          }
        }
        if (CMD_REMOVE_ALL.equals(command)) {
          if (queue.removeAll()) {
            System.out.println("Successfully removed all elements from the queue");
          } else {
            System.out.println("Removing all elements failed.");
          }
        }
      } catch (Exception e) {
        System.out.println(String.format("Caught Exception while running %s ", command));
        System.out.println(String.format("Error: %s", e.getMessage()));
      }
    } finally {
      zkClientService.stop();
    }
  }

  private void printQueueElements(ElementsTrackingQueue queue) {
    System.out.println("Queued: ");
    int count = Iterators.size(queue.getQueued());
    System.out.println("# of queued: " + count);

    Iterator<QueuedElement> beingConsumed = queue.getBeingConsumed();
    System.out.println("Being Consumed:");
    while (beingConsumed.hasNext()) {
      QueuedElement entry = beingConsumed.next();
      System.out.println(entry.getConsumerId() + ": " + entry.getElement().toString());
    }
    System.out.println("# of being consumed: " + Iterators.size(beingConsumed));
  }
}
