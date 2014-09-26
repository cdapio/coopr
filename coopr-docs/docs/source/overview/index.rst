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

:orphan:

.. index::
   single: Overview
.. _index_toplevel:

========
Overview
========
Coopr is a cluster provisioning system designed from the ground up to fully facilitate cluster lifecycle management
in both public and private clouds. It streamlines the process of provisioning clusters of any kind, right from an end user's workstation.
Administrators can easily create cluster templates, which allow end users to quickly instantiate live clusters.

Coopr exposes two primary user interfaces: Coopr Admin and Coopr User. Coopr Admin makes it easy for
administrators to create and maintain complex cluster templates across multiple IaaS clouds, while Coopr User
makes it easy for any end user to select and build complex clusters using these templates. This empowers the end users,
eliminating the need for filing tickets and the pain of configuring complicated clusters. In essence, Coopr
offers a comprehensive self-service provisioning system.

.. figure:: /_images/coopr-diagram.png
    :align: center
    :alt: What is Coopr?
    :figclass: align-center

Within Coopr, a cluster is the basic indivisible element that supports create, delete, amend, update, and
monitor operations. These complex operations are masked by simple REST services that allow easy integrations with
existing IT operations systems. Coopr is built with DevOps and IT in mind: it's highly flexible (through lifecycle hooks)
and easily pluggable (via Chef, Puppet, and other automation tools).

.. _history-of-coopr:

History of Coopr
===========================
At Cask, we built Coopr to fill an ongoing need - to quickly build and deploy clusters for developers.
Since CDAP, our flagship product, utilizes several technologies within the Hadoop ecosystem, it was a constant battle
to regularly build, use and tear down clusters on a variety of IaaS providers. Coopr solved this problem by
rapidly provisioning our clusters to meet the needs of our business, developers and customers. Coopr's
streamlined cluster management was a key investment for Cask.

.. _coopr-in-production:

Coopr in Production
==============================
Currently, Coopr is used to provision Cask Data Application Platform (CDAP) on the Coopr Cloud and as an IT tool by developers to
test new and incremental features on an ongoing basis. Within the Cask build system, Coopr is used via REST APIs to provision
multi-node CDAP clusters to perform functional testing.

