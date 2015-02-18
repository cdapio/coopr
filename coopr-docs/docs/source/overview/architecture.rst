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

.. _overview_architecture:
.. index::
   single: Architecture

============
Architecture
============

.. _architecture:
.. figure:: /_images/Coopr-Architecture.png
    :align: center
    :alt: Coopr Architecture
    :figclass: align-center

Server
===========
The Server is responsible for managing tenants, clusters, and provisioner resources. It exposes web services for adding and
managing providers, services, and cluster templates for use by users. With these services, users can create and manage 
clusters simply by specifying a template and cluster size. The service solves for a valid cluster layout, then creates and
coordinates an execution plan to carry out cluster operations. Additionally, provisioner resources can be managed through
the server, giving provisioners access to new resources as your services evolve. 

Solver
------
The solver is responsible for taking an existing cluster layout, the template associated with the cluster,
user specified properties, and finding a valid cluster layout that satisfies all input. For example, when a user
makes a request to create a cluster, the solver will take the template specified in the request plus any additional
arguments such as the size of the cluster, and solve for a cluster layout that satisfies all the constraints included
in the template and the arguments included in the cluster create request. The output of the solver is either an error
indicating no valid layout could be found, or a new cluster layout.

Planner
-------
The planner is responsible for taking an old cluster layout, a new cluster layout, and creating a plan that will take
the cluster from the old layout to the new layout. For example, during a cluster create operation, the planner takes
the nonexistant old layout, the new layout, and creates a plan to create the cluster that involves creating the nodes,
installing, configuring, initializing, and starting cluster services. The planner will take into account service
dependencies when creating the plan. It will also place tasks onto a queue for provisioner workers to take and execute,
as well as manage progression, retries, and rollbacks of planned tasks as they are successfully or unsuccessfully executed.

Persistent Stores
-----------------
The server stores data for all tenants in a persistent store. Most data, such as providers, hardware types,
image types, services, cluster templates, and cluster information, are stored in a database. However, plugin resources
are stored in a special provisioner data store because of their potentially unbounded size. The default implementation of this
store writes to the local filesystem the server is running on. There is an interface that you can implement if you want to
use another persistent storage backend. You may want to do this if you are running multiple servers and do not want to use
some network file system such as NFS.

Provisioner
================
Provisioners are responsible for executing tasks planned by the Server, such as the creation of nodes, installation of services,
and configuration of services. Each provisioner manages workers that take tasks from the Server, reporting back the tasks' status 
after execution completes. Additionally, the provisioner syncs resources on request from the Server, allowing administrators to
manage what configuration and code should be used to perform various node and service actions. 

Provisioners support a pluggable architecture for integrating different infrastructure providers (e.g. Amazon Web Services, DigitalOcean, Google Compute Engine,
Joyent, OpenStack and Rackspace) and automators (e.g. Chef, Puppet, Shell scripts). Provisioners are not directly installed on the target host, but rather use SSH to interact with the remote host, making Coopr's architecture simple and secure. Since multiple provisioners can work concurrently, this layer of provisioners support execution of thousands of concurrent tasks to render scalability.

Provisioners spin up worker processes to execute the actual tasks that must be performed. Each worker will only execute tasks for
a single tenant and will not execute tasks for other tenants even if it is idle. The server is responsible for telling provisioners
how many workers it should run as well as the tenant those workers are working for. 

Web UI
===========
Coopr Web UI exposes two major functionalities: an Admin view and a User view. The Admin view allows system administrators or server administrators to configure
providers, disk images, machine hardware types, and software services. The UI also supports the construction of cluster templates that
can be accessed and executed by users. Cluster templates are blueprints that administrators expose
to their users that enable their users to instantiate clusters.

Using the user UI, end users are able to see all the cluster templates available to them and use them to create
instances of clusters. The end user can retrieve details of their own clusters, (including the cluster's metadata)
and execute the following operations to their clusters: create, delete, amend, update, and monitor.
