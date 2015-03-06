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
   single: Docker Automator Plugin

=======================
Docker Automator Plugin
=======================

.. include:: /guide/admin/admin-links.rst

This section describes an automator that uses docker commands via Secure Shell (SSH).

Overview
========

The Docker Automator plugin, like all automator plugins, is responsible for performing the installation and operation
of services on remote hosts.  The Docker Automator plugin provides a simple way to install Docker images from Docker Hub
and manage containers of these images.

.. note:: The Docker Automator currently requires that Docker is installed, configured, and running on the target host(s), such as provided by CoreOS.


As an example, consider the following Coopr service definition:
::

    {
        "name": "cdap-standalone",
        "description": "CDAP SDK (Standalone) running in a Docker container (caskdata/cdap-standalone)",
        "provisioner": {
            "actions": {
                "install": {
                    "type": "docker",
                    "fields": {
                        "image_name": "caskdata/cdap-standalone",
                        "publish_ports": "9999,10000"
                    }
                },
                "start": {
                    "type": "docker",
                    "fields": {
                        "image_name": "caskdata/cdap-standalone",
                        "publish_ports": "9999,10000"
                    }
                },
                "stop": {
                    "type": "docker",
                    "fields": {
                        "image_name": "caskdata/cdap-standalone",
                        "publish_ports": "9999,10000"
                    }
                }
            }
        }
    }

This defines a service named "cdap-standalone" which has three defined actions, install, start, and stop.  When each
action is invoked for this service, the ``type`` field indicates to the provisioner to use the Docker Automator plugin
to manage the action.  The Docker Automator defines two custom fields: a required ``image_name`` and an optional
``publish_ports``.  The ``image_name`` field is the image or repository to pull from Docker Hub.  Note that the image
must be a public image, hosted on Docker Hub.  The ``publish_ports`` field is optional, and is a comma-separated list
of exposed container ports to publish on the Docker host.

In this example, when the install action is invoked for this service, the Docker Automator will connect to the remote
host(s), then effectively invoke the following command on them:
:: 

        docker pull caskdata/cdap-standalone

When the start action is invoked for this service, the Docker Automator will connect to the remote host(s), then effectively
invoke the following command on them:
::

        docker run -d -p9999:9999 -p10000:10000 caskdata/cdap-standalone

Note that this example is a real service included with Coopr, which can be used to launch a container with the Cask Data Application
Platform (CDAP) SDK services running!

How It Works
============

A more in-depth look at Docker Automator:
        1. During the "bootstrap" action:
                a. The plugin verifies that ``docker`` is available on the host(s)
        2. When a defined docker service action runs (install, start, stop):
                a. The Docker Automator invokes a command on the remote host which performs one of:
                        i. Pulling an image or repository from Docker Hub (install)
                        ii. Running a new container, or starting a stopped container (start)
                        iii. Stopping a container (stop)
                        iv. Removing an image from the host(s) (remove)


Bootstrap
=========

Each Coopr Automator plugin is responsible for implementing a bootstrap method in which it performs any actions it needs 
to be able to carry out further tasks. The Docker Automator plugin performs the following actions for a bootstrap task:

        Verify that the ``docker`` command is installed and in the PATH
