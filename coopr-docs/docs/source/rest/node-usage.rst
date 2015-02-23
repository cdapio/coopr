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

=================
# Metrics
=========

.. include:: /rest/rest-links.rst

Admins are able to get metrics, such as the number of tasks that are in progress and queued as well as node hour usage.

.. _metrics-node-usage:


## Get Node Usage Metrics

To get node usage metrics, HTTP GET request to URI:
::

/metrics/nodes/usage

######*VERIFY THIS*:
Only admins are allowed to get node usage metrics. Tenant admins will see metrics for their own nodes,
whereas the superadmin will get metrics for all tenant nodes.


## HTTP Responses


The response will be a JSON Object with start and end time as keys, and the node usage metrics as the values.
node usage metrics include the key value pairs entered as parameters, followed by the resulting node hour usage
based on the selected criteria.

.. list-table::
   :widths: 15 10
   :header-rows: 1

   * - Status Code
     - Description
   * - 200 (OK)
     - If update was successful
   * - 403 (FORBIDDEN)
     - If the user is forbidden from getting node usage metrics.



=======

# API Endpoints and Examples


## General usage

.. code-block:: bash

 $ curl -H 'Coopr-UserID:admin' 
        -H 'Coopr-ApiKey:<apikey>'
        -H 'Coopr-TenantID:superadmin'
        http://<server>:<port>/<version>/metrics/nodes/usage
 
 $ {"start":1424475097,"end":1424485733,"data":[{"time":1424475097,"value":29035}]}

The value at the end of the resulting json here, is the number of node seconds for all nodes combined.
##### Add an explanation of what start and end time are in the result json

As an example, if you have a 3 node cluster and a 1 node cluster (total 4 nodes), every 60 seconds, 
you will have 240 (4 nodes x 60 seconds) additional node seconds.  The following two lines are examples
of the json results returned when running a general api node usage call 60 seconds apart:
{"start":1424475097,"end":1424486443,"data":[{"time":1424475097,"value":31872}]}
{"start":1424475097,"end":1424486503,"data":[{"time":1424475097,"value":32112}]}


## Time Ranges


Start and end values are expressed in Unix time (a.k.a. seconds since epoch, or the beginning of Unix time: 
00:00:00 Coordinated Universal Time (UTC), Thursday, 1 January 1970).  
Unix time may be checked on most Unix systems by typing date +%s on the command line.

In the general usage example, in the json response to general node hour usage, we see the start and end values
returned.  If start and end are not specified, 


### start time only

To obtain node usage hours since a specific date and time, we would add 'start=x' where x is Unix time.

.. code-block:: bash

$ curl -H 'Coopr-UserID:admin' 
       -H 'Coopr-ApiKey:<apikey>'
       -H 'Coopr-TenantID:superadmin'
       http://<server>:<port>/<version>/metrics/nodes/usage?start=1424393580
       
$ {"start":1424393580,"end":1424485774,"data":[{"time":1424393580,"value":29199}]}
       
 
### end time only

.. code-block:: bash

$ curl -H 'Coopr-UserID:admin' 
       -H 'Coopr-ApiKey:<apikey>'
       -H 'Coopr-TenantID:superadmin'
       http://<server>:<port>/<version>/metrics/nodes/usage?end=1424482780
 
$ {"start":1424475097,"end":1424482780,"data":[{"time":1424475097,"value":17220}]}
        

### time range (start and end time)
       
.. code-block:: bash
 
$ curl -H 'Coopr-UserID:admin' 
       -H 'Coopr-ApiKey:<apikey>'
       -H 'Coopr-TenantID:superadmin'
       http://<server>:<port>/<version>/metrics/nodes/usage?start=1424481097&end=1424482780
        
$ {"start":1424481097,"end":1424486196,"data":[{"time":1424481097,"value":20396}]}



## Specify a user

.. code-block:: bash

$ curl -H 'Coopr-UserID:admin' 
       -H 'Coopr-ApiKey:<apikey>'
       -H 'Coopr-TenantID:superadmin'
       http://<server>:<port>/<version>/metrics/nodes/usage?user=admin

$ {"start":1424475097,"end":1424486266,"data":[{"time":1424475097,"value":31164}]}


## Specify a tenant

.. code-block:: bash

$ curl -H 'Coopr-UserID:admin' 
       -H 'Coopr-ApiKey:<apikey>'
       -H 'Coopr-TenantID:superadmin'
       http://<server>:<port>/<version>/metrics/nodes/usage?tenant=superadmin
       
$ {"start":1424475097,"end":1424486290,"data":[{"time":1424475097,"value":31260}]} 




