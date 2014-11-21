/*
 * Copyright © 2012-2014 Cask Data, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package co.cask.coopr.shell.command;

import co.cask.common.cli.CLI;
import co.cask.common.cli.Command;
import co.cask.common.cli.CommandSet;
import co.cask.coopr.client.AdminClient;
import co.cask.coopr.client.ClusterClient;
import co.cask.coopr.client.PluginClient;
import co.cask.coopr.client.ProvisionerClient;
import co.cask.coopr.client.TenantClient;
import co.cask.coopr.codec.json.guice.CodecModules;
import co.cask.coopr.shell.CLIConfig;
import co.cask.coopr.shell.command.set.CooprCommandSets;
import com.google.gson.Gson;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import jline.console.completer.Completer;
import org.junit.BeforeClass;
import org.mockito.Mockito;

import java.io.IOException;
import java.util.Collections;

/**
 * Abstract Test class.
 */
public abstract class AbstractTest {

  protected static final CLIConfig CLI_CONFIG = Mockito.mock(CLIConfig.class);
  protected static final AdminClient ADMIN_CLIENT = Mockito.mock(AdminClient.class);
  protected static final ClusterClient CLUSTER_CLIENT = Mockito.mock(ClusterClient.class);
  protected static final PluginClient PLUGIN_CLIENT = Mockito.mock(PluginClient.class);

  protected static final String TEST_PLUGIN_TYPE = "test-plugin-type";
  protected static final String TEST_RESOURCE_TYPE = "test-resource-type";
  protected static final String TEST_RESOURCE_NAME = "test-resource-name";
  protected static final String TEST_RESOURCE_VERSION = "1";
  protected static final TenantClient TENANT_CLIENT = Mockito.mock(TenantClient.class);
  protected static final ProvisionerClient PROVISIONER_CLIENT = Mockito.mock(ProvisionerClient.class);

  private static final Injector injector = Guice.createInjector(
    new CodecModules().getModule()
  );
  private static final Gson GSON = injector.getInstance(Gson.class);

  protected static CLI<Command> CLI;

  @BeforeClass
  public static void init() throws IOException {

    Injector injector = Guice.createInjector(
      new AbstractModule() {
        @Override
        protected void configure() {
          bind(CLIConfig.class).toInstance(CLI_CONFIG);
          bind(AdminClient.class).toInstance(ADMIN_CLIENT);
          bind(ClusterClient.class).toInstance(CLUSTER_CLIENT);
          bind(PluginClient.class).toInstance(PLUGIN_CLIENT);
          bind(TenantClient.class).toInstance(TENANT_CLIENT);
          bind(ProvisionerClient.class).toInstance(PROVISIONER_CLIENT);
        }
      }
    );

    CommandSet<Command> commandSet = CooprCommandSets.getCommandSetForSuperadmin(injector);
    CLI = new CLI<Command>(commandSet, Collections.<String, Completer>emptyMap());
  }

  public static String getJsonFromObject(Object output) {
    return GSON.toJson(output);
  }
}
