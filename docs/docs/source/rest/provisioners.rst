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
   single: REST API: Provisioners 

======================
REST API: Provisioners
======================

.. include:: /rest/rest-links.rst

Using the Superadmin REST API, you can get information about provisioners in the system.

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
        -H 'Coopr-UserID:admin' 
        -H 'Coopr-TenantID:superadmin'
        -H 'Coopr-ApiKey:<apikey>'
        http://<server>:<port>/<version>/provisioners/p1
 $ {
       "id": "p1",
       "host": "host1",
       "port": 58025,
       "capacityTotal": 10,
       "capacityFree": 2,
       "assignments": {
           "tenantX": 5,
           "tenantY": 2,
           "tenantZ": 1
       },
       "usage": {
           "tenantY": 2,
           "tenantZ": 1
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
        -H 'Coopr-UserID:admin' 
        -H 'Coopr-TenantID:superadmin'
        -H 'Coopr-ApiKey:<apikey>'
        http://<server>:<port>/<version>/provisioners
 $ [
       {
           "id": "p1",
           "host": "host1",
           "port": 58025,
           "capacityTotal": 10,
           "capacityFree": 2,
           "assignments": {
               "tenantX": 5,
               "tenantY": 2,
               "tenantZ": 1
           },
           "usage": {
               "tenantY": 2,
               "tenantZ": 1
           }
       },
       {
           "id": "p2",
           "host": "host2",
           "port": 58025,
           "capacityTotal": 10,
           "capacityFree": 2,
           "assignments": {
               "tenantX": 5,
               "tenantY": 3
           },
           "usage": {
               "tenantX": 5,
               "tenantY": 3,
           }
       }
    ]
