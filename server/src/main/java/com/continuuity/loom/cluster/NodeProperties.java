package com.continuuity.loom.cluster;

import com.continuuity.loom.admin.Service;
import com.continuuity.loom.admin.ServiceAction;
import com.google.common.base.Objects;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;

import java.util.Set;

/**
 * Properties of a node.
 */
public class NodeProperties {
  private final String hostname;
  private final String ipaddress;
  private final int nodenum;
  // this is the name of the hardware type
  private final String hardwaretype;
  // this is the name of the image type
  private final String imagetype;
  // TODO: remove flavor, image, sshUser when hardware/image type switches to the objects instead of names
  private final String flavor;
  private final String image;
  private final String sshUser;
  // list of service names
  private final Set<String> services;
  // list of automators that could be used on the node
  private final Set<String> automators;

  public static NodeProperties from(String hostname, String ipaddress, int nodenum, String hardwaretype,
                                    String imagetype, String flavor, String image, String sshUser,
                                    Set<Service> services) {
    Set<String> serviceNames = Sets.newHashSet();
    Set<String> automators = Sets.newHashSet();
    for (Service service : services) {
      serviceNames.add(service.getName());
      for (ServiceAction serviceAction : service.getProvisionerActions().values()) {
        automators.add(serviceAction.getType());
      }
    }
    return new NodeProperties(hostname, ipaddress, nodenum, hardwaretype, imagetype,
                              flavor, image, sshUser, automators, serviceNames);
  }

  public NodeProperties(String hostname, String ipaddress, int nodenum, String hardwaretype, String imagetype,
                        String flavor, String image, String sshUser, Set<String> automators, Set<String> services) {
    this.ipaddress = ipaddress;
    this.hostname = hostname;
    this.imagetype = imagetype;
    this.hardwaretype = hardwaretype;
    this.flavor = flavor;
    this.image = image;
    this.nodenum = nodenum;
    this.sshUser = sshUser;
    this.automators = automators == null ? ImmutableSet.<String>of() : ImmutableSet.copyOf(automators);
    this.services = services == null ? ImmutableSet.<String>of() : ImmutableSet.copyOf(services);
  }

  public String getIpaddress() {
    return ipaddress;
  }

  public String getHostname() {
    return hostname;
  }

  public String getImagetype() {
    return imagetype;
  }

  public String getHardwaretype() {
    return hardwaretype;
  }

  public String getFlavor() {
    return flavor;
  }

  public String getImage() {
    return image;
  }

  public int getNodenum() {
    return nodenum;
  }

  public String getSshUser() {
    return sshUser;
  }

  public Set<String> getAutomators() {
    return automators;
  }

  public Set<String> getServices() {
    return services;
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
      Objects.equal(ipaddress, that.ipaddress) &&
      Objects.equal(nodenum, that.nodenum) &&
      Objects.equal(hardwaretype, that.hardwaretype) &&
      Objects.equal(imagetype, that.imagetype) &&
      Objects.equal(flavor, that.flavor) &&
      Objects.equal(image, that.image) &&
      Objects.equal(sshUser, that.sshUser) &&
      Objects.equal(services, that.services) &&
      Objects.equal(automators, that.automators);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(hostname, ipaddress, nodenum, hardwaretype, imagetype,
                            flavor, image, sshUser, services, automators);
  }
}
