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

.. _guide_installation_toplevel:

.. index::
   single: Quick Start Guide

==================
Quick Start Guide
==================

This guide will help you get started with Coopr. In this section, you will learn to provision a cluster
using one of the preset templates.

Installing Coopr
===========================

Please follow the steps found in the :doc:`Installation Guide </installation/index>`. Once successfully installed,
start all the relevant Coopr components: the server, provisioners, and UI.

.. _quickstart-getting-started:

Getting Started
===============

Open the Coopr UI using a browser at ``http://<coopr-host>:<coopr-ui-port>/`` and login as a superadmin by entering
'superadmin' in the tenant field, 'admin' in the username field, and 'admin' in the password field. A superadmin can
create and manage tenants. Each tenant has its own admin and a number of users. Tenant admins are responsible for configuring
providers and templates to allow their users to easily create and manage clusters. Data from one tenant is isolated to that
tenant and is invisible to users in other tenants. The superadmin is also an admin in its own tenant.

.. figure:: /_images/quickstart/qs_login.png
    :align: center
    :width: 800px
    :alt: Login as the super admin
    :figclass: align-center


Logging in will take you to the administrator home screen. The page, shown below, displays metrics for clusters
that are currently running within the tenant. Note, the 'All Nodes' count metric
indicates all the nodes ever provisioned. (i.e. it is a historical cumulative number including the
deleted nodes.) This page also shows, in the icons at the top of the page, a link to the
'Catalog', which is a list of 'templates' for provisioning clusters. Several default
templates are available out of the box.

.. figure:: /_images/quickstart/qs_home_screen.png
    :align: center
    :width: 800px
    :alt: Administrator home screen
    :figclass: align-center

Adding Workers
==============

Before clusters can be created, workers must be assigned to the tenant. To do so, click on
the Admin button in the upper-right of the page to bring down the Admin profile menu. Select
the 'Tenants' item from the menu:

.. figure:: /_images/quickstart/qs_tenants_menu.png
    :align: center
    :width: 800px
    :alt: Tenant management screen
    :figclass: align-center


This takes you to the tenant management screen, where a superadmin may create, edit, and delete tenants, as well as get an overview
of the system. Near the top of the screen, the total number of workers, the number of available workers, and the number of tasks 
currently in progress and queued are displayed. These numbers are aggregates across all tenants in the system, and are visible only
to the superadmin. 

.. figure:: /_images/quickstart/qs_tenants_overview.png
    :align: center
    :width: 800px
    :alt: Tenant management screen
    :figclass: align-center

Workers are provided by provisioners. If your system is using all its workers, additional provisioners must be added to the system
in order to support additional tenants. With a clean install, only the the superadmin's tenant will exist, and no workers will be 
assigned to any tenant. In order to create a cluster, we must assign some workers to the superadmin tenant. To do this, we must edit
the superadmin tenant by clicking on it, which will bring you to the edit tenant screen.

.. figure:: /_images/quickstart/qs_tenants_edit.png
    :align: center
    :width: 800px
    :alt: Tenant edit screen
    :figclass: align-center

On this screen, assign 10 workers (all available workers) to the tenant and enter some reasonable maximums for the number of clusters
and nodes that can be live at any given time within the tenant. Once you are done, click submit.


Configuring a Provider
=========================

To start provisioning machines, you must first specify an IaaS provider on which the clusters will be created. Click on the 
'Providers' tab at the top of the screen. Several defaults are available on this page. There are providers for Amazon, DigitalOcean,
Google, Joyent, Openstack, and Rackspace. Choose the provider you want to use for this tutorial, then click on its name to navigate 
to its edit screen.

Each provider type has fields specific to your own provider and account.
These inputs may include settings such as username and API key, and can be obtained through the provider's own 
system. If you do not already have an account with the provider, you may register or obtain one on a provider's 
website. Next, we go through how to set up each of the default providers. You will only need to set up the
provider(s) you are using.

Amazon Web Services (AWS)
^^^^^^^^^^^^^^^^^^^^^^^^^
The Amazon providers require a lot of fields. Of these many fields, ``Secret Access Key``, ``Access Key ID``,
``AWS Region``, ``Key Pair Name``, and ``SSH Key Resource Name`` must be set. Once those are set, you must provide
either the ``Security Groups`` field if your account does not use VPC, or the ``Security Groups IDs`` and ``Subnet ID``
fields if your account uses VPC. The rest of the fields are optional.

