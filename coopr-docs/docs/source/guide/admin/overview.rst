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

.. _guide_admin_toplevel:

.. index::
   single: Administration Overview

========
Overview
========

.. include:: /guide/admin/admin-links.rst

In this page, we explore various concepts behind Coopr administration and explain various tools used in Coopr for
administrators to configure and manage their clusters. At the core of a Coopr cluster is the notion of a **Template**, which
is the blueprint or the primordial stuff of what Coopr clusters are comprised of—it is the essence or the DNA of how different
parts and components come together to materialize into a cluster.

Concepts
========

As mentioned above, Coopr works through the use of **Templates**, which dictate the configuration of the
clusters that users can spin up. An administrator can specify any number of such templates to put into
their **Catalog** for users. 

Several concepts central to cluster configuration are definable in Coopr. These aspects are:

* **Providers** - Infrastructure providers (such as Amazon or OpenStack) that supply machines.

* **Hardware types** - Type of hardware (such as small, medium, or large) that can be used for the nodes of a cluster. 

* **Image types** - Basic disk images installed on the nodes of a cluster.

* **Plugin Resources** - Admin provided resources that can be used by plugins. For example, the chef-solo plugin can use cookbooks uploaded by the admin, and the AWS plugin can use ssh keys upload by the admin.

* **Services** - Bundled software services that can be placed on a cluster.

* **Cluster Template** - Blueprint describing show hardware, images, and services should be laid out to form a cluster.

Templates are defined by specifying hardware types, image types, and services that can be used in a cluster, as well
as a set of constraints that describes how those hardware types, image types, and services should be laid out in a cluster.
Template creation can be done in 
two ways: 1) :doc:`Admin UI </guide/admin/ui>` and 2) :doc:`Web Services REST Cluster API</rest/templates>`. 

Because the notion of **Templates** is central to the Coopr cluster creation, please read the :doc:`Web Services REST Cluster 
API</rest/templates>` or :doc:`Admin UI </guide/admin/ui>` carefully to design templates that meet your needs. 


.. _provision-templates2:

