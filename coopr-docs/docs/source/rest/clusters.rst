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
   single: REST API: Clusters

==================
REST API: Clusters
==================

.. include:: /rest/rest-links.rst

Using the REST API, users can create clusters, get cluster details, action plans, and delete clusters.  

.. _cluster-create:

Create a Cluster
==================

To create a new cluster, make a HTTP POST request to URI:
::

 /clusters

The request body must contain name, numMachines, and clusterTemplate.  Optionally, it can contain imagetype, hardwaretype, provider, providerFields, services, initialLeaseDuration, dnsSuffix, and config.  If the user specifies any optional value, it will override the corresponding default value in the cluster template. Trying to create a cluster that would violate the tenant quotas will result in a failure.

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
   * - providerFields
     - JSON Object containing key-values to be used by the provider plugin when provisioning nodes. See the section on :ref:`Provider Fields <cluster-provider-fields>` for more information.
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

The server will respond with the id of the cluster added if the create request was successful. If there were
required fields missing in the request, it will respond with details about the missing fields. See the section on
:ref:`Provider Fields <cluster-provider-fields>` for more information. 


.. list-table:: 
   :widths: 15 10 
   :header-rows: 1

   * - Status Code
     - Description
   * - 200 (OK)
     - Successfully created
   * - 400 (BAD_REQUEST)
     - Bad request.  Missing name, clusterTemplate, numMachines, or required providerFields in the request body.
   * - 401 (UNAUTHORIZED)
     - If the user is unauthorized to make this request.
   * - 409 (CONFLICT)
     - If the creation of the cluster would cause tenant quotas to be violated.

Example
^^^^^^^^
.. code-block:: bash

 $ curl -X POST 
        -H 'Coopr-UserID:<userid>' 
        -H 'Coopr-TenantID:<tenantid>'
        -H 'Coopr-ApiKey:<apikey>'
        -d '{ "name":"hadoop-dev", "description":"my hadoop dev cluster", "numMachines":"5", "clusterTemplate":"hadoop.example" }'
        http://<server>:<port>/<version>/clusters
 $ { "id":"00000079" }

.. _cluster-retrieve-all:

Get All Cluster Details
=======================

To retrieve a summary of details about all clusters visible to a user, make a GET HTTP request to URI:
::

 /clusters

This returns an JSON Array of JSON Objects that represent a cluster. Each cluster contains an id, name, description,
createTime, expireTime, services, numNodes, status, provider, clusterTemplate, and progress.
The provider and clusterTemplate fields are JSON Objects containing a single field called name,
with the entity name as the value. The services field is a JSON Array of the names of all services on the cluster.
The progress field contains the progress of the last job performed on the cluster, or the progress of the job
currently being performed on the cluster. It contains an action, actionstatus, stepstotal, and stepscompleted.

The call may optionally contain a status http parameter which will filter the clusters to only include clusters
that match the given status. The value for the status parameter is a comma separated list of cluster statuses,
where a status is one of ``pending``, ``active``, ``incomplete``, ``inconsistent``, or ``terminated``. A cluster
is pending if there is currently an action being performed on the cluster. For example, a cluster that is in
the process of being created will be in the pending state. A cluster is active if it has been created and the
last action performed on it was successful. For example, a cluster will transition from pending to active active
it is successfully created. A cluster is incomplete if creation failed part way through the job. A cluster is 
inconsistent if the cluster was successfully created in the past, but some subsequent action on the cluster
failed. For example, if the a cluster reconfigure is attempted but fails, the cluster will be inconsistent.
A cluster is terminated if it was successfully deleted. Any state besides terminated indicates that there are 
live nodes associated with the cluster. 

HTTP Parameters
^^^^^^^^^^^^^^^

.. list-table::
   :widths: 15 10
   :header-rows: 1

   * - Parameter
     - Description
   * - status
     - comma separated list of statuses. Returned clusters must have a status that matches one of the stasuses in the list. Possible statuses are 'pending', 'active', 'incomplete', 'inconsistent', and 'terminated'.

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
   * - 401 (UNAUTHORIZED)
     - If the user is unauthorized to make this request.

