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

.. _guide_installation_toplevel:

.. index::
   single: Quick Start Guide

==================
Quick Start Guide
==================

This guide will help you get started with Continuuity Loom. In this section, you will learn to provision a cluster
using one of the preset templates.

Installing Continuuity Loom
===========================

Please follow the steps found in the :doc:`Installation Guide </guide/installation/index>`. Once successfully installed,
start all the relevant Loom components: the Loom server, provisioners, and UI.

Getting Started
===============

Open the Loom UI using a browser at ``http://<loom-host>:<loom-ui-port>/`` and login as an administrator. The default
password is 'admin'.  

.. figure:: /guide/quickstart/quickstart-screenshot-1.png
    :align: center
    :width: 800px
    :alt: Login as an administrator
    :figclass: align-center


This will take you to the administrator home screen. The
page, shown below, shows metrics for clusters that are currently running on the system. Note, the 'All Nodes' count metric
indicates all the nodes provisioned since the beginning. (i.e. it is a historical cumulative number including the
deleted nodes.) This page also shows the 'Catalog', which is a list of 'templates'
for provisioning clusters. Several default templates are available out of the box.

.. figure:: /guide/quickstart/quickstart-screenshot-2.png
    :align: center
    :width: 800px
    :alt: Administrator home screen
    :figclass: align-center

Configuring a Provider
=========================

To start provisioning machines, you must first specify an IaaS provider on which the clusters will be created. Click on the 
'Providers' icon on the sidebar to the left. Several defaults should already be available on this
page, namely OpenStack, Rackspace, and Joyent. Choose the provider you want to use for this
tutorial, then click on its name to navigate to its edit screen.

Each provider type has fields specific to your own provider and account.
These inputs may include settings such as username and API key, and they can be obtained through the provider's own 
system. If you do not already have an account with the provider, you may register or obtain one on a provider's 
website. Next, we go through how to set up each of the three default providers. You will only need to set up the
provider you are using.

Rackspace
^^^^^^^^^
An API key, username, and region are required for using Rackspace (for more information on how to obtain your personalized API key, see
`this page <http://www.rackspace.com/knowledge_center/article/rackspace-cloud-essentials-1-generating-your-api-key>`_ ).

.. figure:: /guide/quickstart/rackspace.png
    :align: center
    :width: 800px
    :alt: Configuring a Rackspace provider
    :figclass: align-center

Enter the necessary fields and click on 'Save' to persist them.

Joyent
^^^^^^
Joyent request a username, api key name, an api key file, api url, and version. The key file must be present on all machines
running the Provisioner, must be owned by the user running Continuuity Loom, and must be readable only by the user that owns it
(0400 permissions). 

.. figure:: /guide/quickstart/joyent.png
    :align: center
    :width: 800px
    :alt: Configuring a Joyent provider
    :figclass: align-center

Enter the necessary fields and click on 'Save' to persist them.

OpenStack
^^^^^^^^^
OpenStack has been extensively tested on Havana, but it also supports Grizzly out of the box. OpenStack support has 
some limitations that are described :doc:`here </guide/installation/openstack-config>`.
Several of these limitations will be eliminated in future releases of Continuuity Loom.
The first step is to configure the openstack provider to use your credentials. 
OpenStack requires a username, password, tenant, api url, ssh key id, and identity file. The identity file must be 
present on all machines running the Provisioner, must be owned by the user running Continuuity Loom, and must be readable only by
the user that owns it (0400 permissions).

.. figure:: /guide/quickstart/openstack-provider.png
    :align: center
    :width: 800px
    :alt: Configuring an OpenStack provider
    :figclass: align-center

Next, we need to configure the default hardware types and image types to be able to use your instance of OpenStack. Navigate
to the Hardware tab on the left and edit each hardware type in the list (small, medium, and large). You will notice that 
joyent and rackspace are already configured for each hardware type with their corresponding flavor. They are already 
configured because their flavors are public and unchanging, whereas your OpenStack instance may use its own flavors. Click on the 
'Add Provider' button, change the provider to openstack, and input your OpenStack's flavor identifier for the corresponding hardware 
type. You may need to contact your OpenStack administrator to get this information. 

.. figure:: /guide/quickstart/openstack-hardware.png
    :align: center
    :width: 800px
    :alt: Configuring an OpenStack hardware type
    :figclass: align-center

Next, we need to configure the default image types to be able to use your instance of OpenStack. Navigate to the 
Images tab of the left and edit each image type in the list (centos6 and ubuntu12). Click on the 'Add Provider' button,
change the provider to openstack, and input your OpenStack's image identifier for the corresponding image type. You may need to 
contact your OpenStack administrator to get this information.

.. figure:: /guide/quickstart/openstack-image.png
    :align: center
    :width: 800px
    :alt: Configuring an OpenStack image type
    :figclass: align-center


Provisioning your First Cluster
===============================

Click on the 'Clusters' icon on the sidebar to the left. For an administrator, this page lists all the clusters
that have been provisioned across all Loom user accounts.

.. figure:: /guide/quickstart/quickstart-screenshot-3.png
    :align: center
    :width: 800px
    :alt: Creating a cluster
    :figclass: align-center

Click on 'Create a cluster' on the top menu bar to enter the cluster creation page. In the 'Name' field,
enter 'loom-quickstart-01' as the name of the cluster to create. The 'Template' field
specifies which template in the catalog to use for this cluster. For this tutorial, let's
create a distributed Hadoop and HBase cluster.

Select 'hadoop-hbase-distributed' from the 'Template' drop down box. Enter the number of nodes you want your cluster
to have (for example, 5) in the field labeled 'Number of machines'.

Display the advanced settings menu by clicking on the small triangle next to the label 'Advanced'. This lists
the default settings for the 'hadoop-hbase-distributed' template. If you chose a provider other than Rackspace
in the previous section, click on the drop down menu labeled 'Provider' to select the provider you want.

.. figure:: /guide/quickstart/quickstart-screenshot-5.png
    :align: center
    :width: 800px
    :alt: Advanced settings
    :figclass: align-center

To start provisioning, click on 'Create' at the bottom of the page (not shown in the image above). This operation will take you back to the Clusters' home
screen, where you can monitor the progress and status of your cluster. Creating a cluster may take several minutes.

.. figure:: /guide/quickstart/quickstart-screenshot-4.png
    :align: center
    :width: 800px
    :alt: Creation running
    :figclass: align-center

Accessing the Cluster
=====================

Once creation is complete, the cluster is ready for use.

For more information on your cluster, click on the name 'loom-quickstart-01' on the
Clusters' home screen. On this cluster description screen, nodes are grouped together by the set
of services that are available on them. To see node details, click on the white triangles next to each
service set to expand the list. The expanded list shows a list of attributes for each node.

.. figure:: /guide/quickstart/quickstart-screenshot-6.png
    :align: center
    :width: 800px
    :alt: Cluster description and details
    :figclass: align-center
