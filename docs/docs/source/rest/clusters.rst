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
   single: REST API: Clusters

==================
REST API: Clusters
==================

.. include:: /rest/rest-links.rst

Using the Loom REST API, users can create clusters, get cluster details, action plans, and delete clusters.  

.. _cluster-create:

Create a Cluster
==================

To create a new cluster, make a HTTP POST request to URI:
::

 /clusters

The request body must contain name, numMachines, and clusterTemplate.  Optionally, it can contain imagetype, hardwaretype, provider, services, initialLeaseDuration, dnsSuffix, and config.  If the user specifies any optional value, it will override the corresponding default value in the cluster template.

POST Parameters
^^^^^^^^^^^^^^^^

Required Parameters

.. list-table::
   :widths: 15 10
   :header-rows: 1

   * - Parameter
     - Description
   * - name
     - Specifies the name of the cluster. The assigned name must have only alphanumeric, dash(-), dot(.), and underscore(_) characters.
   * - description
     - Provides a description for cluster.
   * - clusterTemplate
     - Specifies the name of the cluster template to use for cluster creation.
   * - numMachines
     - Specifies the number of machines to have in the cluster.
   * - imagetype
     - Optional image type to use across the entire cluster.  Overrides default in the given cluster template.
   * - hardwaretype
     - Optional hardware type to use across the entire cluster.  Overrides default in the given cluster template.
   * - provider 
     - Optional provider to use to create nodes. Overrides default in the given cluster template.
   * - services 
     - Optional array of services to place on the cluster.  Overrides default in the given cluster template.  Must be a subset of compatible services specified in cluster template.
   * - initialLeaseDuration
     - Initial lease duration in milliseconds to use for the cluster. Can only be equal to or less than the initial lease duration specified in the template.
   * - dnsSuffix
     - DNS suffix to use for suggested hostnames for nodes in the cluster.
   * - config 
     - Optional JSON Object to use during cluster creation.  Overrides default in the given cluster template.

HTTP Responses
^^^^^^^^^^^^^^

The server will respond with the id of the cluster added.

.. list-table:: 
   :widths: 15 10 
   :header-rows: 1

   * - Status Code
     - Description
   * - 200 (OK)
     - Successfully created
   * - 400 (BAD_REQUEST)
     - Bad request.  Missing name, clusterTemplate, or numMachines in the request body.
   * - 401 (UNAUTHORIZED)
     - If the user is unauthorized to make this request.

Example
^^^^^^^^
.. code-block:: bash

 $ curl -X POST 
        -H 'X-Loom-UserID:<userid>' 
        -H 'X-Loom-ApiKey:<apikey>'
        -d '{ "name":"hadoop-dev", "description":"my hadoop dev cluster", "numMachines":"5", "clusterTemplate":"hadoop.example" }'
        http://<loom-server>:<loom-port>/<version>/loom/clusters
 $ { "id":"00000079" }

.. _cluster-details:

Get Cluster Details
===================

To retrieve full details about a cluster, make a GET HTTP request to URI:
::

 /clusters/{id}

The cluster is represented as a JSON Object which contains an id, name, description, services, createTime, provider,
clusterTemplate, nodes, jobs, ownerId, and status.  The provider and clusterTemplate details are copied over 
from the respective entities at cluster creation time.  This is so that future changes to a cluster template 
do not affect clusters that were previously created by older versions of the template.  The status is one of
pending, active, incomplete, and terminated.  Jobs are ids of cluster action plans that are described in 
the section about getting an action plan for a cluster.  The ownerId holds the owner of the cluster, the createTime
is a timestamp in milliseconds, and services is a list of services that are on the cluster. Finally, nodes is
an array of nodes that are in the cluster.  Each node is a JSON Object with the id of the node, the clusterId,
an array of services on the node, properties of the node such as hostname and ipaddress, and an array of actions
that have been performed on the node. 

HTTP Responses
^^^^^^^^^^^^^^

.. list-table::
   :widths: 15 10
   :header-rows: 1

   * - Status Code
     - Description
   * - 200 (OK)
     - Successful
   * - 401 (UNAUTHORIZED)
     - If the user is unauthorized to make this request.
   * - 404 (NOT FOUND)
     - If the resource requested is not configured and available in system.

