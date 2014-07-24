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
package com.continuuity.loom.store.provisioner;

import com.continuuity.loom.account.Account;
import com.continuuity.loom.common.conf.Constants;
import com.continuuity.loom.provisioner.plugin.ResourceMeta;
import com.continuuity.loom.provisioner.plugin.ResourceStatus;
import com.continuuity.loom.provisioner.plugin.ResourceType;
import com.continuuity.loom.provisioner.plugin.Type;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;

import java.util.Set;

/**
 *
 */
public abstract class PluginResourceMetaStoreTest {
  ResourceType type1 = new ResourceType(Type.AUTOMATOR, "chef-solo", "cookbooks");
  ResourceType type2 = new ResourceType(Type.PROVIDER, "openstack", "keys");
  Account account1 = new Account(Constants.ADMIN_USER, "tenant1");
  Account account2 = new Account(Constants.ADMIN_USER, "tenant2");

  abstract PluginMetaStoreService getPluginResourceMetaStoreService() throws Exception;

  abstract void clearData() throws Exception;

  @After
  public void cleanupTest() throws Exception {
    clearData();
  }

  @Test
  public void testWriteDeleteExistsGetWithinAccount() throws Exception {
    PluginMetaStoreService service = getPluginResourceMetaStoreService();
    PluginMetaStoreView view = service.getView(account1, type1);
    String name = "name";
    int version = 1;
    ResourceMeta meta = new ResourceMeta(name, version);

    view.add(meta);
    Assert.assertTrue(view.exists(name, version));
    Assert.assertEquals(meta, view.get(name, version));

    view.delete(name, version);
    Assert.assertFalse(view.exists(name, version));
    Assert.assertNull(view.get(name, version));
  }

  @Test
  public void testAccountSeparation() throws Exception {
    PluginMetaStoreService service = getPluginResourceMetaStoreService();
    PluginMetaStoreView view1 = service.getView(account1, type1);
    PluginMetaStoreView view2 = service.getView(account2, type1);
    String name = "name";
    int version = 1;
    ResourceMeta meta = new ResourceMeta(name, version);

    view1.add(meta);
    Assert.assertTrue(view1.exists(name, version));
    Assert.assertFalse(view2.exists(name, version));
    Assert.assertEquals(meta, view1.get(name, version));
    Assert.assertNull(view2.get(name, version));

    view2.add(meta);
    Assert.assertTrue(view1.exists(name, version));
    Assert.assertTrue(view2.exists(name, version));
    Assert.assertEquals(meta, view1.get(name, version));
    Assert.assertEquals(meta, view2.get(name, version));

    view1.delete(name, version);
    Assert.assertFalse(view1.exists(name, version));
    Assert.assertTrue(view2.exists(name, version));
    Assert.assertNull(view1.get(name, version));
    Assert.assertEquals(meta, view2.get(name, version));

    view2.delete(name, version);
    Assert.assertFalse(view1.exists(name, version));
    Assert.assertFalse(view2.exists(name, version));
    Assert.assertNull(view1.get(name, version));
    Assert.assertNull(view2.get(name, version));
  }

  @Test
  public void testTypeSeparation() throws Exception {
    PluginMetaStoreService service = getPluginResourceMetaStoreService();
    PluginMetaStoreView view1 = service.getView(account1, type1);
    PluginMetaStoreView view2 = service.getView(account1, type2);
    String name = "name";
    int version = 1;
    ResourceMeta meta = new ResourceMeta(name, version);

    view1.add(meta);
    Assert.assertTrue(view1.exists(name, version));
    Assert.assertFalse(view2.exists(name, version));
    Assert.assertEquals(meta, view1.get(name, version));
    Assert.assertNull(view2.get(name, version));

    view2.add(meta);
    Assert.assertTrue(view1.exists(name, version));
    Assert.assertTrue(view2.exists(name, version));
    Assert.assertEquals(meta, view1.get(name, version));
    Assert.assertEquals(meta, view2.get(name, version));

    view1.delete(name, version);
    Assert.assertFalse(view1.exists(name, version));
    Assert.assertTrue(view2.exists(name, version));
    Assert.assertNull(view1.get(name, version));
    Assert.assertEquals(meta, view2.get(name, version));

    view2.delete(name, version);
    Assert.assertFalse(view1.exists(name, version));
    Assert.assertFalse(view2.exists(name, version));
    Assert.assertNull(view1.get(name, version));
    Assert.assertNull(view2.get(name, version));
  }

