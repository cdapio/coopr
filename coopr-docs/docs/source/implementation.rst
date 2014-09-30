==============
Under the Hood 
==============

This section gives a brief overview of how Coopr works. Developers who are interested in contributing
to Coopr are encouraged to read this before diving into code.

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
success or failure of its given task.

A description of the architecture can be seen :doc:`here </overview/architecture>`.

Solver
======
Users can make requests to perform different cluster management operations, such as creating, deleting, shrinking, expanding, configuring,
starting, stopping, and upgrading clusters.  Some of these operations change a cluster layout while others are performed on an existing
cluster without any layout change.  A cluster layout defines the exact set of nodes for a cluster, where each node contains which hardware
and image types to use, as well as the set of services that should be placed on the node.  Operations that can change a cluster layout are
first sent to the Solver, which will find a valid cluster layout and then send the layout and operation on to the Planner. Operations that
will not change a cluster layout are sent directly to the Planner.

Overview
--------
The solver is responsible for taking an existing cluster layout, the template associated with the cluster, user specified properties, and
finding a valid cluster layout that satisfies all input. There are 3 stages involved in solving a cluster layout. The first is finding
valid service sets. The second is finding valid node layouts. The third is finding a valid cluster layout. It should be noted that what
is described is just one way to find a cluster layout. There are many ways this constraint satisfaction problem could be solved.

Finding Service Sets
--------------------
A service set is a set of services that can be placed on a node. The set of valid service sets will depend on the services
that should be placed on the cluster, as well as the constraints defined in the template.
We define N as the number of services that must be placed on the cluster, and n as the number of services in a particular service set.
For each n from 1 to N, we go through every possible service combination and check if the service combination is valid, given the constraints
defined in the template. If the service set is valid, it is added to the list of valid service sets. An example with 3 services is shown
in the figure below.

.. figure:: /_images/implementation/service_sets.png
    :align: center
    :alt: Service Sets
    :figclass: align-center

We start with n=3, which has only one combination.  This service set is invalid because s1 cannot coexist with s2, so it is not added to the
valid service sets.  Next we move on to n=2, which has 3 combinations.  Of these, {s1, s2} is invalid because s1 cannot coexist with s2.
{s1, s3} is valid because it satisfies all the constraints and is added to the valid service sets.  {s2, s3} is invalid because s2 cannot coexist
with s3.  Finally, we move on to n=1, which has 3 combinations.  {s1} is invalid because s1 must coexist with s3.  {s2} is valid because it
satisfies all the constraints and is added to the valid service sets.  {s3} is invalid because s1 must coexist with s3.  Thus, we end up with
2 valid service sets in this scenario. If there are no valid service sets, there is no solution and the cluster operation fails.

Finding Node Layouts
--------------------
A node layout describes a node and consists of a service set, hardware type, and image type. The goal in this stage is to take the valid
service sets from the previous stage and find all valid node layouts that can be used in the cluster. A similar approach is taken to first
find all valid node layouts. For each valid service set, each combination of service set, hardware type, and image type is examined. If the
node layout satisfies all constraints, it is added a valid node layouts. If not it is discarded.
After that, if there are multiple valid node layouts for a service set, one is chosen and the others are discarded. Which node layout is
chosen is deterministically chosen by a comparator that compares node layouts. An example of this process is shown in the figure below.

.. figure:: /_images/implementation/node_layouts.png
    :align: center
    :alt: Node Layouts
    :figclass: align-center

In this example, there are two hardware types that can be used: hw1 and hw2. Also, there are two image types that can be used: img1 and img2.
The starting valid service sets are taken from the previous example.  Every possible node layout is examined.  Since there are 2 hardware
types and 2 image types, this means there are 4 possible node layouts for each service set. Each one is checked against the constraints.
In this example, s1 must be placed on a node with hw1, and s2 must be placed on a node with img1. After each possible node layout is examined,
we end up with 4 valid node layouts.  However, there are 2 valid node layouts for each service set, which lets us narrow down the final set
until we end up with 2 final node layouts.  Which layout is chosen is deterministically chosen by a pluggable comparator.

Finding Cluster Layout
----------------------
After the final set of node layouts is determined, the solver finds how many of each node layout there should be based on the number of nodes
in the cluster. It does this by first ordering the node layouts by preference, then searching through every possible cluster layout until it
finds a cluster layout that satisfies all constraints. The search is done in a deterministic fashion by trying to use as many of the more
preferred node layouts as possible. Again the preference order is determined using a pluggable comparator. An example is illustrated in the
figure below.

.. figure:: /_images/implementation/cluster_layout.png
    :align: center
    :alt: Cluster Layout
    :figclass: align-center

In this example, the cluster must have 5 nodes, and there is a constraint that s1 must only be placed on one node, and there must be at least
one node with s2. The comparator decides that the node layout with s1 and s3 is preferred over the node layout with just s2. The search then
begins with as many of the first node as possible. At each step, if the current cluster layout is invalid, a single node is taken away from
the most preferred node and given to the next most preferred node. The search continues in this way until a valid cluster layout is found,
or until the search space is completely exhausted. In reality, there are some search optimizations that occur that are not illustrated in the
figure. For example, there can only be at most 1 node of the first node layout since there can only be one node with s1. We can therefore skip
ahead to a cluster layout with only 1 of the first node layout and continue searching from there.

