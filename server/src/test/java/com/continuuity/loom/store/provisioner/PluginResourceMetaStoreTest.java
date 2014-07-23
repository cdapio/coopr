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
import com.continuuity.loom.provisioner.PluginResourceMeta;
import com.continuuity.loom.provisioner.PluginResourceStatus;
import com.continuuity.loom.provisioner.PluginResourceType;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;

import java.util.Set;
import java.util.UUID;

/**
 *
 */
public abstract class PluginResourceMetaStoreTest {
  PluginResourceType type1 = new PluginResourceType(PluginType.AUTOMATOR, "chef-solo", "cookbooks");
  PluginResourceType type2 = new PluginResourceType(PluginType.PROVIDER, "openstack", "keys");
  Account account1 = new Account(Constants.ADMIN_USER, "tenant1");
  Account account2 = new Account(Constants.ADMIN_USER, "tenant2");

  abstract PluginResourceMetaStoreService getPluginResourceMetaStoreService() throws Exception;

  abstract void clearData() throws Exception;

  @After
  public void cleanupTest() throws Exception {
    clearData();
  }

  @Test
  public void testWriteDeleteExistsGetWithinAccount() throws Exception {
    PluginResourceMetaStoreService service = getPluginResourceMetaStoreService();
    PluginResourceMetaStoreView view = service.getView(account1, type1);
    String name = "name";
    String version = "version";
    PluginResourceMeta meta = PluginResourceMeta.createNew(name, version);

    view.write(meta);
    Assert.assertTrue(view.exists(name, version));
    Assert.assertEquals(meta, view.get(name, version));

    view.delete(name, version);
    Assert.assertFalse(view.exists(name, version));
    Assert.assertNull(view.get(name, version));
  }

  @Test
  public void testAccountSeparation() throws Exception {
    PluginResourceMetaStoreService service = getPluginResourceMetaStoreService();
    PluginResourceMetaStoreView view1 = service.getView(account1, type1);
    PluginResourceMetaStoreView view2 = service.getView(account2, type1);
    String name = "name";
    String version = "version";
    PluginResourceMeta meta = PluginResourceMeta.createNew(name, version);

    view1.write(meta);
    Assert.assertTrue(view1.exists(name, version));
    Assert.assertFalse(view2.exists(name, version));
    Assert.assertEquals(meta, view1.get(name, version));
    Assert.assertNull(view2.get(name, version));

    view2.write(meta);
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
    PluginResourceMetaStoreService service = getPluginResourceMetaStoreService();
    PluginResourceMetaStoreView view1 = service.getView(account1, type1);
    PluginResourceMetaStoreView view2 = service.getView(account1, type2);
    String name = "name";
    String version = "version";
    PluginResourceMeta meta = PluginResourceMeta.createNew(name, version);

    view1.write(meta);
    Assert.assertTrue(view1.exists(name, version));
    Assert.assertFalse(view2.exists(name, version));
    Assert.assertEquals(meta, view1.get(name, version));
    Assert.assertNull(view2.get(name, version));

    view2.write(meta);
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
    PluginResourceMetaStoreService service = getPluginResourceMetaStoreService();
    service.getView(new Account("notadmin", "tenant"),
                    new PluginResourceType(PluginType.AUTOMATOR, "chef-solo", "cookbooks"));
  }