Example
^^^^^^^^
.. code-block:: bash

 $ curl -H 'Coopr-UserID:<userid>'
        -H 'Coopr-TenantID:<tenantid>'
        -H 'Coopr-ApiKey:<apikey>'
        http://<coopr-server>:<coopr-port>/<version>/coopr/clusters?status=pending,active
 $ [
       {
           "id":"00000079",
           "name":"hadoop-dev",
           "description":"my hadoop dev cluster",
           "createTime": 1391756249454,
           "expireTime": 1391767249454,
           "provider": {
               "name": "aws"
           },
           "clusterTemplate": {
               "name": "hadoop-distributed"
           },
           "services": [ "hadoop-hdfs-namenode", "hadoop-hdfs-datanode", ... ],
           "ownerId": "user123",
           "status": "pending",
           "numNodes": 3,
           "progress": {
               "action": "cluster_create",
               "actionstatus": "running",
               "stepstotal": 81,
               "stepscompleted" 49
           }
       },
       ...
   ]

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

 $ curl -H 'Coopr-UserID:<userid>' 
        -H 'Coopr-TenantID:<tenantid>'
        -H 'Coopr-ApiKey:<apikey>'
        http://<server>:<port>/<version>/clusters/00000079
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
                   "hostname": "beamer90-1003.local",
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

The request may require a JSON Object as its body if the provider used to create the cluster requires it.
See the section on :ref:`Provider Fields <cluster-provider-fields>` for more information.

HTTP Responses
^^^^^^^^^^^^^^

.. list-table::
   :widths: 15 10
   :header-rows: 1

   * - Status Code
     - Description
   * - 200 (OK)
     - If delete was successful
   * - 400 (BAD REQUEST)
     - If there are provider fields missing from the request or the request body is malformed.
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
        -H 'Coopr-UserID:<userid>' 
        -H 'Coopr-TenantID:<tenantid>'
        -H 'Coopr-ApiKey:<apikey>'
        http://<server>:<port>/<version>/clusters/00000079
 $ curl -X DELETE
        -H 'Coopr-UserID:<userid>' 
        -H 'Coopr-TenantID:<tenantid>'
        -H 'Coopr-ApiKey:<apikey>'
        http://<server>:<port>/<version>/clusters/00000083
 $ {
       "missingFields": [
           {
               "api_password": {
                   "label": "API key",
                   "override": false,
                   "sensitive": true,
                   "tip": "Your API key",
                   "type": "password"
               },
               "ssh_key": {
                   "label": "SSH key",
                   "override": false,
                   "sensitive": true,
                   "tip": "Your ssh key",
                   "type": "file"
               }
           }
       ]
   }

.. _cluster-status:

Cluster Status
==================

To get the status of a cluster, make a GET HTTP request to URI:
::

 /clusters/{id}/status

Status of a cluster is a JSON Object with a clusterid, stepstotal, stepscompleted, 
status, actionstatus, and action.  

The status can be one of pending, active, incomplete, and terminated.
Pending means there is some actions pending, active means the cluster 
is active and can be used, incomplete means there was some previous action failure so 
the cluster may not be usable, but is deletable, and terminated means the cluster is 
inaccessible and all nodes have been removed. 

The action represents the different types of actions that can be performed on a cluster.  
It is one of solve_layout, cluster_create, cluster_delete, cluster_configure,
cluster_configure_with_restart, stop_services, start_services, restart_services, and add_services. 
The actionstatus describes the status of the action being performed on the cluster, and is one of 
not_submitted, running, complete, or failed.

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
   * - 401 (UNAUTHORIZED)
     - If the user is unauthorized to make this request.
   * - 404 (NOT_FOUND)
     - If the cluster could not be found.

Example
^^^^^^^^
.. code-block:: bash

 $ curl -H 'Coopr-UserID:<userid>'
        -H 'Coopr-TenantID:<tenantid>'
        -H 'Coopr-ApiKey:<apikey>'
        http://<coopr-server>:<coopr-port>/<version>/coopr/clusters/00000079/status
 $ {
       "clusterid":"00000079",
       "status": "pending",
       "action": "cluster_create",
       "actionstatus": "running",
       "stepstotal": 81,
       "stepscompleted" 49
   }

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

 $ curl -H 'Coopr-UserID:admin' 
        -H 'Coopr-TenantID:<tenantid>'
        -H 'Coopr-ApiKey:<apikey>'
        http://<server>:<port>/<version>/clusters/00000079/plans/00000079-001
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

 $ curl -H 'Coopr-UserID:admin' 
        -H 'Coopr-TenantID:<tenantid>'
        -H 'Coopr-ApiKey:<apikey>'
        http://<server>:<port>/<version>/clusters/00000079/plans

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

 $ curl -H 'Coopr-UserID:<user-id>' 
        -H 'Coopr-TenantID:<tenantid>'
        -H 'Coopr-ApiKey:<apikey>'
        http://<server>:<port>/<version>/clusters/<cluster-id>/config
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

