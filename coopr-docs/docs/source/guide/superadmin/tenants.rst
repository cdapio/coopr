..
   Copyright © 2012-2015 Cask Data, Inc.

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
   single: Tenant Management

=================
Tenant Management
=================

The superadmin can create, edit, and delete tenants. The superadmin is also an admin of
its own superadmin tenant, meaning the superadmin can create, edit, and delete providers,
services, hardware types, image types, cluster templates, and plugin resources for its own
tenant. This also means the superadmin can create clusters. Tenants are completely
isolated from other tenants, meaning admins and users in one tenant cannot access objects
from another tenant. As a superadmin, you may see an overview of all tenants by clicking
on the Admin button in the upper-right of the page to bring down the Admin profile menu.
Select the 'Tenants' item from the menu:

.. figure:: /_images/superadmin/tenants/sa_profile.png
    :align: center
    :width: 800px
    :alt: Profile menu
    :figclass: align-center

This takes you to the 'Tenant List' Overview page:


.. figure:: /_images/superadmin/tenants/sa_overview.png
    :align: center
    :width: 800px
    :alt: Tenants overview page
    :figclass: align-center

This page displays the total number of workers in the system, the total number of available workers, and the total number of tasks
that are in progress and queued. Each tenant is assigned a number of workers to perform cluster tasks. Once a worker is assigned to
a tenant, it is no longer available and will only perform tasks for that tenant. Thus, when adding workers to a tenant,
or when creating a new tenant, the number of workers added must not exceed the number of available workers in the system.
If your system does not have enough workers to meet your needs, you will need to add provisioners. The number of workers assigned
to a tenant is exactly equal to the number of tasks that can be executed in parallel for the tenant. Tenants with a lot of activity
will require more workers. 

The number of in-progress tasks is the number of tasks that are currently being executed by a worker in some tenant. The number of
queue tasks are tasks that are queued, but are not currently being executed. If your queued tasks are consistently high, it is a 
sign that there is at least one tenant that does not have enough workers. These numbers can be broken down for each tenant by using the
:doc:`Tenant APIs </rest/tenants>` and :doc:`Provisioner APIs </rest/provisioners>`. 

Creating a Tenant
=================

To create a tenant, click on the 'Create' button near the top right of the tenants overview screen. This takes you to the
tenant creation page where you can assign workers to the new tenant and set limits on the number of clusters and nodes allowed
in the tenant.

.. figure:: /_images/superadmin/tenants/sa_tenant_create.png
    :align: center
    :width: 800px
    :alt: Tenant creation page
    :figclass: align-center

You cannot assign more workers to the tenant than the number of available workers. When creating a tenant, it will be
completely empty, and the tenant admin will need to populate all entities before it can be used to create any clusters.

**Note:** If you :ref:`create a tenant using the REST API <tenants-create>`, you can
optionally 'bootstrap' the tenant. Bootstrapping a tenant copies all providers, hardware
types, image types, services, cluster templates, and plugin resources from the superadmin
tenant to the newly created tenant. Be aware that bootstrapping copies all plugin
resources, meaning any provider keys that have been uploaded to the superadmin tenant will
be copied to the new tenant.

If a user in the tenant tries to create a cluster that would cause the max clusters or  
max nodes limit to be violated, that create request will fail.

Once you are done, hit the 'Create' button to create the tenant. This will take you back to the overview page, where you should
notice that the number of available workers has decreased in response to assigning workers to the new tenant.

Edit a Tenant
===============

You can edit a tenant by clicking on the name of the tenant on the overview page. 

.. figure:: /_images/superadmin/tenants/sa_tenant_edit.png
    :align: center
    :width: 800px
    :alt: Tenant edit page
    :figclass: align-center

If the superadmin attempts to decrease the max clusters or nodes below
the current number of clusters or nodes in a tenant, the operation will fail. Similarly, if you try to give the 
tenant more workers than are available, the operation will fail. 

Deleting a Tenant
=================

A superadmin may delete a tenant by clicking on the trash icon on the tenant overview page. Deleting a tenant is only allowed if the
number of workers assigned to the tenant has first been reduced to 0. The superadmin tenant may not be deleted.

When the superadmin deletes a tenant, all providers, services, hardware types, image types, cluster templates, plugin resources, and 
clusters are removed from the system. Any jobs that are in progress in the tenant are aborted, though tasks that are currently in progress
may continue running until they complete. Clusters and nodes created by the tenant will not actually be deleted from the providers used
to create them, they will just be removed from the system database keeping track of the clusters and nodes. It is the responsibility of the
tenant administrator to delete any nodes that are no longer needed. For example, if a tenant has 10 nodes running in AWS, when the superadmin
deletes the tenant, those 10 nodes will still be running in AWS. They will just no longer be managed through the system.