  @Test(expected = IllegalArgumentException.class)
  public void testOnlyAdminsHaveAccess() throws Exception {
    PluginMetaStoreService service = getPluginResourceMetaStoreService();
    service.getView(new Account("notadmin", "tenant"),
                    new ResourceType(Type.AUTOMATOR, "chef-solo", "cookbooks"));
  }

  @Test
  public void testGetAll() throws Exception {
    PluginMetaStoreService service = getPluginResourceMetaStoreService();
    PluginMetaStoreView view = service.getView(account1, type1);
    ResourceMeta hadoop1 = new ResourceMeta("hadoop", 1, ResourceStatus.INACTIVE);
    ResourceMeta hadoop2 = new ResourceMeta("hadoop", 2, ResourceStatus.STAGED);
    ResourceMeta hadoop3 = new ResourceMeta("hadoop", 3, ResourceStatus.ACTIVE);
    ResourceMeta mysql1 = new ResourceMeta("mysql", 1, ResourceStatus.STAGED);
    ResourceMeta mysql2 = new ResourceMeta("mysql", 2, ResourceStatus.ACTIVE);
    ResourceMeta apache = new ResourceMeta("apache", 1, ResourceStatus.UNSTAGED);

    Set<ResourceMeta> all = ImmutableSet.of(hadoop1, hadoop2, hadoop3, mysql1, mysql2, apache);
    Set<ResourceMeta> hadoops = ImmutableSet.of(hadoop1, hadoop2, hadoop3);
    Set<ResourceMeta> mysqls = ImmutableSet.of(mysql1, mysql2);
    Set<ResourceMeta> apaches = ImmutableSet.of(apache);

    for (ResourceMeta meta : all) {
      view.add(meta);
    }
    Assert.assertEquals(
      ImmutableMap.<String, Set<ResourceMeta>>of(
        "hadoop", ImmutableSet.<ResourceMeta>of(hadoop1, hadoop2, hadoop3),
        "mysql", ImmutableSet.<ResourceMeta>of(mysql1, mysql2),
        "apache", ImmutableSet.<ResourceMeta>of(apache)),
      ImmutableMap.copyOf(view.getAll())
    );
    Assert.assertEquals(hadoops, ImmutableSet.copyOf(view.getAll("hadoop")));
    Assert.assertEquals(mysqls, ImmutableSet.copyOf(view.getAll("mysql")));
    Assert.assertEquals(apaches, ImmutableSet.copyOf(view.getAll("apache")));

    // test get active
    Assert.assertEquals(
      ImmutableMap.<String, Set<ResourceMeta>>of(
        "hadoop", ImmutableSet.<ResourceMeta>of(hadoop3),
        "mysql", ImmutableSet.<ResourceMeta>of(mysql2)),
      ImmutableMap.copyOf(view.getAll(ResourceStatus.ACTIVE))
    );
    Assert.assertEquals(Sets.newHashSet(hadoop3), view.getAll("hadoop", ResourceStatus.ACTIVE));
    Assert.assertEquals(Sets.newHashSet(mysql2), view.getAll("mysql", ResourceStatus.ACTIVE));
    Assert.assertTrue(view.getAll("apache", ResourceStatus.ACTIVE).isEmpty());

    // test get staged
    Assert.assertEquals(
      ImmutableMap.<String, Set<ResourceMeta>>of(
        "hadoop", ImmutableSet.<ResourceMeta>of(hadoop2),
        "mysql", ImmutableSet.<ResourceMeta>of(mysql1)),
      ImmutableMap.copyOf(view.getAll(ResourceStatus.STAGED))
    );
    Assert.assertEquals(Sets.newHashSet(hadoop2), view.getAll("hadoop", ResourceStatus.STAGED));
    Assert.assertEquals(Sets.newHashSet(mysql1), view.getAll("mysql", ResourceStatus.STAGED));
    Assert.assertTrue(view.getAll("apache", ResourceStatus.STAGED).isEmpty());

    // test get unstaged
    Assert.assertEquals(
      ImmutableMap.<String, Set<ResourceMeta>>of(
        "apache", ImmutableSet.<ResourceMeta>of(apache)),
      ImmutableMap.copyOf(view.getAll(ResourceStatus.UNSTAGED))
    );
    Assert.assertTrue(view.getAll("hadoop", ResourceStatus.UNSTAGED).isEmpty());
    Assert.assertTrue(view.getAll("mysql", ResourceStatus.UNSTAGED).isEmpty());
    Assert.assertEquals(Sets.newHashSet(apache), view.getAll("apache", ResourceStatus.UNSTAGED));

    // test get inactive
    Assert.assertEquals(
      ImmutableMap.<String, Set<ResourceMeta>>of(
        "hadoop", ImmutableSet.<ResourceMeta>of(hadoop1)),
      ImmutableMap.copyOf(view.getAll(ResourceStatus.INACTIVE))
    );
    Assert.assertEquals(Sets.newHashSet(hadoop1), view.getAll("hadoop", ResourceStatus.INACTIVE));
    Assert.assertTrue(view.getAll("mysql", ResourceStatus.INACTIVE).isEmpty());
    Assert.assertTrue(view.getAll("apache", ResourceStatus.INACTIVE).isEmpty());
  }

