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
package co.cask.coopr.store.provisioner;

import co.cask.coopr.account.Account;
import co.cask.coopr.common.conf.Constants;
import co.cask.coopr.provisioner.plugin.PluginType;
import co.cask.coopr.provisioner.plugin.ResourceCollection;
import co.cask.coopr.provisioner.plugin.ResourceMeta;
import co.cask.coopr.provisioner.plugin.ResourceStatus;
import co.cask.coopr.provisioner.plugin.ResourceType;
import co.cask.coopr.spec.plugin.ResourceTypeFormat;
import co.cask.coopr.spec.plugin.ResourceTypeSpecification;
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
  ResourceType type1 = new ResourceType(PluginType.AUTOMATOR, "chef-solo", "cookbooks");
  ResourceType type2 = new ResourceType(PluginType.PROVIDER, "openstack", "keys");
  Account account1 = new Account(Constants.ADMIN_USER, "tenant1");
  Account account2 = new Account(Constants.ADMIN_USER, "tenant2");

  abstract PluginMetaStoreService getPluginResourceMetaStoreService() throws Exception;

  abstract void clearData() throws Exception;

  @After
  public void cleanupTest() throws Exception {
    clearData();
  }

  @Test
  public void testGetNumResources() throws Exception {
    PluginMetaStoreService service = getPluginResourceMetaStoreService();
    // for account1 write 6 resources (7 but one is deleted) in all different states
    service.getResourceTypeView(account1, type1).add(new ResourceMeta("r1", 1, ResourceStatus.ACTIVE));
    service.getResourceTypeView(account1, type1).add(new ResourceMeta("r1", 2, ResourceStatus.INACTIVE));
    service.getResourceTypeView(account1, type1).add(new ResourceMeta("r2", 1, ResourceStatus.INACTIVE));
    service.getResourceTypeView(account1, type1).add(new ResourceMeta("r2", 2, ResourceStatus.STAGED));
    service.getResourceTypeView(account1, type2).add(new ResourceMeta("r3", 1, ResourceStatus.RECALLED));
    service.getResourceTypeView(account1, type2).add(new ResourceMeta("r3", 2, ResourceStatus.STAGED));
    service.getResourceTypeView(account1, type2).add(new ResourceMeta("r3", 3, ResourceStatus.STAGED));
    service.getResourceTypeView(account1, type2).delete("r3", 3);

    Assert.assertEquals(6, service.getAccountView(account1).numResources());
    // account 2 should have nothing
    Assert.assertEquals(0, service.getAccountView(account2).numResources());
  }

  @Test
  public void testWriteDeleteExistsGetWithinAccount() throws Exception {
    PluginMetaStoreService service = getPluginResourceMetaStoreService();
    PluginResourceTypeView view = service.getResourceTypeView(account1, type1);
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
    PluginResourceTypeView view1 = service.getResourceTypeView(account1, type1);
    PluginResourceTypeView view2 = service.getResourceTypeView(account2, type1);
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
    PluginResourceTypeView view1 = service.getResourceTypeView(account1, type1);
    PluginResourceTypeView view2 = service.getResourceTypeView(account1, type2);
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
    service.getResourceTypeView(new Account("notadmin", "tenant"),
                                new ResourceType(PluginType.AUTOMATOR, "chef-solo", "cookbooks"));
  }

  @Test
  public void testGetAll() throws Exception {
    PluginMetaStoreService service = getPluginResourceMetaStoreService();
    PluginResourceTypeView view = service.getResourceTypeView(account1, type1);
    ResourceMeta hadoop1 = new ResourceMeta("hadoop", 1, ResourceStatus.INACTIVE);
    ResourceMeta hadoop2 = new ResourceMeta("hadoop", 2, ResourceStatus.STAGED);
    ResourceMeta hadoop3 = new ResourceMeta("hadoop", 3, ResourceStatus.ACTIVE);
    ResourceMeta mysql1 = new ResourceMeta("mysql", 1, ResourceStatus.STAGED);
    ResourceMeta mysql2 = new ResourceMeta("mysql", 2, ResourceStatus.ACTIVE);
    ResourceMeta apache = new ResourceMeta("apache", 1, ResourceStatus.RECALLED);

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

    // test get recalled
    Assert.assertEquals(
      ImmutableMap.<String, Set<ResourceMeta>>of(
        "apache", ImmutableSet.<ResourceMeta>of(apache)),
      ImmutableMap.copyOf(view.getAll(ResourceStatus.RECALLED))
    );
    Assert.assertTrue(view.getAll("hadoop", ResourceStatus.RECALLED).isEmpty());
    Assert.assertTrue(view.getAll("mysql", ResourceStatus.RECALLED).isEmpty());
    Assert.assertEquals(Sets.newHashSet(apache), view.getAll("apache", ResourceStatus.RECALLED));

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
    PluginResourceTypeView view = service.getResourceTypeView(account1, type1);
    ResourceMeta hadoop1 = new ResourceMeta("hadoop", 1, ResourceStatus.INACTIVE);
    ResourceMeta hadoop2 = new ResourceMeta("hadoop", 2, ResourceStatus.RECALLED);
    ResourceMeta hadoop3 = new ResourceMeta("hadoop", 3, ResourceStatus.INACTIVE);
    ResourceMeta mysql = new ResourceMeta("mysql", 1, ResourceStatus.STAGED);
    ResourceMeta apache = new ResourceMeta("apache", 1, ResourceStatus.ACTIVE);
    ResourceMeta php1 = new ResourceMeta("php", 1, ResourceStatus.ACTIVE);
    ResourceMeta php2 = new ResourceMeta("php", 2, ResourceStatus.INACTIVE);

    view.add(hadoop1);
    view.add(hadoop2);
    view.add(hadoop3);
    view.add(mysql);
    view.add(apache);
    view.add(php1);
    view.add(php2);

    // check no-ops
    view.stage(mysql.getName(), mysql.getVersion());
    Assert.assertEquals(ResourceStatus.STAGED, view.get(mysql.getName(), mysql.getVersion()).getStatus());

    view.stage(apache.getName(), apache.getVersion());
    Assert.assertEquals(ResourceStatus.ACTIVE, view.get(apache.getName(), apache.getVersion()).getStatus());

    // check staging a recalled makes it active
    view.stage(hadoop2.getName(), hadoop2.getVersion());
    Assert.assertEquals(ResourceStatus.INACTIVE, view.get(hadoop1.getName(), hadoop1.getVersion()).getStatus());
    Assert.assertEquals(ResourceStatus.ACTIVE, view.get(hadoop2.getName(), hadoop2.getVersion()).getStatus());
    Assert.assertEquals(ResourceStatus.INACTIVE, view.get(hadoop3.getName(), hadoop3.getVersion()).getStatus());

    // check staging from inactive
    view.stage(hadoop1.getName(), hadoop1.getVersion());
    Assert.assertEquals(ResourceStatus.STAGED, view.get(hadoop1.getName(), hadoop1.getVersion()).getStatus());
    Assert.assertEquals(ResourceStatus.RECALLED, view.get(hadoop2.getName(), hadoop2.getVersion()).getStatus());
    Assert.assertEquals(ResourceStatus.INACTIVE, view.get(hadoop3.getName(), hadoop3.getVersion()).getStatus());

    // check staging deactivates previous staged version
    view.stage(hadoop3.getName(), hadoop3.getVersion());
    Assert.assertEquals(ResourceStatus.INACTIVE, view.get(hadoop1.getName(), hadoop1.getVersion()).getStatus());
    Assert.assertEquals(ResourceStatus.RECALLED, view.get(hadoop2.getName(), hadoop2.getVersion()).getStatus());
    Assert.assertEquals(ResourceStatus.STAGED, view.get(hadoop3.getName(), hadoop3.getVersion()).getStatus());
  }

  @Test
  public void testStageOnNothingIsNoOp() throws Exception {
    PluginMetaStoreService service = getPluginResourceMetaStoreService();
    PluginResourceTypeView view = service.getResourceTypeView(account1, type1);
    ResourceMeta hadoop = new ResourceMeta("hadoop", 1, ResourceStatus.STAGED);
    view.add(hadoop);
    // if we stage a non-existent version, the current staged version should not be affected
    view.stage(hadoop.getName(), hadoop.getVersion() + 1);
    Assert.assertEquals(ResourceStatus.STAGED, view.get(hadoop.getName(), hadoop.getVersion()).getStatus());
  }

  @Test
  public void testRecall() throws Exception {
    PluginMetaStoreService service = getPluginResourceMetaStoreService();
    PluginResourceTypeView view = service.getResourceTypeView(account1, type1);
    ResourceMeta hadoop1 = new ResourceMeta("hadoop", 1, ResourceStatus.INACTIVE);
    ResourceMeta hadoop2 = new ResourceMeta("hadoop", 2, ResourceStatus.RECALLED);
    ResourceMeta hadoop3 = new ResourceMeta("hadoop", 3, ResourceStatus.INACTIVE);
    ResourceMeta mysql1 = new ResourceMeta("mysql", 1, ResourceStatus.STAGED);
    ResourceMeta mysql2 = new ResourceMeta("mysql", 2, ResourceStatus.ACTIVE);

    view.add(hadoop1);
    view.add(hadoop2);
    view.add(hadoop3);
    view.add(mysql1);
    view.add(mysql2);

    // check no-ops
    view.recall(hadoop1.getName(), hadoop1.getVersion());
    Assert.assertEquals(ResourceStatus.INACTIVE, view.get(hadoop1.getName(), hadoop1.getVersion()).getStatus());
    Assert.assertEquals(ResourceStatus.RECALLED, view.get(hadoop2.getName(), hadoop2.getVersion()).getStatus());
    Assert.assertEquals(ResourceStatus.INACTIVE, view.get(hadoop3.getName(), hadoop3.getVersion()).getStatus());

    view.recall(hadoop2.getName(), hadoop2.getVersion());
    Assert.assertEquals(ResourceStatus.INACTIVE, view.get(hadoop1.getName(), hadoop1.getVersion()).getStatus());
    Assert.assertEquals(ResourceStatus.RECALLED, view.get(hadoop2.getName(), hadoop2.getVersion()).getStatus());
    Assert.assertEquals(ResourceStatus.INACTIVE, view.get(hadoop3.getName(), hadoop3.getVersion()).getStatus());

    view.recall(hadoop3.getName(), hadoop3.getVersion());
    Assert.assertEquals(ResourceStatus.INACTIVE, view.get(hadoop1.getName(), hadoop1.getVersion()).getStatus());
    Assert.assertEquals(ResourceStatus.RECALLED, view.get(hadoop2.getName(), hadoop2.getVersion()).getStatus());
    Assert.assertEquals(ResourceStatus.INACTIVE, view.get(hadoop3.getName(), hadoop3.getVersion()).getStatus());

    // check recalling a staged resource deactivates it
    view.recall(mysql1.getName(), mysql1.getVersion());
    Assert.assertEquals(ResourceStatus.INACTIVE, view.get(mysql1.getName(), mysql1.getVersion()).getStatus());
    Assert.assertEquals(ResourceStatus.ACTIVE, view.get(mysql2.getName(), mysql2.getVersion()).getStatus());

    // check recalling an active moves it to recalled
    view.recall(mysql2.getName(), mysql2.getVersion());
    Assert.assertEquals(ResourceStatus.INACTIVE, view.get(mysql1.getName(), mysql1.getVersion()).getStatus());
    Assert.assertEquals(ResourceStatus.RECALLED, view.get(mysql2.getName(), mysql2.getVersion()).getStatus());
  }

  @Test
  public void testSyncStatus() throws Exception {
    PluginMetaStoreService service = getPluginResourceMetaStoreService();
    PluginResourceTypeView view1 = service.getResourceTypeView(account1, type1);
    PluginResourceTypeView view2 = service.getResourceTypeView(account1, type2);
    ResourceMeta hadoop1 = new ResourceMeta("hadoop", 1, ResourceStatus.INACTIVE);
    ResourceMeta hadoop2 = new ResourceMeta("hadoop", 2, ResourceStatus.STAGED);
    ResourceMeta hadoop3 = new ResourceMeta("hadoop", 3, ResourceStatus.RECALLED);
    ResourceMeta mysql1 = new ResourceMeta("mysql", 1, ResourceStatus.INACTIVE);
    ResourceMeta mysql2 = new ResourceMeta("mysql", 2, ResourceStatus.STAGED);
    ResourceMeta apache1 = new ResourceMeta("apache", 1, ResourceStatus.INACTIVE);
    ResourceMeta apache2 = new ResourceMeta("apache", 2, ResourceStatus.RECALLED);

    view1.add(hadoop1);
    view1.add(hadoop2);
    view1.add(hadoop3);
    view1.add(mysql1);
    view1.add(mysql2);
    view1.add(apache1);
    view1.add(apache2);

    ResourceMeta bob1 = new ResourceMeta("bob", 1, ResourceStatus.INACTIVE);
    ResourceMeta bob2 = new ResourceMeta("bob", 2, ResourceStatus.STAGED);
    ResourceMeta sally1 = new ResourceMeta("sally", 1, ResourceStatus.ACTIVE);
    ResourceMeta sue1 = new ResourceMeta("sue", 1, ResourceStatus.RECALLED);

    view2.add(bob1);
    view2.add(bob2);
    view2.add(sally1);
    view2.add(sue1);

    ResourceCollection syncedResources = new ResourceCollection();
    syncedResources.addResources(type1, new ResourceTypeSpecification(ResourceTypeFormat.ARCHIVE, null),
                                 ImmutableSet.of(hadoop2, mysql2));
    syncedResources.addResources(type2, new ResourceTypeSpecification(ResourceTypeFormat.FILE, "400"),
                                 ImmutableSet.of(bob2, sally1));
    service.getAccountView(account1).syncResources(syncedResources);

    // inactive should stay inactive
    Assert.assertEquals(ResourceStatus.INACTIVE, view1.get(hadoop1.getName(), hadoop1.getVersion()).getStatus());
    // staged should become active
    Assert.assertEquals(ResourceStatus.ACTIVE, view1.get(hadoop2.getName(), hadoop2.getVersion()).getStatus());
    // recalled should become inactive
    Assert.assertEquals(ResourceStatus.INACTIVE, view1.get(hadoop3.getName(), hadoop3.getVersion()).getStatus());

    // inactive should stay inactive
    Assert.assertEquals(ResourceStatus.INACTIVE, view1.get(mysql1.getName(), mysql1.getVersion()).getStatus());
    // staged should become active
    Assert.assertEquals(ResourceStatus.ACTIVE, view1.get(mysql2.getName(), mysql2.getVersion()).getStatus());

    // inactive should stay inactive
    Assert.assertEquals(ResourceStatus.INACTIVE, view1.get(apache1.getName(), apache1.getVersion()).getStatus());
    // recalled should become inactive
    Assert.assertEquals(ResourceStatus.INACTIVE, view1.get(apache2.getName(), apache2.getVersion()).getStatus());

    // check other type
    // inactive should stay inactive
    Assert.assertEquals(ResourceStatus.INACTIVE, view2.get(bob1.getName(), bob1.getVersion()).getStatus());
    // stage should become active
    Assert.assertEquals(ResourceStatus.ACTIVE, view2.get(bob2.getName(), bob2.getVersion()).getStatus());
    // active should stay active
    Assert.assertEquals(ResourceStatus.ACTIVE, view2.get(sally1.getName(), sally1.getVersion()).getStatus());
    // recalled should become inactive
    Assert.assertEquals(ResourceStatus.INACTIVE, view2.get(sue1.getName(), sue1.getVersion()).getStatus());
  }
}
