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

.. _overview_features:
.. index::
   single: Features
.. _features:

========
Features
========


Continuuity Loom provides a core set of complementary features to provision and monitor clusters. Collectively, they
make large scale deployment manageable. 

Core Features
=============
• Simple on-demand cluster provisioning
• Automatic placement and management of services during cluster creation based on constraint based templates and desired cluster size
• Seamless integration with OpenStack and IaaS providers
• Pluggable automation platform (e.g. Chef, Puppet)
• Scalability to hundreds of clusters
• Modular configuration and service management
• Admin UI to create and manage configuration, and for user to customize and create clusters.
• Fully driven by REST APIs


Other Features
==============
• Pre-defined templates for clusters (e.g. Hadoop, LAMP)
• Works out of the box with in-memory zookeeper and embedded DB
• Uses chef-solo as SCM engine, and hence not dependent on a Chef server
• Push model for provisioning and installation, and therefore can provision clusters outside firewall
• One-click import and export of Loom catalogs and associated entities 
• Status updates during cluster creation in UI 
• User defined configuration for cluster creation
• Centralized dashboard to view and manage multiple clusters

Reduced IT Overhead
^^^^^^^^^^^^^^^^^^^
In many organizations, developers submit requests to access a Hadoop cluster
in order to run a MapReduce job. With Continuuity Loom, the IT department sets up a
catalog of clusters that can be provisioned directly by developers. Developers
can allocate or destroy a cluster right from their workstation.
Instant access to IT resources reduces wait time and increases productivity.
Continuuity Loom provides a centralized dashboard to view and manage multiple clusters.

Private and Public Clouds
^^^^^^^^^^^^^^^^^^^^^^^^^
Continuuity Loom works and integrates with any IaaS provider in the public cloud including OpenStack for behind-the-firewall cluster provisioning and management.

Extensibility
^^^^^^^^^^^^^
Continuuity Loom simplifies the installation and configuration of any software stack,
including Hadoop. It ensures that all installations are verified before they
are made available. Administrators are able to create custom cluster types ranging from Hadoop and LAMP
to Solr search clusters. Most importantly, using the open source automation platform Chef, you can 
manage any Big Data application; many Chef recipes are readily available, and as an
administrator or developer, you can develop your own.

REST APIs and a Rich UI
^^^^^^^^^^^^^^^^^^^^^^^
Integrate with existing tools and workflows via the Loom REST API. Continuuity Loom also
provides a simple and intuitive UI that allows users to create and manage clusters.