.. figure:: /_images/quickstart/qs_providers_aws.png
    :align: center
    :width: 800px
    :alt: Configuring an AWS provider
    :figclass: align-center

Your AWS SSH key is a plugin resource, and must be uploaded to the Coopr server 
before it can be used by workers. Coopr will take care of distributing the key to workers that need it. A UI for managing
resources is coming in a later release. Until then, you must use the REST API directly (see
:doc:`Plugin Resource API </rest/plugins>`), or use the data upload tool included in the provisioner package.

For example, assume your SSH key is located at '/keys/aws/id_rsa' and you want to upload it as a resource named 'ec2'.
Enter 'ec2' into the ``SSH Key Resource Name``, then upload the resource.
If you are using Coopr Standalone, run the following command from the unzipped standalone directory:

.. code-block:: bash

 $ ruby provisioner/bin/data-uploader.rb sync /keys/aws/id_rsa providertypes/aws/ssh_keys/ec2 -u http://<server>:<port>
 upload successful for http://<server>:<port>/v2/plugins/providertypes/aws/ssh_keys/ec2, version: 1
 stage successful for http://<server>:<port>/v2/plugins/providertypes/aws/ssh_keys/ec2/versions/1/stage
 sync successful

If you have installed Coopr from packages or are running it using the VM image:

.. code-block:: bash

 $ /opt/coopr/provisioner/embedded/bin/ruby /opt/coopr/provisioner/bin/data-uploader.rb sync /keys/aws/id_rsa providertypes/aws/ssh_keys/ec2 -u http://<server>:<port>
 upload successful for http://<server>:<port>/v2/plugins/providertypes/aws/ssh_keys/ec2, version: 1
 stage successful for http://<server>:<port>/v2/plugins/providertypes/aws/ssh_keys/ec2/versions/1/stage
 sync successful

The port to use is the server port, which defaults to 55054 if you have not set it in your server configuration.
This will upload your key to the server, then sync it to make it available for use. After this you may 
use this key in any aws provider by referring to it as 'ec2'. Similarly, you may upload other keys you may want to use.
For example, you could upload the another key and name it 'ec2-east-1'. Then in the ``SSH Key Resource Name`` field,
you would enter 'ec2-east-1'.

Once you are finished, click on 'Submit' to save your changes.


DigitalOcean
^^^^^^^^^^^^

DigitalOcean requires a client ID, API key and SSH key. For the SSH key, you will also need to enter an SSH key name.
Note: SSH key name is the name under which you uploaded your key in the DigitalOcean portal.

Enter values for all those fields.  If applicable, change the region from the default (currently nyc2).

.. figure:: /_images/quickstart/qs_providers_digitalocean.png
    :align: center
    :width: 800px
    :alt: Configuring a DigitalOcean provider
    :figclass: align-center

Your DigitalOcean SSH key is a plugin resource, and must be uploaded to the Coopr server before it can be used by workers. 
Coopr will take care of distributing the key to workers that need it. A UI for managing resources is coming in a later release. 
Until then, you must use the REST API directly (see :doc:`Plugin Resource API </rest/plugins>`), or use the data upload tool included 
in the provisioner package.

For example, assume your key is located at '/keys/digitalocean/id_rsa' and you want to add it as a resource named 'coopr'.
Enter 'coopr' into the ``SSH Key Resource Name``, then upload the resource.
If you are using Coopr Standalone, run the following command from the unzipped standalone directory:

.. code-block:: bash

 $ ruby provisioner/bin/data-uploader.rb sync /keys/digitalocean/id_rsa providertypes/digitalocean/ssh_keys/coopr -u http://<server>:<port>
 upload successful for ...
 stage successful for ...
 sync successful


Google
^^^^^^
The Google provider requires a p12 API key, a service account email address, some default data disk size, a project id,
a SSH key to SSH onto nodes, a SSH username for that key, and a zone. Enter the corresponding values in the 
``Service account email address``, ``Project ID``, ``SSH Username``, and ``Zone`` field.

.. figure:: /_images/quickstart/qs_providers_google.png
    :align: center
    :width: 800px
    :alt: Configuring a Google provider
    :figclass: align-center

The required ``API Key Resource Name`` and ``SSH Key Resource Name`` fields are plugin resources, and must
be uploaded to the Coopr server before it can be used by workers. 
A UI for managing resources is coming in a later release. Until then, you must use the REST API directly (see
:doc:`Plugin Resource API </rest/plugins>`), or use the data upload tool included in the provisioner package.

For example, assume your Google API key is located at '/keys/gce/gce.p12' and your SSH key is located at '/keys/gce/id_rsa'.
Enter 'gce' in the ``API Key Resource Name`` field and 'coopr' in the ``SSH Key Resource Name`` field.
We must then upload your API key and name it 'gce', and upload your SSH key and name it 'coopr'.
If you are using Coopr Standalone, run the following commands from the unzipped standalone directory:

.. code-block:: bash

 $ ruby provisioner/bin/data-uploader.rb sync /keys/gce/gce.p12 providertypes/google/api_keys/gce -u http://<server>:<port>
 upload successful for http://<server>:<port>/v2/plugins/providertypes/google/api_keys/gce, version: 1
 stage successful for http://<server>:<port>/v2/plugins/providertypes/google/api_keys/gce/versions/1/stage
 sync successful
 $ ruby provisioner/bin/data-uploader.rb sync /keys/gce/id_rsa providertypes/google/ssh_keys/coopr -u http://<server>:<port>
 upload successful for http://<server>:<port>/v2/plugins/providertypes/google/ssh_keys/coopr, version: 1
 stage successful for http://<server>:<port>/v2/plugins/providertypes/google/ssh_keys/coopr/versions/1/stage
 sync successful

If you have installed Coopr from packages or are running it using the VM image:

.. code-block:: bash

 $ /opt/coopr/provisioner/embedded/bin/ruby /opt/coopr/provisioner/bin/data-uploader.rb sync /keys/gce/gce.p12 providertypes/google/api_keys/gce -u http://<server>:<port>
 upload successful for http://<server>:<port>/v2/plugins/providertypes/google/api_keys/gce, version: 1
 stage successful for http://<server>:<port>/v2/plugins/providertypes/google/api_keys/gce/versions/1/stage
 sync successful
 $ /opt/coopr/provisioner/embedded/bin/ruby /opt/coopr/provisioner/bin/data-uploader.rb sync /keys/gce/id_rsa providertypes/google/ssh_keys/coopr -u http://<server>:<port>
 upload successful for http://<server>:<port>/v2/plugins/providertypes/google/ssh_keys/coopr, version: 1
 stage successful for http://<server>:<port>/v2/plugins/providertypes/google/ssh_keys/coopr/versions/1/stage
 sync successful

The port to use is the server port, which defaults to 55054 if you have not set it in your server configuration.
This will upload your API and SSH keys to the server, then sync them to make them available to use. After this you may 
use these keys in any other Google provider you manage. Similarly, you may upload other keys you may want to use.

Once you are finished, click 'Submit' to save your changes.


Joyent
^^^^^^
Joyent requires a CloudAPI password, username, region, API version, SSH key, and SSH key name. Enter values for all fields
except for the ``SSH Key Resource Name``.

.. figure:: /_images/quickstart/qs_providers_joyent.png
    :align: center
    :width: 800px
    :alt: Configuring a Joyent provider
    :figclass: align-center

Your Joyent SSH key is a plugin resource, and must be uploaded to the Coopr server 
before it can be used by workers. Coopr will take care of distributing the key to workers that need it. A UI for managing
resources is coming in a later release. Until then, you must use the REST API directly (see
:doc:`Plugin Resource API </rest/plugins>`), or use the data upload tool included in the provisioner package.

For example, assume your key is located at '/keys/joyent/id_rsa' and you want to add it as a resource named 'coopr'.
Enter 'coopr' into the ``SSH Key Resource Name``, then upload the resource.
If you are using Coopr Standalone, run the following command from the unzipped standalone directory:

