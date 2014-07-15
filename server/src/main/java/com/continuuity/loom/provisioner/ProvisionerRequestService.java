package com.continuuity.loom.provisioner;

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
   * Make a request to the provisioner to put the given tenant.
   *
   * @param provisioner Provisioner to send the request to.
   * @param tenantId Id of the tenant to put to the provisioner.
   * @return True if the request was successful, false if not.
   */
  boolean putTenant(Provisioner provisioner, String tenantId);
}
