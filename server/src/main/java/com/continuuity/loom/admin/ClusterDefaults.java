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

package com.continuuity.loom.admin;

import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import com.google.gson.JsonObject;

import java.util.Set;

/**
 * Default values for a cluster.  Everything in here can be overwritten by a user.
 */
public class ClusterDefaults {
  private final Set<String> services;
  private final String provider;
  private final String hardwaretype;
  private final String imagetype;
  private final String dnsSuffix;
  private final JsonObject config;

  public ClusterDefaults(Set<String> services, String provider, String hardwaretype,
                         String imagetype, String dnsSuffix, JsonObject config) {
    Preconditions.checkArgument(services != null, "default services must be specified");
    Preconditions.checkArgument(provider != null, "default provider must be specified");
    this.services = services;
    this.provider = provider;
    this.hardwaretype = hardwaretype;
    this.imagetype = imagetype;
    this.dnsSuffix = dnsSuffix;
    this.config = config == null ? new JsonObject() : config;
  }

  /**
   * Get the set of services to place on a cluster by default.  When solving for a cluster layout, a set of
   * services can be given to the solver to place on the cluster.  If no service set is given, this default set will
   * be used.
   *
   * @return Set of services that will be placed on the cluster by default.
   */
  public Set<String> getServices() {
    return services;
  }

  /**
   * Get the name of the provider to use to create the cluster.
   *
   * @return Name of the provider to use to create the cluster.
   */
  public String getProvider() {
    return provider;
  }

  /**
   * Get the name of the hardware type to use on all nodes in the cluster.  Null means there is no cluster wide
   * constraint.
   *
   * @return Name of hardware type to use on all nodes in the cluster.
   */
  public String getHardwaretype() {
    return hardwaretype;
  }

  /**
   * Get the name of the image type to use on all nodes in the cluster.  Null means there is no cluster wide
   * constraint.
   *
   * @return Name of image type to use on all nodes in the cluster.
   */
  public String getImagetype() {
    return imagetype;
  }

  /**
   * Get the dns suffix to use for hostnames of nodes in the cluster.
   *
   * @return DNS suffix to use for hostnames of nodes in the cluster.
   */
  public String getDnsSuffix() {
    return dnsSuffix;
  }

  /**
   * Get the admin defined config for the cluster.
   *
   * @return Config for the cluster.
   */
  public JsonObject getConfig() {
    return config;
  }

  @Override
  public String toString() {
    return Objects.toStringHelper(this)
      .add("services", services)
      .add("provider", provider)
      .add("hardwaretype", hardwaretype)
      .add("imagetype", imagetype)
      .add("dnsSuffix", dnsSuffix)
      .add("config", config)
      .toString();
  }
}
