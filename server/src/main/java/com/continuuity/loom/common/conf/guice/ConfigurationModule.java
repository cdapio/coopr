package com.continuuity.loom.common.conf.guice;

import com.continuuity.loom.common.conf.Configuration;
import com.google.inject.AbstractModule;

/**
 * Guice module that binds {@link Configuration} to a specific configuration instance.
 */
public class ConfigurationModule extends AbstractModule {
  private final Configuration conf;

  public ConfigurationModule() {
    this(Configuration.create());
  }

  public ConfigurationModule(Configuration conf) {
    this.conf = conf;
  }

  @Override
  protected void configure() {
    bind(Configuration.class).toInstance(conf);
  }
}
