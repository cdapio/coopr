package co.cask.coopr.common.conf.guice;

import co.cask.coopr.common.conf.Configuration;
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
