..
   Copyright © 2012-2014 Cask Data, Inc.

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

Coopr can be configured to be resilient to machine or component failures. This document describes the recommended configuration
for setting up Coopr for HA within a single datacenter. Please refer to :doc:`multi-datacenter HA <multi-data-center-bcp>` documentation
for configuring HA across multiple datacenters.

In order to support resiliency against machine or component failures within a datacenter, Coopr components can be configured to 
run with redundancies on multiple machines. Each machine running Coopr can have a maximum of -

* One coopr-ui process
* One coopr-server process
* Multiple coopr-provisioner processes (See config COOPR_NUM_WORKERS in :doc:`installation guide </installation/index>`)
* One ZooKeeper process
* One database process

The diagram below shows the logical deployment diagram of Coopr for HA in a datacenter-

.. _single-dc:
.. figure:: /_images/ha_within_colo.png
    :align: center
    :alt: Within Datacenter Architecture Diagram

Coopr UI
--------
Coopr UI (coopr-ui) is stateless, and communicates with Coopr Server using REST endpoints. Hence Coopr UI can be easily run on multiple machines. User traffic is routed to multiple instances of Coopr UI using load balancers (such as HAproxy or Varnish or VIP).

Coopr Provisioner
-----------------
Coopr Provisioner (coopr-provisioner) is also stateless, and communicates with Coopr Server using REST endpoints. Hence Coopr Provisioner can be easily run on multiple machines.

Coopr Server
------------
Coopr Server (coopr-server) can be run on multiple machines too. When run in this mode, there will be a load balancer fronting the Coopr Servers. Coopr UI and Coopr Provisioners will be configured to communicate via a load balancer with the Coopr Server. Also, all Coopr Servers in a datacenter should connect to the same ZooKeeper quorum.

ZooKeeper
---------
A ZooKeeper quorum of at least 3 machines is required for redundancy. Note that ZooKeeper needs to run with an odd number of total machines for redundancy.

Database
--------
Database needs to be replicated with automatic failover in case the master database goes down. Database is also fronted by a proxy that directs all operations to master database, and Coopr Server connects to database using the proxy.