  @Test
  public void testGetAll() throws Exception {
    PluginResourceMetaStoreService service = getPluginResourceMetaStoreService();
    PluginResourceMetaStoreView view = service.getView(account1, type1);
    PluginResourceMeta hadoop1 =
      PluginResourceMeta.fromExisting(UUID.randomUUID().toString(), "hadoop", "1", PluginResourceStatus.INACTIVE);
    PluginResourceMeta hadoop2 =
      PluginResourceMeta.fromExisting(UUID.randomUUID().toString(), "hadoop", "2", PluginResourceStatus.STAGED);
    PluginResourceMeta hadoop3 =
      PluginResourceMeta.fromExisting(UUID.randomUUID().toString(), "hadoop", "3", PluginResourceStatus.ACTIVE);
    PluginResourceMeta mysql1 =
      PluginResourceMeta.fromExisting(UUID.randomUUID().toString(), "mysql", "1", PluginResourceStatus.STAGED);
    PluginResourceMeta mysql2 =
      PluginResourceMeta.fromExisting(UUID.randomUUID().toString(), "mysql", "2", PluginResourceStatus.ACTIVE);
    PluginResourceMeta apache =
      PluginResourceMeta.fromExisting(UUID.randomUUID().toString(), "apache", "1", PluginResourceStatus.UNSTAGED);

    Set<PluginResourceMeta> all = ImmutableSet.of(hadoop1, hadoop2, hadoop3, mysql1, mysql2, apache);
    Set<PluginResourceMeta> hadoops = ImmutableSet.of(hadoop1, hadoop2, hadoop3);
    Set<PluginResourceMeta> mysqls = ImmutableSet.of(mysql1, mysql2);
    Set<PluginResourceMeta> apaches = ImmutableSet.of(apache);

    for (PluginResourceMeta meta : all) {
      view.write(meta);
    }
    Assert.assertEquals(
      ImmutableMap.<String, Set<PluginResourceMeta>>of(
        "hadoop", ImmutableSet.<PluginResourceMeta>of(hadoop1, hadoop2, hadoop3),
        "mysql", ImmutableSet.<PluginResourceMeta>of(mysql1, mysql2),
        "apache", ImmutableSet.<PluginResourceMeta>of(apache)),
      ImmutableMap.copyOf(view.getAll())
    );
    Assert.assertEquals(hadoops, ImmutableSet.copyOf(view.getAll("hadoop")));
    Assert.assertEquals(mysqls, ImmutableSet.copyOf(view.getAll("mysql")));
    Assert.assertEquals(apaches, ImmutableSet.copyOf(view.getAll("apache")));

    // test get active
    Assert.assertEquals(
      ImmutableMap.<String, Set<PluginResourceMeta>>of(
        "hadoop", ImmutableSet.<PluginResourceMeta>of(hadoop3),
        "mysql", ImmutableSet.<PluginResourceMeta>of(mysql2)),
      ImmutableMap.copyOf(view.getAll(PluginResourceStatus.ACTIVE))
    );
    Assert.assertEquals(Sets.newHashSet(hadoop3), view.getAll("hadoop", PluginResourceStatus.ACTIVE));
    Assert.assertEquals(Sets.newHashSet(mysql2), view.getAll("mysql", PluginResourceStatus.ACTIVE));
    Assert.assertTrue(view.getAll("apache", PluginResourceStatus.ACTIVE).isEmpty());

    // test get staged
    Assert.assertEquals(
      ImmutableMap.<String, Set<PluginResourceMeta>>of(
        "hadoop", ImmutableSet.<PluginResourceMeta>of(hadoop2),
        "mysql", ImmutableSet.<PluginResourceMeta>of(mysql1)),
      ImmutableMap.copyOf(view.getAll(PluginResourceStatus.STAGED))
    );
    Assert.assertEquals(Sets.newHashSet(hadoop2), view.getAll("hadoop", PluginResourceStatus.STAGED));
    Assert.assertEquals(Sets.newHashSet(mysql1), view.getAll("mysql", PluginResourceStatus.STAGED));
    Assert.assertTrue(view.getAll("apache", PluginResourceStatus.STAGED).isEmpty());

    // test get unstaged
    Assert.assertEquals(
      ImmutableMap.<String, Set<PluginResourceMeta>>of(
        "apache", ImmutableSet.<PluginResourceMeta>of(apache)),
      ImmutableMap.copyOf(view.getAll(PluginResourceStatus.UNSTAGED))
    );
    Assert.assertTrue(view.getAll("hadoop", PluginResourceStatus.UNSTAGED).isEmpty());
    Assert.assertTrue(view.getAll("mysql", PluginResourceStatus.UNSTAGED).isEmpty());
    Assert.assertEquals(Sets.newHashSet(apache), view.getAll("apache", PluginResourceStatus.UNSTAGED));

    // test get inactive
    Assert.assertEquals(
      ImmutableMap.<String, Set<PluginResourceMeta>>of(
        "hadoop", ImmutableSet.<PluginResourceMeta>of(hadoop1)),
      ImmutableMap.copyOf(view.getAll(PluginResourceStatus.INACTIVE))
    );
    Assert.assertEquals(Sets.newHashSet(hadoop1), view.getAll("hadoop", PluginResourceStatus.INACTIVE));
    Assert.assertTrue(view.getAll("mysql", PluginResourceStatus.INACTIVE).isEmpty());
    Assert.assertTrue(view.getAll("apache", PluginResourceStatus.INACTIVE).isEmpty());
  }

