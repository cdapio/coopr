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

.. _plugin-reference:


.. index::
   single: Chef Solo Automator Plugin

==========================
Chef Solo Automator Plugin
==========================

.. include:: /guide/admin/admin-links.rst

This section describes an automator that uses chef-solo. Basic knowledge of Chef and its primitives is assumed.

Overview
========

The Chef Solo Automator plugin, like all automator plugins, is responsible for performing the installation and
operation of services on remote hosts. The Chef Solo Automator plugin achieves this by running chef-solo on the remote host
with a custom run-list and set of JSON attributes. The attributes provided to each chef-solo run will be a combination
of cluster-wide configuration attributes, as well as service-specific attributes definable for each action. Each
chef-solo run is self-contained and intended to perform a specific task such as starting a service. This differs from
the typical usage of Chef where one builds up a large run-list managing all resources on a box in one run.

To illustrate this, consider the following example which shows how we can manage the apache web server on Coopr cluster
nodes using the "apache2" community cookbook. We define a Coopr service "apache-httpd" as follows::

    {
        "dependson": [
            "hosts"
        ],
        "description": "Apache HTTP Server",
        "name": "apache-httpd",
        "provisioner": {
            "actions": {
                "install": {
                    "type": "chef-solo"
                    "fields": {
                        "run_list": "recipe[apache2::default]",
                    }
                },
                "configure": {
                    "type": "chef-solo"
                    "fields": {
                        "run_list": "recipe[apache2::default]",
                    }
                },
                "start": {
                    "type": "chef-solo"
                    "fields": {
                        "json_attributes": "{\"coopr\": { \"node\": { \"services\": { \"apache2\": \"start\" } } } }",
                        "run_list": "recipe[apache2::default],recipe[coopr_service_manager::default]",
                    }
                },
                "stop": {
                    "type": "chef-solo"
                    "fields": {
                        "json_attributes": "{\"coopr\": { \"node\": { \"services\": { \"apache2\": \"stop\" } } } }",
                        "run_list": "recipe[apache2::default],recipe[coopr_service_manager::default]",
                    }
                }
            }
        }
    }

For each action, we define the ``type``, and the custom fields ``run_list`` and ``json_attributes``. (defaults to empty string if not specified). The ``type``
field indicates to the provisioner to use the Chef Solo Automator plugin to manage this action. The ``run_list`` field specifies
the run-list to use. The ``json_attributes`` field is any additional JSON data we wish to include in the Chef run (more on this
later). When the Chef Solo Automator plugin executes any of these actions for the apache-httpd service, it performs
the following actions:

        1. generate a task-specific JSON file containing any attributes defined in the json_attributes field, as well as base cluster attributes defined elsewhere in Coopr.
        2. invoke chef-solo using the ``run_list`` field as the run-list as follows:  ``chef-solo -o [run_list] -j [task-specific json]``


In this example, to execute an "install" task for the apache-httpd service, the provisioner will simply run the default
recipe from the apache2 cookbook as a single chef-solo run. No additional JSON attributes are provided beyond the base
cluster configuration attributes.

For a "configure" task, the provisioner will also run the default recipe from the apache2 cookbook. For this community
cookbook, the installation and configuration are done in the same recipe, which is common but not always the case. So
one may wonder why we need both 'install' and 'configure' when they perform identical actions. It is best practice to
keep them both, since configure may be run many times throughout the lifecycle of the cluster, and install is needed
to satisfy dependencies.

The "start" and "stop" tasks introduce a couple of features. They make use of the ``json_attributes`` field to specify custom JSON
attributes. Note that the format is an escaped JSON string. The ``run_list`` field also contains an additional recipe,
``coopr_service_manager::default``. More on this later, but essentially this is a helper cookbook that can operate on
any Chef service resource. It looks for any service names listed in node['coopr']['node']['services'], finds the
corresponding Chef service resource, and invokes the specified action.


JSON Attributes
================

Coopr maintains significant JSON data for a cluster, and makes it available for each task. This JSON data includes:
    * cluster-wide configuration defined in cluster templates (Catalog -> cluster template -> defaults -> config)
    * node data for each node of the cluster: hostname, ip, etc
    * service data, specified in the actions for each service

