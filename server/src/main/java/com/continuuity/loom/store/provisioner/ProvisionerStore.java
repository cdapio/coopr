package com.continuuity.loom.store.provisioner;

import com.continuuity.loom.provisioner.Provisioner;
import com.google.common.util.concurrent.Service;

import java.io.IOException;
import java.util.Collection;

/**
 * Persistent store for provisioner related information, such as capacity used.
 */
public interface ProvisionerStore extends Service {

  Collection<Provisioner> getAllProvisioners() throws IOException;

  Collection<Provisioner> getProvisionersWithFreeCapacity() throws IOException;

  Collection<Provisioner> getIdleProvisioners(long idleTimestamp) throws IOException;

  Collection<Provisioner> getTenantProvisioners(String tenantId) throws IOException;

  Provisioner getProvisioner(String id) throws IOException;

  void unassignTenantProvisioners(String tenantId) throws IOException;

  void writeProvisioner(Provisioner provisioner) throws IOException;

  void deleteProvisioner(String id) throws IOException;

  void setHeartbeat(String provisionerId, long ts) throws IOException;

  int getFreeCapacity() throws IOException;

  int getNumAssignedWorkers(String tenantID) throws IOException;
}