.. code-block:: bash

 $ ruby provisioner/bin/data-uploader.rb sync /keys/joyent/id_rsa providertypes/joyent/ssh_keys/coopr -u http://<server>:<port>
 upload successful for http://<server>:<port>/v2/plugins/providertypes/joyent/ssh_keys/coopr, version: 1
 stage successful for http://<server>:<port>/v2/plugins/providertypes/joyent/ssh_keys/coopr/versions/1/stage
 sync successful

If you have installed Coopr from packages or are running it using the VM image:

.. code-block:: bash

 $ /opt/coopr/provisioner/embedded/bin/ruby /opt/coopr/provisioner/bin/data-uploader.rb sync /keys/joyent/id_rsa providertypes/joyent/ssh_keys/coopr -u http://<server>:<port>
 upload successful for http://<server>:<port>/v2/plugins/providertypes/joyent/ssh_keys/coopr, version: 1
 stage successful for http://<server>:<port>/v2/plugins/providertypes/joyent/ssh_keys/coopr/versions/1/stage
 sync successful

The port to use is the server port, which defaults to 55054 if you have not set it in your server configuration.
This will upload your key to the server, then sync it to make it available for use. After this you may 
use this key in any joyent provider by referring to it as 'coopr'. Similarly, you may upload other keys you may want to use.
For example, you could upload the another key and name it 'joyentuser'. Then in the ``SSH Key Resource Name`` field,
you would enter 'joyentuser'.


OpenStack
^^^^^^^^^
The OpenStack provider has been tested on Havana, but also supports Grizzly out of the box. OpenStack support has 
some limitations that are described :doc:`here </installation/openstack-config>`.
Several of these limitations will be eliminated in future releases of Coopr.
The first step is to configure the openstack provider to use your credentials. 
OpenStack requires a password, username, auth url, tenant, SSH key, and SSH key name. Enter the correct value for
every field, except for the ``SSH Key Resource Name``.

.. figure:: /_images/quickstart/qs_providers_openstack.png
    :align: center
    :width: 800px
    :alt: Configuring an OpenStack provider
    :figclass: align-center

The ``SSH Key Resource Name`` is a plugin resource, and must be uploaded to the Coopr server 
before it can be used by workers. Coopr will take care of distributing the key to workers that need it. A UI for managing
resources is coming in a later release. Until then, you must use the REST API directly (see
:doc:`Plugin Resource API </rest/plugins>`), or use the data upload tool included in the provisioner package.

For example, assume your key is located at ``/keys/openstack/id_rsa`` and you want to upload it as a resource named 'coopr'.
Enter 'coopr' into the ``SSH Key Resource Name``, then upload the resource.
If you are using Coopr Standalone, run the following command from the unzipped standalone directory:

.. code-block:: bash

 $ ruby provisioner/bin/data-uploader.rb sync /keys/openstack/id_rsa providertypes/openstack/ssh_keys/coopr -u http://<server>:<port>
 upload successful for http://<server>:<port>/v2/plugins/providertypes/openstack/ssh_keys/coopr, version: 1
 stage successful for http://<server>:<port>/v2/plugins/providertypes/openstack/ssh_keys/coopr/versions/1/stage
 sync successful

If you have installed Coopr from packages or are running it using the VM image:

.. code-block:: bash

 $ /opt/coopr/provisioner/embedded/bin/ruby /opt/coopr/provisioner/bin/data-uploader.rb sync /keys/openstack/id_rsa providertypes/openstack/ssh_keys/coopr -u http://<server>:<port>
 upload successful for http://<server>:<port>/v2/plugins/providertypes/openstack/ssh_keys/coopr, version: 1
 stage successful for http://<server>:<port>/v2/plugins/providertypes/openstack/ssh_keys/coopr/versions/1/stage
 sync successful

The port to use is the server port, which defaults to 55054 if you have not set it in your server configuration.
This will upload your key to the server, then sync it to make it available for use. After this you may 
use this key in any openstack provider by referring to it as 'coopr'. Similarly, you may upload other keys you may want to use.
For example, you could upload another key and name it 'havana'. Then in the ``SSH Key Resource Name`` field,
you would enter 'havana'. Once you are finished, click 'Submit' to save your changes.

