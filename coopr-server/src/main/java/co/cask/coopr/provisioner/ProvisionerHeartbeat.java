package co.cask.coopr.provisioner;

import com.google.common.base.Objects;

import java.util.Map;

/**
 * Data sent in a provisioner heartbeat.  Currently only tenant usage information.
 */
public class ProvisionerHeartbeat {
  private final Map<String, Integer> usage;

  public ProvisionerHeartbeat(Map<String, Integer> usage) {
    this.usage = usage;
  }

  /**
   * Get usage information for the provisioner.
   *
   * @return Usage information for the provisioner, which is a mapping of tenant id to number of live workers for that
   * tenant.
   */
  public Map<String, Integer> getUsage() {
    return usage;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof ProvisionerHeartbeat)) {
      return false;
    }

    ProvisionerHeartbeat that = (ProvisionerHeartbeat) o;

    return Objects.equal(usage, that.usage);
  }

  @Override
  public int hashCode() {
    return usage.hashCode();
  }

  @Override
  public String toString() {
    return Objects.toStringHelper(this)
      .add("usage", usage)
      .toString();
  }
}
