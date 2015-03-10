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

.. _guide_admin_ui:

.. index::
   single: User Interface

=============================
Administration User Interface
=============================

.. include:: /guide/admin/admin-links.rst

This guide describes the different interfaces and functions of the administrator UI.

Each screen in the administration interface provides ways to create and edit settings for cluster provisioning.

Login as an Administrator
=========================

The Admin UI can be accessed at ``http://<coopr-host>:<coopr-ui-port>/``. Login using the admin credentials to access the administrator interface.

.. figure:: /_images/admin/ui/ui_login.png
    :align: center
    :width: 800px
    :alt: Admin login screen
    :figclass: align-center

The Overview Screen
===================

An administrator is redirected to the overview screen after log in. This home page displays all the cluster configuration
elements that have already been defined. (However, upon logging for the first time, this page will be empty).
Clicking on the name of each element allows an administrator to enter its management page, where they can examine the element in detail and modify its configuration.

.. figure:: /_images/admin/ui/ui_overview.png
    :align: center
    :width: 800px
    :alt: Administrator overview screen
    :figclass: align-center

.. _provision-templates:

Managing Cluster Templates
============================

Templates allow the administrator to define blueprints describing how different types of clusters should be laid out.
For example, there may be a template for Hadoop clusters, a template for LAMP clusters, a template for Solr clusters, etc. 
Templates contain enough information that an end user only needs to specify a template and a number of machines to create a cluster. 
This is done by first describing the set of services, hardware types, and image types that a cluster is compatible with. 
Next, default values for provider, services, and configuration are given, with optional defaults for cluster-wide hardware and image type. 
Finally, a set of constraints are defined that describe how services, hardware, and images should be placed on a cluster.

The Catalog Home Screen
^^^^^^^^^^^^^^^^^^^^^^^

The Catalog screen lists the existing templates that the administrator has created. The page also provides a way
to delete, view, and edit each template.

.. figure:: /_images/admin/ui/ui_catalog.png
    :align: center
    :width: 800px
    :alt: Catalog home screen
    :figclass: align-center

Clicking on a template name will take you to the 'Edit template' page, where you can view or edit template details.

Creating a Template
^^^^^^^^^^^^^^^^^^^

To create a new template, click on the 'Create' button on the top-right the screen. This action will display the Templates' creation page.
In addition to specifying a name and description for the template, the initialization screen allows you to set parameters
for the 'Lease Duration'. This field allows an administrator to specify the initial and maximum lease durations to be applied
to clusters created using this template, as well as a step size for use when extending a cluster lease.

.. figure:: /_images/admin/ui/ui_template_create_general.png
    :align: center
    :width: 800px
    :alt: Template creation - general
    :figclass: align-center

The Compatibility tab defines sets of services, hardware types, and image types that are allowed for use in a cluster.
Services not in the list specified in this section cannot be placed on the cluster.
Coopr will not automatically pull in service dependencies, so the full set of compatible services must be defined.
Hardware types not in the list specified in this section cannot be used in the cluster. Similarly, image types 
not in the list specified in this section cannot to be used in the cluster.
Services, hardware types, and image types can all be added by clicking the appropriate '+
add' button, and then selecting an element from the drop-down menu that appears. To remove
an element, press the down-pointing arrow next to the element you want removed, and select
'Remove' from the drop-down menu that appears.

.. figure:: /_images/admin/ui/ui_template_create_compatibility.png
    :align: center
    :width: 800px
    :alt: Template creation - compatibility
    :figclass: align-center

The Defaults tab screen defines the default services and provider, and optionally a cluster wide image type
or hardware type, to use when a cluster is initially created. The provider, hardware type, and image type can be
selected from the drop-down menu among those defined in their corresponding sections. The 'Config' field allows
the admin to specify cluster configuration as JSON-formatted input.  Generally, all service configuration settings
should go in this section. Configuration is consumed as is, except for some macros which allow you to reference 
other nodes in the cluster (for more information, see :doc:`Macros </guide/admin/macros>`).

Multiple services can be placed on a cluster by default. Click the '+ add service' button,
and select a service from the drop-down menu that results to add it. To remove a service,
press the down-pointing arrow next to the service you want removed, and select 'Remove'
from the drop-down menu that appears.