Next, we need to configure the default hardware types and image types to be able to use your instance of OpenStack. Navigate
to the Hardware tab on the top of the screen and edit each hardware type in the list that you wish to use. You will notice that 
other providers like google and aws are already configured for each hardware type with their corresponding flavor. They are already 
configured because their flavors are public and unchanging, whereas your OpenStack instance may use its own flavors. Click on the 
'Add Provider' button, change the provider to openstack, and input your OpenStack's flavor identifier for the corresponding hardware 
type. You may need to contact your OpenStack administrator to get this information. 

.. figure:: /_images/quickstart/qs_providers_openstack_hardware.png
    :align: center
    :width: 800px
    :alt: Configuring an OpenStack hardware type
    :figclass: align-center

Next, we need to configure the default image types. Navigate to the 
Images tab of the left and edit each image type in the list that you wish to use. Click on the 'Add Provider' button,
change the provider to openstack, and input your OpenStack's image identifier for the corresponding image type. You may need to 
contact your OpenStack administrator to get this information.

.. figure:: /_images/quickstart/qs_providers_openstack_image.png
    :align: center
    :width: 800px
    :alt: Configuring an OpenStack image type
    :figclass: align-center


Rackspace
^^^^^^^^^
An API key, username, and region are required for using Rackspace (for more information on how to obtain your personalized API key, see
`this page <http://www.rackspace.com/knowledge_center/article/rackspace-cloud-essentials-1-generating-your-api-key>`_ ).

.. figure:: /_images/quickstart/qs_providers_rackspace.png
    :align: center
    :width: 800px
    :alt: Configuring a Rackspace provider
    :figclass: align-center

Enter the necessary fields and click on 'Submit' to save your changes.


Provisioning your First Cluster
===============================

Click on the 'Clusters' icon on the right most icon on the top bar. This page lists all the clusters
that have been provisioned that are accessible to the logged-in user.

.. figure:: /_images/quickstart/qs_clusters.png
    :align: center
    :width: 800px
    :alt: Clusters list
    :figclass: align-center

Click on the 'Create' buttom at the top right (or, as you have no clusters currently, the
'Create a cluster' button in the middle of the list) to enter the cluster creation page:

.. figure:: /_images/quickstart/qs_cluster_create.png
    :align: center
    :width: 800px
    :alt: Creating a cluster
    :figclass: align-center

In the 'Name' field, enter a name (for example, 'hadoop-quickstart') as the name of the
cluster to create. The 'Template' field specifies which template in the catalog to use for
this cluster. For this tutorial, let's create a distributed Hadoop cluster.  Select
'hadoop-distributed' from the 'Template' drop down box. Enter the number of nodes you want
your cluster to have (for example, 5) in the field labeled '# of nodes'.

Display the advanced settings menu by clicking on the small triangle next to the label 'Advanced'. This lists
the default settings for the 'hadoop-distributed' template. If you chose a provider other than the default 
in the previous section, click on the drop down menu labeled 'Provider' to select the provider you want.

.. figure:: /_images/quickstart/qs_cluster_create_advanced.png
    :align: center
    :width: 800px
    :alt: Advanced cluster settings
    :figclass: align-center

To start provisioning, click on 'Create' at the bottom of the page (not shown in the image above). This operation will take you back to the Clusters' home
screen, where you can monitor the progress and status of your cluster. Creating a cluster may take several minutes.

.. figure:: /_images/quickstart/qs_clusters_list.png
    :align: center
    :width: 800px
    :alt: Creation running
    :figclass: align-center

Accessing the Cluster
=====================

Once creation is complete, the cluster is ready for use.

For more information on your cluster, click on the name 'hadoop-quickstart' on the
Clusters' home screen. On this cluster description screen, nodes are grouped together by the set
of services that are available on them. To see node details, click on the descriptions
next to the number of nodes ('2 nodes with 5 services each') to expand the list. The
expanded list shows a list of attributes for each node.

.. figure:: /_images/quickstart/qs_cluster_details.png
    :align: center
    :width: 800px
    :alt: Cluster description and details
    :figclass: align-center

In this example, there is 1 master node that contains the 'base', 'hadoop-hdfs-namenode',
'hadoop-yarn-resourcemanager', 'hbase-master', and 'zookeeper-server' services. There are
also 2 slave nodes that contain the 'base', 'hadoop-hdfs-datanode',
'hadoop-yarn-nodemanager', and 'hbase-regionserver' services.
