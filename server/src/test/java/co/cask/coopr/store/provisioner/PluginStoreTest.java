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
import co.cask.coopr.provisioner.plugin.ResourceType;
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
    ResourceType resourceType = new ResourceType(PluginType.AUTOMATOR, "chef-solo", "cookbooks");
    String name = "hadoop";
    int version = 1;

    writeToStore(store, account1, resourceType, name, version, contents);

    Assert.assertEquals(contents, readFromStore(store, account1, resourceType, name, version));
  }

  @Test
  public void testMultipleWrites() throws Exception {
    PluginStore store = getInitializedStore();
    String contents = "these are the first contents";
    ResourceType resourceType = new ResourceType(PluginType.AUTOMATOR, "chef-solo", "cookbooks");
    String name = "hadoop";
    int version = 1;

    // write once
    writeToStore(store, account1, resourceType, name, version, contents);
    // overwrite
    contents = "these are the second contents";
    writeToStore(store, account1, resourceType, name, version, contents);

    Assert.assertEquals(contents, readFromStore(store, account1, resourceType, name, version));
  }

  @Test
  public void testWriteDifferentVersions() throws Exception {
    PluginStore store = getInitializedStore();
    String contents1 = "v1 contents";
    String contents2 = "v2 contents";
    ResourceType resourceType = new ResourceType(PluginType.AUTOMATOR, "chef-solo", "cookbooks");
    String name = "hadoop";
    int version1 = 1;
    int version2 = 2;

    // write different versions of the same module
    writeToStore(store, account1, resourceType, name, version1, contents1);
    writeToStore(store, account1, resourceType, name, version2, contents2);

    // check both versions
    Assert.assertEquals(contents1, readFromStore(store, account1, resourceType, name, version1));
    Assert.assertEquals(contents2, readFromStore(store, account1, resourceType, name, version2));
  }

  @Test
  public void testWritesFromDifferentTenants() throws Exception {
    PluginStore store = getInitializedStore();
    String contents1 = "tenant1 contents";
    String contents2 = "tenant2 contents";
    ResourceType resourceType = new ResourceType(PluginType.AUTOMATOR, "chef-solo", "cookbooks");
    String name = "hadoop";
    int version = 1;

    // write the same module and version but to 2 different tenants
    writeToStore(store, account1, resourceType, name, version, contents1);
    writeToStore(store, account2, resourceType, name, version, contents2);

    // check both tenants
    Assert.assertEquals(contents1, readFromStore(store, account1, resourceType, name, version));
    Assert.assertEquals(contents2, readFromStore(store, account2, resourceType, name, version));
  }

  @Test
  public void testWriteDifferentModules() throws Exception {
    PluginStore store = getInitializedStore();
    String contents1 = "hadoop cookbook";
    String contents2 = "mysql cookbook";
    ResourceType resourceType = new ResourceType(PluginType.AUTOMATOR, "chef-solo", "cookbooks");
    String name1 = "hadoop";
    String name2 = "mysql";
    int version = 1;

    // write different versions of the same module
    writeToStore(store, account1, resourceType, name1, version, contents1);
    writeToStore(store, account1, resourceType, name2, version, contents2);

    // check both modules
    Assert.assertEquals(contents1, readFromStore(store, account1, resourceType, name1, version));
    Assert.assertEquals(contents2, readFromStore(store, account1, resourceType, name2, version));
  }

  @Test
  public void testDeleteWithinTenant() throws Exception {
    PluginStore store = getInitializedStore();
    String contents = "hadoop cookbook";
    ResourceType resourceType = new ResourceType(PluginType.AUTOMATOR, "chef-solo", "cookbooks");
    String name = "hadoop";
    int version = 1;

    // write different versions of the same module
    writeToStore(store, account1, resourceType, name, version, contents);

    // check it's there.
    Assert.assertEquals(contents, readFromStore(store, account1, resourceType, name, version));

    // delete and check there's nothing
    store.deleteResource(account1, resourceType, name, version);
    Assert.assertNull(store.getResourceInputStream(account1, resourceType, name, version));
  }

  @Test
  public void testDeleteOnlyAffectsTenant() throws Exception {
    PluginStore store = getInitializedStore();
    String contents = "hadoop cookbook";
    ResourceType resourceType = new ResourceType(PluginType.AUTOMATOR, "chef-solo", "cookbooks");
    String name = "hadoop";
    int version = 1;

    // write different versions of the same module
    writeToStore(store, account1, resourceType, name, version, contents);
    writeToStore(store, account2, resourceType, name, version, contents);

    // check it's there.
    Assert.assertEquals(contents, readFromStore(store, account1, resourceType, name, version));
    Assert.assertEquals(contents, readFromStore(store, account2, resourceType, name, version));

    // delete and check there's nothing for the tenant it was deleted from, but is still there for the other
    store.deleteResource(account1, resourceType, name, version);
    Assert.assertNull(store.getResourceInputStream(account1, resourceType, name, version));
    Assert.assertEquals(contents, readFromStore(store, account2, resourceType, name, version));
  }

  private void writeToStore(PluginStore store, Account account, ResourceType resourceType,
                            String name, int version, String content) throws IOException {
    OutputStream outputStream = store.getResourceOutputStream(account, resourceType, name, version);
    try {
      outputStream.write(content.getBytes(Charsets.UTF_8));
    } finally {
      outputStream.close();
    }
  }

  private String readFromStore(PluginStore store, Account account, ResourceType resourceType,
                               String name, int version) throws IOException {
    Reader reader = new InputStreamReader(
      store.getResourceInputStream(account, resourceType, name, version), Charsets.UTF_8);
    try {
      return CharStreams.toString(reader);
    } finally {
      reader.close();
    }
  }
}
