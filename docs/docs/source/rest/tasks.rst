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
   single: REST API: Tasks

==================
REST API: Tasks 
==================

.. include:: /rest/rest-links.rst

The Loom task APIs for taking and finishing tasks are used by the Provisioners to execute node level tasks. Provisioners poll
a REST API, looking for tasks to take and execute. Upon completion of the task, Provisioners call another REST API to tell the server 
whether or not execution of the task was successful. These APIs are only of interest to those who want to add or extend Provisioner 
plugins.

.. _tasks-take:

Taking a Task
=============

To take a task from the Server, make a HTTP POST request to URI:
::

 /tasks/take

The response will be a JSON object describing the task to perform. Each task will contain ``taskId``, ``jobId``, ``clusterId``, ``taskName``, 
``nodeId``, and ``config`` keys. The task name describes what type of task it is. It will be one of CREATE, CONFIRM, BOOTSTRAP, DELETE,
INSTALL, CONFIGURE, INITIALIZE, START, STOP, or REMOVE. The config section contains all the information a Provisioner may need to 
execute the task. Based on the task name and config section, the Provisioner will use the appropriate plugin to actually execute the task.
The config section is a JSON object. It has a "cluster" section containing the cluster config taken straight from the cluster template
, with macros expanded where applicable. If the task is a service task, the config section will also contain a "service" 
section containing the action to perform on the service. This comes straight from the service definition defined by the admin.
There is also a "provider" section that comes straight from the provider defined by the admin and in use for the cluster. Similarly,
there are "imagetype" and "hardwaretype" sections which come straight from the respective admin defined entities. In addition, an 
array of "automators" is given. This describes what automators will be used to perform tasks on the node and is useful for certain tasks,
such as BOOTSTRAP tasks, in order to figure out what automators need to be bootstrapped on the node. There is also a "nodes" section which
lists all other nodes in the cluster as well as some of their properties. In addition, the config section may contain other arbitrary 
key-value pairs that have been passed through by Provisioners earlier on. For example, upon finishing a CONFRIM task, Provisioners pass the 
IP address of the node back to the Server, which then attaches the IP address to the node and passes it on to any future task. 

POST Parameters
^^^^^^^^^^^^^^^^

Required Parameters

.. list-table::
   :widths: 15 10
   :header-rows: 1

   * - Parameter
     - Description
   * - workerId
     - Id of the worker. Must be unique across all workers. Only the worker that took the task will be
       able to finish the task. 

HTTP Responses
^^^^^^^^^^^^^^

.. list-table:: 
   :widths: 15 10 
   :header-rows: 1

   * - Status Code
     - Description
   * - 200 (OK)
     - Task successfully given to worker. 
   * - 204 (NO CONTENT)
     - No tasks to hand out. 
   * - 400 (BAD REQUEST)
     - Bad request, no workerId specified.