The request body may require a ``providerFields`` entry if the provider used to create the cluster requires it.
See the section on :ref:`Provider Fields <cluster-provider-fields>` for more information.

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
     - If the request body is malformed, or if required provider fields are missing.
   * - 401 (UNAUTHORIZED)
     - If the user is unauthorized to make this request.
   * - 404 (NOT FOUND)
     - If the cluster requested is not found.
   * - 409 (CONFLICT)
     - The cluster is not in a configurable state.

Example
^^^^^^^
.. code-block:: bash

 $ curl -H 'Coopr-UserID:<user-id>' 
        -H 'Coopr-ApiKey:<apikey>'
        -H 'Coopr-TenantID:<tenantid>'
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
        http://<server>:<port>/<version>/clusters/<cluster-id>/config

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

 $ curl -H 'Coopr-UserID:<user-id>' 
        -H 'Coopr-TenantID:<tenantid>'
        -H 'Coopr-ApiKey:<apikey>'
        http://<server>:<port>/<version>/clusters/<cluster-id>/services
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

The request body may require a ``providerFields`` entry if the provider used to create the cluster requires it.
See the section on :ref:`Provider Fields <cluster-provider-fields>` for more information.

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
     - If the request is malformed, or if the services to add are incompatible,
       or the cluster is missing services they require, or if required provider fields are missing.
   * - 401 (UNAUTHORIZED)
     - If the user is unauthorized to make this request.
   * - 404 (NOT FOUND)
     - If the cluster requested is not found.
   * - 409 (CONFLICT)
     - If the cluster is not in a state where services can be added.

Example
^^^^^^^
.. code-block:: bash

 $ curl -H 'Coopr-UserID:<user-id>' 
        -H 'Coopr-TenantID:<tenantid>'
        -H 'Coopr-ApiKey:<apikey>'
        -X POST
        -d '{ "services": [ "zookeeper-server", "hbase-master", "hbase-regionserver" ] }'
        http://<server>:<port>/<version>/clusters/<cluster-id>/services

.. _cluster-stop-services:

Stop all Services on a Cluster
==============================
To stop all services on a cluster, make a POST HTTP request to URI:
::

 /clusters/{cluster-id}/services/stop

The ordering of service stops is based on the dependencies
defined for the services. If service A requires service B, service A will be stopped before
service B. After the request is made, a status call can be made to check on the status of 
the service stop operation. A cluster must be in the ACTIVE state in order for the stop
operation to be allowed.

The request may require a JSON Object as its body if the provider used to create the cluster requires it.
See the section on :ref:`Provider Fields <cluster-provider-fields>` for more information.

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
     - If the request body is malformed or if there are missing provider fields.
   * - 401 (UNAUTHORIZED)
     - If the user is unauthorized to make this request.
   * - 404 (NOT FOUND)
     - If the cluster requested is not found.
   * - 409 (CONFLICT)
     - If the cluster is not in a state where services can be stopped.

Example
^^^^^^^
.. code-block:: bash

 $ curl -H 'Coopr-UserID:<user-id>' 
        -H 'Coopr-TenantID:<tenantid>'
        -H 'Coopr-ApiKey:<apikey>'
        -X POST
        http://<server>:<port>/<version>/clusters/<cluster-id>/services/stop

.. _cluster-stop-service:

Stop a Service on a Cluster
===========================
To stop a specific service on a cluster, make a POST HTTP request to URI:
::

 /clusters/{cluster-id}/services/{service-id}/stop

If there are services that depend on the service being stopped,
those services will be stopped first. For example, if service A depends on service B, and 
service B is being stopped, service A will automatically be stopped before service B is stopped.
After the request is made, a status call can be made to check on the status of 
the service start operation. A cluster must be in the ACTIVE state in order for the stop
operation to be allowed.

The request may require a JSON Object as its body if the provider used to create the cluster requires it.
See the section on :ref:`Provider Fields <cluster-provider-fields>` for more information.

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
     - If the request body is malformed or if there are missing provider fields.
   * - 401 (UNAUTHORIZED)
     - If the user is unauthorized to make this request.
   * - 404 (NOT FOUND)
     - If the cluster requested is not found.
   * - 409 (CONFLICT)
     - If the cluster is not in a state where services can be restarted.