Example
^^^^^^^^
.. code-block:: bash

 $ curl -H 'X-Loom-UserID:<userid>' 
        -H 'X-Loom-ApiKey:<apikey>'
        http://<loom-server>:<loom-port>/<version>/loom/clusters/00000079
 $ {
       "id":"00000079",
       "name":"hadoop-dev",
       "description":"my hadoop dev cluster",
       "createTime": 1391756249454,
       "provider": { ... },
       "clusterTemplate": { ... },
       "services": [ "hadoop-hdfs-namenode", "hadoop-hdfs-datanode", ... ],
       "jobs": [ "00000079-001", "00000079-002" ],
       "ownerId": "user123",
       "status": "pending",
       "nodes": [
           {
               "id": "ee6a7be9-aa81-4601-88eb-6b49d6ff7919",
               "clusterId": "00000079",
               "services": [ ... ],
               "properties": {
                   "hardwaretype": "medium",
                   "flavor": "5",
                   "hostname": "loom-beamer90-1003.local",
                   "imagetype": "centos6",
                   "ipaddress": "123.456.0.1"
               },
               "actions": [
                   {
                       "service": "",
                       "action": "CREATE",
                       "submitTime": 1391756252719,
                       "statusTime": 1391756254791,
                       "status": "complete"
                   },
                   {
                       "service": "",
                       "action": "CONFIRM",
                       "submitTime": 1391756265710,
                       "statusTime": 1391756362476,
                       "status": "complete"
                   },
                   ...
               ]
           },
           ...
       ]
   }


.. _cluster-delete:

Delete a Cluster
=================

To delete a cluster, make a DELETE HTTP request to URI:
::

 /clusters/{id}

This resource request represents an individual cluster for deletion.

HTTP Responses
^^^^^^^^^^^^^^

.. list-table::
   :widths: 15 10
   :header-rows: 1

   * - Status Code
     - Description
   * - 200 (OK)
     - If delete was successful
   * - 401 (UNAUTHORIZED)
     - If the user is unauthorized to make this request.
   * - 404 (NOT FOUND)
     - If the resource requested is not found.
   * - 409 (CONFLICT)
     - If the cluster is in the process of performing some other action.

Example
^^^^^^^^
.. code-block:: bash

 $ curl -X DELETE
        -H 'X-Loom-UserID:<userid>' 
        -H 'X-Loom-ApiKey:<apikey>'
        http://<loom-server>:<loom-port>/<version>/loom/clusters/00000079

.. _cluster-status:

Cluster Status
==================

To get the status of a cluster, make a GET HTTP request to URI:
::

 /clusters/{id}/status

Status of a cluster is a JSON Object with a clusterid, stepstotal, stepscompleted, 
status, actionstatus, and action.  

The status can be one of PENDING, ACTIVE, INCOMPLETE,
and TERMINATED.  PENDING means there is some actions pending, ACTIVE means the cluster 
is active and can be used, INCOMPLETE means there was some previous action failure so 
the cluster may not be usable, but is deletable, and TERMINATED means the cluster is 
inaccessible and all nodes have been removed. 

The action represents the different types of actions that can be performed on a cluster.  As
of today, it is one of SOLVE_LAYOUT, CLUSTER_CREATE, and CLUSTER_DELETE. The actionstatus
describes the status of the action being performed on the cluster, and is one of 
NOT_SUBMITTED, RUNNING, COMPLETE, and FAILED.  


.. _cluster-plan:

Get an Action Plan for a Cluster
================================
To get the plan for a cluster action, make a GET HTTP request to URI:
::

 /clusters/{cluster-id}/plans/{plan-id}

A cluster action plan lists out the tasks that must be performed in order
to complete the cluster action.  A plan is broken up into stages, where each
task in a stage must be completed before the plan is allowed to proceed to 
the next stage.  A stage is an array of tasks.
Each task consists of an id, taskName, nodeId, and optionally
a service.  In short, tasks describe an action that needs to occure on a specific
node in the cluster.  The taskName describe the type of task it is, and is one of 
CREATE, CONFIRM, BOOTSTRAP, INSTALL, CONFIGURE, INITIALIZE, START, and STOP.  The
nodeId specifies which node in the cluster the task needs to run on, and the service
specifies which service the task is for.   

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
   * - 401 (UNAUTHORIZED)
     - If the user is unauthorized to make this request.
   * - 404 (NOT FOUND)
     - If the resource requested is not found.