  @Test
  public void testStage() throws Exception {
    PluginMetaStoreService service = getPluginResourceMetaStoreService();
    PluginMetaStoreView view = service.getView(account1, type1);
    ResourceMeta hadoop1 = new ResourceMeta("hadoop", 1, ResourceStatus.INACTIVE);
    ResourceMeta hadoop2 = new ResourceMeta("hadoop", 2, ResourceStatus.UNSTAGED);
    ResourceMeta hadoop3 = new ResourceMeta("hadoop", 3, ResourceStatus.INACTIVE);
    ResourceMeta mysql = new ResourceMeta("mysql", 1, ResourceStatus.STAGED);
    ResourceMeta apache = new ResourceMeta("apache", 1, ResourceStatus.ACTIVE);

    view.add(hadoop1);
    view.add(hadoop2);
    view.add(hadoop3);
    view.add(mysql);
    view.add(apache);

    // check no-ops
    view.stage(mysql.getName(), mysql.getVersion());
    Assert.assertEquals(ResourceStatus.STAGED, view.get(mysql.getName(), mysql.getVersion()).getStatus());

    view.stage(apache.getName(), apache.getVersion());
    Assert.assertEquals(ResourceStatus.ACTIVE, view.get(apache.getName(), apache.getVersion()).getStatus());

    // check staging an unstaged makes it active
    view.stage(hadoop2.getName(), hadoop2.getVersion());
    Assert.assertEquals(ResourceStatus.INACTIVE, view.get(hadoop1.getName(), hadoop1.getVersion()).getStatus());
    Assert.assertEquals(ResourceStatus.ACTIVE, view.get(hadoop2.getName(), hadoop2.getVersion()).getStatus());
    Assert.assertEquals(ResourceStatus.INACTIVE, view.get(hadoop3.getName(), hadoop3.getVersion()).getStatus());

    // check staging from inactive
    view.stage(hadoop1.getName(), hadoop1.getVersion());
    Assert.assertEquals(ResourceStatus.STAGED, view.get(hadoop1.getName(), hadoop1.getVersion()).getStatus());
    Assert.assertEquals(ResourceStatus.ACTIVE, view.get(hadoop2.getName(), hadoop2.getVersion()).getStatus());
    Assert.assertEquals(ResourceStatus.INACTIVE, view.get(hadoop3.getName(), hadoop3.getVersion()).getStatus());

    // check staging deactivates previous staged version
    view.stage(hadoop3.getName(), hadoop3.getVersion());
    Assert.assertEquals(ResourceStatus.INACTIVE, view.get(hadoop1.getName(), hadoop1.getVersion()).getStatus());
    Assert.assertEquals(ResourceStatus.ACTIVE, view.get(hadoop2.getName(), hadoop2.getVersion()).getStatus());
    Assert.assertEquals(ResourceStatus.STAGED, view.get(hadoop3.getName(), hadoop3.getVersion()).getStatus());
  }

