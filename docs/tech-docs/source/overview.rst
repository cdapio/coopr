:orphan:
.. index::
   single: Overview
.. _index_toplevel:

============
Architecture
============

.. _architecture:

Overview
========
Before moving on, it is necessary to define some terminology that will be used throughout the remainder of the document. 

A node is a machine, real or virtual, that consists of a service or collection of services running on hardware, where
a service is some piece of software.  

A cluster is a collection of nodes.  Typically, nodes in a cluster can communicate
with one another to provide some functionality that each individual node is unable to fully provide.  An example of a cluster
is a collection of nodes that work together to provide a distributed file system like Hadoop HDFS.  

A cluster management operation is an action that is performed on a cluster, affecting some or all of the nodes in the cluster.
Some examples are creating, deleting, shrinking, expanding, upgrading, rollback, configuring, starting, and stopping a cluster. 
Cluster level operations typically involve many node level tasks. For example, configuring a cluster usually requires configuring 
services on each node in the cluster, and may also involve stop and starting those services in a particular order.

A task is an action that is performed on a node.  Some examples are creation and deletion of the node, and the installation,
initialization, configuration, start, stop, or removal of a service on the node.  

Coopr is a system that allows users to manage clusters as single entities.
It consists of two major pieces: the server and the provisioners.  The server is responsible for determining what needs to be 
done for different cluster management operations.  It breaks down cluster level operations into node level tasks, coordinating 
which tasks should be performed at what time.  It also stores the state of all provisioned clusters as well as a history of all
operations performed.  The server does not perform any of the tasks, but places tasks onto a queue for the provisioners to 
execute.  Provisioners are responsible for actually executing the tasks on the desired node, reporting back to the server 
success or failure of its given task.  An architectural overview is shown in the figure below. 

.. figure:: /_images/architecture.png
    :align: center
    :alt: Architecture
    :figclass: align-center

We now give an overview of each component before going into their details.

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

Janitor
-------
The janitor is responsible for timing out and retrying tasks if they have been in progress for too long. The janitor is also
responsible for auto-deleting clusters that have an expired lease.

Provisioner
================
Provisioners are responsible for executing tasks planned by the Server, such as the creation of nodes, installation of services,
and configuration of services. Each provisioner manages workers that take tasks from the Server, reporting back the tasks' status
after execution completes. Additionally, the provisioner syncs resources on request from the Server, allowing administrators to
manage what configuration and code should be used to perform various node and service actions.

Provisioners support a pluggable architecture for integrating different infrastructure providers (e.g. OpenStack, Rackspace, Amazon Web Services, Google App Engine, and Joyent)
and automators (e.g. Chef, Puppet, Shell scripts). Provisioners are not directly installed on the target host, but rather use SSHD to interact with the remote host, making Coopr's architecture simple and secure. Since multiple provisioners can work concurrently, this layer of provisioners support execution of thousands of concurrent tasks to render scalability.

Provisioners spin up worker processes to execute the actual tasks that must be performed. Each worker will only execute tasks for
a single tenant and will not execute tasks for other tenants even if it is idle. The server is responsible for telling provisioners
how many workers it should run as well as the tenant those workers are working for.