The Chef Solo Automator plugin automatically merges this data into a single JSON file, which is then passed to chef-solo via
the ``--json-attributes argument``. Any custom cookbooks that want to make use of this Coopr data need to be familiar
with the JSON layout of the Coopr data. In brief, cluster-wide configuration defined in cluster templates and
service-level action data are deep-merged together, with service-level action data taking precedence. This data is
preserved at the top level, and also merged in under ``coopr/*``. For example::

    {
        // cluster config attributes defined in clustertemplates are preserved here at top-level
        // service-level action data string converted to json and deep-merged here at top-level
        "coopr": {
            "clusterId": "00000001",
            "cluster": {
                //cluster config here as well
                "nodes": {
                    // node data
                }
            }
            "services": [
              // list of coopr services on this node
            ]
        }
    }


Consider the following rules of thumb:
        * When using community cookbooks, attributes can be specified in Coopr templates exactly as the cookbook expects (at the top level). If separate services require the same recipe with different attributes, these attributes can be specified in the service ``json_attributes`` field
        * When writing cookbooks specifically utilizing Coopr metadata (cluster node data for example), recipes can access the metadata at ``node['coopr']['cluster']...``

Bootstrap
=========

Each Coopr Automator plugin is responsible for implementing a bootstrap method in which it performs any action it needs to be able to carry out further tasks. The Chef Solo Automator plugin performs the following actions for a bootstrap task:
        1. Bundle its local copy of the cookbooks/roles/data_bags directories into three tarballs, ``cookbooks.tar.gz``, ``roles.tar.gz``, ``data_bags.tar.gz``
                * This only happens if prior versions of the tarballs were not created in the past 10 minutes
        #. Logs into the remote box and installs chef in one of three ways in this order:
                a. via ``yum install`` to leverage any internal yum repositories preconfigured on the remote host
                b. via ``apt-get install`` to leverage any internal apt repositories preconfigured on the remote host
                c. via the Opscode Omnibus installer (``curl -L https://www.opscode.com/chef/install.sh | bash``). This requires internet access on the remote host
        #. Creates the remote coopr cache directory ``/var/cache/coopr``
        #. SCPs the local tarballs to the remote Coopr cache directory
        #. Extracts the tarballs on the remote host to the default chef directory ``/var/chef``

The most important things to note:
        * Upon adding any new cookbooks, roles, or data_bags to the local directories, the tarballs will be regenerated within 10 minutes and used by all running provisioners.
        * Internet access may be needed to install chef unless steps are taken to provide chef in alternate ways.  Most of the bundled community cookbooks require internet access as well.

Internet Access
===============

Coopr and the Chef Solo Automator plugin can still be used in an environment without Internet access, with some restrictions:
        1. Chef must be pre-installed on the target hosts' images, or installable via a pre-configured Yum or Apt repository
        2. The bundled community cookbooks cannot be used.  Note the provided `Helper Cookbooks`_ do not require internet access

Adding Your Own Cookbooks
=========================
**Cookbook requirements**

Since the Chef Solo Automator plugin is implemented using chef-solo, the following restrictions apply:

        * No Chef search capability
        * No persistent attributes

Cookbooks should be fully attribute-driven. At this time the Chef Solo Automator does not support the chef-solo "environment" primitive. 
Attributes normally specified in an environment can instead be populated in Coopr primitives such as cluster templates or service action data.

**Cookbooks as plugin resources**

The Chef Solo Automator utilizes the :doc:`Plugin Resources </guide/admin/plugin-resources>` capability of Coopr in order to manage cookbooks, data bags, and roles.  Each of these Chef primitives can be uploaded to the Coopr server individually as resources.  Refer to the :doc:`Plugin Resources Guide </guide/admin/plugin-resources>` for more details on plugin resource management.

Examples:

.. parsed-literal::

 /opt/coopr/provisioner/embedded/bin/ruby /opt/coopr/provisioner/bin/data-uploader.rb -u |http:|//localhost:55054 -t superadmin -U admin sync ./my/local/cookbooks/myapp automatortypes/chef-solo/cookbooks/myapp

 /opt/coopr/provisioner/embedded/bin/ruby /opt/coopr/provisioner/bin/data-uploader.rb -u |http:|//localhost:55054 -t superadmin -U admin sync ./my/local/data_bags/mydata_bag automatortypes/chef-solo/data_bags/my_data_bag

 /opt/coopr/provisioner/embedded/bin/ruby /opt/coopr/provisioner/bin/data-uploader.rb -u |http:|//localhost:55054 -t superadmin -U admin sync ./my/local/roles/my_role.rb automatortypes/chef-solo/cookbooks/my_role.rb

.. note:: If you are uploading an archive of a directory, the top level directory of your unpacked archive must be named exactly the same as your resource name. For example, if you are uploading an archive of a directory named 'mycookbook', you *must* name your resource 'mycookbook' in order for it to be used correctly. This issue will be fixed in the next release. 


**Invoking your Cookbook**

In order to actually invoke your cookbook or role as part of a cluster provision, you will need to define a Coopr service
definition with the following parameters:

        * Category: any action (install, configure, start, stop, etc)
        * Type: chef-solo
        * run_list: a run-list containing your cookbook's recipe(s) or roles. If your recipe depends on resources defined in other cookbooks which aren't declared dependencies in your cookbook's metadata, make sure to also add them to the run-list.
        * json_attributes: (optional), any additional custom attributes you want to specify, unique to this action

Then simply add your service to a cluster template.

Helper Cookbooks
================

Coopr ships with several helper cookbooks. These are provided as convenience and are completely optional. This section provides a brief overview of each.


**coopr_base**
--------------
This is a convenience cookbook which is intended to provide base functionality for all hosts provisioned by Coopr. The default ``base`` service definition runs
the ``coopr_base::default`` recipe, which may include additional helper cookbooks. It currently does the following:

        * run ``apt-get update`` (on Ubuntu hosts)
        * optionally configure the Yum ``epel`` repository (on Rhel hosts)
        * optionally include ``coopr_dns::default`` (discussed below)
        * optionally include ``coopr_firewall::default`` (discussed below)
        * optionally include ``coopr_hosts::default`` (discussed below)
        * optionally include ``coopr_packages::default`` (discussed below)
        * include ``ulimit::default`` to enable user-defined ulimits
        * optionally runs the ``users`` and ``sudo`` cookbooks to add any users if a ``users`` data_bag resource is present

To disable the configuring of the Yum ``epel`` repository on Rhel hosts, set the ``node['base']['use_epel']`` attribute to ``false``.

To disable the inclusion of the remaining helper cookbooks, set the following attributes. Note, though, that these cookbooks are designed to not take action unless
specific attributes are added to your Coopr templates::

    node['base']['no_dns'] = 'true'
    node['base']['no_firewall'] = 'true'
    node['base']['no_hosts'] = 'true'
    node['base']['no_packages'] = 'true'

.. rubric:: User Management

The ``coopr_base`` cookbook provides a simple method for user management, utilizing the Chef community `users <https://supermarket.chef.io/cookbooks/users>`_ and
`sudo <https://supermarket.chef.io/cookbooks/sudo>`_ cookbooks.  To take advantage of this, simply upload a data_bag resource named ``users`` to the Coopr
Server.  As with cookbooks, you can use the data-uploader utility to do this, for example:

.. parsed-literal::

 /opt/coopr/provisioner/embedded/bin/ruby /opt/coopr/provisioner/bin/data-uploader.rb -u |http:|//localhost:55054 -t superadmin -U admin sync ./my/local/data_bags/users automatortypes/chef-solo/data_bags/users

where ``./my/local/data_bags/users`` is the data_bag to be used by the ``users`` cookbook. It should be a directory containing data_bag_items (JSON files) of the form::

    {
        "id": "bob",
        "Full Name": "Bob User",
        "groups": [ "sysadmin" ],
        "shell": "\/bin\/bash",
        "ssh_keys": "ssh-rsa ..."
    }

If no ``users`` data_bag resource is present, no action is taken.  To explicitly disable this functionality, simply set the ``node['base']['no_users']`` attribute to ``true``

**coopr_dns**
-------------

This cookbook provides a hook to run any user-specific DNS cookbooks.  Currently, the DNSimple provider is included as a reference. To use, set the following
attributes in the included ``base`` service's ``install`` action JSON attributes field::

    {
        "coopr_dns": {
            "provider": "dnsimple",
            "subdomain_whitelist": [
                "subdomain1.example.com",
                "subdomain2.example.com"
            ]
        }
    }

The ``provider`` attribute is simply the name of a recipe that must be implemented.  The ``subdomain_whitelist`` is an optional safety mechanism to only allow
DNS records to be created for certain domains.  In this example, as part of the ``base`` service the ``coopr_dns::dnsimple`` recipe will be run, which includes
DNSimple's cookbook and its LWRPs to create "A" records for each cluster host.

**coopr_hosts**
---------------

This simple cookbook's only purpose is to populate ``/etc/hosts`` with the hostnames and IP addresses of the cluster.
It achieves this by accessing the ``coopr-populated`` attributes at ``node['coopr']['cluster']['nodes']`` to get a list of
all the nodes in the cluster. It then simply utilizes the community "hostsfile" cookbook's LWRP to write entries for
each node.

The example coopr service definition invoking this cookbook is called "base". It simply sets up a "configure" service
action of type "chef-solo" and run_list ``recipe[coopr_base::default]`` (which includes ``recipe[coopr_hosts::default]``).
Note that the community "hostsfile" cookbook is not needed in the run-list since it is declared in coopr_hosts's metadata.

By default, it will create an ``/etc/hosts`` entry for each node of the cluster, using its private ``bind_v4`` IP.  It is possible to modify this behavior
using the ``node['coopr_hosts']['address_types']`` attribute array. See :doc:`Macros </guide/admin/macros>` for more information on the available address types.

**coopr_service_manager**
-------------------------

This cookbook comes in handy as a simple way to isolate the starting and stopping of various services within your
cluster. It allows you to simply specify the name of a Chef service resource and an action within a Coopr service
definition. When run, it will simply lookup the Chef service resource of the given name, regardless of which cookbook
it is defined in, and run the given action. In the apache-httpd service definition example above, it is simply included
in the run-list to start or stop the apache2 service defined in the apache2 community cookbook.  Just set the following
attribute to "start" or "stop"::

    node['coopr']['node']['services']['apache2'] = "start"


**coopr_firewall**
------------------

This cookbook is a simple iptables firewall manager, with the added functionality of automatically whitelisting all
nodes in a cluster. To use, simply set any of the following attributes::

    node['coopr_firewall']['INPUT_policy']  = (string)
    node['coopr_firewall']['FORWARD_policy'] = (string)
    node['coopr_firewall']['OUTPUT_policy'] = (string)
    node['coopr_firewall']['notrack_ports'] = [ array ]
    node['coopr_firewall']['open_tcp_ports'] = [ array ]
    node['coopr_firewall']['open_udp_ports'] = [ array ]

If this recipe is included in the run-list and no attributes specified, the default behavior is to disable the firewall.

**coopr_packages**
------------------

This cookbook provides a convenient way to install, upgrade, or remove any number of Yum or Apt packages as part of your ``base`` service.  Simply populate
any of the following (default) attributes::

    node['coopr_packages']['debian']['install'] = []
    node['coopr_packages']['debian']['upgrade'] = []
    node['coopr_packages']['debian']['remove'] = []
    node['coopr_packages']['rhel']['install'] = []
    node['coopr_packages']['rhel']['upgrade'] = []
    node['coopr_packages']['rhel']['remove'] = ['yum-cron']

Best Practices
==============

* Coopr is designed to use attribute-driven cookbooks. All user-defined attributes are specified in Coopr primitives. Recipes that use Chef server capabilities like discovery and such do not operate well with Coopr.
* Separate the install, configuration, initialization, starting/stopping, and deletion logic of your cookbooks into granular recipes. This way Coopr services can often be defined with a 1:1 mapping to recipes. Remember that Coopr will need to install, configure, initialize, start, stop, and remove your services, each independently through a combination of run-list and attributes.
* Use wrapper cookbooks in order to customize community cookbooks to suit your needs.
* Remember to declare cookbook dependencies in metadata.
