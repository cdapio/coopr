/*
 * Copyright © 2012-2014 Cask Data, Inc.
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

package co.cask.coopr.spec.template;

import com.google.common.base.Objects;
import com.google.common.collect.ImmutableSet;
import com.google.gson.JsonObject;

import java.util.Set;

/**
 * Default values for a cluster.  Everything in here can be overwritten by a user.
 */
public final class ClusterDefaults {
  public static final ClusterDefaults EMPTY_CLUSTER_DEFAULTS = builder().build();
  final Set<String> services;
  final String provider;
  final String hardwaretype;
  final String imagetype;
  final String dnsSuffix;
  final JsonObject config;

  private ClusterDefaults(Set<String> services, String provider, String hardwaretype,
                          String imagetype, String dnsSuffix, JsonObject config) {
    this.services = services;
    this.provider = provider;
    this.hardwaretype = hardwaretype;
    this.imagetype = imagetype;
    this.dnsSuffix = dnsSuffix;
    this.config = config;
  }

  /**
   * Get the immutable set of services to place on a cluster by default.  When solving for a cluster layout, a set of
   * services can be given to the solver to place on the cluster.  If no service set is given, this default set will
   * be used.
   *
   * @return Immutable set of services that will be placed on the cluster by default.
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

  /**
   * Get a builder for creating cluster defaults.
   *
   * @return Builder for creating cluster defaults.
   */
  public static Builder builder() {
    return new Builder();
  }

  /**
   * Builder for creating cluster defaults.
   */
  public static class Builder {
    private Set<String> services = ImmutableSet.of();
    private String provider;
    private String hardwaretype;
    private String imagetype;
    private String dnsSuffix;
    private JsonObject config = new JsonObject();

    public Builder setServices(Set<String> services) {
      this.services = services == null ? ImmutableSet.<String>of() : services;
      return this;
    }

    public Builder setServices(String... services) {
      this.services = ImmutableSet.copyOf(services);
      return this;
    }

    public Builder setProvider(String provider) {
      this.provider = provider;
      return this;
    }

    public Builder setHardwaretype(String hardwaretype) {
      this.hardwaretype = hardwaretype;
      return this;
    }

    public Builder setImagetype(String imagetype) {
      this.imagetype = imagetype;
      return this;
    }

    public Builder setDNSSuffix(String getDNSSuffix) {
      this.dnsSuffix = getDNSSuffix;
      return this;
    }

    public Builder setConfig(JsonObject config) {
      this.config = config;
      return this;
    }

    public ClusterDefaults build() {
      return new ClusterDefaults(services, provider, hardwaretype, imagetype, dnsSuffix, config);
    }
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
