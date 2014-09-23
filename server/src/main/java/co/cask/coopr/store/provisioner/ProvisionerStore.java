package co.cask.coopr.store.provisioner;

import co.cask.coopr.provisioner.Provisioner;
import com.google.common.util.concurrent.Service;

import java.io.IOException;
import java.util.Collection;

/**
 * Persistent store for provisioner related information, such as capacity used.
 */
public interface ProvisionerStore extends Service {

  /**
   * Get all provisioners in the system.
   *
   * @return Unmodifiable collection of all provisioners in the system
   * @throws IOException
   */
  Collection<Provisioner> getAllProvisioners() throws IOException;

  /**
   * Get all provisioners that have free capacity above zero.
   *
   * @return Unmodifiable collection of all provisioners with free capacity above zero
   * @throws IOException
   */
  Collection<Provisioner> getProvisionersWithFreeCapacity() throws IOException;

  /**
   * Get all provisioners that have not sent a heartbeat since the given timestamp in milliseconds.
   *
   * @param idleTimestamp Timestamp to use as a cutoff. Provisioners that have not sent a heartbeat since this timestamp
   *                      are returned
   * @return Unmodifiable collection of all provisioners that have not sent a heartbeat
   *         since the given timestamp in milliseconds.
   * @throws IOException
   */
  Collection<Provisioner> getTimedOutProvisioners(long idleTimestamp) throws IOException;

  /**
   * Get all provisioners that are assigned workers for the given tenant.
   *
   * @param tenantId Id of the tenant to get provisioners for
   * @return Unmodifiable collection of all provisioners that are assigned workers for the given tenant
   * @throws IOException
   */
  Collection<Provisioner> getTenantProvisioners(String tenantId) throws IOException;

  /**
   * Get the provisioner for the given id.
   *
   * @param id Id of the provisioner to get
   * @return Provisioner for the given id
   * @throws IOException
   */
  Provisioner getProvisioner(String id) throws IOException;

  /**
   * Write the given provisioner.
   *
   * @param provisioner Provisioner to write
   * @throws IOException
   */
  void writeProvisioner(Provisioner provisioner) throws IOException;

  /**
   * Delete the provisioner with the given id.
   *
   * @param id Id of the provisioner to delete
   * @throws IOException
   */
  void deleteProvisioner(String id) throws IOException;

  /**
   * Set the last heartbeat time of the given provisioner to the given timestamp in milliseconds.
   *
   * @param provisionerId Id of the provisioner whose heartbeat time should be set
   * @param ts Timestamp in milliseconds to set the heartbeat time to
   * @throws IOException
   */
  void setHeartbeat(String provisionerId, long ts) throws IOException;

  /**
   * Get the total amount of free capacity available across all provisioners.
   *
   * @return Total amount of free capacity available across all provisioners
   * @throws IOException
   */
  int getFreeCapacity() throws IOException;

  /**
   * Get the number of workers assigned to the given tenant across all provisioners.
   *
   * @param tenantID Id of the tenant to get the number of assigned workers for
   * @return Number of workers assigned to the given tenant across all provisioners
   * @throws IOException
   */
  int getNumAssignedWorkers(String tenantID) throws IOException;
}
