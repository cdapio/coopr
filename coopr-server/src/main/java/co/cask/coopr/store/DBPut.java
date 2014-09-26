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
package co.cask.coopr.store;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * Interface used to put an object into a database by first trying to perform an update, and then performing an
 * insert if the update did not affect any rows.
 */
public abstract class DBPut {

  /**
   * Execute the put using the given connection.
   *
   * @param conn Connection to use to execute the put
   * @throws SQLException
   */
  public void executePut(Connection conn) throws SQLException {
    PreparedStatement updateStatement = createUpdateStatement(conn);
    try {
      int rowsUpdated = updateStatement.executeUpdate();
      // if no rows are updated, perform the insert
      if (rowsUpdated == 0) {
        PreparedStatement insertStatement = createInsertStatement(conn);
        try {
          insertStatement.executeUpdate();
        } finally {
          insertStatement.close();
        }
      }
    } finally {
      updateStatement.close();
    }
  }

  protected abstract PreparedStatement createUpdateStatement(Connection conn) throws SQLException;

  protected abstract PreparedStatement createInsertStatement(Connection conn) throws SQLException;
}
