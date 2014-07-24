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
import com.continuuity.loom.provisioner.plugin.PluginResourceMeta;
import com.continuuity.loom.provisioner.plugin.PluginResourceType;
import com.continuuity.loom.provisioner.plugin.PluginType;
import com.google.common.base.Charsets;
import com.google.common.io.CharStreams;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;

/**
 *
 */
public abstract class PluginStoreTest {
  private final Account account1 = new Account(Constants.ADMIN_USER, "tenant1");
  private final Account account2 = new Account(Constants.ADMIN_USER, "tenant2");

  abstract PluginStore getInitializedStore() throws Exception;

  abstract void clearData();

  @After
  public void cleanupTest() {
    clearData();
  }

  @Test
  public void testBasicInputOutputStreams() throws Exception {
    PluginStore store = getInitializedStore();

    String contents = "this is the cookbook\nthis is the second line";
    PluginResourceType resourceType = new PluginResourceType(PluginType.AUTOMATOR, "chef-solo", "cookbooks");
    PluginResourceMeta resourceMeta = new PluginResourceMeta("hadoop", 1);

    writeToStore(store, account1, resourceType, resourceMeta, contents);

    Assert.assertEquals(contents, readFromStore(store, account1, resourceType, resourceMeta));
  }

  @Test
  public void testMultipleWrites() throws Exception {
    PluginStore store = getInitializedStore();

    String contents = "these are the first contents";
    PluginResourceType resourceType = new PluginResourceType(PluginType.AUTOMATOR, "chef-solo", "cookbooks");
    PluginResourceMeta resourceMeta = new PluginResourceMeta("hadoop", 1);

    // write once
    writeToStore(store, account1, resourceType, resourceMeta, contents);
    // overwrite
    contents = "these are the second contents";
    writeToStore(store, account1, resourceType, resourceMeta, contents);

    Assert.assertEquals(contents, readFromStore(store, account1, resourceType, resourceMeta));
  }

  @Test
  public void testWriteDifferentVersions() throws Exception {
    PluginStore store = getInitializedStore();

    String contents1 = "v1 contents";
    String contents2 = "v2 contents";
    PluginResourceType resourceType = new PluginResourceType(PluginType.AUTOMATOR, "chef-solo", "cookbooks");
    PluginResourceMeta resourceMeta1 = new PluginResourceMeta("hadoop", 1);
    PluginResourceMeta resourceMeta2 = new PluginResourceMeta("hadoop", 2);

    // write different versions of the same module
    writeToStore(store, account1, resourceType, resourceMeta1, contents1);
    writeToStore(store, account1, resourceType, resourceMeta2, contents2);

    // check both versions
    Assert.assertEquals(contents1, readFromStore(store, account1, resourceType, resourceMeta1));
    Assert.assertEquals(contents2, readFromStore(store, account1, resourceType, resourceMeta2));
  }

  @Test
  public void testWritesFromDifferentTenants() throws Exception {
    PluginStore store = getInitializedStore();

    String contents1 = "tenant1 contents";
    String contents2 = "tenant2 contents";
    PluginResourceType resourceType = new PluginResourceType(PluginType.AUTOMATOR, "chef-solo", "cookbooks");
    PluginResourceMeta resourceMeta = new PluginResourceMeta("hadoop", 1);

    // write the same module and version but to 2 different tenants
    writeToStore(store, account1, resourceType, resourceMeta, contents1);
    writeToStore(store, account2, resourceType, resourceMeta, contents2);

    // check both tenants
    Assert.assertEquals(contents1, readFromStore(store, account1, resourceType, resourceMeta));
    Assert.assertEquals(contents2, readFromStore(store, account2, resourceType, resourceMeta));
  }

  @Test
  public void testWriteDifferentModules() throws Exception {
    PluginStore store = getInitializedStore();

    String contents1 = "hadoop cookbook";
    String contents2 = "mysql cookbook";
    PluginResourceType resourceType = new PluginResourceType(PluginType.AUTOMATOR, "chef-solo", "cookbooks");
    PluginResourceMeta resourceMeta1 = new PluginResourceMeta("hadoop", 1);
    PluginResourceMeta resourceMeta2 = new PluginResourceMeta("mysql", 1);

    // write different versions of the same module
    writeToStore(store, account1, resourceType, resourceMeta1, contents1);
    writeToStore(store, account1, resourceType, resourceMeta2, contents2);

    // check both modules
    Assert.assertEquals(contents1, readFromStore(store, account1, resourceType, resourceMeta1));
    Assert.assertEquals(contents2, readFromStore(store, account1, resourceType, resourceMeta2));
  }

  @Test
  public void testDeleteWithinTenant() throws Exception {
    PluginStore store = getInitializedStore();

    String contents = "hadoop cookbook";
    PluginResourceType resourceType = new PluginResourceType(PluginType.AUTOMATOR, "chef-solo", "cookbooks");
    PluginResourceMeta resourceMeta = new PluginResourceMeta("hadoop", 1);

    // write different versions of the same module
    writeToStore(store, account1, resourceType, resourceMeta, contents);

    // check it's there.
    Assert.assertEquals(contents, readFromStore(store, account1, resourceType, resourceMeta));

    // delete and check there's nothing
    store.deleteResource(account1, resourceType, resourceMeta);
    Assert.assertNull(store.getResourceInputStream(account1, resourceType, resourceMeta));
  }

  @Test
  public void testDeleteOnlyAffectsTenant() throws Exception {
    PluginStore store = getInitializedStore();

    String contents = "hadoop cookbook";
    PluginResourceType resourceType = new PluginResourceType(PluginType.AUTOMATOR, "chef-solo", "cookbooks");
    PluginResourceMeta resourceMeta = new PluginResourceMeta("hadoop", 1);

    // write different versions of the same module
    writeToStore(store, account1, resourceType, resourceMeta, contents);
    writeToStore(store, account2, resourceType, resourceMeta, contents);

    // check it's there.
    Assert.assertEquals(contents, readFromStore(store, account1, resourceType, resourceMeta));
    Assert.assertEquals(contents, readFromStore(store, account2, resourceType, resourceMeta));

    // delete and check there's nothing for the tenant it was deleted from, but is still there for the other
    store.deleteResource(account1, resourceType, resourceMeta);
    Assert.assertNull(store.getResourceInputStream(account1, resourceType, resourceMeta));
    Assert.assertEquals(contents, readFromStore(store, account2, resourceType, resourceMeta));
  }

  private void writeToStore(PluginStore store, Account account, PluginResourceType resourceType,
                            PluginResourceMeta resourceMeta, String content) throws IOException {
    OutputStream outputStream = store.getResourceOutputStream(account, resourceType, resourceMeta);
    try {
      outputStream.write(content.getBytes(Charsets.UTF_8));
    } finally {
      outputStream.close();
    }
  }

  private String readFromStore(PluginStore store, Account account, PluginResourceType resourceType,
                               PluginResourceMeta resourceMeta) throws IOException {
    Reader reader = new InputStreamReader(
      store.getResourceInputStream(account, resourceType, resourceMeta), Charsets.UTF_8);
    try {
      return CharStreams.toString(reader);
    } finally {
      reader.close();
    }
  }
}
