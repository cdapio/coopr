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
package com.continuuity.loom.store.guice;

import com.continuuity.loom.store.DBConnectionPool;
import com.continuuity.loom.store.cluster.ClusterStoreService;
import com.continuuity.loom.store.cluster.SQLClusterStoreService;
import com.continuuity.loom.store.entity.EntityStoreService;
import com.continuuity.loom.store.entity.SQLEntityStoreService;
import com.continuuity.loom.store.node.NodeStoreService;
import com.continuuity.loom.store.node.SQLNodeStoreService;
import com.continuuity.loom.store.provisioner.ProvisionerStore;
import com.continuuity.loom.store.provisioner.SQLProvisionerStore;
import com.continuuity.loom.store.tenant.SQLTenantStore;
import com.continuuity.loom.store.tenant.TenantStore;
import com.google.inject.AbstractModule;
import com.google.inject.Scopes;

/**
 * Guice module for binding store related classes.
 */
public class StoreModule extends AbstractModule {

  @Override
  protected void configure() {
    bind(EntityStoreService.class).to(SQLEntityStoreService.class).in(Scopes.SINGLETON);
    bind(NodeStoreService.class).to(SQLNodeStoreService.class).in(Scopes.SINGLETON);
    bind(ClusterStoreService.class).to(SQLClusterStoreService.class).in(Scopes.SINGLETON);
    bind(TenantStore.class).to(SQLTenantStore.class).in(Scopes.SINGLETON);
    bind(ProvisionerStore.class).to(SQLProvisionerStore.class).in(Scopes.SINGLETON);
    bind(DBConnectionPool.class).in(Scopes.SINGLETON);
    bind(SQLClusterStoreService.class).in(Scopes.SINGLETON);
    bind(SQLEntityStoreService.class).in(Scopes.SINGLETON);
    bind(SQLTenantStore.class).in(Scopes.SINGLETON);
    bind(SQLProvisionerStore.class).in(Scopes.SINGLETON);
  }
}
