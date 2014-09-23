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
   single: REST API: Provisioner Plugins 

=============================
REST API: Provisioner Plugins
=============================

.. include:: /rest/rest-links.rst

Using the admin REST API, you can upload and manage plugin resources.
Each tenant admin can upload resources that plugins can use, such as chef cookbooks
or shell scripts. There are two types of plugins: automator and provider. Automator 
plugins are responsible for performing service operations, such as installing or
starting a service on a node. Provider plugins are responsible for node operations
such as creating and deleting a node through a cloud provider like Openstack. 

Plugin specifications are defined by the author of the plugin, and are registered with
the server once upon installation of the plugin.

.. _plugin-spec-all-list:

Retrieve a plugin specification
===============================

To retrieve the specification for a specific plugin, make a HTTP GET request to URI:
::

 /plugins/{plugin-type}/{plugin-name}

Plugin type is either automatortypes or providertypes. The plugin name and specification
is defined by the plugin author. For example, one of the automator plugins that comes
with the system is the chef-solo plugin. 

HTTP Responses
^^^^^^^^^^^^^^

The response is a JSON Object with ``name``, ``description``, ``resourcetypes``, and ``parameters`` as keys. 
The name is unique within the plugin type and tenant. 

Parameters define what fields the plugin needs to
perform actions and whether the parameters should come from the admin or user. For provider plugins, admin
parameters are given when creating a provider of the given provider type. For example, when creating 
an openstack provider, the admin may need to supply an API url. For provider plugins, user parameters 
are given when a cluster is being created. For automator plugins, admin fields are given when creating or 
editing services. For example, when defining how a service should be started with the chef-solo plugin, the
admin must provide a run_list parameter.

Resource types define what types of resources the plugin can use. For example, the chef-solo plugin can use
cookbooks, databags, and roles. Each resource type specifies the type of file the resource is, which must be
either file or archive. Resources of each type can be uploaded and managed through the resource APIs. 

.. list-table:: 
   :widths: 15 10 
   :header-rows: 1

   * - Status Code
     - Description
   * - 200 (OK)
     - Successfully created
   * - 400 (BAD_REQUEST)
     - Bad request, server is unable to process the request because it is ill-formed. 
   * - 404 (NOT_FOUND)
     - No plugin type specification was found. 

Example
^^^^^^^^
.. code-block:: bash

 $ curl -H 'Coopr-UserID:admin' 
        -H 'Coopr-TenantID:<tenantid>'
        -H 'Coopr-ApiKey:<apikey>'
        http://<server>:<port>/<version>/plugins/automatortypes/chef-solo
 $ {
       "name": "chef-solo",
       "description": "",
       "resourcetypes": {
           "cookbooks" : {
               "filetype": "archive"
           },
           "databags": {
               "filetype": "archive"
           },
           "roles": {
               "filetype": "archive"
           }
       },
       "parameters": {
           "admin": {
               "fields": {
                   "json_attributes": {
                       "label": "JSON attributes",
                       "override": false,
                       "tip": "Custom JSON data for this chef-solo run",
                       "type": "text"
                   },
                   "run_list": {
                       "label": "run-list",
                       "override": false,
                       "tip": "The chef-solo run-list for this action",
                       "type": "text"
                   }
               },
               "required": [
                   [ "run_list" ]
               ]
           }
       }
   }

.. _plugin-spec-retrieve:

Retrieve all plugin specifications
==================================

To retrieve all specifications of a given plugin type, make a GET HTTP request to URI:
::

 /plugins/{plugin-type}

Plugin type is either automatortypes or providertypes.

HTTP Responses
^^^^^^^^^^^^^^

The response is a JSON Array of plugin specifications. If no plugins are present, an empty array is returned. 

.. list-table::
   :widths: 15 10
   :header-rows: 1

   * - Status Code
     - Description
   * - 200 (OK)
     - Successful
   * - 400 (BAD_REQUEST)
     - Bad request, server is unable to process the request because it is ill-formed. 

