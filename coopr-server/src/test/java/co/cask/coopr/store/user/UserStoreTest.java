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
package co.cask.coopr.store.user;

import co.cask.coopr.account.Account;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.util.Map;

/**
 *
 */
public abstract class UserStoreTest {

  protected abstract UserStore getUserStore();

  protected abstract void clearState() throws Exception;

  @After
  public void cleanupTest() throws Exception {
    clearState();
  }

  @Test
  public void testGetWriteDelete() throws IOException {
    UserStore userStore = getUserStore();
    Account account = new Account("user1", "tenant1");
    // check null is returned if there is nothing
    Assert.assertNull(userStore.getProfile(account));

    // write a profile
    Map<String, Object> profile = Maps.newHashMap();
    profile.put("email", "user1@company.com");
    profile.put("attributes", ImmutableMap.of("attr1", "val1", "attr2", "val2"));
    profile.put("list", ImmutableList.of("item1", "item2", "item3"));
    userStore.writeProfile(account, profile);

    // check we can get the profile now
    Assert.assertEquals(profile, userStore.getProfile(account));

    // check delete
    userStore.deleteProfile(account);
    Assert.assertNull(userStore.getProfile(account));
  }
}
