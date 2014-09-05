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

import org.apache.twill.internal.zookeeper.InMemoryZKServer;
import org.apache.twill.zookeeper.ZKClientService;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;

import java.io.IOException;

/**
 *
 */
public class ElementsTrackingQueueZkTest extends ElementsTrackingQueueTestBase {
  @Rule
  public TemporaryFolder tmpFolder = new TemporaryFolder();
  private InMemoryZKServer zkServer;
  private ZKClientService zkClient;

  private ElementsTrackingQueue queue;

  @Before
  public void before() throws IOException {
    zkServer = InMemoryZKServer.builder().setDataDir(tmpFolder.newFolder()).setTickTime(1000).build();
    zkServer.startAndWait();

    zkClient = ZKClientService.Builder.of(zkServer.getConnectionStr()).build();
    zkClient.startAndWait();
  }

  @After
  public void after() {
    zkClient.stopAndWait();
    zkServer.stopAndWait();
  }

  @Override
  protected ElementsTrackingQueue getQueue() throws Exception {
    String queueName = "/tracking-queue";
    System.out.println("queue name: " + queueName);
    queue = new ElementsTrackingQueue(new ZKElementsTracking(zkClient, queueName));
    return queue;
  }
}
