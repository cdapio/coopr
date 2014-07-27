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
   single: Metrics 

=======
Metrics
=======

.. include:: /rest/rest-links.rst

Admins are able to get metrics, such as the number of tasks that are in progress and queued.

.. _metrics-queues:

Get Task Queue Metrics
==========================
To get task queue metrics, HTTP GET request to URI:
::

 /metrics/queues

Only admins are allowed to get queue metrics. Tenant admins will see metrics for their own queue,
whereas the superadmin will get metrics for all tenant queues.

HTTP Responses
^^^^^^^^^^^^^^

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
^^^^^^^^
.. code-block:: bash

 $ curl -H 'X-Loom-UserID:admin' 
        -H 'X-Loom-ApiKey:<apikey>'
        -H 'X-Loom-TenantID:loom'
        http://<loom-server>:<loom-port>/<version>/loom/metrics/queues
 $ {
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