  @Test
  public void testStage() throws Exception {
    PluginResourceMetaStoreService service = getPluginResourceMetaStoreService();
    PluginResourceMetaStoreView view = service.getView(account1, type1);
    PluginResourceMeta hadoop1 =
      PluginResourceMeta.fromExisting(UUID.randomUUID().toString(), "hadoop", "1", PluginResourceStatus.INACTIVE);
    PluginResourceMeta hadoop2 =
      PluginResourceMeta.fromExisting(UUID.randomUUID().toString(), "hadoop", "2", PluginResourceStatus.UNSTAGED);
    PluginResourceMeta hadoop3 =
      PluginResourceMeta.fromExisting(UUID.randomUUID().toString(), "hadoop", "3", PluginResourceStatus.INACTIVE);
    PluginResourceMeta mysql1 =
      PluginResourceMeta.fromExisting(UUID.randomUUID().toString(), "mysql", "1", PluginResourceStatus.STAGED);
    PluginResourceMeta mysql2 =
      PluginResourceMeta.fromExisting(UUID.randomUUID().toString(), "mysql", "2", PluginResourceStatus.ACTIVE);

    view.write(hadoop1);
    view.write(hadoop2);
    view.write(hadoop3);
    view.write(mysql1);
    view.write(mysql2);

    // check no-ops
    view.stage(mysql1.getName(), mysql1.getVersion());
    Assert.assertEquals(PluginResourceStatus.STAGED, view.get(mysql1.getName(), mysql1.getVersion()).getStatus());
    Assert.assertEquals(PluginResourceStatus.ACTIVE, view.get(mysql2.getName(), mysql2.getVersion()).getStatus());

    view.stage(mysql2.getName(), mysql2.getVersion());
    Assert.assertEquals(PluginResourceStatus.STAGED, view.get(mysql1.getName(), mysql1.getVersion()).getStatus());
    Assert.assertEquals(PluginResourceStatus.ACTIVE, view.get(mysql2.getName(), mysql2.getVersion()).getStatus());

    // check staging an unstaged makes it active
    view.stage(hadoop2.getName(), hadoop2.getVersion());
    Assert.assertEquals(PluginResourceStatus.INACTIVE, view.get(hadoop1.getName(), hadoop1.getVersion()).getStatus());
    Assert.assertEquals(PluginResourceStatus.ACTIVE, view.get(hadoop2.getName(), hadoop2.getVersion()).getStatus());
    Assert.assertEquals(PluginResourceStatus.INACTIVE, view.get(hadoop3.getName(), hadoop3.getVersion()).getStatus());

    // check staging from inactive
    view.stage(hadoop1.getName(), hadoop1.getVersion());
    Assert.assertEquals(PluginResourceStatus.STAGED, view.get(hadoop1.getName(), hadoop1.getVersion()).getStatus());
    Assert.assertEquals(PluginResourceStatus.ACTIVE, view.get(hadoop2.getName(), hadoop2.getVersion()).getStatus());
    Assert.assertEquals(PluginResourceStatus.INACTIVE, view.get(hadoop3.getName(), hadoop3.getVersion()).getStatus());

    // check staging deactivates previous staged version
    view.stage(hadoop3.getName(), hadoop3.getVersion());
    Assert.assertEquals(PluginResourceStatus.INACTIVE, view.get(hadoop1.getName(), hadoop1.getVersion()).getStatus());
    Assert.assertEquals(PluginResourceStatus.ACTIVE, view.get(hadoop2.getName(), hadoop2.getVersion()).getStatus());
    Assert.assertEquals(PluginResourceStatus.STAGED, view.get(hadoop3.getName(), hadoop3.getVersion()).getStatus());
  }

  @Test
  public void testUnstage() throws Exception {
    PluginResourceMetaStoreService service = getPluginResourceMetaStoreService();
    PluginResourceMetaStoreView view = service.getView(account1, type1);
    PluginResourceMeta hadoop1 =
      PluginResourceMeta.fromExisting(UUID.randomUUID().toString(), "hadoop", "1", PluginResourceStatus.INACTIVE);
    PluginResourceMeta hadoop2 =
      PluginResourceMeta.fromExisting(UUID.randomUUID().toString(), "hadoop", "2", PluginResourceStatus.UNSTAGED);
    PluginResourceMeta hadoop3 =
      PluginResourceMeta.fromExisting(UUID.randomUUID().toString(), "hadoop", "3", PluginResourceStatus.INACTIVE);
    PluginResourceMeta mysql1 =
      PluginResourceMeta.fromExisting(UUID.randomUUID().toString(), "mysql", "1", PluginResourceStatus.STAGED);
    PluginResourceMeta mysql2 =
      PluginResourceMeta.fromExisting(UUID.randomUUID().toString(), "mysql", "2", PluginResourceStatus.ACTIVE);

    view.write(hadoop1);
    view.write(hadoop2);
    view.write(hadoop3);
    view.write(mysql1);
    view.write(mysql2);

    // check no-ops
    view.unstage(hadoop1.getName(), hadoop1.getVersion());
    Assert.assertEquals(PluginResourceStatus.INACTIVE, view.get(hadoop1.getName(), hadoop1.getVersion()).getStatus());
    Assert.assertEquals(PluginResourceStatus.UNSTAGED, view.get(hadoop2.getName(), hadoop2.getVersion()).getStatus());
    Assert.assertEquals(PluginResourceStatus.INACTIVE, view.get(hadoop3.getName(), hadoop3.getVersion()).getStatus());

    view.unstage(hadoop2.getName(), hadoop2.getVersion());
    Assert.assertEquals(PluginResourceStatus.INACTIVE, view.get(hadoop1.getName(), hadoop1.getVersion()).getStatus());
    Assert.assertEquals(PluginResourceStatus.UNSTAGED, view.get(hadoop2.getName(), hadoop2.getVersion()).getStatus());
    Assert.assertEquals(PluginResourceStatus.INACTIVE, view.get(hadoop3.getName(), hadoop3.getVersion()).getStatus());

    view.unstage(hadoop3.getName(), hadoop3.getVersion());
    Assert.assertEquals(PluginResourceStatus.INACTIVE, view.get(hadoop1.getName(), hadoop1.getVersion()).getStatus());
    Assert.assertEquals(PluginResourceStatus.UNSTAGED, view.get(hadoop2.getName(), hadoop2.getVersion()).getStatus());
    Assert.assertEquals(PluginResourceStatus.INACTIVE, view.get(hadoop3.getName(), hadoop3.getVersion()).getStatus());

    // check unstaging a staged resource deactivates it
    view.unstage(mysql1.getName(), mysql1.getVersion());
    Assert.assertEquals(PluginResourceStatus.INACTIVE, view.get(mysql1.getName(), mysql1.getVersion()).getStatus());
    Assert.assertEquals(PluginResourceStatus.ACTIVE, view.get(mysql2.getName(), mysql2.getVersion()).getStatus());

    // check unstaging an active moves it to unstaged
    view.unstage(mysql2.getName(), mysql2.getVersion());
    Assert.assertEquals(PluginResourceStatus.INACTIVE, view.get(mysql1.getName(), mysql1.getVersion()).getStatus());
    Assert.assertEquals(PluginResourceStatus.UNSTAGED, view.get(mysql2.getName(), mysql2.getVersion()).getStatus());
  }

