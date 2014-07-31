package com.continuuity.loom.provisioner.plugin;

import com.continuuity.loom.admin.ResourceTypeFormat;
import com.continuuity.loom.provisioner.plugin.ResourceMeta;
import com.google.common.collect.Lists;

import java.util.List;

/**
 *
 */
public class ActiveResourceList {
  private final ResourceTypeFormat format;
  private final List<ResourceMeta> active;

  public ActiveResourceList(ResourceTypeFormat format) {
    this.format = format;
    this.active = Lists.newArrayList();
  }

  public void addMeta(ResourceMeta meta) {
    active.add(meta);
  }
}
