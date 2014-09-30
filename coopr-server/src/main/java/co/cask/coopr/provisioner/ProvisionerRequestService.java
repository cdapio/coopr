package co.cask.coopr.provisioner;

import co.cask.coopr.provisioner.plugin.ResourceCollection;

/**
 * Service for making requests to provisioners.
 */
public interface ProvisionerRequestService {

  /**
   * Make a request to the provisioner to delete the given tenant.
   *
   * @param provisioner Provisioner to send the request to.
   * @param tenantId Id of the tenant to remove from the provisioner.
   * @return True if the request was successful, false if not.
   */
  boolean deleteTenant(Provisioner provisioner, String tenantId);

  /**
   * Make a request to the provisioner to set the tenant information, such as number of workers and plugin resources
   * it should be using.
   *
   * @param provisioner Provisioner to send the request to.
   * @param tenantId Id of the tenant on the provisioner to write to.
   * @param resourceCollection Metadata for all resources that can be used by the tenant workers for the provisioner.
   * @return True if the request was successful, false if not.
   */
  boolean putTenant(Provisioner provisioner, String tenantId, ResourceCollection resourceCollection);

  /**
   * Make a request to the provisioner to set the number of workers for the given tenant.
   *
   * @param provisioner Provisioner to send the request to.
   * @param tenantId Id of the tenant on the provisioner to write to.
   * @return True if the request was successful, false if not.
   */
  boolean putTenantWorkers(Provisioner provisioner, String tenantId);

  /**
   * Make a request to the provisioner to set the plugin resources it should be using.
   *
   * @param provisioner Provisioner to send the request to.
   * @param tenantId Id of the tenant on the provisioner to write to.
   * @param resourceCollection Metadata for all resources that can be used by the tenant workers for the provisioner.
   * @return True if the request was successful, false if not.
   */
  boolean putTenantResources(Provisioner provisioner, String tenantId, ResourceCollection resourceCollection);
}
