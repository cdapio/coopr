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

.. _guide_superadmin_toplevel:

.. index::
   single: Super Admin Overview

========
Overview
========

The superadmin can create, edit, and delete tenants. The superadmin is also an admin of his own superadmin tenant, meaning the 
superadmin can create, edit, and delete providers, services, hardware types, image types, cluster templates, and plugin resources
for his own tenant. This also means the superadmin can create clusters. Tenants are completely isolated from other tenants, meaning
admins and users in one tenant cannot access objects from another tenant. The one exception is that tenant admins can bootstrap their
tenant by copying every provider, service, hardware type, image type, cluster template, and plugin resource from the superadmin tenant
to their own tenant. 

Tenant Settings
===============

Each tenant also contains several settings specific to their tenant. The first is the number of workers assigned to the tenant.
Workers perform tasks to complete cluster operations for users in a tenant. For example, a worker may create a node as part of a 
cluster create operation. The more workers a tenant has, the more tasks it can execute in parallel. Workers are dedicated to a tenant
and are not shared across tenants. 

The second setting is a max on the number of clusters for the tenant, and the third setting is a max
on the number of total nodes for the tenant. If a user in the tenant tries to create a cluster that would cause the max clusters or 
max nodes limit to be violated, that create request will fail. If the superadmin attempts to decrease the max clusters or nodes below
the current number of clusters or nodes in a tenant, the operation will fail. 

Deleting a Tenant
=================

When the superadmin deletes a tenant, all providers, services, hardware types, image types, cluster templates, plugin resources, and 
clusters are removed from the system. Any jobs that are in progress in the tenant are aborted, though tasks that are currently in progress
may continue running until they complete. Clusters and nodes created by the tenant will not actually be deleted from the providers used
to create them, they will just be removed from the system database keeping track of the clusters and nodes. It is the responsibility of the
tenant administrator to delete any nodes that are no longer needed. For example, if a tenant has 10 nodes running in AWS, when the superadmin
deletes the tenant, those 10 nodes will still be running in AWS. They will just no longer be managed through the system.