  @Test
  public void testUnstage() throws Exception {
    PluginMetaStoreService service = getPluginResourceMetaStoreService();
    PluginMetaStoreView view = service.getView(account1, type1);
    ResourceMeta hadoop1 = new ResourceMeta("hadoop", 1, ResourceStatus.INACTIVE);
    ResourceMeta hadoop2 = new ResourceMeta("hadoop", 2, ResourceStatus.UNSTAGED);
    ResourceMeta hadoop3 = new ResourceMeta("hadoop", 3, ResourceStatus.INACTIVE);
    ResourceMeta mysql1 = new ResourceMeta("mysql", 1, ResourceStatus.STAGED);
    ResourceMeta mysql2 = new ResourceMeta("mysql", 2, ResourceStatus.ACTIVE);

    view.add(hadoop1);
    view.add(hadoop2);
    view.add(hadoop3);
    view.add(mysql1);
    view.add(mysql2);

    // check no-ops
    view.unstage(hadoop1.getName(), hadoop1.getVersion());
    Assert.assertEquals(ResourceStatus.INACTIVE, view.get(hadoop1.getName(), hadoop1.getVersion()).getStatus());
    Assert.assertEquals(ResourceStatus.UNSTAGED, view.get(hadoop2.getName(), hadoop2.getVersion()).getStatus());
    Assert.assertEquals(ResourceStatus.INACTIVE, view.get(hadoop3.getName(), hadoop3.getVersion()).getStatus());

    view.unstage(hadoop2.getName(), hadoop2.getVersion());
    Assert.assertEquals(ResourceStatus.INACTIVE, view.get(hadoop1.getName(), hadoop1.getVersion()).getStatus());
    Assert.assertEquals(ResourceStatus.UNSTAGED, view.get(hadoop2.getName(), hadoop2.getVersion()).getStatus());
    Assert.assertEquals(ResourceStatus.INACTIVE, view.get(hadoop3.getName(), hadoop3.getVersion()).getStatus());

    view.unstage(hadoop3.getName(), hadoop3.getVersion());
    Assert.assertEquals(ResourceStatus.INACTIVE, view.get(hadoop1.getName(), hadoop1.getVersion()).getStatus());
    Assert.assertEquals(ResourceStatus.UNSTAGED, view.get(hadoop2.getName(), hadoop2.getVersion()).getStatus());
    Assert.assertEquals(ResourceStatus.INACTIVE, view.get(hadoop3.getName(), hadoop3.getVersion()).getStatus());

    // check unstaging a staged resource deactivates it
    view.unstage(mysql1.getName(), mysql1.getVersion());
    Assert.assertEquals(ResourceStatus.INACTIVE, view.get(mysql1.getName(), mysql1.getVersion()).getStatus());
    Assert.assertEquals(ResourceStatus.ACTIVE, view.get(mysql2.getName(), mysql2.getVersion()).getStatus());

    // check unstaging an active moves it to unstaged
    view.unstage(mysql2.getName(), mysql2.getVersion());
    Assert.assertEquals(ResourceStatus.INACTIVE, view.get(mysql1.getName(), mysql1.getVersion()).getStatus());
    Assert.assertEquals(ResourceStatus.UNSTAGED, view.get(mysql2.getName(), mysql2.getVersion()).getStatus());
  }

  @Test
  public void testSyncStatus() throws Exception {
    PluginMetaStoreService service = getPluginResourceMetaStoreService();
    PluginMetaStoreView view = service.getView(account1, type1);
    ResourceMeta hadoop1 = new ResourceMeta("hadoop", 1, ResourceStatus.INACTIVE);
    ResourceMeta hadoop2 = new ResourceMeta("hadoop", 2, ResourceStatus.STAGED);
    ResourceMeta hadoop3 = new ResourceMeta("hadoop", 3, ResourceStatus.UNSTAGED);
    ResourceMeta mysql1 = new ResourceMeta("mysql", 1, ResourceStatus.INACTIVE);
    ResourceMeta mysql2 = new ResourceMeta("mysql", 2, ResourceStatus.STAGED);
    ResourceMeta apache1 = new ResourceMeta("apache", 1, ResourceStatus.INACTIVE);
    ResourceMeta apache2 = new ResourceMeta("apache", 2, ResourceStatus.UNSTAGED);

    view.add(hadoop1);
    view.add(hadoop2);
    view.add(hadoop3);
    view.add(mysql1);
    view.add(mysql2);
    view.add(apache1);
    view.add(apache2);

    // check staged becomes active and unstaged becomes inactive
    view.syncStatus(hadoop1.getName());
    Assert.assertEquals(ResourceStatus.INACTIVE, view.get(hadoop1.getName(), hadoop1.getVersion()).getStatus());
    Assert.assertEquals(ResourceStatus.ACTIVE, view.get(hadoop2.getName(), hadoop2.getVersion()).getStatus());
    Assert.assertEquals(ResourceStatus.INACTIVE, view.get(hadoop3.getName(), hadoop3.getVersion()).getStatus());

    // check staged becomes active
    view.syncStatus(mysql1.getName());
    Assert.assertEquals(ResourceStatus.INACTIVE, view.get(mysql1.getName(), mysql1.getVersion()).getStatus());
    Assert.assertEquals(ResourceStatus.ACTIVE, view.get(mysql2.getName(), mysql2.getVersion()).getStatus());

    // check no-op
    view.syncStatus(apache1.getName());
    Assert.assertEquals(ResourceStatus.INACTIVE, view.get(apache1.getName(), apache1.getVersion()).getStatus());
    Assert.assertEquals(ResourceStatus.UNSTAGED, view.get(apache2.getName(), apache2.getVersion()).getStatus());
  }
}
