/*
 * Copyright © 2014 Cask Data, Inc.
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

package co.cask.coopr.store.credential;

import com.google.common.collect.ImmutableMap;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;

import java.util.Map;

/**
 *
 */
public abstract class CredentialStoreTest {

  protected abstract CredentialStore getRunningStore();

  protected abstract void wipeStore() throws Exception;

  @After
  public void cleanupTest() throws Exception {
    wipeStore();
  }

  @Test
  public void testGetSetWipe() throws Exception {
    CredentialStore store = getRunningStore();
    String tenant1 = "tenantX";
    String tenant2 = "tenantY";
    String cluster1 = "123";
    String cluster2 = "456";
    Map<String, Object> fields = ImmutableMap.<String, Object>of(
      "f1", "asdf",
      "f2", true,
      "f3", "kxzvjncv"
    );

    // shouldn't be able to get anything
    Assert.assertTrue(store.get(tenant1, cluster1).isEmpty());

    // test set and get
    store.set(tenant1, cluster1, fields);
    Assert.assertEquals(fields, store.get(tenant1, cluster1));

    // test different tenant and cluster returns nothing
    Assert.assertTrue(store.get(tenant2, cluster1).isEmpty());
    Assert.assertTrue(store.get(tenant1, cluster2).isEmpty());

    // test overwrite
    fields = ImmutableMap.<String, Object>of(
      "f1", "qwerty",
      "f2", true
    );
    store.set(tenant1, cluster1, fields);
    Assert.assertEquals(fields, store.get(tenant1, cluster1));

    // test wipe of others don't affect this tenant + cluster
    store.wipe(tenant2, cluster1);
    store.wipe(tenant1, cluster2);
    Assert.assertEquals(fields, store.get(tenant1, cluster1));

    // test wipe
    store.wipe(tenant1, cluster1);
    Assert.assertTrue(store.get(tenant1, cluster1).isEmpty());

    // test global wipe
    store.set(tenant1, cluster1, fields);
    store.set(tenant1, cluster2, fields);
    store.set(tenant2, cluster1, fields);
    store.wipe();
    Assert.assertTrue(store.get(tenant1, cluster1).isEmpty());
    Assert.assertTrue(store.get(tenant1, cluster2).isEmpty());
    Assert.assertTrue(store.get(tenant2, cluster1).isEmpty());
  }
}
