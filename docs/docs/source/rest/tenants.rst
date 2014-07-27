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
   single: REST API: Tenants 

==================
REST API: Tenants
==================

.. include:: /rest/rest-links.rst

Using the Loom Superadmin REST API, you can create, modify, retrieve, and delete tenants.
Each tenant consists of a unique id, a name, some number of workers, and optional additional 
settings, such as max clusters allowed in the tenant or max nodes allowed in the tenant.
Only the superadmin is allowed to access the tenant APIs.

.. _tenants-create:

Create a Tenant
==================

To create a new tenant, make a HTTP POST request to URI:
::

 /tenants

The POST body is a JSON Object that must contain ``name`` and ``workers`` as keys specifying 
the name of the tenant and the number of workers assigned to the tenant. Other key-value pairs
can be added to specify other tenant specific settings, such as quotas on the maximum number
of clusters and nodes allowed within the tenant. 

POST Parameters
^^^^^^^^^^^^^^^^

Required Parameters

.. list-table::
   :widths: 15 10
   :header-rows: 1

   * - Parameter
     - Description
   * - name
     - Specifies the name for the tenant. The assigned name must have only
       alphanumeric, dash(-), dot(.), or underscore(_) characters.
   * - workers
     - Number of workers assigned to the tenant.
   * - maxClusters
     - Max number of clusters allowed for the tenant
   * - maxNodes
     - Max nodes allowed for the tenant

HTTP Responses
^^^^^^^^^^^^^^

The response is a JSON Object with ``id`` as a key and value containing the id of the newly created tenant.

.. list-table:: 
   :widths: 15 10 
   :header-rows: 1

   * - Status Code
     - Description
   * - 200 (OK)
     - Successfully created
   * - 400 (BAD_REQUEST)
     - Bad request, server is unable to process the request because it is ill-formed. 

Example
^^^^^^^^
.. code-block:: bash

 $ curl -X POST 
        -H 'X-Loom-UserID:superadmin' 
        -H 'X-Loom-Tenant:ID:loom'
        -H 'X-Loom-ApiKey:<apikey>'
        -d '{"name":"my-company", "workers":10}' 
        http://<loom-server>:<superadmin-port>/<version>/tenants
 $ { "id": "f78dae92-a27b-4e3b-8c6a-cfc19f844259" }

.. _tenants-retrieve:

Retrieve a Tenant
===================

To retrieve details about a tenant, make a GET HTTP request to URI:
::

 /tenants/{id}

This resource request represents an individual tenant for viewing.

HTTP Responses
^^^^^^^^^^^^^^

The response is a JSON Object representing the tenant. It contains ``id``,
``name``, ``workers``, and any additional tenant specific settings.

.. list-table::
   :widths: 15 10
   :header-rows: 1

   * - Status Code
     - Description
   * - 200 (OK)
     - Successful
   * - 404 (NOT FOUND)
     - If the resource requested is not configured or available in system.

Example
^^^^^^^^
.. code-block:: bash

 $ curl -X GET 
        -H 'X-Loom-UserID:superadmin' 
        -H 'X-Loom-Tenant:ID:loom'
        -H 'X-Loom-ApiKey:<apikey>'
        http://<loom-server>:<superadmin-port>/<version>/tenants/f78dae92-a27b-4e3b-8c6a-cfc19f844259
 $ { "id":"f78dae92-a27b-4e3b-8c6a-cfc19f844259", "name":"my-company", "workers":10 }


.. _tenants-delete:

Delete a Tenant
=================

To delete a tenant, make a DELETE HTTP request to URI:
::

 /tenants/{id}

A tenant can only be deleted if its workers have been set to 0.

HTTP Responses
^^^^^^^^^^^^^^

.. list-table::
   :widths: 15 10
   :header-rows: 1

   * - Status Code
     - Description
   * - 200 (OK)
     - If delete was successful
   * - 409 (CONFLICT)
     - If the tenant is not in a deletable state.

Example
^^^^^^^^
.. code-block:: bash

 $ curl -X DELETE
        -H 'X-Loom-UserID:superadmin' 
        -H 'X-Loom-Tenant:ID:loom'
        -H 'X-Loom-ApiKey:<apikey>'
        http://<loom-server>:<superadmin-port>/<version>/tenants/f78dae92-a27b-4e3b-8c6a-cfc19f844259

.. _tenants-modify:

Update a Tenant
==================

To update a tenant, make a PUT HTTP request to URI:
::

 /tenants/{id}

The resource specified above respresents an individual tenant that is being updated.
Currently, the update of a tenant resource requires the complete tenant object to in
the request body. Trying to lower the max clusters or max nodes belong the number
currently in use is not allowed. 

PUT Parameters
^^^^^^^^^^^^^^^^

Required Parameters

.. list-table::
   :widths: 15 10
   :header-rows: 1

   * - Parameter
     - Description
   * - id 
     - Id of the resource to be updated. Id must match.
   * - name
     - Name of the resource to be updated. 
   * - workers
     - New number of workers assigned to the tenant.
   * - maxClusters
     - New max number of clusters allowed for the tenant.
   * - maxNodes
     - New max number of nodes allowed for the tenant.

HTTP Responses
^^^^^^^^^^^^^^

.. list-table::
   :widths: 15 10
   :header-rows: 1

   * - Status Code
     - Description
   * - 200 (OK)
     - If update was successful
   * - 400 (BAD REQUEST)
     - If the resource in the request is invalid
   * - 404 (NOT FOUND)
     - If the resource requested is not found
   * - 409 (CONFLICT)
     - If writing the tenant resource would cause quota violations

Example
^^^^^^^^
.. code-block:: bash

 $ curl -X PUT
        -H 'X-Loom-UserID:superadmin' 
        -H 'X-Loom-Tenant:ID:loom'
        -H 'X-Loom-ApiKey:<apikey>'
        -d '{ "id":"f78dae92-a27b-4e3b-8c6a-cfc19f844259", "name":"my-company", "workers":20 }'  
        http://<loom-server>:<superadmin-port>/<version>/tenants/f78dae92-a27b-4e3b-8c6a-cfc19f844259
 $ curl -X GET 
        -H 'X-Loom-UserID:superadmin' 
        -H 'X-Loom-Tenant:ID:loom'
        -H 'X-Loom-ApiKey:<apikey>'
        http://<loom-server>:<superadmin-port>/<version>/tenants/f78dae92-a27b-4e3b-8c6a-cfc19f844259
 $ { "id":"f78dae92-a27b-4e3b-8c6a-cfc19f844259", "name":"my-company", "workers":20 }

.. _tenants-all-list:

List All Tenants
================

The list of all tenants is also available for you to retrieve. The tenant list resource represents 
the comprehensive set of tenants within the Continuuity Loom system.

To list all the tenants, make GET HTTP request to URI:
::

 /tenants

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

 $ curl -X GET 
        -H 'X-Loom-UserID:superadmin' 
        -H 'X-Loom-Tenant:ID:loom'
        -H 'X-Loom-ApiKey:<apikey>'
        http://<loom-server>:<superadmin-port>/<version>/tenants
 $ [
     { "id":"f78dae92-a27b-4e3b-8c6a-cfc19f844259", "name":"my-company", "workers":10 },
     { "id":"e94ndl34-b38d-3n7a-0c1e-dpl84q438920", "name":"companyX", "workers":100 }
   ]
