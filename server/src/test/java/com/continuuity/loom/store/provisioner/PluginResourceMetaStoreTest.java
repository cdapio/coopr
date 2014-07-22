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
import com.continuuity.loom.provisioner.PluginResourceType;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;

import java.util.Set;

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
  public void testWriteDeleteExistsWithinAccount() throws Exception {
    PluginResourceMetaStoreService service = getPluginResourceMetaStoreService();
    PluginResourceMetaStoreView view = service.getView(account1, type1);
    PluginResourceMeta meta = new PluginResourceMeta("name", "version", false);

    view.write(meta);
    Assert.assertTrue(view.exists(meta));

    view.delete(meta);
    Assert.assertFalse(view.exists(meta));
  }

  @Test
  public void testAccountSeparation() throws Exception {
    PluginResourceMetaStoreService service = getPluginResourceMetaStoreService();
    PluginResourceMetaStoreView view1 = service.getView(account1, type1);
    PluginResourceMetaStoreView view2 = service.getView(account2, type1);
    PluginResourceMeta meta = new PluginResourceMeta("name", "version", false);

    view1.write(meta);
    Assert.assertTrue(view1.exists(meta));
    Assert.assertFalse(view2.exists(meta));

    view2.write(meta);
    Assert.assertTrue(view1.exists(meta));
    Assert.assertTrue(view2.exists(meta));

    view1.delete(meta);
    Assert.assertFalse(view1.exists(meta));
    Assert.assertTrue(view2.exists(meta));

    view2.delete(meta);
    Assert.assertFalse(view1.exists(meta));
    Assert.assertFalse(view2.exists(meta));
  }

  @Test
  public void testTypeSeparation() throws Exception {
    PluginResourceMetaStoreService service = getPluginResourceMetaStoreService();
    PluginResourceMetaStoreView view1 = service.getView(account1, type1);
    PluginResourceMetaStoreView view2 = service.getView(account1, type2);
    PluginResourceMeta meta = new PluginResourceMeta("name", "version", false);

    view1.write(meta);
    Assert.assertTrue(view1.exists(meta));
    Assert.assertFalse(view2.exists(meta));

    view2.write(meta);
    Assert.assertTrue(view1.exists(meta));
    Assert.assertTrue(view2.exists(meta));

    view1.delete(meta);
    Assert.assertFalse(view1.exists(meta));
    Assert.assertTrue(view2.exists(meta));

    view2.delete(meta);
    Assert.assertFalse(view1.exists(meta));
    Assert.assertFalse(view2.exists(meta));
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
    PluginResourceMeta hadoop1 = new PluginResourceMeta("hadoop", "1", false);
    PluginResourceMeta hadoop2 = new PluginResourceMeta("hadoop", "2", false);
    PluginResourceMeta hadoop3 = new PluginResourceMeta("hadoop", "3", true);
    PluginResourceMeta mysql1 = new PluginResourceMeta("mysql", "1", false);
    PluginResourceMeta mysql2 = new PluginResourceMeta("mysql", "2", true);

    Set<PluginResourceMeta> all = ImmutableSet.of(hadoop1, hadoop2, hadoop3, mysql1, mysql2);
    Set<PluginResourceMeta> hadoops = ImmutableSet.of(hadoop1, hadoop2, hadoop3);
    Set<PluginResourceMeta> mysqls = ImmutableSet.of(mysql1, mysql2);
    Set<PluginResourceMeta> allActive = ImmutableSet.of(hadoop3, mysql2);

    for (PluginResourceMeta meta : all) {
      view.write(meta);
    }
    Assert.assertEquals(
      ImmutableMap.<String, Set<PluginResourceMeta>>of(
        "hadoop", ImmutableSet.<PluginResourceMeta>of(hadoop1, hadoop2, hadoop3),
        "mysql", ImmutableSet.<PluginResourceMeta>of(mysql1, mysql2)),
      ImmutableMap.copyOf(view.getAll(false))
    );
    Assert.assertEquals(hadoops, ImmutableSet.copyOf(view.getAll("hadoop", false)));
    Assert.assertEquals(mysqls, ImmutableSet.copyOf(view.getAll("mysql", false)));
    Assert.assertEquals(
      ImmutableMap.<String, Set<PluginResourceMeta>>of(
        "hadoop", ImmutableSet.<PluginResourceMeta>of(hadoop3),
        "mysql", ImmutableSet.<PluginResourceMeta>of(mysql2)),
      ImmutableMap.copyOf(view.getAll(true))
    );
  }

  @Test
  public void testActivate() throws Exception {
    PluginResourceMetaStoreService service = getPluginResourceMetaStoreService();
    PluginResourceMetaStoreView view = service.getView(account1, type1);
    PluginResourceMeta hadoop2 = new PluginResourceMeta("hadoop", "2", false);
    PluginResourceMeta hadoop3 = new PluginResourceMeta("hadoop", "3", false);

    view.write(hadoop2);
    view.write(hadoop3);
    Assert.assertTrue(view.getAll(true).isEmpty());

    view.activate("hadoop", "2");
    hadoop2 = new PluginResourceMeta("hadoop", "2", true);
    Assert.assertEquals(1, view.getAll(true).size());
    Assert.assertEquals(hadoop2, view.getAll(true).get("hadoop").iterator().next());

    view.activate("hadoop", "3");
    hadoop3 = new PluginResourceMeta("hadoop", "3", true);
    Assert.assertEquals(1, view.getAll(true).size());
    Assert.assertEquals(hadoop3, view.getAll(true).get("hadoop").iterator().next());
  }

  @Test
  public void testDeactivate() throws Exception {
    PluginResourceMetaStoreService service = getPluginResourceMetaStoreService();
    PluginResourceMetaStoreView view = service.getView(account1, type1);
    PluginResourceMeta hadoop2 = new PluginResourceMeta("hadoop", "2", false);
    PluginResourceMeta hadoop3 = new PluginResourceMeta("hadoop", "3", true);
    PluginResourceMeta mysql1 = new PluginResourceMeta("mysql", "1", false);
    PluginResourceMeta mysql2 = new PluginResourceMeta("mysql", "2", true);

    view.write(hadoop2);
    view.write(hadoop3);
    view.write(mysql1);
    view.write(mysql2);
    Assert.assertEquals(2, view.getAll(true).size());

    view.deactivate("hadoop");
    Assert.assertEquals(1, view.getAll(true).size());
    Assert.assertEquals(mysql2, view.getAll(true).get("mysql").iterator().next());

    view.deactivate("mysql");
    Assert.assertTrue(view.getAll(true).isEmpty());
  }
}