Everything in this section can be overwritten by the user during cluster creation time, though it is likely that 
only advanced users will want to do so. (In future releases, we'll have more granular access control capabilities so
that novice users may not change default configurations.)

.. figure:: /_images/admin/ui/ui_template_create_defaults.png
    :align: center
    :width: 800px
    :alt: Template creation - default services
    :figclass: align-center

The Constraints tab allows the administrator to set rules for the sets of services that
are installed on a cluster. The most basic type of constraint are size constraints, which
set a minimum and maximum number of nodes for the cluster.

A 'Must coexist' constraint is used to specify services that must be placed together on
the same node. For example, in a Hadoop cluster, you generally want datanodes,
regionservers, and nodemanagers to all be placed together, so you would put all 3 services
in the same 'Must coexist' constraint. 

The 'Must coexist' constraints are not transitive. If there is one constraint saying
service A must coexist with service B, and another constraint saying service B must
coexist with service C, this does not mean that service A must coexist with service C.
Coopr was designed to prevent unintended links between services, especially as the number
of 'Must coexist' constraints increase. If a 'Must coexist' rule contains a service that
is not on the cluster, it is shrunk to ignore the service not on the cluster. 

For example, your template may be compatible with datanodes, nodemanagers, and
regionservers. However, by default, you only put datanodes and nodemanagers on the
cluster. At cluster creation time, a constraint stating that datanodes, nodemanagers, and
regionservers must coexist on the same node will get transformed into a constraint that
just says datanodes and nodemanagers must coexist on the same node.

The other type of layout constraint are 'Can't coexist' constraints, which are also given
as an array of arrays. Each inner array is a set of services that cannot all coexist
together on the same node. For example, in a Hadoop cluster, you generally do not want
your namenode to be on the same node as a datanode. Specifying more than 2 services in a
'Can't coexist' rule means the entire set cannot exist on the same node. For example, if
there is a constraint that service A, service B, and service C 'Can't coexist,' service A
and service B can still coexist on the same node. Though supported, this can be confusing,
so the best practice is to keep 'Can't coexist' constraints binary. 

Anything not mentioned in the must or can't coexist constraints is allowed.

.. figure:: /_images/admin/ui/ui_template_create_constraints_empty.png
    :align: center
    :width: 800px
    :alt: Template creation - constraints
    :figclass: align-center

To create a constraint, click on either '+ Add must co-exist group' or '+ Add can't co-exist
group', select a service you want to add to the group and select '+ Add Service'. Services
can be removed from the group by pressing the down-pointing arrow next to the name of the
service.

.. figure:: /_images/admin/ui/ui_template_create_constraints_mustco.png
    :align: center
    :width: 800px
    :alt: Template creation - constraints - add must coexists
    :figclass: align-center


.. figure:: /_images/admin/ui/ui_template_create_constraints_cantco.png
    :align: center
    :width: 800px
    :alt: Template creation - constraints - add can't coexist
    :figclass: align-center

Additionally, administrators can limit the number of instances of each service. An example of this is to limit the
number of instances of HDFS NameNode and YARN ResourceManager to one in a Hadoop cluster. To do so, click '+ Add
service constraint', choose the item you want to limit from the drop-down list, and set the minimum and maximum
number of instances permitted. The constraint itself or the number of instances can be changed from the list of
service constraints. 

A service constraint can also specify a set of hardware types that a service is allowed to
be placed on an instance. Any node with that service must use one of the hardware types in
the array. If nothing is given, the service can go on a node with any type of hardware. 

Similarly, a service constraint can specify a set of image types that it is allowed to be
placed on an instance. Any node with that service must use one of the image types in the
array. If nothing is given, the service can go on a node with any type of image.

.. figure:: /_images/admin/ui/ui_template_create_constraints_service.png
    :align: center
    :width: 800px
    :alt: Template creation - constraints - add service constraint
    :figclass: align-center

When finished, to add the new setting to the list of templates, click 'Create'.

Managing Existing Templates
^^^^^^^^^^^^^^^^^^^^^^^^^^^^
A user can view and edit a template by clicking on the template's name on the Home screen,
clicking the 'Edit' button on the template's line in the list, or by selecting 'Cluster
Templates' **->** '<name of template>' from the menu in the upper-right of the page.

The edit template page provides a similar interface to the 'Template Create' screen. Current settings for the
template can be modified and deleted accordingly.

