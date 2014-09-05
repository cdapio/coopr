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
package co.cask.coopr.cluster;

import co.cask.coopr.spec.service.Service;
import co.cask.coopr.spec.service.ServiceAction;
import com.google.common.base.Objects;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import java.util.Map;
import java.util.Set;

/**
 * Properties of a node.
 */
public class NodeProperties {
  private Map<String, String> ipaddresses;
  private String hostname;
  private final int nodenum;
  // this is the name of the hardware type
  private final String hardwaretype;
  // this is the name of the image type
  private final String imagetype;
  // TODO: remove flavor, image, sshUser when hardware/image type switches to the objects instead of names
  private final String flavor;
  private final String image;
  private final String sshuser;
  // list of service names
  private final Set<String> services;
  // list of automators that could be used on the node
  private final Set<String> automators;

  private NodeProperties(String hostname, Map<String, String> ipaddresses, int nodenum, String hardwaretype,
                         String imagetype, String flavor, String image, String sshUser,
                         Set<String> automators, Set<String> services) {
    this.ipaddresses = ImmutableMap.copyOf(ipaddresses);
    this.hostname = hostname;
    this.imagetype = imagetype;
    this.hardwaretype = hardwaretype;
    this.flavor = flavor;
    this.image = image;
    this.nodenum = nodenum;
    this.sshuser = (sshUser == null || sshUser.isEmpty()) ? "root" : sshUser;
    this.automators = automators == null ? ImmutableSet.<String>of() : ImmutableSet.copyOf(automators);
    this.services = services == null ? ImmutableSet.<String>of() : ImmutableSet.copyOf(services);
  }

  /**
   * Get an immutable mapping of IP type to IP address.
   *
   * @return Immutable mapping of IP type to IP address.
   */
  public Map<String, String> getIPAddresses() {
    return ipaddresses;
  }

  /**
   * Get a specific type of IP address of the node.
   *
   * @param ipType Type of IP address to get.
   * @return IP address of the given type of the node, or null if it does not exist.
   */
  public String getIPAddress(String ipType) {
    if (ipaddresses != null) {
      return ipaddresses.get(ipType);
    }
    return null;
  }

  /**
   * Get the hostname of the node.
   *
   * @return Hostname of the node.
   */
  public String getHostname() {
    return hostname;
  }

  /**
   * Get the name of the image type of the node.
   *
   * @return Name of the image type of the node.
   */
  public String getImagetype() {
    return imagetype;
  }

  /**
   * Get the name of the hardware type of the node.
   *
   * @return Name of the hardware type of the node.
   */
  public String getHardwaretype() {
    return hardwaretype;
  }

  /**
   * Get the flavor of the node.
   *
   * @return Flavor of the node.
   */
  public String getFlavor() {
    return flavor;
  }

  /**
   * Get the image of the node.
   *
   * @return Image of the node.
   */
  public String getImage() {
    return image;
  }

  /**
   * Get the number of the node in the cluster. Each node in a cluster has a different number.
   *
   * @return Number of the node in the cluster.
   */
  public int getNodenum() {
    return nodenum;
  }

  /**
   * Get the user the provisioner will use to use to ssh in to the node.
   * Set in {@link co.cask.coopr.spec.ImageType}.
   *
   * @return User the provisioner will use to ssh in to the node.
   */
  public String getSshUser() {
    return sshuser;
  }

  /**
   * Get an immutable set of the names of the automators that can perform actions on the node.
   *
   * @return Immutable set of the names of the automators that can perform actions on the node.
   */
  public Set<String> getAutomators() {
    return automators;
  }

  /**
   * Get an immutable set of the names of the services on the node.
   *
   * @return Immutable set of the names of the services on the node.
   */
  public Set<String> getServices() {
    return services;
  }

  /**
   * Set the IP addresses of the node.
   *
   * @param ipaddresses IP addresses to set.
   */
  public void setIpaddresses(Map<String, String> ipaddresses) {
    this.ipaddresses = ipaddresses;
  }

  /**
   * Set the hostname of the node.
   *
   * @param hostname Hostname to set.
   */
  public void setHostname(String hostname) {
    this.hostname = hostname;
  }

  /**
   * Get a builder for building node properties.
   *
   * @return Builder for building node properties.
   */
  public static Builder builder() {
    return new Builder();
  }

  /**
   * Builder for creating node properties.
   */
  public static class Builder {
    private Map<String, String> ipaddresses = Maps.newHashMap();
    private String hostname;
    private int nodenum;
    private String hardwaretype;
    private String imagetype;
    private String flavor;
    private String image;
    private String sshUser;
    private Set<String> serviceNames;
    private Set<String> automators;

    public Builder addIPAddress(String ipType, String ipaddress) {
      this.ipaddresses.put(ipType, ipaddress);
      return this;
    }

    public Builder setIPAddresses(Map<String, String> ipAddresses) {
      this.ipaddresses = ipAddresses;
      return this;
    }

    public Builder setHostname(String hostname) {
      this.hostname = hostname;
      return this;
    }

    public Builder setNodenum(int nodenum) {
      this.nodenum = nodenum;
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

    public Builder setFlavor(String flavor) {
      this.flavor = flavor;
      return this;
    }

    public Builder setImage(String image) {
      this.image = image;
      return this;
    }

    public Builder setSSHUser(String sshUser) {
      this.sshUser = sshUser;
      return this;
    }

    public Builder setServiceNames(Set<String> serviceNames) {
      this.serviceNames = serviceNames;
      return this;
    }

    public Builder setAutomators(Set<String> automators) {
      this.automators = automators;
      return this;
    }

    public Builder setServices(Set<Service> services) {
      Set<String> serviceNames = Sets.newHashSet();
      Set<String> automators = Sets.newHashSet();
      for (Service service : services) {
        serviceNames.add(service.getName());
        for (ServiceAction serviceAction : service.getProvisionerActions().values()) {
          automators.add(serviceAction.getType());
        }
      }
      this.serviceNames = serviceNames;
      this.automators = automators;
      return this;
    }

    public NodeProperties build() {
      return new NodeProperties(hostname, ipaddresses, nodenum, hardwaretype, imagetype,
                                flavor, image, sshUser, automators, serviceNames);
    }
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    NodeProperties that = (NodeProperties) o;

    return Objects.equal(hostname, that.hostname) &&
      Objects.equal(ipaddresses, that.ipaddresses) &&
      Objects.equal(nodenum, that.nodenum) &&
      Objects.equal(hardwaretype, that.hardwaretype) &&
      Objects.equal(imagetype, that.imagetype) &&
      Objects.equal(flavor, that.flavor) &&
      Objects.equal(image, that.image) &&
      Objects.equal(sshuser, that.sshuser) &&
      Objects.equal(services, that.services) &&
      Objects.equal(automators, that.automators);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(hostname, ipaddresses, nodenum, hardwaretype, imagetype,
                            flavor, image, sshuser, services, automators);
  }

  @Override
  public String toString() {
    return Objects.toStringHelper(this)
      .add("hostname", hostname)
      .add("ipaddresses", ipaddresses)
      .add("nodenum", nodenum)
      .add("hardwaretype", hardwaretype)
      .add("imagetype", imagetype)
      .add("flavor", flavor)
      .add("image", image)
      .add("sshuser", sshuser)
      .add("services", services)
      .add("automators", automators)
      .toString();
  }
}
