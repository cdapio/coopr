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
package co.cask.coopr.runtime;

import co.cask.coopr.common.conf.Constants;
import co.cask.coopr.common.daemon.DaemonMain;
import co.cask.coopr.provisioner.mock.MockProvisionerService;
import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Mock provisioner. Will communicate with a server, registering, heartbeating, taking tasks, and spinning up
 * and down tenant workers.  However, the workers will not perform any actual work and will instead always return
 * back a success.
 */
public final class MockProvisionerMain extends DaemonMain {
  private static final Logger LOG = LoggerFactory.getLogger(MockProvisionerMain.class);
  private MockProvisionerService mockProvisionerService;
  private Options options;

  public static void main(final String[] args) throws Exception {
    new MockProvisionerMain().doMain(args);
  }

  @Override
  public void init(String[] args) {
    try {
      setOptions();
      CommandLineParser parser = new BasicParser();
      CommandLine cmd = parser.parse(options, args);
      if (cmd.hasOption('h')) {
        printHelp();
        System.exit(0);
      }
      String id = cmd.hasOption('i') ? cmd.getOptionValue('i') : "dummy";
      String host = cmd.hasOption('s') ? cmd.getOptionValue('s') : "localhost";
      int port = cmd.hasOption('p') ? Integer.valueOf(cmd.getOptionValue('p')) : 55055;
      int capacity = cmd.hasOption('c') ? Integer.valueOf(cmd.getOptionValue('c')) : 10;
      long msBetweenTasks = cmd.hasOption('r') ? Long.valueOf(cmd.getOptionValue('r')) : 1000;
      long taskMs = cmd.hasOption('d') ? Long.valueOf(cmd.getOptionValue('d')) : 1000;
      int failureRate = cmd.hasOption('f') ? Integer.valueOf(cmd.getOptionValue('f')) : 0;
      String serverUrl = "http://" + host + ":" + port + Constants.API_BASE;
      LOG.info("id = {}, capacity = {}, server url = {}, task frequency = {}, task duration = {}, failure rate = {}",
               id, capacity, serverUrl, msBetweenTasks, taskMs, failureRate);

      mockProvisionerService = new MockProvisionerService(id, serverUrl, capacity, taskMs, msBetweenTasks, failureRate);
    } catch (ParseException e) {
      printHelp();
      System.exit(0);
    }
  }

  @Override
  public void start() {
    LOG.info("starting mock provisioner...");
    mockProvisionerService.startAndWait();
    LOG.info("mock provisioner started.");
  }

  /**
   * Invoked by jsvc to stop the program.
   */
  @Override
  public void stop() {
    if (mockProvisionerService != null) {
      LOG.info("stopping mock provisioner...");
      mockProvisionerService.stopAndWait();
      LOG.info("mock provisioner stopped.");
    }
  }

  /**
   * Invoked by jsvc for resource cleanup.
   */
  @Override
  public void destroy() {
  }

  private void setOptions() {
    options = new Options();
    options.addOption("h", "help", false, "Display help information.");
    options.addOption("i", "id", true, "Id for the provisioner. Defaults to 'dummy'");
    options.addOption("s", "server", true, "Server to connect to. Defaults to 'localhost'");
    options.addOption("p", "port", true, "Server port to connect to. Defaults to 55054");
    options.addOption("c", "capacity", true, "total worker capacity for the provisioner. Defaults to 10");
    options.addOption("r", "rate", true,
                      "milliseconds for workers to wait between taking tasks. Defaults to 1000");
    options.addOption("d", "duration", true,
                      "milliseconds a task should take to complete. Defaults to 1000");
    options.addOption("f", "failure", true,
                      "percentage of time a task should be failed by the mock worker (0 - 100). Defaults to 0");
  }

  private void printHelp() {
    HelpFormatter formatter = new HelpFormatter();
    formatter.printHelp("java -cp <path-to-jar>.jar co.cask.coopr.runtime.MockProvisionerMain", options);
  }
}
