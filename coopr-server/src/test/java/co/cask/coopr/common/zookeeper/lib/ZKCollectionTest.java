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
import com.google.common.collect.Lists;
import org.junit.Assert;
import org.junit.Test;

import java.util.Collection;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

/**
 *
 */
public class ZKCollectionTest extends BaseZKTest {

  @Test(timeout = 60000)
  public void test() throws ExecutionException, InterruptedException {
    final String path = "/foo/collection";

    ZKCollection<String> collection1 = new ZKCollection<String>(zkClient, path, Serializers.stringSerializer());
    ZKCollection<String> collection2 = new ZKCollection<String>(zkClient, path, Serializers.stringSerializer());
    Assert.assertEquals(0, collection1.size());
    Assert.assertEquals(0, collection2.size());

    collection1.remove("foo");
    Assert.assertEquals(0, collection1.size());
    Assert.assertEquals(0, collection2.size());

    collection1.add("value1");
    collection2.add("value2");
    collection1.add("value3");
    Assert.assertTrue(collectionEventuallyEquals(collection1, "value1", "value2", "value3"));
    Assert.assertTrue(collectionEventuallyEquals(collection2, "value1", "value2", "value3"));

    collection1.remove("value2");
    Assert.assertTrue(collectionEventuallyEquals(collection1, "value1", "value3"));
    Assert.assertTrue(collectionEventuallyEquals(collection2, "value1", "value3"));

    // should be ok to remove non-existed
    collection1.remove("value2");
    Assert.assertTrue(collectionEventuallyEquals(collection1, "value1", "value3"));
    Assert.assertTrue(collectionEventuallyEquals(collection2, "value1", "value3"));

    collection2.add("value4");
    Assert.assertTrue(collectionEventuallyEquals(collection1, "value1", "value3", "value4"));
    Assert.assertTrue(collectionEventuallyEquals(collection2, "value1", "value3", "value4"));

    // should be ok to add same item
    collection1.add("value4");
    Assert.assertTrue(collectionEventuallyEquals(collection1, "value1", "value3", "value4", "value4"));
    Assert.assertTrue(collectionEventuallyEquals(collection2, "value1", "value3", "value4", "value4"));

    collection2.clear();
    Assert.assertTrue(collectionEventuallyEquals(collection1));
    Assert.assertTrue(collectionEventuallyEquals(collection2));

    // checking that we can clear an empty collection
    collection1.clear();
    Assert.assertEquals(0, collection1.size());
    Assert.assertEquals(0, collection2.size());
  }

  // When adding to a ZKCollection, the local view gets updated right away, but another view may not get updated
  // right away.  So if the collection may not be as we expect right away, but should get there eventually
  private boolean collectionEventuallyEquals(Collection<String> all, String... values)
    throws InterruptedException {
    int numRetries = 0;
    while (numRetries < 10) {
      if (checkCollectionEquals(all, values)) {
        return true;
      }
      numRetries++;
      TimeUnit.MILLISECONDS.sleep(10);
    }
    return false;
  }

  private boolean checkCollectionEquals(Collection<String> all, String... values) {
    Collection<String> copy = Lists.newArrayList(all);
    if (values.length != copy.size()) {
      return false;
    }
    for (String value : values) {
      copy.remove(value);
    }
    return copy.isEmpty();
  }
}
