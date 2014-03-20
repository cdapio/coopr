/*
 * Copyright 2012-2014, Continuuity, Inc.
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
package com.continuuity.loom.store;

import com.continuuity.loom.conf.Configuration;
import com.continuuity.loom.conf.Constants;
import com.google.common.base.Throwables;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;

import java.sql.DriverManager;
import java.sql.SQLException;

/**
 *
 */
public class SQLEntityStoreTest extends EntityStoreTest {
  private static SQLEntityStore sqlStore;

  @BeforeClass
  public static void beforeClass() throws SQLException, ClassNotFoundException {
    Configuration sqlConf = new Configuration();
    sqlConf.set(Constants.JDBC_DRIVER, "org.apache.derby.jdbc.EmbeddedDriver");
    sqlConf.set(Constants.JDBC_CONNECTION_STRING, "jdbc:derby:memory:loom;create=true");
    DBConnectionPool dbConnectionPool = new DBConnectionPool(sqlConf);
    sqlStore = new SQLEntityStore(dbConnectionPool);
    sqlStore.initDerbyDB();
    sqlStore.clearData();
    entityStore = sqlStore;
  }

  @Before
  public void before() throws SQLException {
    sqlStore.clearData();
  }

  @AfterClass
  public static void afterClass() {
    try {
      DriverManager.getConnection("jdbc:derby:memory:loom;drop=true");
    } catch (SQLException e) {
      // this is normal when a drop happens
      if (!e.getSQLState().equals("08006") ) {
        Throwables.propagate(e);
      }
    }
  }
}
