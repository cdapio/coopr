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

.. _plugin-reference:


.. index::
   single: Provisioner Plugins

===================
Provisioner Plugins
===================

.. include:: /guide/admin/admin-links.rst

The Loom provisioner allows you to create custom plugins for allocating machines on your providers or to custom
implement your services. This document provides the necessary information to build a custom plugin for Continuuity Loom.

Types of Plugins
================

Provider plugins
----------------
Provider plugins interact with various provider APIs to allocate machines. As such, they must be able to request machines,
confirm/validate when they are ready, and delete machines. Provider plugins are also responsible for returning the ssh credentials
to be used in subsequent tasks.

Automator plugins
-----------------
Automator plugins are responsible for implementing the various services defined on a cluster. For example, a Chef
automator plugin could be used to invoke Chef recipes that install, configure, start or stop your application. Alternatively,
you may choose to implement with a Puppet plugin, or even Shell commands.

Task Types
==========
In order to build plugins for Continuuity Loom, it is first necessary to understand the tasks each plugin will be responsible for
executing. To bring up a cluster, Continuuity Loom issues the following tasks:

.. list-table::
   :header-rows: 1

   * - Task name
     - Description
     - Possible return values
     - Handled by plugin
   * - CREATE
     - sends a request to the provider to initiate the provisioning of a machine. Typically, it should return as soon as the request is made
     - status, provider-id, root password
     - Provider
   * - CONFIRM
     - polls/waits for the machine to be ready and does any required provider-specific validation/preparation
     - status, routable ip address
     - Provider
   * - DELETE
     - sends a request to the provider to destroy a machine
     - status
     - Provider
   * - BOOTSTRAP
     - plugin can perform any operation it needs to carry out further tasks, for example, copy Chef cookbooks to the machine. This operation should be idempotent and safe to run together with multiple plugins
     - status
     - Automator
   * - INSTALL
     - run the specified install service action
     - status
     - Automator
   * - CONFIGURE
     - run the specified configure service action
     - status
     - Automator
   * - INITIALIZE
     - run the specified initialize service action
     - status
     - Automator
   * - START
     - run the specified start service action
     - status
     - Automator
   * - STOP
     - run the specified stop service action
     - status
     - Automator
   * - REMOVE
     - run the specified remove service action
     - status
     - Automator

Note that status is the only required return value, since it indicates success or failure. For information such
as the IP address, any task can write an arbitrary key-value pair which will then be included in subsequent
requests. This allows for different providers to have different values.

Writing a Plugin
================

Currently, a plugin must be written in Ruby and extend from the Continuuity Loom base plugin classes.

Writing a Provider plugin
-------------------------

A provider plugin must extend from the base ``Provider`` class and implement three methods: ``create``, ``confirm``, and ``delete``.  Each of these methods are called with a hash of key-value pairs. This hash is pre-populated with useful attributes such as ``hostname`` or ``providerid``.  The ``fields`` attribute will always be present and contains a hash of the custom fields that are defined by each plugin (more on this below).  Note that your implementation can also refer to the ``@task`` instance variable, which contains the entire
input for this task.

Below is a skeleton for a provider plugin:
::

  #!/usr/bin/env ruby

  class MyProvider < Provider

    def create(inputmap)
      flavor = inputmap['flavor']
      image = inputmap['image']
      hostname = inputmap['hostname']
      fields = inputmap['fields']
      #
      # implement requesting a machine from provider
      #
      @result['status'] = 0
      @result['result']['foo'] = "bar"
    end

    def confirm(inputmap)
      providerid = inputmap['providerid']
      fields = inputmap['fields']
      #
      # implement confirmation/validation of this machine from provider
      #
      @result['status'] = 0
    end

    def delete(inputmap)
      providerid = inputmap['providerid']
      fields = inputmap['fields']
      #
      # implement deletion of machine from provider
      #
      @result['status'] = 0
    end

When the task is complete, your implementation should simply write the results back to the ``@result`` instance variable.
The only required return value is ``status``, where ``'status': 0`` represents success and any other value returned
represents failure. A raised exception will also result in failure.

