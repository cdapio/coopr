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

.. _overview_single_data_center:
.. index::
   single: Datacenter High Availability

=============================
Datacenter High Availability
=============================

Continuuity Loom can be configured to be resilient to machine or component failures. This document describes the recommended configuration
for setting up Continuuity Loom for HA within a single datacenter. Please refer to :doc:`multi-datacenter HA <multi-data-center-bcp>` documentation
for configuring HA across multiple datacenters.

In order to support resiliency against machine or component failures within a datacenter, Loom components can be configured to 
run with redundancies on multiple machines. Each machine running Continuuity Loom can have a maximum of -

* One loom-ui process
* One loom-server process
* Multiple loom-provisioner processes (See config LOOM_NUM_WORKERS in :doc:`installation guide </guide/installation/index>`)
* One ZooKeeper process
* One database process

The diagram below shows the logical deployment diagram of Continuuity Loom for HA in a datacenter-

.. _single-dc:
.. figure:: /_images/ha_within_colo.png
    :align: center
    :alt: Within Datacenter Architecture Diagram

Loom UI
-------
Loom UI (loom-ui) is stateless, and communicates with Loom Server using REST endpoints. Hence Loom UI can be easily run on multiple machines. User traffic is routed to multiple instances of Loom UI using load balancers (such as HAproxy or Varnish or VIP).

Loom Provisioner
----------------
Loom Provisioner (loom-provisioner) is also stateless, and communicates with Loom Server using REST endpoints. Hence Loom Provisioner can be easily run on multiple machines.

Loom Server
-----------
Loom Server (loom-server) can be run on multiple machines too. When run in this mode, there will be a load balancer fronting the Loom Servers. Loom UI and Loom Provisioners will be configured to communicate via a load balancer with the Loom Server. Also, all Loom Servers in a datacenter should connect to the same ZooKeeper quorum.

ZooKeeper
---------
A ZooKeeper quorum of at least 3 machines is required for redundancy. Note that ZooKeeper needs to run with an odd number of total machines for redundancy.

Database
--------
Database needs to be replicated with automatic failover in case the master database goes down. Database is also fronted by a proxy that directs all operations to master database, and Loom Server connects to database using the proxy.

