package com.continuuity.loom.common.queue.internal;

import com.continuuity.loom.common.queue.QueueGroup;
import com.continuuity.loom.common.queue.QueueType;
import org.apache.twill.internal.zookeeper.InMemoryZKServer;
import org.apache.twill.zookeeper.ZKClientService;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.rules.TemporaryFolder;

import java.io.IOException;

/**
 *
 */
public class ZKQueueGroupTest extends QueueGroupTest {
  private InMemoryZKServer zkServer;
  private ZKClientService zkClient;

  @ClassRule
  public static TemporaryFolder tmpFolder = new TemporaryFolder();

  @Before
  public void before() throws IOException {
    zkServer = InMemoryZKServer.builder().setDataDir(tmpFolder.newFolder()).setTickTime(2000).build();
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
  QueueGroup getQueueGroup(QueueType type) {
    return new ZKQueueGroup(zkClient, type);
  }
}