Example
^^^^^^^
.. code-block:: bash

 $ curl -H 'Coopr-UserID:<user-id>' 
        -H 'Coopr-TenantID:<tenantid>'
        -H 'Coopr-ApiKey:<apikey>'
        -X POST
        http://<server>:<port>/<version>/clusters/<cluster-id>/services/<service-id>/stop

.. _cluster-start-services:

Start all Services on a Cluster
================================
To start all services on a cluster, make a POST HTTP request to URI:
::

 /clusters/{cluster-id}/services/start

The ordering of service starts is based on the dependencies
defined for the services. If service A requires service B, service B will be started before
service A. After the request is made, a status call can be made to check on the status of 
the service stop operation. A cluster must be in the ACTIVE state in order for the start
operation to be allowed.

The request may require a JSON Object as its body if the provider used to create the cluster requires it.
See the section on :ref:`Provider Fields <cluster-provider-fields>` for more information.

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
     - If the request body is malformed or if there are missing provider fields.
   * - 401 (UNAUTHORIZED)
     - If the user is unauthorized to make this request.
   * - 404 (NOT FOUND)
     - If the cluster requested is not found.
   * - 409 (CONFLICT)
     - If the cluster is not in a state where services can be started.

Example
^^^^^^^
.. code-block:: bash

 $ curl -H 'Coopr-UserID:<user-id>' 
        -H 'Coopr-TenantID:<tenantid>'
        -H 'Coopr-ApiKey:<apikey>'
        -X POST
        http://<server>:<port>/<version>/clusters/<cluster-id>/services/start

.. _cluster-start-service:

Start a Service on a Cluster
============================
To start a specific service on a cluster, make a POST HTTP request to URI:
::

 /clusters/{cluster-id}/services/{service-id}/start

If the service being started depends on any other services,
those services will be started first. For example, if service A depends on service B, and 
service A is being started, service B will automatically be started before service A is started.
After the request is made, a status call can be made to check on the status of 
the service start operation. A cluster must be in the ACTIVE state in order for the start
operation to be allowed.

The request may require a JSON Object as its body if the provider used to create the cluster requires it.
See the section on :ref:`Provider Fields <cluster-provider-fields>` for more information.

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
     - If the request body is malformed or if there are missing provider fields.
   * - 401 (UNAUTHORIZED)
     - If the user is unauthorized to make this request.
   * - 404 (NOT FOUND)
     - If the cluster requested is not found.
   * - 409 (CONFLICT)
     - If the cluster is not in a state where services can be restarted.

Example
^^^^^^^
.. code-block:: bash

 $ curl -H 'Coopr-UserID:<user-id>' 
        -H 'Coopr-TenantID:<tenantid>'
        -H 'Coopr-ApiKey:<apikey>'
        -X POST
        http://<server>:<port>/<version>/clusters/<cluster-id>/services/<service-id>/start

.. _cluster-restart-services:

Restart all Services on a Cluster
=================================
To restart all services on a cluster, make a POST HTTP request to URI:
::

 /clusters/{cluster-id}/services/restart

The ordering of service starts and stops is based on the 
dependencies defined for the services. If service A requires service B, service A will be 
stopped before service B is stopped. Service B will be started after it has been stopped,
and finally service A will be started after service B has been started.
After the request is made, a status call can be made to check on the status of 
the service restart operation. A cluster must be in the ACTIVE state in order for the restart
operation to be allowed.

The request may require a request body if the provider used to create the cluster requires it.
See the section on :ref:`Provider Fields <cluster-provider-fields>` for more information.

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
     - If the request body is malformed or if there are missing provider fields.
   * - 401 (UNAUTHORIZED)
     - If the user is unauthorized to make this request.
   * - 404 (NOT FOUND)
     - If the cluster requested is not found.
   * - 409 (CONFLICT)
     - If the cluster is not in a state where services can be restarted.

Example
^^^^^^^
.. code-block:: bash

 $ curl -H 'Coopr-UserID:<user-id>' 
        -H 'Coopr-TenantID:<tenantid>'
        -H 'Coopr-ApiKey:<apikey>'
        -X POST
        http://<server>:<port>/<version>/clusters/<cluster-id>/services/restart


.. _cluster-restart-service:

Restart a Service on a Cluster
==============================
To restart a specific service on a cluster, make a POST HTTP request to URI:
::

 /clusters/{cluster-id}/services/{service-id}/restart

