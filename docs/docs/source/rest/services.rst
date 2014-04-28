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
   single: REST API: Services

==================
REST API: Services
==================

.. include:: /rest/rest-links.rst

Loom REST APIs allow the administrator to add services. A service is some piece of software
that can be placed on a cluster. Examples include a mysql server, a Hadoop namenode, a 
Lucene indexer, and much more. The administrator defines the service entirely, so any software
supported by the underlying provisioners can be added to Continuuity Loom. By writing a provisioner
automator plugin, or by using the included chef and shell plugins, an administrator can manage
any service they want.

A service is uniquely identified by its name. It also contains a short description and a section for
provisioner information and a section for its dependencies. The provisioner and dependencies sections
are described in more detail below.  

Provisioner
^^^^^^^^^^^

The provisioner section contains any information the provisioner may need to 
carry out actions related to the service. Service actions include INSTALL,
CONFIGURE, INITIALIZE, START, STOP, and REMOVE. The administrator needs to 
add any relevant actions for a service. For example, it is possible to have a
service that only installs and configures, but does none of the other actions.
For that service, the administrator would need to add 2 actions for the service,
and choose which automator type will be used to carry out that action. Depending
on the automator type, different fields may need to be provided. Required fields
are defined by the automator type, which is given by the plugin itself.

The provisioner section itself is a JSON object with ``actions`` as a key, and a
JSON object as its value.  The value contains a mapping of service action 
(one of install, configure, initialize, start, stop, and remove) to 
automator details. The automator details is a JSON object containing a ``type``,
which defines which automator type to use for that action, and ``fields``. Fields
contains another JSON object which is a set of key-value pairs required by the 
automator plugin.

This example is taken from an example Hadoop namenode service. It uses chef as the 
automator type, which requires a run_list field and also allows an optional json_attributes field.