Additionally, your provider plugin will likely need to return information such as a machine's ID, SSH credentials and
public IP, so that it can be used in subsequent tasks. For these cases, simply write the results as key-value pairs underneath
the line ``@result['result']['key'] = 'value'``. Subsequent tasks will then contain this information in ``config``, for
example ``@task['config']['key'] = 'value'``. By convention, most plugins should reuse the following fields:
::

  @result['result']['providerid']
  @result['result']['ssh-auth']['user']
  @result['result']['ssh-auth']['password']
  @result['result']['ipaddress']

Writing an Automator plugin
---------------------------

An automator plugin must extend from the base ``Automator`` class and implement seven methods: ``bootstrap``, ``install``, ``configure``, ``init``, ``start``, ``stop``, and ``remove``. Each of these methods are called with a hash of key-value pairs.  This hash is pre-populated with useful attributes such as ``hostname``, ``ipaddress``, and a hash of ssh credentials.  Additionally, the ``fields`` attribute will contain a hash of the custom fields that are defined by each plugin (more on this below).  Note that your implementation can also refer to the ``@task`` instance variable, which contains the entire input for this task.

Below is a skeleton for an automator plugin:
::

  #!/usr/bin/env ruby

  class MyAutomator < Automator

    def bootstrap(inputmap)
      ssh_auth_hash = inputmap['sshauth']
      hostname = inputmap['hostname']
      ipaddress = inputmap['ipaddress']
      #
      # implement any preparation work required by this plugin (copy cookbooks, etc.)
      # this should be idempotent and unintrusive to any other registered plugins
      @result['status'] = 0
    end

    def install(inputmap)
      ssh_auth_hash = inputmap['sshauth']
      hostname = inputmap['hostname']
      ipaddress = inputmap['ipaddress']
      fields = inputmap['fields']
      #
      # implement installing a service as specified by the custom fields in inputmap['fields']
      #
      @result['status'] = 0
    end

    def configure(inputmap)
      ssh_auth_hash = inputmap['sshauth']
      hostname = inputmap['hostname']
      ipaddress = inputmap['ipaddress']
      fields = inputmap['fields']
      #
      # implement configuring a service as specified by the custom fields in inputmap['fields']
      #
      @result['status'] = 0
    end

    def init(inputmap)
      ssh_auth_hash = inputmap['sshauth']
      hostname = inputmap['hostname']
      ipaddress = inputmap['ipaddress']
      fields = inputmap['fields']
      #
      # implement initializing a service as specified by the custom fields in inputmap['fields']
      #
      @result['status'] = 0
    end

    def start(inputmap)
      ssh_auth_hash = inputmap['sshauth']
      hostname = inputmap['hostname']
      ipaddress = inputmap['ipaddress']
      fields = inputmap['fields']
      #
      # implement starting a service as specified by the custom fields in inputmap['fields']
      #
      @result['status'] = 0
    end

    def stop(inputmap)
      ssh_auth_hash = inputmap['sshauth']
      hostname = inputmap['hostname']
      ipaddress = inputmap['ipaddress']
      fields = inputmap['fields']
      #
      # implement stopping a service as specified by the custom fields in inputmap['fields']
      #
      @result['status'] = 0
    end

    def remove(inputmap)
      ssh_auth_hash = inputmap['sshauth']
      hostname = inputmap['hostname']
      ipaddress = inputmap['ipaddress']
      fields = inputmap['fields']
      #
      # implement removing a service as specified by the custom fields in inputmap['fields']
      #
      @result['status'] = 0
    end

Note that the bootstrap step is unique in that it is not tied to a service. Since services on the same cluster may be implemented with different automator plugins, the bootstrap task will run the bootstrap implementations for all automator plugins used by any service on the cluster.  The bootstrap task may also be run multiple times throughout the cluster lifecycle.  Therefore, bootstrap implementations should be idempotent and not interfere with one another.



Logging and Capturing Output
----------------------------

During execution, a plugin can write to the provisioner's instance of the Ruby standard logger using the 'log' method:
::

  log.debug "my message"
  log.info "my message"
  log.warn "my warning message"
  log.error "my error message"

Additionally, each task can return strings representing ``stdout`` and ``stderr`` to be displayed on the Loom UI. Simply return the values:
::

  @result['stdout'] = "my captured stdout message"
  @result['stderr'] = "my captured stderr message"


Defining Your Plugin
--------------------

Loom plugins are required to provide a JSON file which defines the plugin, including the following:
  * plugin name
  * named providertypes and automatortypes
  * the class name for each providertype and automatortype
  * any custom fields required by each providertype and automatortype

Custom fields allow a plugin to announce the fields that it requires.  For example a Rackspace provider plugin may require a username and password while the Joyent provider plugin can require either a password or a key.  Likewise, the Chef Automator plugin requires a run-list and JSON attributes, while the Shell Automator plugin requires a command and arguments.

For example, consider the JSON definition file for the Rackspace provider plugin:
::

    {
        "name": "rackspace",
        "description": "Rackspace Public Cloud provider",
        "providertypes": [
            "rackspace"
        ],
        "rackspace": {
            "name": "rackspace",
            "classname": "RackspaceProvider",
            "parameters": {
                "admin": {
                    "fields": {
                        "rackspace_username": {
                            "label": "User",
                            "type": "text",
                            "tip": "Your Rackspace user name"
                        },
                        "rackspace_api_key": {
                            "label": "APIkey",
                            "type": "password",
                            "tip": "Your Rackspace API key"
                        },
                        "rackspace_region": {
                            "label": "Region",
                            "type": "select",
                            "options": [
                                "dfw",
                                "ord",
                                "lon",
                                "syd"
                            ],
                            "default": "dfw",
                            "override": true,
                            "tip": "Rackspace region (DFW default)"
                        }
                    },
                    "required": [
                        [
                            "rackspace_username",
                            "rackspace_api_key",
                            "rackspace_region"
                        ]
                    ]
                }
            }
        }
    }

This JSON defines a single plugin which contains a single providertype named "rackspace".  The implementing ruby class for this "rackspace" providertype is "RackspaceProvider".  Additionally, it defines three custom fields which can be set by a Loom administrator: "rackspace_username", "rackspace_api_key", and "rackspace_region".  In this example, they are defined within an "admin" block, which indicates these fields can be set only by a Loom administrator.  Alternatively, defining fields within a "user" block indicates a Loom user can set them when creating a cluster.  Each field has additional elements to specify behavior and describe how the field should be presented to the user on the Loom UI:
  * field name: will be used as the key in the hash sent to the provisioner plugin.
  * ``label``: the user-friendly label presented in the Loom UI.
  * ``type``: any of the HTTP input field types such as ``text`` or ``password``, or "select" for a drop-down menu.
  * ``tip``: the text hint displayed as the value before the user enters anything
  * ``options``: (specific to type "select"), the drop-down menu options
  * ``default``: (specific to type "select"), the default option.
  * ``override``: specifies whether or not a field defined in an admin block can be overridden by the user during cluster creation.  default: false

Note that a single plugin may contain multiple providertypes and automatortypes.  The top-level ``providertypes`` and ``automatortypes`` arrays should list them all, then each will have a corresponding JSON element where the classname and custom fields are defined.


Loading Your Plugin
-------------------

When the provisioner starts up, it will scan its directories looking for JSON definition files.  In order for your plugin to be loaded, it needs to adhere to the following directory structure.

Below is an example directory structure:
::

  $LOOM_HOME/
      provisioner/
          daemon/
              plugins/
                  providers/
                      my_provider/
                          my_provider.json
                          my_provider.rb
                          [any additional data or lib directories]
                  automators/
                      my-automator/
                          my_provider.json
                          my_provider.rb
                          [any additional data or lib directories]


When writing a custom plugin, please consider the following:
  * Plugin loading errors are not fatal.  The provisioner will load the remaining plugins and will poll for tasks as long as it has at least one provider and one automator loaded.  Please check the provisioner logs for any reported errors loading plugins.
  * Currently the same set of plugins must be installed on all running provisioners, since any given provisioner can receive any task.


Registering Your Plugin
-----------------------

The Loom Server needs to be aware of the installed provisioner plugins and their collective list of providertypes and automatortypes.  This can currently be done by starting a provisioner with the ``--register`` argument.  For example:
::

  $LOOM_HOME/provisioner/daemon/provisioner.rb --register --uri http://myloomserver:55054

The above command will start a provisioner which will load its plugins as usual, then register all providertypes and automatortypes using the Loom Server's API.  After registering each providertype or automatortype, it will exit.

When running the Loom standalone distribution, this command is run automatically during initial startup.


Included Plugins
================

.. toctree::
   :maxdepth: 1

   chef-automator-plugin
   shell-automator-plugin