.. figure:: /_images/admin/ui/ui_template_edit.png
    :align: center
    :width: 800px
    :alt: Template management
    :figclass: align-center

.. _infrastructure-providers:

Managing Infrastructure Providers
=================================

Coopr provides functionality to provision servers across a number of infrastructure providers, including but not limited to
Amazon Web Services, DigitalOcean, Google Compute Engine, Joyent, and Rackspace. Coopr also supports OpenStack to enable integration with
custom infrastructures for both public and private cloud.

The Providers interface allows administrators to add available cloud providers and manage their credentials.

The Providers Home Screen
^^^^^^^^^^^^^^^^^^^^^^^^^

The Providers home screen lists the existing providers currently supported by the administrators. The page also
provides a way to delete, view, and edit each provider.

.. figure:: /_images/admin/ui/ui_providers.png
    :align: center
    :width: 800px
    :alt: Providers home screen
    :figclass: align-center

Clicking on a provider's name will take you to the 'Provider Edit' page for viewing provider details and
editing provider configurations.

Creating a Provider
^^^^^^^^^^^^^^^^^^^

Click on the 'Create' button on the top-right of the screen to go to the Providers creation
page. On this page, administrators can configure the Name, Description, and Provider type of the service.

.. figure:: /_images/admin/ui/ui_provider_create.png
    :align: center
    :width: 800px
    :alt: Create a provider
    :figclass: align-center

When selecting a Provider type, additional parameters will appear on a provider specific screen, where an administrator can
manage its credentials and include any other information needed.

.. figure:: /_images/admin/ui/ui_provider_create_example.png
    :align: center
    :width: 800px
    :alt: Create a provider
    :figclass: align-center

To add the new configuration to the list of providers, click 'Create'.

.. _manage-existing-providers:

Managing Existing Providers
^^^^^^^^^^^^^^^^^^^^^^^^^^^

A user can view/edit a provider by clicking on the provider's name on the Home screen, or selecting 'Providers' **->**
'<name of provider>' in the upper-right of the page.

The provider edit page renders a similar interface to the 'Provider Create' screen. Current settings for the
provider can be modified and deleted accordingly.

.. figure:: /_images/admin/ui/ui_provider_edit.png
    :align: center
    :width: 800px
    :alt: Managing providers
    :figclass: align-center


.. _hardware-configs:

Managing Hardware Configurations
================================

The hardware types section allows administrators to explicitly manage the hardware configurations available to users and
how the configurations are specified in each provider.

The Hardware Home Screen
^^^^^^^^^^^^^^^^^^^^^^^^

The hardware home screen lists the hardware types currently managed by the administrators. The page
also provides operations to delete and view/edit each hardware type.

Clicking on an item's name will take you to the 'Hardware Type Edit' page for viewing hardware type details
and for editing its configurations.

.. figure:: /_images/admin/ui/ui_hardwaretypes.png
    :align: center
    :width: 800px
    :alt: Hardware type home screen
    :figclass: align-center

Creating a Hardware Configuration
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

Click on the 'Create' button on the top-right of the screen to go to the Hardware
type creation page.

On this page, administrators can configure the Name, Description, and how the hardware setting is specified on a provider.
The 'Providers' section define how the hardware setting maps to the identifiers used on each of the cloud infrastructure providers. 
Note that hardware settings on the provider side are specified using virtual hardware templates called flavors.

.. figure:: /_images/admin/ui/ui_hardwaretype_create.png
    :align: center
    :width: 800px
    :alt: Creating a hardware type
    :figclass: align-center

Values specified in 'Providers' must map to a valid flavor on the corresponding provider. Below is a list of flavor
identifier codes commonly used by providers.

- `Amazon Web Services <http://aws.amazon.com/ec2/instance-types/index.html>`_

- `DigitalOcean <https://developers.digitalocean.com/v1/sizes/>`_

- `Google Compute Engine <https://cloud.google.com/compute/docs/machine-types>`_

- `Joyent <http://serverbear.com/9798/joyent>`_

- `OpenStack <http://docs.openstack.org/trunk/openstack-ops/content/flavors.html>`_

- `Rackspace <http://docs.rackspace.com/servers/api/v2/cs-releasenotes/content/supported_flavors.html>`_