Example
^^^^^^^^
.. code-block:: bash

 $ curl -X POST 
        -d '{ "workerId": "worker1" }'
        http://<loom-server>:<loom-port>/<version>/loom/tasks/take
 $ 
    {
        "taskId": "00000001-002-071",
        "jobId": "00000001-002",
        "clusterId": "00000001",
        "taskName": "START",
        "nodeId": "edbafd4c-191a-42e7-9f8b-6625a74fa04e",
        "config": {
            "cluster": {
                "hadoop": {
                    "core_site": {
                        "fs.defaultFS": "hdfs://test1-1000.local"
                    },
                    "hdfs_site": {
                        "dfs.datanode.max.xcievers": "4096"
                    },
                    "mapred_site": {
                        "mapreduce.framework.name": "yarn"
                    },
                    "yarn_site": {
                        "yarn.resourcemanager.hostname": "test1-1000.local"
                    }
                },
                "hbase": {
                    "hbase_site": {
                        "hbase.rootdir": "hdfs://test1-1000.local/hbase",
                        "hbase.cluster.distributed": "true",
                        "hbase.zookeeper.quorum": "test1-1000.local:2181"
                    }
                }
            },
            "service": {
                "name": "hbase-regionserver",
                "action": {
                    "type": "chef",
                    "script": "recipe[hadoop_wrapper::default],recipe[hadoop::hbase_regionserver],recipe[loom_service_runner::default]",
                    "data": "{\"loom\": { \"node\": { \"services\": { \"hbase-regionserver\": \"start\" } } } }"
                }
            },
            "nodenum": "1001",
            "hostname": "test1-1001.local",
            "ipaddress": "123.456.0.1",
            "hardwaretype": {
                "name": "medium",
                "flavor": "5"
            },
            "imagetype": {
                "name": "ubuntu12",
                "image": "80fbcb55-b206-41f9-9bc2-2dd7aac6c061"
            },
            "automators": [ "chef" ],
            "provider": {
                "name": "rackspace",
                "description": "Rackspace Public Cloud",
                "providertype": "rackspace",
                "provisioner": {
                    "auth": {
                        "rackspace_username": "USERNAME",
                        "rackspace_api_key": "API_KEY",
                        "rackspace_region": "dfw"
                    }
                }
            },
            "nodes": {
                "39aad219-3bc8-4be3-a594-e1eb19d1c6a6": {
                    "hostname": "test1-1000.local",
                    "nodenum": "1000",
                    "ipaddress": "123.456.0.100",
                    "hardwaretype": {
                        "name": "medium",
                        "flavor": "5"
                    },
                    "imagetype": {
                        "name": "ubuntu12",
                        "image": "80fbcb55-b206-41f9-9bc2-2dd7aac6c061"
                    },
                    "automators": [ "chef" ]
                },
                "9b167e5d-02a9-4a64-afba-cf4806ca0d71": {
                    "hostname": "test1-1001.local",
                    "nodenum": "1001",
                    "ipaddress": "123.456.0.101",
                    "hardwaretype": {
                        "name": "medium",
                        "flavor": "5"
                    },
                    "imagetype": {
                        "name": "ubuntu12",
                        "image": "80fbcb55-b206-41f9-9bc2-2dd7aac6c061"
                    },
                    "automators": [ "chef" ]
                }
            }
        }
    }


.. _tasks-finish:

Finish a Task
=============

After a provisioner finishes executing the task, it sends a POST request to URI:
::

 /tasks/finish

The POST body is a JSON object that must contain at least ``workerId``, ``taskId``, and ``status``. It can also optionally contain
``stdout`` and ``stderr`` for storing logs on task failures, and a ``result`` key with a JSON object as its value to pass on any 
information that may be needed to complete future tasks on the node. A non-zero status indicates failure of the task. stdout
and stderr values will be trimmed by the Server if they are too long. The log length is a configurable Server setting. The 
contents of the result block will be included for the node in all future requests. This is to enable provisioners to pass 
information to each other that cannot be provided by the Server. For example, most providers return a provider specific id
upon creation of a node. This id is needed for other tasks such as node deletion, so the Provisioner plugin that creates a
node will pass back the id in the result block. Another example is IP address. The IP address of a node is often required
during configuration of services and can be reference through macros. However, the IP address is not usually available until
after the CONFIRM task has run on a node. Therefore, the Provisioner plugin will return the IP address in its result block 
after successfully confirming a node so that it can be used down the line. 

HTTP Responses
^^^^^^^^^^^^^^

.. list-table::
   :widths: 15 10
   :header-rows: 1

   * - Status Code
     - Description
   * - 200 (OK)
     - Task successfully finished.
   * - 400 (BAD REQUEST)
     - Bad request, workerId, taskId, or status was missing in POST body.
   * - 417 (EXPECTATION FAILED)
     - Specified worker did not own the task it was trying to finish. Can happen if 
       the Server timed out the task before the worker sent the finish request.

Example
^^^^^^^^
.. code-block:: bash

 $ curl -d '{
            "workerId": "worker1",
            "taskId": "",
            "status": 1,
            "stdout": "",
            "stderr": "some error logs",
        }'
        http://<loom-server>:<loom-port>/<version>/loom/tasks/finish

 $ curl -d '{
            "workerId": "worker2",
            "taskId": "",
            "status": 0,
            "result": {
                "ipaddress":"123.456.0.1"
            }
        }'
        http://<loom-server>:<loom-port>/<version>/loom/tasks/finish