.. code-block:: bash

  "provisioner": {
    "actions": {
        "configure": {
            "type": "chef",
            "fields": {
                "run_list": "recipe[hadoop_wrapper::default],recipe[hadoop::default]"
            }
        },
        "initialize": {
            "type": "chef",
            "fields": {
                "run_list": "recipe[hadoop_wrapper::hadoop_hdfs_namenode_init]"
            }
        },
        "install": {
            "type": "chef",
            "fields": {
                "run_list": "recipe[hadoop::hadoop_hdfs_namenode]"
            }
        },
        "start": {
            "type": "chef",
            "fields": {
                "json_attributes": "{\"loom\": { \"node\": { \"services\": { \"hadoop-hdfs-namenode\": \"start\" } } } }",
                "run_list": "recipe[hadoop_wrapper::default],recipe[hadoop::hadoop_hdfs_namenode],recipe[loom_service_runner::default]"
            }
        },
        "stop": {
            "type": "chef",
            "fields": {
                "json_attributes": "{\"loom\": { \"node\": { \"services\": { \"hadoop-hdfs-namenode\": \"stop\" } } } }",
                "run_list": "recipe[hadoop_wrapper::default],recipe[hadoop::hadoop_hdfs_namenode],recipe[loom_service_runner::default]"
            }
        }
    }

Dependencies
^^^^^^^^^^^^

Dependencies serve two general purposes. The first is to enforce that a service
cannot be placed onto a cluster without also placing the services it requires. The second is to enforce a safe
ordering of service actions while performing cluster operations. It is easiest to understand the different
types of dependencies by going through example. In this example we have a service called "myapp-2.0". 

.. code-block:: bash

    "dependencies": {
        "provides": [ "myapp" ],
        "conflicts": [ "myapp-1.0", "myapp-1.5" ],
        "install": {
            "requires": [ "base" ],
            "uses": [ "ntp" ]
        },
        "runtime": {
            "requires": [ "sql-db" ],
            "uses": [ "load-balancer" ]
        }
    }

Conflicts
^^^^^^^^^
The ``conflicts`` key specifies an array of services that cannot be placed on a cluster with the given service.
In this example, that means that "myapp-2.0" cannot be placed on a cluster with "myapp-1.0" or "myapp-1.5".

Install
^^^^^^^
Install defines install time dependencies. Install time dependencies take effect for the INSTALL and REMOVE
service actions. It contains a ``requires`` key which specifies an array of services that the given services
requires for its installation. In this example, "myapp-2.0" *requires* the "base" service at install time. This
means that the installation of the "base" service will occur before the install of the "myapp-2.0" service. Similarly,
the removal of the "myapp-2.0" service will occur before the removal of the "base" service. This also means that
the "myapp-2.0" service cannot be placed on a cluster without the "base" service also being placed on the cluster.
The ``uses`` key is like the ``requires`` key in that it enforces the same ordering of service actions. However,
``uses`` will not enforce the presence of the dependent service. In this example, "myapp-2.0" *uses* the "ntp" service
at install time. This means that if the "ntp" service is also on the cluster, the installation of "ntp" will occur
before the installation of "myapp-2.0". However, "ntp" does not have to be placed on the cluster in order for "myapp-2.0"
to be placed on the cluster.

Runtime
^^^^^^^
Runtime defines run time dependencies. It also contains ``requires`` and ``uses`` keys that are analagous to those
in the install section. The only difference is the service actions that they apply to. Install dependencies affect
the INSTALL and REMOVE service actions, whereas runtime dependencies affect the INITIALIZE, START, and STOP dependencies.
In this example, "myapp-2.0" *requires* the "sql-db" service. This means that "myapp-2.0" cannot be placed on a cluster
without a "sql-db" service. It also means that the initialization of "myapp-2.0" will occur after the start of "sql-db".
It also means the start of "myapp-2.0" will occur after the start of "sql-db" and that the stop of "myapp-2.0" 
will occur before the stop of "sql-db". Similarly, because "myapp-2.0" *uses* "load-balancer", initialization and start of
"myapp-2.0" will occur after the start of "load-balancer". Similarly, the stop of "myapp-2.0" will occur before the stop
of "load-balancer". Since it is in ``uses``, enforcement of this ordering only applies if "load-balancer" is present on the
same cluster as "myapp-2.0". The "myapp-2.0" service can be placed on a cluster without the "load-balancer" service.

Provides
^^^^^^^^
The provides section provides an extra level of indirection when specifying dependencies. In this example, the "myapp-2.0" 
service provides the "myapp" service. This means that if other services can put "myapp" in their runtime or install dependencies,
"myapp-2.0" can satisfy that dependency. As another example, "myapp-2.0" *requires* the "sql-db" service. If there was a
service called "mysql-db" that provides "sql-db", then it would be fine for "mysql-db" and "myapp-2.0" to be on the same 
cluster. All the ordering enforced by that runtime *requires* dependency would be enforced between the "myapp-2.0" and "mysql-db"
services.
 
.. _service-create:

Add a Service
==================

To create a new services, make a HTTP POST request to URI:
::

 /services

The POST body must contain a unique ``name``, and a ``provisioner`` section
describing how provisioner automators should carry out service actions. 
Optionally, a ``description`` can be given describing the service, and a 
``dependencies`` section can be given, describing other services that the 
service depends on. 

POST Parameters
^^^^^^^^^^^^^^^^

Parameters

.. list-table::
   :widths: 15 10
   :header-rows: 1

   * - Parameter
     - Description
   * - name 
     - Specifies the name for the services. The assigned name must have only
       alphanumeric, dash(-), dot(.), and underscore(_) characters.
   * - description
     - Provides a description for the service. 
   * - dependencies
     - Dependencies of the service. Includes information about what the service
       requires and uses at install time and at runtime, as well as what services
       it conflicts with.
   * - provisioner
     - Provisioner related information, including how provisioner automators should
       perform service actions like install, configure, initialize, start, stop, 
       and remove. 

HTTP Responses
^^^^^^^^^^^^^^

.. list-table:: 
   :widths: 15 10 
   :header-rows: 1

   * - Status Code
     - Description
   * - 200 (OK)
     - Successfully created
   * - 400 (BAD_REQUEST)
     - Bad request, server is unable to process the request, or a services' name already exists 
       in the system.

Example
^^^^^^^^
.. code-block:: bash

 $ curl -X POST 
        -H 'X-Loom-UserID:admin' 
        -H 'X-Loom-ApiKey:<apikey>'
        -d '{
                "name": "hadoop-hdfs-datanode",
                "description": "Hadoop HDFS DataNode",
                "dependencies": {
                    "conflicts": [],
                    "provides": [],
                    "install": {
                        "requires": [],
                        "uses": []
                    },
                    "runtime": {
                        "requires": [ "hadoop-hdfs-namenode" ],
                        "uses": []
                    }
                },
                "provisioner": {
                    "actions": {
                        "configure": {
                            "type": "chef",
                            "fields": {
                                "run_list": "recipe[hadoop_wrapper::default],recipe[hadoop::default]"
                            }
                        },
                        "initialize": {
                            "type": "chef",
                            "fields": {
                                "run_list": "recipe[hadoop_wrapper::hadoop_hdfs_namenode_init]"
                            },
                        },
                        ...
                    }
                }
            }'
        http://<loom-server>:<loom-port>/<version>/loom/services