If there are services that depend on the service being
restarted, they will be restarted as well. The ordering of service starts and stops is based on the 
dependencies defined for the services. If service A requires service B, service A will be 
stopped before service B is stopped. Service B will be started after it has been stopped,
and finally service A will be started after service B has been started.
After the request is made, a status call can be made to check on the status of 
the service restart operation. A cluster must be in the ACTIVE state in order for the restart
operation to be allowed.

The request may require a JSON Object as its body if the provider used to create the cluster requires it.
See the section on :ref:`Provider Fields <cluster-provider-fields>` for more information.

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
     - If the request body is malformed or if there are missing provider fields.
   * - 401 (UNAUTHORIZED)
     - If the user is unauthorized to make this request.
   * - 404 (NOT FOUND)
     - If the cluster requested is not found.
   * - 409 (CONFLICT)
     - If the cluster is not in a state where services can be restarted.

Example
^^^^^^^
.. code-block:: bash

 $ curl -H 'Coopr-UserID:<user-id>' 
        -H 'Coopr-TenantID:<tenantid>'
        -H 'Coopr-ApiKey:<apikey>'
        -X POST
        http://<server>:<port>/<version>/clusters/<cluster-id>/services/<service-id>/restart

.. _cluster-sync-template:

Sync Cluster Template to Current Version
========================================
When a cluster is created, a copy of the template used to create the cluster is copied into the 
cluster. Any future operations are based off the copy of the template. As templates evolve, it is
sometimes desirable to sync an existing cluster to the current version of the template used to 
create it. For example, cluster 123 is created with a template. As the template evolves, service A 
is added to the compatibility list of the template. Since cluster 123 has a copy of a previous version
of the template, service A cannot be added to the cluster. In order to allow service A to be added to
cluster 123, the owner of the cluster syncs the template to its current version. It should be noted 
that things like a templates defaults section will have no effect on existing clusters, since those are
default values used during cluster creation. In effect, only template compatibilities and constraints
will have any effect on what can and cant be done to an existing cluster. 
To sync a cluster's template to the current version, make a POST HTTP request to URI:
::

 /clusters/{cluster-id}/clustertemplate/sync

There is no POST body expected. A cluster must be in the ACTIVE state in order for the sync
to be allowed. If the template used to create the cluster no longer exists, the sync will not
be allowed. If syncing the template would cause the cluster to become invalid according to the
updated template, the sync will not be allowed.

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
     - If the template sync would result in an invalid cluster.
   * - 401 (UNAUTHORIZED)
     - If the user is unauthorized to make this request.
   * - 404 (NOT FOUND)
     - If the cluster requested is not found, its template is not found, or its nodes are not found.
   * - 409 (CONFLICT)
     - If the cluster is not in a state where its template can be synced to the current version.

Example
^^^^^^^
.. code-block:: bash

 $ curl -H 'Coopr-UserID:<user-id>' 
        -H 'Coopr-TenantID:<tenantid>'
        -H 'Coopr-ApiKey:<apikey>'
        -X POST
        http://<server>:<port>/<version>/clusters/<cluster-id>/clustertemplate/sync

.. _cluster-extend-lease:

Extend the Lease on a Cluster
=============================
A cluster may have a lease attached to it if the template specified a lease, or if the user specified
a lease at cluster creation time. Depending on the lease settings, a user may be allowed to extend
the lease on the cluster. To extend a cluster's lease, make a POST HTTP request to URI:
::

 /clusters/{cluster-id}

The POST body must be a JSON Object with an 'expireTime' key whose value is the new desired expiration
timestamp in milliseconds. If there is a lease step specified in the template, the new timestamp cannot
be greater than the current expire time plus the lease step. If there is a max lease specified in the
template, the new timestamp cannot be greater than the create time plus the max lease.

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
     - If the request is malformed, or the new expire time is not allowed.
   * - 401 (UNAUTHORIZED)
     - If the user is unauthorized to make this request.
   * - 404 (NOT FOUND)
     - If the cluster requested is not found.

Example
^^^^^^^
.. code-block:: bash

 $ curl -H 'Coopr-UserID:<user-id>' 
        -H 'Coopr-TenantID:<tenantid>'
        -H 'Coopr-ApiKey:<apikey>'
        -X POST
        -d '{
               "expireTime": 1234567890
           }'
        http://<server>:<port>/<version>/clusters/<cluster-id>


.. _cluster-provider-fields:

Provider Fields in Cluster Requests
===================================

