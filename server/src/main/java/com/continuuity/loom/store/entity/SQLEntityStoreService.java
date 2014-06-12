package com.continuuity.loom.store.entity;

import com.continuuity.loom.account.Account;
import com.continuuity.loom.store.DBConnectionPool;
import com.continuuity.loom.store.DBQueryHelper;
import com.google.common.util.concurrent.AbstractIdleService;
import com.google.inject.Inject;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/**
 *
 */
public class SQLEntityStoreService extends AbstractIdleService implements EntityStoreService {
  private DBConnectionPool dbConnectionPool;

  @Inject
  public SQLEntityStoreService(DBConnectionPool dbConnectionPool) {
    this.dbConnectionPool = dbConnectionPool;
  }

  // for unit tests only
  public void clearData() throws SQLException {
    Connection conn = dbConnectionPool.getConnection();
    try {
      for (BaseEntityStoreView.EntityType type : BaseEntityStoreView.EntityType.values()) {
        Statement stmt = conn.createStatement();
        try {
          stmt.execute("DELETE FROM " + type.getId() + "s");
        } finally {
          stmt.close();
        }
      }
    } finally {
      conn.close();
    }
  }

  @Override
  protected void startUp() throws Exception {
    if (dbConnectionPool.isEmbeddedDerbyDB()) {
      for (BaseEntityStoreView.EntityType entityType : BaseEntityStoreView.EntityType.values()) {
        String entityName = entityType.getId();
        // immune to sql injection since it comes from the enum
        String createString = "CREATE TABLE " + entityName +
          "s ( name VARCHAR(255), tenant_id VARCHAR(255), " + entityName + " BLOB )";
        DBQueryHelper.createDerbyTable(createString, dbConnectionPool);
      }
    }
  }

  @Override
  protected void shutDown() throws Exception {
    // No-op
  }

  @Override
  public EntityStoreView getView(Account account) {
    if (account.isAdmin()) {
      return new SQLAdminEntityStoreView(account, dbConnectionPool);
    } else {
      return new SQLUserEntityStoreView(account, dbConnectionPool);
    }
  }
}
