..
   Copyright © 2012-2015 Cask Data, Inc.

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
   single: Metrics 

=======
Metrics
=======

.. include:: /rest/rest-links.rst

Admins are able to get metrics, such as node usage and the number of tasks that are in progress and queued.

.. _metrics-queues:

Get Task Queue Metrics
==========================

To retrieve task queue metrics, issue an HTTP GET request to the URI:
::

 /metrics/queues

Only admins are allowed to retrieve node usage metrics. Tenant admins will see metrics for their own nodes,
while the superadmin tenant will see metrics for all tenant nodes.


HTTP Responses
--------------

The response will be a JSON Object with tenants as the keys, and the queue metrics as the values.
Queue metrics include the number of tasks in progress, the number queued but not in progress, and
the total queued and in progress. 

.. list-table::
   :widths: 15 10
   :header-rows: 1

   * - Status Code
     - Description
   * - 200 (OK)
     - If update was successful
   * - 403 (FORBIDDEN)
     - If the user is forbidden from getting queue metrics.

Example
-------
.. code-block:: bash

 $ curl -H 'Coopr-UserID:admin' 
        -H 'Coopr-ApiKey:<apikey>'
        -H 'Coopr-TenantID:superadmin'
        http://<server>:<port>/<version>/metrics/queues
 $ {
       "superadmin": {
           "inProgress": 0,
           "queued": 0,
           "total": 0
       },
       "tenant1": {
           "inProgress": 8,
           "queued": 34,
           "total": 42
       },
       "tenant2": {
           "inProgress": 1,
           "queued": 0,
           "total": 1
       }
   }



Get Node Usage Metrics
======================

To retrieve node usage metrics, issue an HTTP GET request to the URI:
::

 /metrics/nodes/usage

Only admins are allowed to get node usage metrics. Tenant admins will see metrics for their own nodes,
whereas the superadmin will get metrics for all tenant nodes.


HTTP Responses
==============

The response will be a JSON Object with start and end time as keys, and the node usage metrics as the values.
node usage metrics include the key-value pairs entered as parameters, followed by the resulting node hour usage
based on the selected criteria.

.. list-table::
   :widths: 15 10
   :header-rows: 1

   * - Status Code
     - Description
   * - 200 (OK)
     - If update was successful
   * - 401 (UNAUTHORIZED)
     - If the api call is missing required headers for a metric (status is one of the few that does not require user/tenant headers)
   * - 403 (FORBIDDEN)
     - If the user is forbidden from getting node usage metrics.
   * - 405 (METHOD NOT ALLOWED)
     - If for example, a non-superadmin tenant user is requesting any tenant metrics, including their own (this metric is only made available for the superadmin tenant)



API Endpoints and Examples
==========================


General Usage
-------------

.. code-block:: bash

 $ curl -H 'Coopr-UserID:admin'
        -H 'Coopr-ApiKey:<apikey>'
        -H 'Coopr-TenantID:superadmin'
        http://<server>:<port>/<version>/metrics/nodes/usage

 $ {"start":1424475097,"end":1424485733,"data":[{"time":1424475097,"value":29035}]}


The value at the end of the resulting JSON is the number of node seconds for all nodes combined.

As an example, if you have a 3 node cluster and a 1 node cluster (total 4 nodes), every 60 seconds,
you will have 240 (4 nodes x 60 seconds) additional node seconds.  The following two lines are examples
of the JSON results returned when running a general API node usage call every 60 seconds:

.. code-block:: bash

 {"start":1424475097,"end":1424486443,"data":[{"time":1424475097,"value":31872}]}
 {"start":1424475097,"end":1424486503,"data":[{"time":1424475097,"value":32112}]}



Time Ranges
-----------

Start and end values are expressed in Unix time (a.k.a. seconds since epoch, or the beginning of Unix time:
00:00:00 Coordinated Universal Time (UTC), Thursday, 1 January 1970).
Unix time may be checked on most Unix systems by typing ``date +%s`` on the command line.

