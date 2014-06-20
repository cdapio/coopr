package com.continuuity.loom.codec.json.guice;

import com.continuuity.loom.codec.json.JsonSerde;
import com.continuuity.loom.codec.json.current.CurrentJsonSerde;
import com.google.inject.AbstractModule;

/**
 * Guice module for binding serialization/deserialization related classes.
 */
public class CodecModule extends AbstractModule {
  private final JsonSerde codec;

  public CodecModule() {
    this(new CurrentJsonSerde());
  }

  public CodecModule(JsonSerde codec) {
    this.codec = codec;
  }

  @Override
  protected void configure() {
    bind(JsonSerde.class).toInstance(codec);
  }
}
