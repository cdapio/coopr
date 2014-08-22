/*
 * Copyright 2012-2014, Continuuity, Inc.
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
package com.continuuity.loom.provisioner.plugin;

import com.continuuity.loom.common.utils.ImmutablePair;
import com.continuuity.loom.spec.plugin.ResourceTypeFormat;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

import java.util.Collection;

/**
 * Collection of resources for a specific tenant.  This is sent to provisioners to let them know what resources
 * are available for each plugin.
 */
public class ResourceCollection {
  private final Multimap<ImmutablePair<ResourceType, ResourceTypeFormat>, ResourceMeta> resources;

  public ResourceCollection() {
    this.resources = HashMultimap.create();
  }

  public Multimap<ImmutablePair<ResourceType, ResourceTypeFormat>, ResourceMeta> getResources() {
    return resources;
  }

  /**
   * Add resources of the given type and format with the given metadata.
   *
   * @param type Type of resource
   * @param format Format of resource
   * @param metas Collection of resource metadata to add
   */
  public void addResources(ResourceType type, ResourceTypeFormat format, Collection<ResourceMeta> metas) {
    resources.putAll(ImmutablePair.of(type, format), metas);
  }
}
