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
package com.continuuity.loom.store.entity;

import com.continuuity.loom.account.Account;
import com.continuuity.loom.codec.json.JsonSerde;
import com.continuuity.loom.store.DBConnectionPool;
import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.List;

/**
 * Implementation of {@link com.continuuity.loom.store.entity.BaseEntityStoreView} from the view of a tenant user.
 */
public class SQLUserEntityStoreView extends BaseSQLEntityStoreView {

  SQLUserEntityStoreView(Account account, DBConnectionPool dbConnectionPool, JsonSerde codec) {
    super(account, dbConnectionPool, codec);
  }

  @Override
  protected void writeEntity(EntityType entityType, String entityName, byte[] data)
    throws IOException, IllegalAccessException {
    throw new IllegalAccessException("Non-admins are not allowed to write entities.");
  }

  @Override
  protected void deleteEntity(EntityType entityType, String entityName) throws IOException, IllegalAccessException {
    throw new IllegalAccessException("Non-admins are not allowed to delete entities.");
  }
}
