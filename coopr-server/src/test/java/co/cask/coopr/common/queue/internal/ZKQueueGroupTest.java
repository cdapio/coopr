package co.cask.coopr.common.queue.internal;

import co.cask.coopr.common.queue.Element;
import co.cask.coopr.common.queue.GroupElement;
import co.cask.coopr.common.queue.QueueGroup;
import co.cask.coopr.common.queue.QueueType;
import co.cask.coopr.common.queue.guice.QueueModule;
import co.cask.coopr.common.zookeeper.guice.ZookeeperModule;
import com.google.common.collect.Sets;
import com.google.inject.Guice;
import com.google.inject.Injector;
import org.apache.twill.internal.zookeeper.InMemoryZKServer;
import org.apache.twill.zookeeper.ZKClientService;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.IOException;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 *
 */
public class ZKQueueGroupTest extends QueueGroupTest {
  private InMemoryZKServer zkServer;
  private ZKClientService zkClient;
  private ZKQueueService zkQueueGroupService;

  @ClassRule
  public static TemporaryFolder tmpFolder = new TemporaryFolder();

  @Before
  public void before() throws IOException {
    zkServer = InMemoryZKServer.builder().setDataDir(tmpFolder.newFolder()).setTickTime(30000).build();
    zkServer.startAndWait();

    zkClient = ZKClientService.Builder.of(zkServer.getConnectionStr()).build();
    zkClient.startAndWait();

    Injector injector = Guice.createInjector(
      new ZookeeperModule(zkClient),
      new QueueModule(zkClient)
    );
    zkQueueGroupService = injector.getInstance(ZKQueueService.class);
    zkQueueGroupService.startAndWait();
  }

  @After
  public void after() {
    zkClient.stopAndWait();
    zkServer.stopAndWait();
  }

  @Override
  QueueGroup getQueueGroup(QueueType type) {
    return zkQueueGroupService.getQueueGroup(type);
  }

  @Test
  public void testChangesSeenAcrossInstances() throws Exception {
    QueueGroup instance1 = new ZKQueueGroup(zkClient, QueueType.PROVISIONER);
    QueueGroup instance2 = new ZKQueueGroup(zkClient, QueueType.PROVISIONER);
    instance1.startAndWait();
    instance2.startAndWait();

    // add a queue for tenant3 with 2 elements
    String tenant = "tenantX";
    Set<String> expectedQueueNames = Sets.newHashSet(tenant);
    instance1.add(tenant, new Element("id3-1", "val1"));
    instance1.add(tenant, new Element("id3-2", "val2"));
    // check both instances see tenant3
    Assert.assertEquals(expectedQueueNames, instance1.getQueueNames());
    waitForQueueNames(expectedQueueNames, instance2);

    // make sure each instance gets an accurate picture of the queue
    Iterator<GroupElement> queuesIter1 = instance1.takeIterator("consumer1");
    Iterator<GroupElement> queuesIter2 = instance1.takeIterator("consumer2");
    GroupElement gelement = queuesIter1.next();
    Assert.assertEquals(tenant, gelement.getQueueName());
    Assert.assertEquals("id3-1", gelement.getElement().getId());
    Assert.assertEquals("val1", gelement.getElement().getValue());
    gelement = queuesIter2.next();
    Assert.assertEquals(tenant, gelement.getQueueName());
    Assert.assertEquals("id3-2", gelement.getElement().getId());
    Assert.assertEquals("val2", gelement.getElement().getValue());
    Assert.assertFalse(queuesIter1.hasNext());
    Assert.assertFalse(queuesIter2.hasNext());
    instance1.stop();
    instance2.stop();
  }

  @Test
  public void testInstanceInitializedWithExistingData() throws Exception {
    QueueGroup instance1 = new ZKQueueGroup(zkClient, QueueType.PROVISIONER);
    instance1.startAndWait();
    instance1.add("tenant1", new Element("val1"));
    instance1.add("tenant2", new Element("val2"));
    QueueGroup instance2 = new ZKQueueGroup(zkClient, QueueType.PROVISIONER);
    instance2.startAndWait();
    waitForQueueNames(Sets.newHashSet("tenant1", "tenant2"), instance2);
    instance1.stop();
    instance2.stop();
  }

  private void waitForQueueNames(Set<String> expectedQueueNames, QueueGroup queueGroup) throws InterruptedException {
    for (int i = 0; i < 20; i++) {
      Set<String> queueNames = queueGroup.getQueueNames();
      if (queueNames.equals(expectedQueueNames)) {
        return;
      } else {
        TimeUnit.MILLISECONDS.sleep(200);
      }
    }
    Assert.fail();
  }
}