Example
^^^^^^^^
.. code-block:: bash

 $ curl -H 'X-Loom-UserID:admin' 
        -H 'X-Loom-ApiKey:<apikey>'
        http://<loom-server>:<loom-port>/<version>/loom/clusters/00000079/plans/00000079-001
 $ {
      "id":"1",
      "clusterId":"2",
      "action":"CLUSTER_CREATE",
      "currentStage":0,
      "stages":[
          [
              {
                  "id":"3",
                  "taskName":"CREATE",
                  "nodeId":"4",
                  "service":""
              }
          ],
          [
              {
                  "id":"5",
                  "taskName":"CONFIRM",
                  "nodeId":"6",
                  "service":""
              }
          ],
          [
              {
                  "id":"7",
                  "taskName":"BOOTSTRAP",
                  "nodeId":"8",
                  "service":""
               }
          ],
          ...
     ]
  }

Get all Action Plans for a Cluster
==================================

It is also possible to get all action plans for a cluster for actions
that have been performed or are being performed on a cluster.

To get all the action plans for a cluster, make a GET HTTP request to URI:
::

 /clusters/{cluster-id}/plans

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
   * - 401 (UNAUTHORIZED)
     - If the user is unauthorized to make this request.
   * - 404 (NOT FOUND)
     - If the resource requested is not found.

Example
^^^^^^^^
.. code-block:: bash

 $ curl -H 'X-Loom-UserID:admin' 
        -H 'X-Loom-ApiKey:<apikey>'
        http://<loom-server>:<loom-port>/<version>/loom/clusters/00000079/plans

.. _cluster-get-config:

Get Cluster Configuration
=========================
To get the configuration of a cluster, make a GET HTTP request to URI:
::

 /clusters/{cluster-id}/config

HTTP Responses
^^^^^^^^^^^^^^
.. list-table::
   :widths: 15 10
   :header-rows: 1

   * - Status Code
     - Description
   * - 200 (OK)
     - Successful
   * - 401 (UNAUTHORIZED)
     - If the user is unauthorized to make this request.
   * - 404 (NOT FOUND)
     - If the cluster requested is not found.

Example
^^^^^^^
.. code-block:: bash

 $ curl -H 'X-Loom-UserID:<user-id>' 
        -H 'X-Loom-ApiKey:<apikey>'
        http://<loom-server>:<loom-port>/<version>/loom/clusters/<cluster-id>/config
 $ {
     "hadoop": {
         "core_site": {
             "fs.defaultFS": "hdfs://%host.service.hadoop-hdfs-namenode%",
             "io.file.buffer.size": "131072"
         },
         "hdfs_site": {
             "dfs.blocksize": "134217728",
             "dfs.datanode.du.reserved": "1073741824",
             "dfs.datanode.handler.count": "8",
             "dfs.datanode.max.transfer.threads": "4096",
             "dfs.datanode.max.xcievers": "4096",
             "dfs.namenode.handler.count": "30",
             "dfs.replication": "1"
         },
         "yarn_site": {
             "yarn.nodemanager.resource.memory-mb": "4096",
             "yarn.resourcemanager.address": "%host.service.hadoop-yarn-resourcemanager%:8032",
             "yarn.resourcemanager.admin.address": "%host.service.hadoop-yarn-resourcemanager%:8033",
             "yarn.resourcemanager.hostname": "%host.service.hadoop-yarn-resourcemanager%",
             "yarn.resourcemanager.resource-tracker.address": "%host.service.hadoop-yarn-resourcemanager%:8031",
             "yarn.resourcemanager.scheduler.address": "%host.service.hadoop-yarn-resourcemanager%:8030"
         }
     }
  }

.. _cluster-update-config:

Update Cluster Configuration
============================
To update the configuration of a cluster, make a PUT HTTP request to URI:
::

 /clusters/{cluster-id}/config

The request body must be a JSON Object that includes a ``config`` key whose value is a 
JSON Object that will replace the current cluster configuration. It may contain a 
``restart`` key whose value is true or false, indicating whether or not cluster services should 
be restarted along with the configuration change. If the ``restart`` key is not present, the 
value defaults to true. The order of service restarts is derived from
service dependencies, ensuring that if service A depends on service B, service A will be stopped
before service B is stopped, and service B will be started before service A is started. After the
request is made, status calls can be made to check on the status of the cluster configure job. 
If the configure operation fails, the cluster is placed in an inconsistent state where the cluster
can be configured again or deleted.

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
     - If the request body is specified incorrectly.
   * - 401 (UNAUTHORIZED)
     - If the user is unauthorized to make this request.
   * - 404 (NOT FOUND)
     - If the cluster requested is not found.
   * - 409 (CONFLICT)
     - The cluster is not in a configurable state.

Example
^^^^^^^
.. code-block:: bash

 $ curl -H 'X-Loom-UserID:<user-id>' 
        -H 'X-Loom-ApiKey:<apikey>'
        -X PUT
        -d '{
                "config": {
                    "hadoop": {
                        "core_site": {
                            "fs.defaultFS": "hdfs://%host.service.hadoop-hdfs-namenode%",
                            "io.file.buffer.size": "131072"
                        },
                        "hdfs_site": {
                            "dfs.blocksize": "134217728",
                            "dfs.datanode.du.reserved": "1073741824",
                            "dfs.datanode.handler.count": "8",
                            "dfs.datanode.max.transfer.threads": "4096",
                            "dfs.datanode.max.xcievers": "4096",
                            "dfs.namenode.handler.count": "30",
                            "dfs.replication": "1"
                        },
                        "yarn_site": {
                            "yarn.nodemanager.resource.memory-mb": "4096",
                            "yarn.resourcemanager.address": "%host.service.hadoop-yarn-resourcemanager%:8032",
                            "yarn.resourcemanager.admin.address": "%host.service.hadoop-yarn-resourcemanager%:8033",
                            "yarn.resourcemanager.hostname": "%host.service.hadoop-yarn-resourcemanager%",
                            "yarn.resourcemanager.resource-tracker.address": "%host.service.hadoop-yarn-resourcemanager%:8031",
                            "yarn.resourcemanager.scheduler.address": "%host.service.hadoop-yarn-resourcemanager%:8030"
                        }
                    }
                },
                "restart": "false" 
            }'
        http://<loom-server>:<loom-port>/<version>/loom/clusters/<cluster-id>/config

.. _cluster-get-services:

Get Cluster Services
====================
To get the services on a cluster, make a GET HTTP request to URI:
::

 /clusters/{cluster-id}/services

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
   * - 401 (UNAUTHORIZED)
     - If the user is unauthorized to make this request.
   * - 404 (NOT FOUND)
     - If the cluster requested is not found.

Example
^^^^^^^
.. code-block:: bash

 $ curl -H 'X-Loom-UserID:<user-id>' 
        -H 'X-Loom-ApiKey:<apikey>'
        http://<loom-server>:<loom-port>/<version>/loom/clusters/<cluster-id>/services
 [
     "hadoop-hdfs-namenode",
     "hadoop-hdfs-datanode",
     "hadoop-yarn-resourcemanager",
     "hadoop-yarn-nodemanager"
 ]


.. _cluster-add-services:

Add Services to a Cluster
=========================
To add services to a cluster, make a POST HTTP request to URI:
::

 /clusters/{cluster-id}/services

The POST body must be a JSON Object containing a ``services`` key whose value is a JSON Array
of services to add to the cluster. The services must be compatible with the cluster template
used to create the cluster. After the request is made, status calls can be made to check on
the status of the add services cluster operation.

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
     - If the request is specified incorrectly, or if the services to add are incompatible
       or the cluster is missing services they require.
   * - 401 (UNAUTHORIZED)
     - If the user is unauthorized to make this request.
   * - 404 (NOT FOUND)
     - If the cluster requested is not found.
   * - 409 (CONFLICT)
     - If the cluster is not in a state where services can be added.

