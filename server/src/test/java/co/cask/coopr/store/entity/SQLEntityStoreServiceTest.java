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
package co.cask.coopr.store.entity;

import co.cask.coopr.BaseTest;
import co.cask.coopr.codec.json.guice.CodecModules;
import co.cask.coopr.common.conf.Configuration;
import co.cask.coopr.common.conf.guice.ConfigurationModule;
import co.cask.coopr.store.DBHelper;
import co.cask.coopr.store.guice.TestStoreModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import org.junit.AfterClass;
import org.junit.BeforeClass;

import java.sql.SQLException;

/**
 *
 */
public class SQLEntityStoreServiceTest extends EntityStoreServiceTest {
  private static SQLEntityStoreService sqlStore;

  @BeforeClass
  public static void beforeClass() throws SQLException, ClassNotFoundException {
    Configuration sqlConf = BaseTest.createTestConf();
    Injector injector = Guice.createInjector(
      new ConfigurationModule(sqlConf),
      new TestStoreModule(),
      new CodecModules().getModule()
    );
    sqlStore = injector.getInstance(SQLEntityStoreService.class);
    sqlStore.startAndWait();
    entityStoreService = sqlStore;
  }

  @Override
  public void clearState() throws Exception {
    sqlStore.clearData();
  }

  @AfterClass
  public static void afterClass() {
    DBHelper.dropDerbyDB();
  }
}