As these codes are subject to change, please ensure the values reflect correctly with the provider's system.

To add the new configuration to the list of hardware types, click 'Create'.

Managing Existing Hardware Configurations
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

A user can view/edit a hardware type by clicking on the hardware type's name on the Home screen or by selecting
'Hardware types' **->** <name of hardware type> in the upper-right of the page.

The edit hardware type page provides a similar interface to the 'Hardware Type Create' screen. Current
settings for the hardware type can be modified and deleted accordingly.

.. figure:: /_images/admin/ui/ui_hardwaretype_edit.png
    :align: center
    :width: 800px
    :alt: Managing hardware types
    :figclass: align-center

.. _image-types:

Managing Image Types
====================

The image types interface allows administrators to configure the options for basic disk images on the clusters
provisioned by end users.

The Images Home Screen
^^^^^^^^^^^^^^^^^^^^^^
The images home screen lists the image types currently configured by the administrators. The page also provides delete and view/edit 
operations on each image type.

Clicking on an item's name will take you to the 'Image Type Edit' page for viewing more image type details and
editing its configurations.

.. figure:: /_images/admin/ui/ui_imagetypes.png
    :align: center
    :width: 800px
    :alt: Image types home screen
    :figclass: align-center

Creating an Image Type
^^^^^^^^^^^^^^^^^^^^^^
Click on the 'Create' button on the top-right of the home screen to go to the Image type creation page.

On this page, administrators can configure the Name, Description, and how the image type is specified on a provider. The
'Providers' section can be used to define how the image type maps to the identifiers used on each provider.

Image settings are specified by a unique ID code on different providers. Values specified in
'Providers' must map to a valid image on the corresponding provider. As the list may change over time,
the most current list of IDs for images should be queried directly from the provider.

.. figure:: /_images/admin/ui/ui_imagetype_create.png
    :align: center
    :width: 800px
    :alt: Creating an image type
    :figclass: align-center

To add the new configuration to the list of image types, click 'Create'.

Managing Existing Images
^^^^^^^^^^^^^^^^^^^^^^^^

An administrator can view/edit an image type by clicking on the image type's name on the Home screen or by selecting 'Image types'
**->** <name of image type> in the upper-right of the page.

The edit image type page provides a similar interface to the 'Image Type Create' screen. Current settings for the
image type can be modified and deleted accordingly.

.. figure:: /_images/admin/ui/ui_imagetype_edit.png
    :align: center
    :width: 800px
    :alt: Managing an image type
    :figclass: align-center

.. _cluster-services:

Managing Services
=================

The Services interface allows the administrator to select the software features and services that can be installed on a cluster.

The Services Home Screen
^^^^^^^^^^^^^^^^^^^^^^^^

The services' home screen lists the services currently configured by the administrators. The page also provides
delete and view/edit operations for each service.

Clicking on an item's name will take you to the 'Service Edit' page for viewing more service details and
editing its configurations.

.. figure:: /_images/admin/ui/ui_services.png
    :align: center
    :width: 800px
    :alt: Services home screen
    :figclass: align-center

Creating a Service 
^^^^^^^^^^^^^^^^^^

Click on the 'Create' button on the top-right of the home screen to go to the Service creation page.
When adding a service, an administrator specifies dependencies for the service and a list of actions that can
be executed on the service.

Dependencies
------------
Dependencies serve two general purposes. The first is to enforce that a service
cannot be placed onto a cluster without also placing the services it requires. The second is to enforce a safe
ordering of service actions while performing cluster operations. It is easiest to understand the different
types of dependencies by going through example. In this example we have a service called "myapp-2.0".

.. figure:: /_images/admin/ui/ui_service_create_dependencies.png
    :align: center
    :width: 800px
    :alt: Service Dependencies
    :figclass: align-center

Provides
--------
The ``Provides`` field defines an extra level of indirection when specifying dependencies.
In this example, the "myapp-2.0" service provides the "myapp" service. This means that
other services can put "myapp" in their runtime or install dependencies, and "myapp-2.0"
will satisfy that dependency. 

As another example, "myapp-2.0" *requires* the "mysql-server" service. If there was a
service called "mysql-custom-server" that provides "mysql-server", then it would be fine
for "mysql-custom-server" and "myapp-2.0" to be on the same cluster. All the ordering
enforced by that runtime *requires* dependency would be enforced between the "myapp-2.0"
and "mysql-server" services.

