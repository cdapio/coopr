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

==================================
Multi-Datacenter High Availability
==================================

When running across multiple datacenters, Continuuity Loom can be configured to be resilient to datacenter failures. This document describes the recommended configuration
for setting up Continuuity Loom for HA across multiple datacenters. Together with :doc:`Datacenter High Availability <data-center-bcp>`, this setup provides for a comprehensive plan for Continuuity Loom HA.

In this setup, Continuuity Loom runs in active mode in all datacenters (Hot-Hot). In case of a datacenter failure, traffic from the failed datacenter will be automatically routed to other datacenters by the load balancer. This ensures that service is not affected on a datacenter failure.

A couple of things need to be considered when configuring Continuuity Loom to run across multiple datacenters for HA-

* As discussed in the previous section, all components of Continuuity Loom, except for database, either deal with local data or are stateless. The most important part of the HA setup is to share the data across datacenters in a consistent manner. HA configuration setup for multi-datacenter mostly depends on how the database is setup as discussed in the next sections.
* Since Loom Servers across all datacenters run in Hot-Hot mode, we have to make sure that they do not conflict while creating cluster IDs. The ID space needs to be partitioned amongst the Loom Servers. This can be done using ``loom.ids.start.num`` and ``loom.ids.increment.by`` server config parameters. For more information on the config parameters see :doc:`Server Configuration </guide/admin/server-config>` section. Also note that Loom Servers in a datacenter can share the same ID space.



We discuss two possible multi-datacenter HA configurations for Continuuity Loom in the next sections.
Note that the second option - HA with Custom Replication, is still work in progress. 

.. toctree::
   :maxdepth: 2

   option1/index
   option4/index
