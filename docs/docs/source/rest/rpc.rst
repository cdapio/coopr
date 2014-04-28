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

In addition to the standard REST endpoints, a few RPC functions are available to obtain
cluster information.

.. _rpc-statuses:

Get Status of All Clusters
==========================
To get the status of all clusters accessible to the caller, make a POST HTTP request to URI:
::

 /getClusterStatuses

HTTP Responses
^^^^^^^^^^^^^^

.. list-table::
   :widths: 15 10
   :header-rows: 1

   * - Status Code
     - Description
   * - 200 (OK)
     - If update was successful
   * - 401 (UNAUTHORIZED)
     - If the user is unauthorized to make this request.
   * - 404 (NOT FOUND)
     - If the resource requested is not found.

Example
^^^^^^^^
.. code-block:: bash

 $ curl -H 'X-Loom-UserID:admin' 
        -H 'X-Loom-ApiKey:<apikey>'
        http://<loom-server>:<loom-port>/<version>/loom/clusters/00000079/status
 $ {
       "clusterid":"00000079",
       "stepstotal":109,
       "stepscompleted":8,
       "status":"PENDING",
       "actionStatus":"RUNNING",
       "action":"CLUSTER_CREATE"
   }

 $ curl -X POST
        -H 'X-Loom-UserID:<userid>'
        -H 'X-Loom-ApiKey:<apikey>'
        http://<loom-server>:<loom-port>/<version>/loom/getClusterStatuses
 $ [
       {
           "action": "CLUSTER_DELETE",
           "actionstatus": "COMPLETE",
           "clusterid": "00000051",
           "status": "TERMINATED",
           "stepscompleted": 3,
           "stepstotal": 3
       },
       {
           "action": "CLUSTER_DELETE",
           "actionstatus": "COMPLETE",
           "clusterid": "00000021",
           "status": "TERMINATED",
           "stepscompleted": 4,
           "stepstotal": 4
       }
   ]

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