Conflicts
---------
The ``Conflicts`` field specifies a list of services that cannot be placed on a cluster with the given service.
In this example, that means that "myapp-2.0" cannot be placed on a cluster with "docker".

Install time
------------
Install dependencies are dependencies that take effect for the INSTALL and REMOVE service
actions. The ``Install Requires`` field specifies a list of services that the given
service requires for its installation. 

In this example, "myapp-2.0" *requires* the "base" service at install time. This means
that the installation of the "base" service will occur before the install of the
"myapp-2.0" service. Similarly, the removal of the "myapp-2.0" service will occur before
the removal of the "base" service. This also means that the "myapp-2.0" service cannot be
placed on a cluster without the "base" service also being placed on the cluster.

The ``Install Uses`` key is like the ``Install Requires`` key in that it enforces the same
ordering of service actions. However, ``Install Uses`` will not enforce the presence of
the dependent service. In this example, "myapp-2.0" *uses* the "nodejs" service at install
time. This means that if the "ntp" service is also on the cluster, the installation of
"nodejs" will occur before the installation of "myapp-2.0". However, "nodejs" does not
have to be placed on the cluster in order for "myapp-2.0" to be placed on the cluster.

Runtime
-------
Runtime dependencies contain fields that are analagous to those in the install section.
The only difference is the service actions that they apply to. Install dependencies affect
the INSTALL and REMOVE service actions, whereas runtime dependencies affect the
INITIALIZE, START, and STOP service actions.

In this example, "myapp-2.0" *requires* the "mysql-server" service. This means that
"myapp-2.0" cannot be placed on a cluster without a "mysql-server" service. It also means
that the initialization of "myapp-2.0" will occur after the start of "mysql-server". It
also means the start of "myapp-2.0" will occur after the start of "mysql-server" and that
the stop of "myapp-2.0" will occur before the stop of "mysql-server". 

Similarly, because "myapp-2.0" *uses* "node-modules", initialization and start of
"myapp-2.0" will occur after the start of "node-modules". Similarly, the stop of
"myapp-2.0" will occur before the stop of "node-modules". Since it is in ``uses``,
enforcement of this ordering only applies if "node-modules" is present on the same cluster
as "myapp-2.0". The "myapp-2.0" service can be placed on a cluster without the
"node-modules" service.

Automator Details
-----------------
The administrator then defines the list of actions to occur or execute in order to make the service available
and operational on a cluster. Such actions may include CONFIGURE, INITIALIZE, INSTALL, REMOVE, START, and STOP.
For each action, you enter the data required by the underlying automator plugin.  For example, for the "chef-solo" plugin, 
you enter the 'run_list' and 'json_attributes' in their respective fields.  These fields
vary depending on which automator type is selected. Chef-solo, docker, and shell automator types are included by default.
To add an action, click on 'Add,' and an additional section will be added below. Follow the same steps.

.. figure:: /_images/admin/ui/ui_service_create_actions.png
    :align: center
    :width: 800px
    :alt: Service Automator Details
    :figclass: align-center

To add the new service to the list of services, click 'Create'.

Managing Existing Services
^^^^^^^^^^^^^^^^^^^^^^^^^^

An administrator can view/edit a provider by clicking on the service's name on the Home screen, or selecting 'Services'
**->** <name of service> in the upper-right of the page.

The edit service page provides a similar interface to the 'Service Create' screen. Current
settings for the service can be modified and deleted accordingly.

.. figure:: /_images/admin/ui/ui_service_edit.png
    :align: center
    :width: 800px
    :alt: Managing services
    :figclass: align-center


The Cluster Management Interface
================================

The Cluster page provides administrators a way to create, delete, and monitor the clusters created on their system.
The management page is virtually identical to that of the :doc:`User Home Screen </guide/user/index>`. The only
difference between the two pages is that the administrator's page shows all clusters across all users
of a tenant, while a user's page shows only clusters they own. (The superadmin can see all clusters across
all tenants and users.)

.. figure:: /_images/admin/ui/ui_clusters.png
    :align: center
    :width: 800px
    :alt: Cluster management home screen
    :figclass: align-center


For more information on how to view, create, and delete clusters, please see the :doc:`User Guide </guide/user/index>`
page.