It should be noted that the above examples only illustrate a small number of constraints, whereas many more constraints are possible.
In fact, when shrinking and expanding a cluster, or when removing or adding services from an existing cluster, the current cluster itself
is used as a constraint. That is, the hardware and image types on existing nodes cannot change and are enforced as constraints.
Similarly, services uninvolved in the cluster operation are not allowed to move to a different node.

Once a valid cluster layout has been found, it is sent to the Planner to determine what tasks need to happen to execute the cluster operation.
If no layout is found, the operation fails.

Planner
=======
The planner takes a cluster, its layout and a cluster management operation, and creates an execution plan of node level tasks that must be
performed in order to perform the cluster operation.  It coordinates which tasks must occur before other tasks, and which tasks can be
run in parallel. Ordering of tasks is based on action dependencies that are inherent to the type of cluster operation being performed, and
also based on the service dependencies defined by the administrator. For example, when creating a cluster, creation of nodes must always
happen before installing services on those nodes. That is an example of a dependency that is inherent to the cluster create operation.
An example of a dependency derived from services is if service A depends on service B, then starting service A must happen after service B was started.
The planner works by examining the cluster layout and action dependencies, creating a direct acyclic graphed (DAG) based on the cluster action
and cluster layout, grouping tasks that can be run in parallel into stages, and placing tasks that can currently be run onto a queue for
consumption by the Provisioners.

Creating the DAG
----------------

Below is an example DAG created from a cluster create operation with the cluster layout shown in the examples above.

.. figure:: /_images/implementation/planner_dag.png
    :align: center
    :alt: Planner Dag
    :figclass: align-center

For a cluster create operation, each node must be created, then each service on it must be installed, then configured,
then initialized, then started. In this example, service s3 depends on both s1 and s2. Neither s1 nor s2 depend on any
other service. Since s3 depends on both s1 and s2, the initialize s3 task cannot be performed until all services s1
and s2 on all other nodes in the cluster have been started. There is, however, no dependencies required for installation
and configuration of services.

Grouping into Stages
--------------------
In the above example, many of the tasks can be performed in parallel, while some tasks can only be performed
after others have completed. For example, all of the create node tasks can be done in parallel, but the install
s2 task on node 2 can only be done after the create node 2 task has completed successfully. The Planner takes
the DAG and divides it into stages based on what can be done in parallel. An example is shown in the figure below.

.. figure:: /_images/implementation/planner_dag_stages.png
    :align: center
    :alt: Planner Dag Stages
    :figclass: align-center

The basic algorithm is to identify "sources" in the dag, group all sources into a stage, remove all sources and their edges,
and continue the loop until all tasks are gone from the dag. A "source" is a task that depends on no other task in the DAG.
For example, in the first iteration, all the create node tasks are sources and are therefore grouped into the same stage. Once
the create node tasks and their edges are removed from the DAG, the next iteration begins. All the install tasks are identified
as sources and grouped together into the second stage. This continues until we end up with the stages shown in the figure.
Finally, the Planner also ensures that there is only one task for a given node in a stage. In the above example, stage 2 has
the install s1 task and install s3 task that both need to be performed on node 1. They are therefore split into separate stages
as shown in the final plan shown below.

.. figure:: /_images/implementation/planner_dag_stages2.png
    :align: center
    :alt: Planner Dag Stages 2
    :figclass: align-center


Task Coordination
-----------------
Each task in a stage can be performed concurrently, and all tasks in a stage must be completed before moving on to the next stage.
That is, tasks in stage i+1 are not performed until all tasks in stage i have completed successfully.
Note that this staged approach is not the only way to coordinate execution of the tasks. For example, from the original DAG,
there is nothing wrong with performing the install s2 task on node 2 once the create node 2 task has completed, but the staged approach
will wait until all other create node tasks have completed before perform the install s2 task. Execution order and parallization can
be done in many ways; this is just one simple way to do it.

After the stages have been determined, the Planner will place all tasks in a stage onto a queue for consumption by the Provisioners.
In case a task fails, it is retried a configurable amount of times. Almost all tasks are idempotent with the exception of the create task.
If a create fails, it is possible that the actual machine was provisioned, but there was an issue with the machine. In this case,
the machine is deleted before another is created to prevent resource leaks. In case a Provisioner fails to reply back with a task failure
or success after some configurable timeout, the Planner will assume a failure and retry the task up to the configurable retry limit.
There is a Janitor that runs in the background to perform the timeout.
Once all tasks in a stage are complete, the Planner places all tasks in the next stage onto the queue.


Provisioner
===========
The Provisioner is responsible for managing workers that perform tasks given to it by the Server.
These are the tasks necessary to orchestrate cluster operations and may include provisioning nodes from cloud providers,
installing/configuring software, or running custom commands.  Each worker polls for the next task in
the queue, and handles it to completion. A plugin framework is utilized to handle any task for extensibility.
The provisioner workers are lightweight and stateless, therefore many can be run in parallel.

