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
    :width: 800px
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

Provisioner
================
Provisioners are responsible for executing tasks planned by the Server, such as the creation of nodes, installation of services,
and configuration of services. Each provisioner manages workers that take tasks from the Server, reporting back the tasks' status 
after execution completes. Additionally, the provisioner syncs resources on request from the Server, allowing administrators to
manage what configuration and code should be used to perform various node and service actions. 

Provisioners support a pluggable architecture for integrating different infrastructure providers (e.g. OpenStack, Rackspace, Amazon Web Services, Google App Engine, and Joyent) 
and automators (e.g. Chef, Puppet, Shell scripts). Provisioners are not directly installed on the target host, but rather use SSHD to interact with the remote host, making Coopr's architecture simple and secure. Since multiple provisioners can work concurrently, this layer of provisioners support execution of thousands of concurrent tasks to render scalability.

Web UI
===========
Coopr Web UI exposes two major functionalities: an Admin view and a User view. The Admin view allows system administrators or server administrators to configure
providers, disk images, machine hardware types, and software services. The UI also supports the construction of cluster templates that
can be accessed and executed by users. Cluster templates are blueprints that administrators expose
to their users that enable their users to instantiate clusters.

Using the user UI, end users are able to see all the cluster templates available to them and use them to create
instances of clusters. The end user can retrieve details of their own clusters, (including the cluster's metadata)
and execute the following operations to their clusters: create, delete, amend, update, and monitor.