  @Test
  public void testActivate() throws Exception {
    PluginResourceMetaStoreService service = getPluginResourceMetaStoreService();
    PluginResourceMetaStoreView view = service.getView(account1, type1);
    PluginResourceMeta hadoop1 =
      PluginResourceMeta.fromExisting(UUID.randomUUID().toString(), "hadoop", "1", PluginResourceStatus.INACTIVE);
    PluginResourceMeta hadoop2 =
      PluginResourceMeta.fromExisting(UUID.randomUUID().toString(), "hadoop", "2", PluginResourceStatus.STAGED);
    PluginResourceMeta hadoop3 =
      PluginResourceMeta.fromExisting(UUID.randomUUID().toString(), "hadoop", "3", PluginResourceStatus.ACTIVE);
    PluginResourceMeta mysql1 =
      PluginResourceMeta.fromExisting(UUID.randomUUID().toString(), "mysql", "1", PluginResourceStatus.INACTIVE);
    PluginResourceMeta mysql2 =
      PluginResourceMeta.fromExisting(UUID.randomUUID().toString(), "mysql", "2", PluginResourceStatus.STAGED);
    PluginResourceMeta apache1 =
      PluginResourceMeta.fromExisting(UUID.randomUUID().toString(), "apache", "1", PluginResourceStatus.INACTIVE);
    PluginResourceMeta apache2 =
      PluginResourceMeta.fromExisting(UUID.randomUUID().toString(), "apache", "2", PluginResourceStatus.UNSTAGED);

    view.write(hadoop1);
    view.write(hadoop2);
    view.write(hadoop3);
    view.write(mysql1);
    view.write(mysql2);
    view.write(apache1);
    view.write(apache2);

    // check staged becomes active and active becomes inactive
    view.activate(hadoop1.getName());
    Assert.assertEquals(PluginResourceStatus.INACTIVE, view.get(hadoop1.getName(), hadoop1.getVersion()).getStatus());
    Assert.assertEquals(PluginResourceStatus.ACTIVE, view.get(hadoop2.getName(), hadoop2.getVersion()).getStatus());
    Assert.assertEquals(PluginResourceStatus.INACTIVE, view.get(hadoop3.getName(), hadoop3.getVersion()).getStatus());

    // check staged becomes active
    view.activate(mysql1.getName());
    Assert.assertEquals(PluginResourceStatus.INACTIVE, view.get(mysql1.getName(), mysql1.getVersion()).getStatus());
    Assert.assertEquals(PluginResourceStatus.ACTIVE, view.get(mysql2.getName(), mysql2.getVersion()).getStatus());

    // check no-op
    view.activate(apache1.getName());
    Assert.assertEquals(PluginResourceStatus.INACTIVE, view.get(apache1.getName(), apache1.getVersion()).getStatus());
    Assert.assertEquals(PluginResourceStatus.UNSTAGED, view.get(apache2.getName(), apache2.getVersion()).getStatus());
  }
}
