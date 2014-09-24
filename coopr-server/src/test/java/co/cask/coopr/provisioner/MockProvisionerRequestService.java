package co.cask.coopr.provisioner;

import co.cask.coopr.provisioner.plugin.ResourceCollection;
import com.google.common.collect.Sets;

import java.util.Set;

/**
 * Mock service for sending requests to provisioners. Used for testing since we don't actually want to perform
 * http requests in tests.
 */
public class MockProvisionerRequestService implements ProvisionerRequestService {
  private Set<String> deadProvisioners = Sets.newHashSet();

  @Override
  public boolean deleteTenant(Provisioner provisioner, String tenantId) {
    return !deadProvisioners.contains(provisioner.getId());
  }

  @Override
  public boolean putTenant(Provisioner provisioner, String tenantId, ResourceCollection resourceCollection) {
    return !deadProvisioners.contains(provisioner.getId());
  }

  @Override
  public boolean putTenantWorkers(Provisioner provisioner, String tenantId) {
    return !deadProvisioners.contains(provisioner.getId());
  }

  @Override
  public boolean putTenantResources(Provisioner provisioner, String tenantId, ResourceCollection resourceCollection) {
    return !deadProvisioners.contains(provisioner.getId());
  }

  public void addDeadProvisioner(String provisionerId) {
    deadProvisioners.add(provisionerId);
  }

  public void removeDeadProvisioner(String provisionerId) {
    deadProvisioners.remove(provisionerId);
  }

  public void reset() {
    deadProvisioners.clear();
  }
}