Requests to perform cluster operations can include in their request body a ``providerFields`` entry.
The value is a JSON Object containing key-value pairs of fields a provider may use when performing tasks.
Usually, these fields are supplied when creating a cluster and are stored for the lifetime of the cluster.
However, providers can specify that certain fields are sensitive. Sensitive fields usually contain private
information such as ssh or api keys or passwords. Sensitive fields may not survive the
lifetime of the cluster, as it is possible to configure them to auto-delete after some amount of time,
or the system administrator may decide to wipe them manually. If a sensitive field is deleted before a
cluster is deleted, subsequent cluster operations may require those fields to be included in the request again.
For example, if an ssh key was deleted, it must be provided again to allow provisioner workers to ssh onto
cluster nodes in order to execute tasks. Cluster create, delete, and configure requests may include
provider fields. Service add, stop, start, and restart requests may also include provider fields.  

If there are some required provider fields missing, the relevant API will return a 400 error code along with
a JSON Object describing the fields that are missing. The response will
contain a ``missingFields`` key which will be an array of JSON Objects containing the missing field as the
key and the schema of the field as the value. It is an array of objects because there can be multiple sets
of required fields. For example, it is possible that a provider may require both field1 and field2, or it may
require field3 and field4. If only field2 is given in the request, then either field1 is missing, or both field3
and field4 are missing.

Example
^^^^^^^
.. code-block:: bash

 $ curl -X DELETE 
        -H 'Coopr-UserID:<userid>' 
        -H 'Coopr-TenantID:<tenantid>'
        -H 'Coopr-ApiKey:<apikey>'
        http://<server>:<port>/<version>/clusters/00000083
   {
       "missingFields": [
           {
               "api_password": {
                   "label": "API key",
                   "override": false,
                   "sensitive": true,
                   "tip": "Your API key",
                   "type": "password"
               },
               "ssh_key": {
                   "label": "SSH key",
                   "override": false,
                   "sensitive": true,
                   "tip": "Your ssh key",
                   "type": "file"
               }
           }
       ]
   }

In the above example, the cluster delete failed because the api password and ssh key fields were deleted at some point. 
In order to delete the cluster, the client must re-supply those fields.

.. code-block:: bash

 $ curl -X DELETE 
        -H 'Coopr-UserID:<userid>' 
        -H 'Coopr-TenantID:<tenantid>'
        -H 'Coopr-ApiKey:<apikey>'
        -d '{
               "providerFields":{
                   "api_password": "password",
                   "ssh_key": "keycontents"
               }
           }'
        http://<server>:<port>/<version>/clusters/00000083


Pause a Cluster Job
=============================
To pause a cluster job that is currently running, make a POST HTTP request to URI:
::

 /clusters/{cluster-id}/pause

A Pausing a job prevents additional tasks from being started, but any tasks that are in progress when a job is paused
will continue to run until they are completed.
Admins and cluster owners are allowed to pause a cluster job. If there is no job in progress,
the request is a no-op and a 200 is returned.

HTTP Responses
^^^^^^^^^^^^^^
.. list-table::
   :widths: 15 10
   :header-rows: 1

   * - Status Code
     - Description
   * - 200 (OK)
     - Successful
   * - 404 (NOT FOUND)
     - If the cluster is in the process of performing some other action.
   * - 500 (INTERNAL_SERVER_ERROR)
     - Internal server error

Example
^^^^^^^
.. code-block:: bash

 $ curl -H 'Coopr-UserID:<user-id>'
        -H 'Coopr-TenantID:<tenantid>'
        -H 'Coopr-ApiKey:<apikey>'
        -X POST

        http://<server>:<port>/<version>/clusters/<cluster-id>/pause

Resume a Cluster Job
=============================
To resume a cluster job that was paused, make a POST HTTP request to URI:
::

 /clusters/{cluster-id}/resume

Admins and cluster owners are allowed to resume a paused cluster job. If there is paused job currently in progress,
the request succeeds and returns a 200.

HTTP Responses
^^^^^^^^^^^^^^
.. list-table::
   :widths: 15 10
   :header-rows: 1

   * - Status Code
     - Description
   * - 200 (OK)
     - Successful
   * - 404 (NOT FOUND)
     - If the cluster is in the process of performing some other action.
   * - 500 (INTERNAL_SERVER_ERROR)
     - Internal server error

Example
^^^^^^^
.. code-block:: bash

 $ curl -H 'Coopr-UserID:<user-id>'
        -H 'Coopr-TenantID:<tenantid>'
        -H 'Coopr-ApiKey:<apikey>'
        -X POST

        http://<server>:<port>/<version>/clusters/<cluster-id>/resume