Example
^^^^^^^
.. code-block:: bash

 $ curl -H 'X-Loom-UserID:<user-id>' 
        -H 'X-Loom-ApiKey:<apikey>'
        -X POST
        -d '{ "services": [ "zookeeper-server", "hbase-master", "hbase-regionserver" ] }'
        http://<loom-server>:<loom-port>/<version>/loom/clusters/<cluster-id>/services

.. _cluster-stop-services:

Stop all Services on a Cluster
==============================
To stop all services on a cluster, make a POST HTTP request to URI:
::

 /clusters/{cluster-id}/services/stop

There is no POST body expected. The ordering of service stops is based on the dependencies
defined for the services. If service A requires service B, service A will be stopped before
service B. After the request is made, a status call can be made to check on the status of 
the service stop operation. A cluster must be in the ACTIVE state in order for the stop
operation to be allowed.

HTTP Responses
^^^^^^^^^^^^^^
.. list-table::
   :widths: 15 10
   :header-rows: 1

   * - Status Code
     - Description
   * - 200 (OK)
     - Successful
   * - 401 (UNAUTHORIZED)
     - If the user is unauthorized to make this request.
   * - 404 (NOT FOUND)
     - If the cluster requested is not found.
   * - 409 (CONFLICT)
     - If the cluster is not in a state where services can be stopped.

Example
^^^^^^^
.. code-block:: bash

 $ curl -H 'X-Loom-UserID:<user-id>' 
        -H 'X-Loom-ApiKey:<apikey>'
        -X POST
        http://<loom-server>:<loom-port>/<version>/loom/clusters/<cluster-id>/services/stop

.. _cluster-stop-service:

Stop a Service on a Cluster
===========================
To stop a specific service on a cluster, make a POST HTTP request to URI:
::

 /clusters/{cluster-id}/services/{service-id}/stop

There is no POST body expected. If there are services that depend on the service being stopped,
those services will be stopped first. For example, if service A depends on service B, and 
service B is being stopped, service A will automatically be stopped before service B is stopped.
After the request is made, a status call can be made to check on the status of 
the service start operation. A cluster must be in the ACTIVE state in order for the stop
operation to be allowed.

HTTP Responses
^^^^^^^^^^^^^^
.. list-table::
   :widths: 15 10
   :header-rows: 1

   * - Status Code
     - Description
   * - 200 (OK)
     - Successful
   * - 401 (UNAUTHORIZED)
     - If the user is unauthorized to make this request.
   * - 404 (NOT FOUND)
     - If the cluster requested is not found.
   * - 409 (CONFLICT)
     - If the cluster is not in a state where services can be restarted.

Example
^^^^^^^
.. code-block:: bash

 $ curl -H 'X-Loom-UserID:<user-id>' 
        -H 'X-Loom-ApiKey:<apikey>'
        -X POST
        http://<loom-server>:<loom-port>/<version>/loom/clusters/<cluster-id>/services/<service-id>/stop

.. _cluster-start-services:

Start all Services on a Cluster
================================
To start all services on a cluster, make a POST HTTP request to URI:
::

 /clusters/{cluster-id}/services/start

There is no POST body expected. The ordering of service starts is based on the dependencies
defined for the services. If service A requires service B, service B will be started before
service A. After the request is made, a status call can be made to check on the status of 
the service stop operation. A cluster must be in the ACTIVE state in order for the start
operation to be allowed.

HTTP Responses
^^^^^^^^^^^^^^
.. list-table::
   :widths: 15 10
   :header-rows: 1

   * - Status Code
     - Description
   * - 200 (OK)
     - Successful
   * - 401 (UNAUTHORIZED)
     - If the user is unauthorized to make this request.
   * - 404 (NOT FOUND)
     - If the cluster requested is not found.
   * - 409 (CONFLICT)
     - If the cluster is not in a state where services can be started.

Example
^^^^^^^
.. code-block:: bash

 $ curl -H 'X-Loom-UserID:<user-id>' 
        -H 'X-Loom-ApiKey:<apikey>'
        -X POST
        http://<loom-server>:<loom-port>/<version>/loom/clusters/<cluster-id>/services/start

