package co.cask.coopr.store.entity;

import co.cask.coopr.account.Account;
import co.cask.coopr.store.DBConnectionPool;
import co.cask.coopr.store.DBHelper;
import co.cask.coopr.store.DBQueryExecutor;
import com.google.common.util.concurrent.AbstractIdleService;
import com.google.gson.Gson;
import com.google.inject.Inject;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Implementation of {@link EntityStoreService} that provides views of the entity store backed by a SQL database.
 */
public class SQLEntityStoreService extends AbstractIdleService implements EntityStoreService {
  private final DBConnectionPool dbConnectionPool;
  private final DBQueryExecutor dbQueryExecutor;
  private final Gson gson;

  @Inject
  private SQLEntityStoreService(DBConnectionPool dbConnectionPool, DBQueryExecutor dbQueryExecutor, Gson gson) {
    this.dbConnectionPool = dbConnectionPool;
    this.dbQueryExecutor = dbQueryExecutor;
    this.gson = gson;
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
          "s ( name VARCHAR(255), tenant_id VARCHAR(255), " + entityName + " BLOB, PRIMARY KEY (tenant_id, name))";
        DBHelper.createDerbyTableIfNotExists(createString, dbConnectionPool);
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
      return new SQLAdminEntityStoreView(account, dbConnectionPool, dbQueryExecutor, gson);
    } else {
      return new SQLUserEntityStoreView(account, dbConnectionPool, gson);
    }
  }
}