Example
^^^^^^^^
.. code-block:: bash

 $ curl -H 'Coopr-UserID:admin' 
        -H 'Coopr-TenantID:<tenantid>'
        -H 'Coopr-ApiKey:<apikey>'
        http://<server>:<port>/<version>/plugins/automatortypes
 $ [
       {
           "name": "chef-solo",
           "description": "chef solo plugin",
           "resourcetypes": { ... },
           "parameters": { ... }
       },
       {
           "name": "shell",
           "description": "shell plugin",
           "resourcetypes": { ... },
           "parameters": { ... }
       },
       ...
   ]

.. _plugin-resource-create:

Add a plugin resource
=====================

To add a plugin resource, make a HTTP POST request to URI:
::

 /plugins/{plugin-type}/{plugin-name}/{resource-type}/{resource-name}

Plugin type is automatortypes or providertypes, plugin name is the name of the plugin, and resource type 
is one of the resource types defined in the plugin specification. The resource name is the name of 
the resource. The POST body contains the binary contents of the resource.

.. note:: If you are uploading an archive, the top level directory of your unpacked archive must be named exactly the same as your resource name. For example, if you are uploading an archive of a directory named 'mycookbook', you *must* name your resource 'mycookbook' in order for it to be used correctly. This issue will be fixed in the next release.

HTTP Responses
^^^^^^^^^^^^^^

Metadata for the resource is returned in the response body. The response is a JSON Object that contains the resource
name, version, and status. The status will be one of "inactive", "active", "staged", or "recalled". An inactive resource
is one that has been added to the system, but which is not in use by provisioners. An active resource is one that is 
current in use by provisioners. A staged resource is one that is not currently in use by provisioners, but which will be 
pushed to and used by provisioners after the next sync call. A recalled resource is one that is currently in use by
provisioners, but which will be removed from use after the next sync call. 

.. list-table::
   :widths: 15 10
   :header-rows: 1

   * - Status Code
     - Description
   * - 200 (OK)
     - If update was successful
   * - 400 (BAD REQUEST)
     - If the resource in the request is invalid

Example
^^^^^^^^
.. code-block:: bash

 $ curl -X POST
        -H 'Coopr-UserID:admin' 
        -H 'Coopr-TenantID:<tenantid>'
        -H 'Coopr-ApiKey:<apikey>'
        --data-binary @<hadoop-cookbook-file>
        http://<server>:<port>/<version>/plugins/automatortypes/chef-solo/cookbooks/hadoop
 $ { 
       "name": "hadoop",
       "version": 1,
       "status": "inactive"
   }

.. _plugin-resourcetype-all-list:

List all resource metadata of a specific type
=============================================

Tenant admins can fetch a map of the metadata of all resources of a specific type by making a 
HTTP GET request to URI:
::

 /plugins/{plugin-type}/{plugin-name}/{resource-type}

Plugin type is automatortypes or providertypes, plugin name is the name of the plugin, and resource type 
is one of the resource types defined in the plugin specification. Results can also be filtered to only
include metadata of resources in a particular state. 

HTTP Parameters
^^^^^^^^^^^^^^^

.. list-table::
   :widths: 15 10
   :header-rows: 1

   * - Parameter
     - Description
   * - status
     - filter results to only contain resources in the given status. Must be one of "active", "inactive", "staged", or "recalled"

HTTP Responses
^^^^^^^^^^^^^^

The response is a JSON Object whose keys are resource names, and whose values are JSON Arrays of resource metadata.

.. list-table::
   :widths: 15 10
   :header-rows: 1

   * - Status Code
     - Description
   * - 200 (OK)
     - Successful
   * - 400 (BAD REQUEST)
     - If the resource uri is specified incorrectly.

Example
^^^^^^^^
.. code-block:: bash

 $ curl -X GET 
        -H 'Coopr-UserID:admin' 
        -H 'Coopr-TenantID:<tenantid>'
        -H 'Coopr-ApiKey:<apikey>'
        http://<server>:<port>/<version>/plugins/automatortypes/chef-solo/cookbooks
 $ {
       "reactor": [
           {
               "name": "reactor",
               "status": "active",
               "version": 6
           },
           {
               "name": "reactor",
               "status": "inactive",
               "version": 3
           },
           {
               "name": "reactor",
               "status": "inactive",
               "version": 1
           },
           {
               "name": "reactor",
               "status": "inactive",
               "version": 4
           }
       ],
       ...
   }