.. _cluster-start-service:

Start a Service on a Cluster
============================
To start a specific service on a cluster, make a POST HTTP request to URI:
::

 /clusters/{cluster-id}/services/{service-id}/start

There is no POST body expected. If the service being started depends on any other services,
those services will be started first. For example, if service A depends on service B, and 
service A is being started, service B will automatically be started before service A is started.
After the request is made, a status call can be made to check on the status of 
the service start operation. A cluster must be in the ACTIVE state in order for the start
operation to be allowed.

HTTP Responses
^^^^^^^^^^^^^^
.. list-table::
   :widths: 15 10
   :header-rows: 1

   * - Status Code
     - Description
   * - 200 (OK)
     - Successful
   * - 401 (UNAUTHORIZED)
     - If the user is unauthorized to make this request.
   * - 404 (NOT FOUND)
     - If the cluster requested is not found.
   * - 409 (CONFLICT)
     - If the cluster is not in a state where services can be restarted.

Example
^^^^^^^
.. code-block:: bash

 $ curl -H 'X-Loom-UserID:<user-id>' 
        -H 'X-Loom-ApiKey:<apikey>'
        -X POST
        http://<loom-server>:<loom-port>/<version>/loom/clusters/<cluster-id>/services/<service-id>/start

.. _cluster-restart-services:

Restart all Services on a Cluster
=================================
To restart all services on a cluster, make a POST HTTP request to URI:
::

 /clusters/{cluster-id}/services/restart

There is no POST body expected. The ordering of service starts and stops is based on the 
dependencies defined for the services. If service A requires service B, service A will be 
stopped before service B is stopped. Service B will be started after it has been stopped,
and finally service A will be started after service B has been started.
After the request is made, a status call can be made to check on the status of 
the service restart operation. A cluster must be in the ACTIVE state in order for the restart
operation to be allowed.

HTTP Responses
^^^^^^^^^^^^^^
.. list-table::
   :widths: 15 10
   :header-rows: 1

   * - Status Code
     - Description
   * - 200 (OK)
     - Successful
   * - 401 (UNAUTHORIZED)
     - If the user is unauthorized to make this request.
   * - 404 (NOT FOUND)
     - If the cluster requested is not found.
   * - 409 (CONFLICT)
     - If the cluster is not in a state where services can be restarted.

Example
^^^^^^^
.. code-block:: bash

 $ curl -H 'X-Loom-UserID:<user-id>' 
        -H 'X-Loom-ApiKey:<apikey>'
        -X POST
        http://<loom-server>:<loom-port>/<version>/loom/clusters/<cluster-id>/services/restart


.. _cluster-restart-service:

Restart a Service on a Cluster
==============================
To restart a specific service on a cluster, make a POST HTTP request to URI:
::

 /clusters/{cluster-id}/services/{service-id}/restart

There is no POST body expected. If there are services that depend on the service being
restarted, they will be restarted as well. The ordering of service starts and stops is based on the 
dependencies defined for the services. If service A requires service B, service A will be 
stopped before service B is stopped. Service B will be started after it has been stopped,
and finally service A will be started after service B has been started.
After the request is made, a status call can be made to check on the status of 
the service restart operation. A cluster must be in the ACTIVE state in order for the restart
operation to be allowed.

HTTP Responses
^^^^^^^^^^^^^^
.. list-table::
   :widths: 15 10
   :header-rows: 1

   * - Status Code
     - Description
   * - 200 (OK)
     - Successful
   * - 401 (UNAUTHORIZED)
     - If the user is unauthorized to make this request.
   * - 404 (NOT FOUND)
     - If the cluster requested is not found.
   * - 409 (CONFLICT)
     - If the cluster is not in a state where services can be restarted.

Example
^^^^^^^
.. code-block:: bash

 $ curl -H 'X-Loom-UserID:<user-id>' 
        -H 'X-Loom-ApiKey:<apikey>'
        -X POST
        http://<loom-server>:<loom-port>/<version>/loom/clusters/<cluster-id>/services/<service-id>/restart
