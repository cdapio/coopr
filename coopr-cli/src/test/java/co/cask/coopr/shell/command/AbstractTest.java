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
import co.cask.coopr.client.AdminClient;
import co.cask.coopr.client.ClusterClient;
import co.cask.coopr.shell.command.set.CommandSet;
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

  protected static final AdminClient ADMIN_CLIENT = Mockito.mock(AdminClient.class);
  protected static final ClusterClient CLUSTER_CLIENT = Mockito.mock(ClusterClient.class);

  protected static CLI<Command> CLI;

  @BeforeClass
  public static void init() throws IOException {

    Injector injector = Guice.createInjector(
      new AbstractModule() {
        @Override
        protected void configure() {
          bind(AdminClient.class).toInstance(ADMIN_CLIENT);
          bind(ClusterClient.class).toInstance(CLUSTER_CLIENT);
        }
      }
    );

    co.cask.common.cli.CommandSet<Command> commandSet = CommandSet.getCliCommandSet(injector);
    CLI = new CLI<Command>(commandSet, Collections.<String, Completer>emptyMap());
  }
}
