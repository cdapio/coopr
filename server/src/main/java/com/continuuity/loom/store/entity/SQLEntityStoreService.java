package com.continuuity.loom.store.entity;

import com.continuuity.loom.account.Account;
import com.continuuity.loom.spec.HardwareType;
import com.continuuity.loom.spec.ImageType;
import com.continuuity.loom.spec.Provider;
import com.continuuity.loom.spec.service.Service;
import com.continuuity.loom.spec.template.ClusterTemplate;
import com.continuuity.loom.store.DBConnectionPool;
import com.continuuity.loom.store.DBHelper;
import com.continuuity.loom.store.DBQueryExecutor;
import com.google.common.util.concurrent.AbstractIdleService;
import com.google.gson.Gson;
import com.google.inject.Inject;

import java.io.IOException;
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

  @Override
  public void copyEntities(Account from, Account to) throws IOException, IllegalAccessException {
    EntityStoreView fromView = getView(from);
    EntityStoreView toView = getView(to);
    for (HardwareType hardwareType : fromView.getAllHardwareTypes()) {
      toView.writeHardwareType(hardwareType);
    }
    for (ImageType imageType : fromView.getAllImageTypes()) {
      toView.writeImageType(imageType);
    }
    for (Service service : fromView.getAllServices()) {
      toView.writeService(service);
    }
    for (ClusterTemplate template : fromView.getAllClusterTemplates()) {
      toView.writeClusterTemplate(template);
    }
    for (Provider provider : fromView.getAllProviders()) {
      toView.writeProvider(provider);
    }
  }
}
