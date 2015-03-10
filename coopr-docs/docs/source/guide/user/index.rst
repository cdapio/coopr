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

.. _guide_user_toplevel:

.. index::
   single: User Guide

==========
User Guide
==========

This page describes the different interfaces and functions that end users can use to manage their own set of clusters within Coopr.
As mentioned earlier in the administration guide, all clusters and nodes within clusters are dictated by templates created by
the administrator, made accessible to individual users (or developers), and displayed in the Catalog. These accessible templates  
are users' blueprint for their individual cluster instantiation.

The User Home Screen
====================
Login to the Coopr UI using your user credentials at ``http://<coopr-host>:<coopr-ui-port>/``.
This will take you to the user home screen, which shows a list of all clusters provisioned by a user. This page displays basic information for each cluster owned
by the user, such as current clusters, clusters under construction, and deleted clusters. Active and terminated clusters, however, 
are shown separately in this interface. Clicking the text at the bottom of the list
take you to a separate screen with terminated cluster descriptions and detailed information.

.. figure:: /_images/user/user_clusters.png
    :align: center
    :width: 800px
    :alt: User home screen
    :figclass: align-center


Provisioning a new Cluster
==========================
Users can provision a cluster by clicking the 'Create' button in the upper-right of the screen. Through this page, a user
can create a cluster with a given name and template setting (as defined by the system administrator), and specify the
number of nodes to allocate to the cluster.

(For more information on how administrators can set templates for provisioning a cluster, see the :doc:`Administration
Guide </guide/admin/index>`.)

.. figure:: /_images/user/user_cluster_create.png
    :align: center
    :width: 800px
    :alt: Cluster creation screen
    :figclass: align-center


Advanced Settings
-----------------

The Coopr User Interface has a number of advanced configuration options.
To access the advanced options, click on the blue triangle next to the label 'Advanced'. This exposes the options to
explicitly specify the provider, hardware, and image type to be used for the current cluster. 

The 'Lease Duration' field allows the user to specify the duration, in days, hours and
minutes, that they want to lease the cluster for. Services to place on the cluster can be
added or removed from this screen. The 'Config' field allows the user to specify
additional custom configurations in a JSON-formatted input (for more information, see
:doc:`Macros </guide/admin/macros>`). 

.. figure:: /_images/user/user_cluster_create_advanced.png
    :align: center
    :width: 800px
    :alt: Cluster creation screen - advanced settings
    :figclass: align-center

To start the creation of the cluster, click the 'Create' button at the bottom on the page.

The Cluster Description Screen
==============================
A user can view more details of an existing cluster by clicking on the cluster name on the Home screen, or by selecting
'Clusters' -> <name of cluster> in the upper-right of the page. The cluster description page provides an up-to-date
status report of a cluster as well as a description of a cluster, the template used
to create the cluster, the infrastructure provider, and the list of services installed.

To abort a cluster that is currently being created, click on 'Abort' next to the progress bar on this screen.

.. figure:: /_images/user/user_cluster_description.png
    :align: center
    :width: 800px
    :alt: Cluster description screen
    :figclass: align-center

Examining and Accessing the Cluster
===================================
On the cluster description screen, nodes are grouped together with the sets of services that are available on them.

.. figure:: /_images/user/user_screenshot_1.png
    :align: center
    :width: 800px
    :alt: Cluster description screen
    :figclass: align-center

To view the individual nodes under each service set, click on the text ("1 node with 17
services") next to the services. From the expanded list, a user can obtain attributes
about each node, including its hostname and ID. For certain providers, the list may also
show authentication credentials for accessing the nodes, through a service such as SSH.

The expanded list shows a list of attributes for each node. These nodes can now be
accessed using the corresponding IP addresses, usernames and passwords (through a service such as SSH).

To view the actions that have been performed on a particular node, click on the 'Show actions' button.

.. figure:: /_images/user/user_screenshot_2.png
    :align: center
    :width: 800px
    :alt: Show actions
    :figclass: align-center

Deleting a Cluster
------------------
The trash can icon with the word 'Delete' in the upper-right of the cluster description
page deletes the cluster and decommissions the associated nodes. Clusters that are
successfully deleted are moved from the 'Live clusters' list to the 'Terminated clusters'
list on the user's home screen.

Reconfiguring Services
----------------------
Services can be reconfigured by clicking on the 'Reconfigure' button in the upper-right of the cluster description page.
Clicking on the button brings you to the reconfigure page. 

.. figure:: /_images/user/user_reconfigure_screenshot_1.png
    :align: center
    :width: 800px
    :alt: Reconfigure cluster
    :figclass: align-center

Click on 'Advanced' to bring up the advanced options. At this
point, though many other settings are shown on the screen, only the 'Config' section can be changed. Edit the config as
desired. There is also an 'Update' toggle at the bottom of the page. If restart is on, all cluster services will be restarted
after they are reconfigured. If it is off, all service will be reconfigured, but they will not be restarted. You may have to 
restart them yourself in order for the changes to take place.  

.. figure:: /_images/user/user_reconfigure_screenshot_2.png
    :align: center
    :width: 800px
    :alt: Reconfigure cluster
    :figclass: align-center

Starting, Stopping, and Restarting Services
-------------------------------------------
Services can be started, stopped, and restarted from the cluster description page screen. 

- To start a service, click on the black downward-triangle next to the service name, to
  expose the service menu. Select 'Start' from the menu.
- To stop a service, bring up the same menu, and click 'Stop'.  
- To restart a service, bring up the menu and select 'Restart'.

When a start, stop, or restart is performed on a cluster service, service runtime
dependencies are examined in order to determine if other cluster services also need to be
started, stopped, or restarted. 

For example, suppose service A depends on service B. A request to stop service B is made.
Since service A depends on service B, service A will be stopped before service B is
stopped. Similarly, if a request to restart service B is made, service A will be stopped,
then service B will be stopped, then service B will be start, and finally service A will
be started. If a request to start service A is made, service B will first be started, and
then service A will be started. It should be noted that at this time, the system does not
track or monitor the status of services. This is why additional starting and stopping of
dependent services may occur. 

Services can only be started, stopped, or restarted if they are in an active state. 

.. figure:: /_images/user/user_service_actions_screenshot.png
    :align: center
    :width: 800px
    :alt: Service actions
    :figclass: align-center

Adding Services
---------------
Services can be added to an active cluster from the cluster description screen. Services
that can be added are services in the template's compatibility list that are not already
on the cluster. To add services to a cluster, click on the 'Add Service' button and select
a service from the drop down menu. After adding all the services desired, click on the
'Submit Additional Services' button to add the new services.

.. figure:: /_images/user/user_add_services_screenshot.png
    :align: center
    :width: 800px
    :alt: Adding services
    :figclass: align-center


User Menu
============

Users can bring up a user menu by clicking on the button on the very top-right of the screen. This brings up a menu
with links to documentation, contacting support, and logging out.

.. figure:: /_images/user/user_profile.png
    :align: center
    :width: 800px
    :alt: User Menu
    :figclass: align-center
