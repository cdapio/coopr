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
   single: RPC

===
RPC
===

.. include:: /rest/rest-links.rst

In addition to the standard REST endpoints, a few RPC functions are available.

.. _rpc-bootstrap:

Bootstrap Tenant
================
Bootstrapping a tenant will copy all providers, hardware types, image types, services, 
cluster templates, and plugin resources from the superadmin to the tenant making the request.
The user making the request must be the tenant admin. This is a copy, meaning if the 
superadmin updates the entities, the changes will not be reflected in the tenant.

To bootstrap a tenant, making a POST HTTP request to URI:
::

 /bootstrap

Normally, a bootstrap is only allowed if the tenant is in an completely clean state, meaning
there are no providers, hardware types, image types, services, cluster templates, or 
plugin resources. The tenant admin can force a potentially destructive bootstrap by setting
a variable in the request body.
The body is a JSON Object containing one optional key ``force`` which indicates whether or 
not superadmin data should be copied regardless of the state of the tenant. This will overwrite
any existing data.

POST Parameters
^^^^^^^^^^^^^^^^

.. list-table::
   :widths: 15 10
   :header-rows: 1

   * - Parameter
     - Description
   * - force 
     - Boolean indicating whether or not to force a bootstrap regardless of the tenant state. Defaults to false.

HTTP Responses
^^^^^^^^^^^^^^

.. list-table::
   :widths: 15 10
   :header-rows: 1

   * - Status Code
     - Description
   * - 200 (OK)
     - If update was successful
   * - 403 (FORBIDDEN)
     - If the user is not allowed to make this request.
   * - 409 (CONFLICT)
     - If the tenant is not in a clean slate and cannot be bootstrapped.

Example
^^^^^^^^
.. code-block:: bash

 $ curl -H 'X-Loom-UserID:admin' 
        -H 'X-Loom-ApiKey:<apikey>'
        -H 'X-Loom-TenantID:<tenantid>'
        -X POST
        -d '{ 
                "overwrite": "true"
            }'
        http://<loom-server>:<loom-port>/<version>/loom/bootstrap

.. _rpc-properties:

Get Properties of Nodes in a Cluster
====================================

The next RPC call gets node properties for nodes in a cluster belonging to the caller. 
The properties fetched can be whitelisted. Nodes returned can also be filtered so that
only nodes containing a specific list of services are returned. To get a list of node
properties from a cluster, make a POST HTTP request to URI: 
::

 /getNodeProperties

The POST body must contain the ``clusterId``. Optionally it can include an array of 
``properties``, containing a list of properties to return. Properties include 
``hostname``, ``ipaddress`` as well as anything else the Provisioners place there.
Optionally the POST body can also contain an array of ``services``. Doing so will 
restrict the result to only contain properties from nodes that have all services
in the list. The response is a JSON Object mapping node id to its properties.  

POST Parameters
^^^^^^^^^^^^^^^^

Required Parameters

.. list-table::
   :widths: 15 10
   :header-rows: 1

   * - Parameter
     - Description
   * - clusterId
     - Id of the cluster containing the node properties to get.
   * - properties
     - JSON Array containing the properties to return. An empty or nonexistant entry will return all properties.
   * - services
     - JSON Array containing services that must be on the node in order for their properties to be returned. An empty 
       or nonexistant entry will return all nodes.

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
     - If the request POST body is malformed or does not contain ``clusterId``.

Example
^^^^^^^^
.. code-block:: bash

 $ curl -X POST
        -H 'X-Loom-UserID:<userid>'
        -H 'X-Loom-TenantID:<tenantid>'
        -H 'X-Loom-ApiKey:<apikey>'
        -d '{ 
                "clusterId":"00000051",
                "properties":["hostname", "ipaddress"],
                "services":["hadoop-hdfs-datanode"]
            }'
        http://<loom-server>:<loom-port>/<version>/loom/getNodeProperties
 $ {
       "16b10331-0bd4-4e0e-9de5-45334cdcf459": {
           "hostname": "host-1001.local",
           "ipaddress": "1.2.3.4"
       },
       "52e35f27-5ef1-4233-a844-297134bd30a9": {
           "hostname": "host-1002.local",
           "ipaddress": "5.6.7.8"
       }
   }
