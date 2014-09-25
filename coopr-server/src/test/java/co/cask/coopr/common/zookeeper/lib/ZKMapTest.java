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
package co.cask.coopr.common.zookeeper.lib;

import co.cask.coopr.common.zookeeper.BaseZKTest;
import com.google.common.base.Preconditions;
import org.junit.Assert;
import org.junit.Test;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

/**
 *
 */
public class ZKMapTest extends BaseZKTest {

  @Test(timeout = 10000)
  public void test() throws ExecutionException, InterruptedException {
    final String path = "/foo/map";

    ZKMap<String> map1 = new ZKMap<String>(zkClient, path, Serializers.stringSerializer());
    ZKMap<String> map2 = new ZKMap<String>(zkClient, path, Serializers.stringSerializer());
    Assert.assertEquals(0, map1.size());
    Assert.assertEquals(0, map2.size());

    map2.remove("foo");
    Assert.assertEquals(0, map1.size());
    Assert.assertEquals(0, map2.size());

    map1.put("key1", "value1");
    map2.put("key2", "value2");
    map1.put("key3", "value3");
    Assert.assertTrue(mapEventuallyEquals(map1, "key1", "value1", "key2", "value2", "key3", "value3"));
    Assert.assertTrue(mapEventuallyEquals(map2, "key1", "value1", "key2", "value2", "key3", "value3"));

    map1.put("key2", "value2_m");
    Assert.assertTrue(mapEventuallyEquals(map1, "key1", "value1", "key2", "value2_m", "key3", "value3"));
    Assert.assertTrue(mapEventuallyEquals(map2, "key1", "value1", "key2", "value2_m", "key3", "value3"));

    map2.remove("key2");
    Assert.assertTrue(mapEventuallyEquals(map1, "key1", "value1", "key3", "value3"));
    Assert.assertTrue(mapEventuallyEquals(map2, "key1", "value1", "key3", "value3"));
    Assert.assertNull(map1.get("key2"));
    Assert.assertNull(map2.get("key2"));

    // removing non-existed should be ok
    map1.remove("key2");
    Assert.assertTrue(mapEventuallyEquals(map1, "key1", "value1", "key3", "value3"));
    Assert.assertTrue(mapEventuallyEquals(map2, "key1", "value1", "key3", "value3"));
    Assert.assertNull(map1.get("key2"));
    Assert.assertNull(map2.get("key2"));

    map2.clear();
    Assert.assertTrue(mapEventuallyEquals(map1));
    Assert.assertTrue(mapEventuallyEquals(map2));
    Assert.assertNull(map1.get("key1"));
    Assert.assertNull(map2.get("key1"));

    // checking that we can clear an empty map
    map1.clear();
    Assert.assertEquals(0, map1.size());
    Assert.assertEquals(0, map2.size());
  }

  // ZKMap doesn't immediately sync up.
  private boolean mapEventuallyEquals(ZKMap<String> map, String... keyvals) throws InterruptedException,
    ExecutionException {
    Preconditions.checkArgument(keyvals.length % 2 == 0);

    int numRetries = 0;
    while (numRetries < 10) {
      if (mapEquals(map, keyvals)) {
        return true;
      }
      numRetries++;
      TimeUnit.MILLISECONDS.sleep(10);
    }
    return false;
  }

  private boolean mapEquals(ZKMap<String> map, String... keyvals) throws ExecutionException, InterruptedException {
    if (map.size() != keyvals.length / 2) {
      return false;
    }
    for (int i = 0; i < keyvals.length; i += 2) {
      String key = keyvals[i];
      String val = keyvals[i+1];
      if (!val.equals(map.get(key))) {
        return false;
      }
      if (!val.equals(map.getOrWait(key).get())) {
        return false;
      }
    }
    return true;
  }
}
