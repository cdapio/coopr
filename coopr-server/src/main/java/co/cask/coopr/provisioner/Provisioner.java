package co.cask.coopr.provisioner;

import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

/**
 * Provisioner information, such as the total capacity of the provisioner and how many workers are assigned and live
 * for each tenant the provisioner has.
 */
public class Provisioner {
  private final String id;
  private final String host;
  private final int port;
  private final int capacityTotal;
  private final Map<String, Integer> assignments;
  private Map<String, Integer> usage;
  private int capacityFree;

  public Provisioner(String id, String host, Integer port, Integer capacityTotal, Map<String, Integer> usage,
                     Map<String, Integer> assignments) {
    Preconditions.checkArgument(id != null && !id.isEmpty(), "id must be specified.");
    Preconditions.checkArgument(host != null && !host.isEmpty(), "host must be specified.");
    Preconditions.checkArgument(port != null, "port must be specified.");
    Preconditions.checkArgument(capacityTotal != null && capacityTotal > 0,
                                "capacityTotal must be specified and positive.");
    this.id = id;
    this.host = host;
    this.port = port;
    this.capacityTotal = capacityTotal;
    this.usage = usage == null ? ImmutableMap.<String, Integer>of() : ImmutableMap.copyOf(usage);
    this.assignments = Maps.newConcurrentMap();
    if (assignments != null) {
      this.assignments.putAll(assignments);
    }
    int totalAssigned = 0;
    for (Integer assigned : this.assignments.values()) {
      totalAssigned += assigned;
    }
    this.capacityFree = capacityTotal - totalAssigned;
    Preconditions.checkArgument(this.capacityFree >= 0, "assignments cannot exceed total capacity");
  }

  /**
   * Get the id of the provisioner.
   *
   * @return Id of the provisioner.
   */
  public String getId() {
    return id;
  }

  /**
   * Get the host the provisioner is running on.
   *
   * @return Host the provisioner is running on.
   */
  public String getHost() {
    return host;
  }

  /**
   * Get the port the provisioner is running on.
   *
   * @return Port the provisioner is running on.
   */
  public int getPort() {
    return port;
  }

  /**
   * Get the total capacity of the provisioner.
   *
   * @return Total capacity of the provisioner.
   */
  public int getCapacityTotal() {
    return capacityTotal;
  }

  /**
   * Get the number of workers that can be added to the provisioner.
   *
   * @return Number of workers that can be added to the provisioner.
   */
  public int getCapacityFree() {
    return capacityFree;
  }

  /**
   * Get an immutable mapping of tenants to number of live workers for that tenant.
   *
   * @return Immutable mapping of tenants to number of live workers for that tenant.
   */
  public Map<String, Integer> getUsage() {
    return usage;
  }

  /**
   * Get the unmodifiable set of tenants that are assigned to the provisioner.
   *
   * @return Unmodifiable set of tenants assigned to the provisioner.
   */
  public Set<String> getAssignedTenants() {
    return Collections.unmodifiableSet(assignments.keySet());
  }

  /**
   * Get the unmodifiable set of tenants that have live workers on the provisioner.
   *
   * @return Unmodifiable set of tenants that have live workers on the provisioner.
   */
  public Set<String> getLiveTenants() {
    return Collections.unmodifiableSet(usage.keySet());
  }

  /**
   * Remove all assigned workers for the given tenant from this provisioner.
   *
   * @param tenantId Tenant to remove from the provisioner.
   */
  public void removeTenantAssignments(String tenantId) {
    if (assignments.containsKey(tenantId)) {
      capacityFree += assignments.get(tenantId);
      assignments.remove(tenantId);
    }
  }

  /**
   * Try assigning additional workers to a tenant, capping out if the number to add would make the total assignments
   * go over the total capacity of the provisioner.
   *
   * @param tenantId Tenant to assign additional workers to.
   * @param numToAdd Number of additional workers to try and assign to the tenant.
   * @return Number of additional workers actually assigned to the tenant.
   */
  public int tryAddTenantAssignments(String tenantId, int numToAdd) {
    if (capacityFree < 1) {
      return 0;
    }
    int actualNumToAdd = Math.min(capacityFree, numToAdd);
    if (assignments.containsKey(tenantId)) {
      assignments.put(tenantId, assignments.get(tenantId) + actualNumToAdd);
    } else {
      assignments.put(tenantId, actualNumToAdd);
    }
    capacityFree -= actualNumToAdd;
    return actualNumToAdd;
  }

  /**
   * Try removing assigned workers from a tenant, capping out if the number to remove is greater than the number
   * currently assigned.
   *
   * @param tenantId Tenant to remove assigned workers from.
   * @param numToRemove Number of workers to try and remove from the tenant.
   * @return Number of assigned workers actually removed from the tenant.
   */
  public int tryRemoveTenantAssignments(String tenantId, int numToRemove) {
    if (assignments.containsKey(tenantId)) {
      int currentVal = assignments.get(tenantId);
      int actualRemoved = Math.min(numToRemove, currentVal);
      if (actualRemoved == currentVal) {
        assignments.remove(tenantId);
      } else {
        assignments.put(tenantId, currentVal - numToRemove);
      }
      capacityFree += actualRemoved;
      return actualRemoved;
    }
    return 0;
  }

  /**
   * Get the number of workers assigned to the given tenant on this provisioner.
   *
   * @param tenantId Tenant to get the number of assigned workers for.
   * @return Number of workers assigned to the given tenant on this provisioner.
   */
  public int getAssignedWorkers(String tenantId) {
    return assignments.containsKey(tenantId) ? assignments.get(tenantId) : 0;
  }

  /**
   * Get the number of live workers for the given tenant on this provisioner.
   *
   * @param tenantId Tenant to get the number of live workers for.
   * @return Number of live workers for the given tenant on this provisioner.
   */
  public int getLiveWorkers(String tenantId) {
    return usage.containsKey(tenantId) ? usage.get(tenantId) : 0;
  }

  /**
   * Sets the usage information for this provisioner, which contains number of live workers per tenant.
   *
   * @param usage Usage information for the provisioner.
   */
  public void setUsage(Map<String, Integer> usage) {
    this.usage = ImmutableMap.copyOf(usage);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof Provisioner)) {
      return false;
    }

    Provisioner that = (Provisioner) o;

    return Objects.equal(id, that.id) &&
      Objects.equal(host, that.host) &&
      port == that.port &&
      capacityTotal == that.capacityTotal &&
      capacityFree == that.capacityFree &&
      Objects.equal(usage, that.usage) &&
      Objects.equal(assignments, that.assignments);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(id, host, port, capacityTotal, capacityFree, usage, assignments);
  }

  @Override
  public String toString() {
    return Objects.toStringHelper(this)
      .add("id", id)
      .add("host", host)
      .add("port", port)
      .add("capacityTotal", capacityTotal)
      .add("capacityFree", capacityFree)
      .add("usage", usage)
      .add("assignment", assignments)
      .toString();
  }
}
