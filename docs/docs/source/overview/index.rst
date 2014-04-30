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

:orphan:

.. index::
   single: Overview
.. _index_toplevel:

========
Overview
========
Continuuity Loom is a cluster provisioning system designed from the ground up to fully facilitate cluster lifecycle management
in both public and private clouds. It streamlines the process of provisioning clusters of any kind, right from an end user's workstation.
Administrators can easily create cluster templates, which allows end users to quickly instantiate live clusters.

Continuuity Loom exposes two primary user interfaces: Loom Admin and Loom User. Loom Admin makes it easy for
administrators to create and maintain complex cluster templates across multiple IaaS clouds, while Continuuity Loom User
makes it easy for any end user to select and build complex clusters using these templates. This empowers the end users
eliminating the need for filing tickets or the pain of configuring complicated clusters. In essence, Continuuity Loom
has been designed to offer a comprehensive self-service provisioning system.

.. figure:: /_images/loom-diagram.png
    :align: center
    :alt: What is Continuuity Loom?
    :figclass: align-center

Within Continuuity Loom, a cluster is the basic indivisible element that supports create, delete, amend, update, and
monitor operations. These complex operations are masked by simple REST services that allow easy integrations with
existing IT operations' systems. Continuuity Loom is built with DevOps and IT in mind: it's highly flexible (through lifecycle hooks)
and easily pluggable (via Chef, Puppet, and other automation tools).
Future development will focus more on managing clusters of machines or clusters of clusters and less around individual machines.

.. _history-of-loom:

History of Continuuity Loom
===========================
At Continuuity, we built our Loom product to fill an ongoing need - to quickly build and deploy clusters for developers.
Since Reactor, our flagship product, utilizes several technologies within the Hadoop ecosystem, it was a constant battle
to build, use and tear down clusters regularly on a variety of IaaS providers. Continuuity Loom solved this problem by
rapidly provisioning our clusters to meet the needs of our business, developers and customers. Continuuity Loom's
streamlined cluster management was a key investment for Continuuity.

.. _loom-in-production:

Continuuity Loom in Production
==============================
Currently, Continuuity Loom is used to provision Reactor Sandboxes on the Continuuity Cloud and as an IT tool by developers to
test new and incremental features on an ongoing basis. Within the Continuuity build system, Continuuity Loom is used via REST APIs to provision
multi-node Reactor clusters to perform functional testing.

