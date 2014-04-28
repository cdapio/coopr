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
   single: Hot-Hot : Custom Replication

=================================
Hot-Hot : With Custom Replication
=================================
.. _custom-replication:
.. figure:: /_images/ha_custom.png
    :align: center
    :alt: Custom Replication Architecture Diagram
    :figclass: align-center

Overview
========
Synchronous database cluster may turn out to be not optimal for large Continuuity Loom installations due to the amount of data that needs to be replicated across datacenters.
In such a case we have to consider an alternative solution where we use local databases in each datacenter with a custom data replication service. 
This will allow for all datacenters to share the data, while reducing the need to replicate all the data in the database.

Since we will not be sharing a database across datacenters now, we'll need to treat the data slightly differently than before. 
The data will be divided into shards based on the cluster ID partitioning. Each shard will be exclusively assigned to an owner database in a datacenter. 
Only Loom Servers running locally will be able to write to such a shard. Any requests to update the clusters in a non-local shard will be routed to the Loom Server running in the datacenter owning the shard. 
This update strategy prevents write conflicts since any updates to a cluster can be done only in a single database.

The custom replication reduces replication overhead in two ways. First, it only replicates data upon completion of operations. 
Second, it replicates minimal state information needed to restart any operations in progress during datacenter failure. Intermediate state change data will not be replicated.

Each shard is synchronously replicated locally to handle intra-datacenter failover, and also remotely to at least one other datacenter.
Reads/writes to local shard, which probably will be majority of operations done in a datacenter, will be optimal. 
But writes to non-local shard will be slow as it has to be routed to another datacenter.

Since we currently do not replicate each state change across datacenters, users of a datacenter will have a delayed view of remote cluster state. 
The replication implementation should try to make this delay as minimal as possible.

Failover
========
When a datacenter fails, the remote datacenters having the shards of the failed datacenter will become the new owners of the shards.
All calls to these shards will need to be routed to the new owner datacenters.

Also, since we replicate state information required to restart any operations, any in-progress jobs during datacenter failure
can be restarted by the new owner datacenter.
However, any transaction that was in progress when the datacenter failed will be lost as it was not committed. 

User traffic from the failed datacenter will be re-routed to other datacenters automatically by the load balancer.

