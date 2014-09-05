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
package co.cask.coopr.common.queue.internal;

import co.cask.coopr.common.queue.Element;
import co.cask.coopr.common.queue.TrackingQueue;
import co.cask.coopr.common.zookeeper.BaseZKTest;
import com.google.common.collect.Iterators;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class ElementsTrackingQueueCliToolTest extends BaseZKTest {
  private String queueName;

  private ElementsTrackingQueue queue;

  @Before
  public void beforeCliTest() throws Exception {
    queueName = "/tracking-queue";
    queue = new ElementsTrackingQueue(new ZKElementsTracking(zkClient, queueName));
  }

  @Test
  public void testBasics() throws Exception {
    // "list" command executed below primarily for eye-balling ;)
    queue.add(new Element("elem1", "data1"));
    queue.add(new Element("elem2", "data2"));
    queue.add(new Element("elem3", "data3"));

    queue.take("consumer1");
    Assert.assertEquals(2, getQueuedCount(queue));
    Assert.assertEquals(1, Iterators.size(queue.getBeingConsumed()));

    String qOpts = " --zk-connection " + zkServer.getConnectionStr() + " --queue-name " + queueName;

    ElementsTrackingQueueCliTool.main(("list" + qOpts).split(" "));

    ElementsTrackingQueueCliTool.main(("remove --element elem1" + qOpts).split(" "));
    Assert.assertEquals(2, getQueuedCount(queue));
    Assert.assertEquals(0, Iterators.size(queue.getBeingConsumed()));

    ElementsTrackingQueueCliTool.main(("list " + qOpts).split(" "));

    // moving elem3 to top of the queue
    Assert.assertEquals("elem2", queue.getQueued().next().getElement().getId());
    ElementsTrackingQueueCliTool.main(("promote --element elem3" + qOpts).split(" "));
    Assert.assertEquals("elem3", queue.getQueued().next().getElement().getId());

    ElementsTrackingQueueCliTool.main(("list " + qOpts).split(" "));
    ElementsTrackingQueueCliTool.main(("remove --element elem2" + qOpts).split(" "));
    Assert.assertEquals(1, getQueuedCount(queue));
    Assert.assertEquals(0, Iterators.size(queue.getBeingConsumed()));
    ElementsTrackingQueueCliTool.main(("list " + qOpts).split(" "));
    ElementsTrackingQueueCliTool.main(("remove_all" + qOpts).split(" "));
    Assert.assertEquals(0, getQueuedCount(queue));
    Assert.assertEquals(0, Iterators.size(queue.getBeingConsumed()));
    ElementsTrackingQueueCliTool.main(("list " + qOpts).split(" "));
  }


  private static int getQueuedCount(TrackingQueue queue) {
    return Iterators.size(queue.getQueued());
  }
}
