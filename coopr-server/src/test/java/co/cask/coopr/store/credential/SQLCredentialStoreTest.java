/*
 * Copyright © 2014 Cask Data, Inc.
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

package co.cask.coopr.store.credential;

import co.cask.coopr.BaseTest;
import co.cask.coopr.common.conf.Configuration;
import co.cask.coopr.common.conf.guice.ConfigurationModule;
import co.cask.coopr.store.guice.TestStoreModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import org.junit.BeforeClass;

/**
 *
 */
public class SQLCredentialStoreTest extends CredentialStoreTest {
  private static SQLCredentialStore store;

  @BeforeClass
  public static void setupClass() {
    Configuration conf = BaseTest.createTestConf();
    Injector injector = Guice.createInjector(
      new ConfigurationModule(conf),
      new TestStoreModule()
    );
    store = injector.getInstance(SQLCredentialStore.class);
    store.startAndWait();
  }

  @Override
  protected CredentialStore getRunningStore() {
    return store;
  }

  @Override
  protected void wipeStore() throws Exception {
    store.wipe();
  }
}