.. _plugin-resource-all-list:

List all resource metadata of a specific type
=============================================

Tenant admins can fetch a list of the metadata for all versions of a specific resource 
by making a HTTP GET request to URI:
::

 /plugins/{plugin-type}/{plugin-name}/{resource-type}/{resource-name}

Plugin type is automatortypes or providertypes, plugin name is the name of the plugin, and resource type 
is one of the resource types defined in the plugin specification. Resource name is the name of the resource. 
Results can also be filtered to only include metadata of resources in a particular state. 

HTTP Parameters
^^^^^^^^^^^^^^^

.. list-table::
   :widths: 15 10
   :header-rows: 1

   * - Parameter
     - Description
   * - status
     - filter results to only contain resources in the given status. Must be one of "active", "inactive", "staged", or "recalled"

HTTP Responses
^^^^^^^^^^^^^^

The response is a JSON Array of the metadata for all versions of the given resource, optionally filtered by status.

.. list-table::
   :widths: 15 10
   :header-rows: 1

   * - Status Code
     - Description
   * - 200 (OK)
     - Successful
   * - 400 (BAD REQUEST)
     - If the resource uri is specified incorrectly.
   * - 404 (NOT FOUND)
     - The resource was not found.

Example
^^^^^^^^
.. code-block:: bash

 $ curl -X GET 
        -H 'Coopr-UserID:admin' 
        -H 'Coopr-TenantID:<tenantid>'
        -H 'Coopr-ApiKey:<apikey>'
        http://<server>:<port>/<version>/plugins/automatortypes/chef-solo/reactor
 $ [
       {
           "name": "reactor",
           "status": "active",
           "version": 6
       },
       {
           "name": "reactor",
           "status": "inactive",
           "version": 3
       },
       {
           "name": "reactor",
           "status": "inactive",
           "version": 1
       },
       {
           "name": "reactor",
           "status": "inactive",
           "version": 4
       }
   ]

.. _plugin-resource-delete:

Delete all versions of a resource
=================================

Tenant admins can delete all versions of a resource by making a HTTP DELETE request to URI::

 /plugins/{plugin-type}/{plugin-name}/{resource-type}/{resource-name}

Plugin type is automatortypes or providertypes, plugin name is the name of the plugin, and resource type 
is one of the resource types defined in the plugin specification. Resource name is the name of the resource. 
All versions of the resource must be inactive for this operation to be allowed.

HTTP Responses
^^^^^^^^^^^^^^

.. list-table::
   :widths: 15 10
   :header-rows: 1

   * - Status Code
     - Description
   * - 200 (OK)
     - Successful
   * - 400 (BAD REQUEST)
     - If the resource uri is specified incorrectly.
   * - 409 (CONFLICT)
     - If at least one resource version is not inactive.

Example
^^^^^^^^
.. code-block:: bash

 $ curl -X DELETE 
        -H 'Coopr-UserID:admin' 
        -H 'Coopr-TenantID:<tenantid>'
        -H 'Coopr-ApiKey:<apikey>'
        http://<server>:<port>/<version>/plugins/automatortypes/chef-solo/reactor

.. _plugin-resource-delete-version:

Delete a specific version of a resource
=======================================

Tenant admins can delete a specific version of a resource by making a HTTP DELETE request to URI::

 /plugins/{plugin-type}/{plugin-name}/{resource-type}/{resource-name}/versions/{version}

Plugin type is automatortypes or providertypes, plugin name is the name of the plugin, and resource type 
is one of the resource types defined in the plugin specification. Resource name is the name of the resource
and version is the version of the resource. The resource version must be inactive for the delete to be allowed. 

HTTP Responses
^^^^^^^^^^^^^^

