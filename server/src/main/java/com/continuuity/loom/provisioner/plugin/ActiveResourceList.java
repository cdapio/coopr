package com.continuuity.loom.provisioner.plugin;

import com.continuuity.loom.admin.ResourceTypeFormat;
import com.google.common.collect.Lists;

import java.util.List;

/**
 * List of active resources to set on a provisioner. Only used as part of {@link ResourceCollection}.
 */
public class ActiveResourceList {
  private final ResourceTypeFormat format;
  private final List<ResourceMeta> active;

  public ActiveResourceList(ResourceTypeFormat format) {
    this.format = format;
    this.active = Lists.newArrayList();
  }

  /**
   * Add a resource to the list.
   *
   * @param meta Metadata of resource to add
   */
  public void addMeta(ResourceMeta meta) {
    active.add(meta);
  }
}