Upon startup, the provisioner will register itself to the server, telling the server the capacity it has and the host and port
it is running on. With this information, the server may decide to assign workers to the provisioner for one or more tenants.
Every so often, the provisioner sends a heartbeat to the server that lets the server know it is still alive, and also communicates
how many workers are currently running on the provisioner. The provisioner also communicates with the server to pull plugin resources
that may be needed by workers to complete their tasks.

Workers
-------

At a high-level, each provisioner worker is responsible for the following:
  * polling the Server for tasks
  * executing the received task by invoking the appropriate task handler plugin
  * reporting back the results of the operation, including success/failure and any appropriate metadata needed.

Each running Provisioner instance will continually poll the Server for tasks.  When a task is received, it consists of a JSON task definition.  This task definition contains all the information needed by the provisioner to carry out the task.

Consider the typical scenario for provisioning a node on a cloud provider asynchronously
  1. the node is requested with given attributes (size, OS, region, etc)
  2. the provider accepts the request and returns an internal ID for the new node it is going to create
  3. during creation, the requestor must continually poll for the new node's status and public IP address using the internal ID
  4. the requestor does some additional validation using the IP address, and declares success

The internal provider ID obtained in step 2 is required input for step 3, and will be required again if we wish to delete this node.  Similarly, the IP address obtained in step 4 will be used in subsequent tasks.

The following diagram illustrates how this is implemented by the Provisioner:

.. figure:: /_images/implementation/provisioner_operational_model.png
    :align: center
    :alt: Provisioner Operational Model
    :figclass: align-center


In the diagram above, the Provisioner first recieves a CREATE task that instructs it to request a node from a specific provider.  The task contains the all the necessary provider-specific options (truncated in the diagram for brevity).  The provisioner then executes the node create request through the provider API and receives the new node's provider ID as a result.  Since this provider ID will be critical for future operations against this node, it must report it back to the Server.  It does so by populating a "result" key-value hash in the task result JSON.  The Server will preserve these key-values in a "config" hash on all subsequent tasks for this node.  In the diagram, the subsequent "CONFIRM" task is given the provider-id for the node, and similarly it reports back the IP address obtained from the provider in this step.  The third task shown now includes all metadata discovered thus far about the node in the request: the provider-id and the ipaddress.  In this way, Coopr is building up a persistent payload of metadata about a node which can be used by any subsequent task.

In addition to this payload of key-value pairs, Coopr also automatically provides additional metadata regarding cluster layout.  For example, once the nodes of a cluster are established, Server will include a "nodes" hash in the task JSON which contains the hostnames and IP addresses of every node in the cluster.  This can be readily used by any task requiring cluster information, for example configuring software on a node which needs a list of all peer nodes in the cluster.

Plugin Framework
----------------

One of the design goals of Coopr is to be agnostic to the type of cluster being managed.  To achieve this, Provisioner makes extensive use of a plugin framework.  Plugins allow Coopr to provision the same cluster in different providers.  They also allow an enterprise to customize implementation of their cluster services, for example integrating with their own SCM system of choice.

A plugin is a self-contained program designed to perform a specific set of tasks.  Currently, Coopr supports plugins written in Ruby.  Each plugin must have a name and a type.  The name uniquely identifies each plugin, while the type groups related plugins together.  The type also corresponds to the list of tasks the plugin is capable of handling.  For example, consider the following diagram:

.. figure:: /_images/implementation/provisioner_plugin_framework.png
    :align: center
    :alt: Provisioner Plugin Framework
    :figclass: align-center

The diagram shows two tasks being consumed by provisioners and the logic used to invoke the appropriate plugin.  When a task is received, the provisioner first determines from the taskName which type of plugin is required to handle the task.  In the first example, a CREATE taskName indicates the task must be handled by a Provider plugin.  Coopr then checks the task JSON for the providertype field to determine which plugin to invoke.  In the second example, an INSTALL taskName indicates the task must be handled by an Automator plugin.  Coopr then checks the task JSON for the service action type field to determine which plugin to invoke.

A plugin must provide a descriptor file in which it declares its name, type, and execution class.  Upon startup, the provisioner scans its own directories looking for these descriptor files.  Upon successful verification, the plugin is considered registered.

A plugin can contain any arbitrary data it needs to perform its tasks.  For example, a provider plugin may store api credentials locally, or a Chef plugin may keep a local repository of cookbooks.  This data can be packaged with and considered as part of the plugin.  Alternatively, a plugin may also specify certain configuration parameters that it expects to be filled in by the UI users.  For example, there are variances among cloud providers regarding the credentials needed to access their API.  Some require a password, some require a key on disk, etc.  Coopr allows a plugin to specify the necessary configuration fields, so that an admin can simply fill in the values on the UI.  Then, when a task is recieved by that particular plugin, it will have the necessary key-value pairs it expects.

This plugin model is integral to supporting many providers and custom installation procedures.  It makes it easy to leverage existing provider plugins or community code as plugins within Coopr.
