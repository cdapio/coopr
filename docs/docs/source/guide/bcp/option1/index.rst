..
   Copyright 2012-2014, Continuuity, Inc.

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at
 
       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.

.. _overview_multi_data_center_high-availability:
.. index::
   single: Hot-Hot : Synchronous Database Cluster

======================================
Hot-Hot : Synchronous Database Cluster
======================================

.. _synchronous-repl:
.. figure:: /_images/ha_synchronous_repl.png
    :align: center
    :alt: Synchronous Database Architecture Diagram
    :figclass: align-center

Overview
--------
Among all Loom components, database is the only component that stores persistent state information. Any HA configuration that runs redundant Loom services across datacenters will have to make sure that the services in all datacenters have a consistent view of this data. One way of achieving this consistency is to share a single database cluster across all datacenters as discussed below.

In this configuration a database cluster with synchronous replication is shared across all datacenters. Loom Servers in each datacenter will connect to the local instance of the database cluster. All other components are configured as mentioned in :doc:`Datacenter High Availability  </guide/bcp/data-center-bcp>` section.

An advantage of this approach is that Loom Servers in all datacenters have the same view of data at all times. Hence, users in all datacenters will get to see the same state for all clusters at all times.

Failover
--------
When a datacenter fails in this setup, the data of the failed datacenter is still available in other datacenters due to synchronous replication. 
Hence Loom Servers in other datacenters should be able to handle user traffic from the failed datacenter. 

However, any transaction that was in progress when the datacenter failed will be lost as it was not committed. 
Also, any jobs that were in progress in the failed datacenter will not make any progress when the datacenter goes down.

User traffic from the failed datacenter will be re-routed to other datacenters automatically by the load balancer.