With the general usage example, the JSON response to general node hour usage shows the start and end values returned. 
If the start is not specified, the start time is the exact date and time when the very first 
task (ever) completes. The end date, if not specified, is the current date and time.


Start Time Only
^^^^^^^^^^^^^^^

To obtain node usage hours since a specific date and time, we would add ``start=x`` where x is a Unix time.

.. code-block:: bash

 $ curl -H 'Coopr-UserID:admin'
        -H 'Coopr-ApiKey:<apikey>'
        -H 'Coopr-TenantID:superadmin'
        http://<server>:<port>/<version>/metrics/nodes/usage?start=1424393580
 
 $ {"start":1424393580,"end":1424485774,"data":[{"time":1424393580,"value":29199}]}


End Time Only
^^^^^^^^^^^^^

.. code-block:: bash

 $ curl -H 'Coopr-UserID:admin'
        -H 'Coopr-ApiKey:<apikey>'
        -H 'Coopr-TenantID:superadmin'
        http://<server>:<port>/<version>/metrics/nodes/usage?end=1424482780
 
 $ {"start":1424475097,"end":1424482780,"data":[{"time":1424475097,"value":17220}]}


Time Range (Start and End Time)
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

.. code-block:: bash

 $ curl -H 'Coopr-UserID:admin'
        -H 'Coopr-ApiKey:<apikey>'
        -H 'Coopr-TenantID:superadmin'
        http://<server>:<port>/<version>/metrics/nodes/usage?start=1424481097&end=1424482780
 
 $ {"start":1424481097,"end":1424486196,"data":[{"time":1424481097,"value":20396}]}



Specifying a User
-----------------

Non-superadmin tenants can retrieve node usage metrics for their users only. The superadmin tenant retrieves
node usage metrics for all users. As an example, if there are users named ``george`` under both the superadmin 
and a non-superadmin tenant named ``mytenant``, the ``mytenant`` admin user will only see node usage for their 
own ``george`` user, while the superadmin will retrieve combined total usage for all users named ``george`` 
(under all tenants, including superadmin). 

The two scenarios in this example are illustrated here. Node usage metrics for:

1. A user named ``george`` while we authenticate as ``user=admin``, and the ``mytenant`` tenant.

   .. code-block:: bash
   
      $ curl -H 'Coopr-UserID:admin'
             -H 'Coopr-ApiKey:<apikey>'
             -H 'Coopr-TenantID:mytenant'
             http://<server>:<port>/<version>/metrics/nodes/usage?user=george
      
      $ {"start":1424475097,"end":1424486266,"data":[{"time":1424475097,"value":8547}]}


2. All users named ``george`` while we authenticate as ``user=admin`` and ``superadmin`` tenant.

   .. code-block:: bash
   
      $ curl -H 'Coopr-UserID:admin'
             -H 'Coopr-ApiKey:<apikey>'
             -H 'Coopr-TenantID:superadmin'
             http://<server>:<port>/<version>/metrics/nodes/usage?user=george
      
      $ {"start":1424475097,"end":1424486266,"data":[{"time":1424475097,"value":14164}]}

The results in the first case are a subset of those in the second.


Specifying a Tenant
-------------------

Only superadmin tenant users are allowed to retrieve tenant-level node usage metrics. Any query by a user
in a regular tenant to retrieve that type of information will return a 405 error (method not allowed), 
since this metric is not available for non-superadmin tenant users.

.. code-block:: bash

 $ curl -H 'Coopr-UserID:admin'
        -H 'Coopr-ApiKey:<apikey>'
        -H 'Coopr-TenantID:superadmin'
        http://<server>:<port>/<version>/metrics/nodes/usage?tenant=superadmin
 
 $ {"start":1424475097,"end":1424486290,"data":[{"time":1424475097,"value":31260}]}


