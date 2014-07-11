package com.continuuity.loom.common.zookeeper.guice;

import com.continuuity.loom.common.zookeeper.IdService;
import com.google.inject.AbstractModule;
import com.google.inject.Scopes;
import org.apache.twill.zookeeper.ZKClient;

/**
 * Guice module for binding zookeeper related classes.
 */
public class ZookeeperModule extends AbstractModule {
  // TODO: should be changed to provide a zkclient instead of passing in one
  private final ZKClient zkClient;

  public ZookeeperModule(ZKClient zkClient) {
    this.zkClient = zkClient;
  }

  @Override
  protected void configure() {
    bind(ZKClient.class).toInstance(zkClient);
    bind(IdService.class).in(Scopes.SINGLETON);
  }
}