.. _service-retrieve:

Retrieve a Service
===================

To retrieve details about a services, make a GET HTTP request to URI:
::

 /services/{name}

This resource request represents an individual services for viewing.

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
     - If the resource requested is not configured and available in system.

Example
^^^^^^^^
.. code-block:: bash

 $ curl -H 'X-Loom-UserID:admin' 
        -H 'X-Loom-ApiKey:<apikey>'
        http://<loom-server>:<loom-port>/<version>/loom/services/small.example
 $ {
       "name": "hadoop-hdfs-datanode",
       "description": "Hadoop HDFS DataNode",
       "dependencies": {
           "conflicts": [],
           "provides": [],
           "install": {
               "requires": [],
               "uses": []
           },
           "runtime": {
               "requires": [ "hadoop-hdfs-namenode" ],
               "uses": []
           }
       },
       "provisioner": {
           "actions": {
               "configure": {
                   "type": "chef",
                   "fields": {
                       "run_list": "recipe[hadoop_wrapper::default],recipe[hadoop::default]"
                   }
               },
               "initialize": {
                   "type": "chef",
                   "fields": {
                       "run_list": "recipe[hadoop_wrapper::hadoop_hdfs_namenode_init]"
                   },
               },
               ...
           }
       }
   }

.. _service-delete:

Delete a Service
=================

To delete services, make a DELETE HTTP request to URI:
::

 /services/{name}

This resource request represents an individual services for deletion.

HTTP Responses
^^^^^^^^^^^^^^

.. list-table::
   :widths: 15 10
   :header-rows: 1

   * - Status Code
     - Description
   * - 200 (OK)
     - If delete was successful
   * - 404 (NOT FOUND)
     - If the resource requested is not found.

Example
^^^^^^^^
.. code-block:: bash

 $ curl -X DELETE
        -H 'X-Loom-UserID:admin' 
        -H 'X-Loom-ApiKey:<apikey>'
        http://<loom-server>:<loom-port>/<version>/loom/services/example

.. _service-modify:

Update a Service
==================

To update a service, make a PUT HTTP request to URI:
::

 /services/{name}

Resource specified above respresents an individual services request for an update operation.
Currently, the update of services resource requires complete services object to be
returned back rather than individual fields.

PUT Parameters
^^^^^^^^^^^^^^^^

Required Parameters

.. list-table::
   :widths: 15 10
   :header-rows: 1

   * - Parameter
     - Description
   * - name
     - Specifies the name of the service to be updated.
   * - description
     - New description or old one for the service.
   * - providermap
     - Provider map is map of providers and equivalent flavor type for current services being configured.
       It's currently a map of map.

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
     - If the resource requested is not found or the fields of the PUT body do not specify all the required fields.

Example
^^^^^^^^
.. code-block:: bash

 $ curl -X PUT 
        -H 'X-Loom-UserID:admin' 
        -H 'X-Loom-ApiKey:<apikey>'
        -d '{
                 "name": "myapp",
                 "description": "my application",
                 "dependson": [ "base" ],
                 "provisioner": {
                     "actions": {
                         "configure": {
                             "type": "chef",
                             "fields": {
                                 "run_list": "recipe[apt::default]"
                             }
                         },
                         "install": {
                             "type": "chef",
                             "fields": {
                                 "run_list": "recipe[apt::default]"
                             }
                         }
                     }
                 }
           }'
        http://<loom-server>:<loom-port>/<version>/loom/services/myapp

.. _service-all-list:

List all Services
=============================

To list all the services configured within Continuuity Loom, make a GET HTTP request to URI:
::

 /services

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

 $ curl -H 'X-Loom-UserID:admin' 
        -H 'X-Loom-ApiKey:<apikey>'
        http://<loom-server>:<loom-port>/<version>/loom/services

