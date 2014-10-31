package co.cask.coopr.common.queue.guice;

import co.cask.coopr.common.conf.Constants;
import co.cask.coopr.common.queue.QueueService;
import co.cask.coopr.common.queue.QueueType;
import co.cask.coopr.common.queue.TrackingQueue;
import co.cask.coopr.common.queue.internal.LazyZKTrackingQueue;
import co.cask.coopr.common.queue.internal.ZKQueueService;
import com.google.inject.AbstractModule;
import com.google.inject.Scopes;
import com.google.inject.name.Names;
import org.apache.twill.zookeeper.ZKClient;

/**
 * Guice module for binding queue related classes and instances.
 */
public class QueueModule extends AbstractModule {
  private final ZKClient zkClient;

  public QueueModule(ZKClient zkClient) {
    this.zkClient = zkClient;
  }

  @Override
  protected void configure() {
    TrackingQueue balancerQueue = new LazyZKTrackingQueue(zkClient, QueueType.BALANCER.getPath());
    bind(TrackingQueue.class)
      .annotatedWith(Names.named(Constants.Queue.WORKER_BALANCE)).toInstance(balancerQueue);

    bind(QueueService.class).to(ZKQueueService.class).in(Scopes.SINGLETON);
  }
}
