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
package com.continuuity.loom.store.cluster;

import com.continuuity.loom.BaseTest;
import com.continuuity.loom.codec.json.guice.CodecModules;
import com.continuuity.loom.common.conf.Configuration;
import com.continuuity.loom.common.conf.Constants;
import com.continuuity.loom.common.conf.guice.ConfigurationModule;
import com.continuuity.loom.store.DBHelper;
import com.continuuity.loom.store.guice.TestStoreModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import org.junit.AfterClass;
import org.junit.BeforeClass;

/**
 *
 */
public class SQLClusterStoreTest extends ClusterStoreTest {
  private static SQLClusterStoreService sqlClusterStoreService;

  @BeforeClass
  public static void setupSQLClusterStoreTest() throws Exception {
    Configuration sqlConf = BaseTest.createTestConf();
    sqlConf.setLong(Constants.ID_START_NUM, 1);
    sqlConf.setLong(Constants.ID_INCREMENT_BY, 1);
    Injector injector = Guice.createInjector(
      new ConfigurationModule(sqlConf),
      new TestStoreModule(),
      new CodecModules().getModule()
    );
    sqlClusterStoreService = injector.getInstance(SQLClusterStoreService.class);
    sqlClusterStoreService.startAndWait();
  }

  @Override
  public void clearState() throws Exception {
    sqlClusterStoreService.clearData();
  }

  @Override
  public ClusterStoreService getClusterStoreService() throws Exception {
    return sqlClusterStoreService;
  }

  @AfterClass
  public static void afterClass() {
    DBHelper.dropDerbyDB();
  }
}
