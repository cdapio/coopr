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
   single: REST API: Provisioners 

======================
REST API: Provisioners
======================

.. include:: /rest/rest-links.rst

Using the Loom Superadmin REST API, you can get information about provisioners in the system.

.. _provisioners-retrieve:

Get a Provisioner
=================

The superadmin is able to get a specific provisioner by its id. The provisioner contains
the host and port it is running on, capacity information, and the number of workers assigned
and running for each tenant for the provisioner.

To get a provisioner, make a GET HTTP request to URI:
::

 /provisioners/{id}

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
     - Provisioner for the given id not found

Example
^^^^^^^^
.. code-block:: bash

 $ curl -X GET 
        -H 'X-Loom-UserID:superadmin' 
        -H 'X-Loom-Tenant:ID:loom'
        -H 'X-Loom-ApiKey:<apikey>'
        http://<loom-server>:<superadmin-port>/<version>/provisioners/p1
 $ {
       "id": "p1",
       "host": "localhost",
       "port": 58025,
       "capacityTotal": 10,
       "capacityFree": 2,
       "assignments": {
           "6397a0c0-c6a7-43de-8fe0-2091ac0e6017": 5,
           "99ac47e3-17b7-4ceb-bfc7-53498c57ca26": 2,
           "abead0c1-53bd-4482-b503-0a4de8dd18d0": 1
       },
       "usage": {
           "99ac47e3-17b7-4ceb-bfc7-53498c57ca26": 2,
           "abead0c1-53bd-4482-b503-0a4de8dd18d0": 1
       }
   }

.. _provisioners-all-list:

List All Provisioners
=====================

The superadmin is able to get the list of all provisioners registered with the server. 
Each provisioner contains the host and port they are running on, capacity information, 
and the number of workers assigned and running for each tenant for the provisioner.

To list all the provisioners, make a GET HTTP request to URI:
::

 /provisioners

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
        http://<loom-server>:<superadmin-port>/<version>/provisioners
 $ [
       {
           "id": "p1",
           "host": "localhost",
           "port": 58025,
           "capacityTotal": 10,
           "capacityFree": 2,
           "assignments": {
               "6397a0c0-c6a7-43de-8fe0-2091ac0e6017": 5,
               "99ac47e3-17b7-4ceb-bfc7-53498c57ca26": 2,
               "abead0c1-53bd-4482-b503-0a4de8dd18d0": 1
           },
           "usage": {
               "99ac47e3-17b7-4ceb-bfc7-53498c57ca26": 2,
               "abead0c1-53bd-4482-b503-0a4de8dd18d0": 1
           }
       },
       {
           "id": "p2",
           "host": "localhost",
           "port": 58039,
           "capacityTotal": 10,
           "capacityFree": 2,
           "assignments": {
               "2ce4f26d-3190-4698-96fa-db4c8ee84a4b": 5,
               "99ac47e3-17b7-4ceb-bfc7-53498c57ca26": 3
           },
           "usage": {
               "2ce4f26d-3190-4698-96fa-db4c8ee84a4b": 5,
               "99ac47e3-17b7-4ceb-bfc7-53498c57ca26": 3,
           }
       }
    ]