.. list-table::
   :widths: 15 10
   :header-rows: 1

   * - Status Code
     - Description
   * - 200 (OK)
     - Successful
   * - 400 (BAD REQUEST)
     - If the resource uri is specified incorrectly.
   * - 409 (CONFLICT)
     - If at least one resource version is not inactive.

Example
^^^^^^^^
.. code-block:: bash

 $ curl -X DELETE 
        -H 'Coopr-UserID:admin' 
        -H 'Coopr-TenantID:<tenantid>'
        -H 'Coopr-ApiKey:<apikey>'
        http://<server>:<port>/<version>/plugins/automatortypes/chef-solo/reactor/versions/1

.. _plugin-resource-stage:

Stage a specific version of a resource
=======================================

Tenant admins can stage a specific version of a resource by making a HTTP POST request to URI:
::

 /plugins/{plugin-type}/{plugin-name}/{resource-type}/{resource-name}/versions/{version}/stage

Staging a resource will make it so that the next sync call will push the resource version to the provisioners.
Staging an active or staged resource will not do anything. Staging a recalled resource will put it back in the active
state. 

HTTP Responses
^^^^^^^^^^^^^^

.. list-table::
   :widths: 15 10
   :header-rows: 1

   * - Status Code
     - Description
   * - 200 (OK)
     - Successful
   * - 400 (BAD REQUEST)
     - If the resource uri is specified incorrectly.
   * - 404 (NOT FOUND)
     - The resource was not found.
   * - 409 (CONFLICT)
     - If at least one resource version is not inactive.

Example
^^^^^^^^
.. code-block:: bash

 $ curl -X POST 
        -H 'Coopr-UserID:admin' 
        -H 'Coopr-TenantID:<tenantid>'
        -H 'Coopr-ApiKey:<apikey>'
        http://<server>:<port>/<version>/plugins/automatortypes/chef-solo/reactor/versions/4/stage

.. _plugin-resource-recall:

Recall a specific version of a resource
========================================

Tenant admins can recall a specific version of a resource by making a HTTP POST request to URI:
::

 /plugins/{plugin-type}/{plugin-name}/{resource-type}/{resource-name}/versions/{version}/recall

Recalling a resource will make it so that the next sync call will remove that resource version from use.
Recalling an inactive or recalled resource version will have no effect. Recalling a staged resource puts it
back in the inactive state and recalling an active resource puts it in the recalled state.

HTTP Responses
^^^^^^^^^^^^^^

.. list-table::
   :widths: 15 10
   :header-rows: 1

   * - Status Code
     - Description
   * - 200 (OK)
     - Successful
   * - 400 (BAD REQUEST)
     - If the resource uri is specified incorrectly.
   * - 404 (NOT FOUND)
     - The resource was not found.
   * - 409 (CONFLICT)
     - If at least one resource version is not inactive.

Example
^^^^^^^^
.. code-block:: bash

 $ curl -X POST 
        -H 'Coopr-UserID:admin' 
        -H 'Coopr-TenantID:<tenantid>'
        -H 'Coopr-ApiKey:<apikey>'
        http://<server>:<port>/<version>/plugins/automatortypes/chef-solo/reactor/versions/4/recall


.. _plugin-sync:

Sync plugins
============

Tenant admins can sync plugin resources to provisioners by making a HTTP POST request to URI:
::

 /plugins/sync

Syncing will push all staged resources to the provisioners, making them available for use. Syncing will
also remove all recalled resources from use, making them unavailable for use.

HTTP Responses
^^^^^^^^^^^^^^

.. list-table::
   :widths: 15 10
   :header-rows: 1

   * - Status Code
     - Description
   * - 200 (OK)
     - Successful
   * - 400 (BAD REQUEST)
     - If the resource uri is specified incorrectly.

Example
^^^^^^^^
.. code-block:: bash

 $ curl -X POST 
        -H 'Coopr-UserID:admin' 
        -H 'Coopr-TenantID:<tenantid>'
        -H 'Coopr-ApiKey:<apikey>'
        http://<server>:<port>/<version>/plugins/sync
