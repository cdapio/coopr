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
   single: Shell Automator Plugin
========================
Shell Automator Plugin
========================

.. include:: /guide/admin/admin-links.rst

This section describes an automator that uses shell commands and custom scripts.

Overview
========

The Shell Automator plugin, like all automator plugins, is responsible for performing the installation and operation
of services on remote hosts.  The Shell Automator plugin provides a simple way to execute shell commands and custom
shell scripts on remote hosts.  Scripts are maintained and stored locally by the provisioner, transferred to the 
remote host, and run remotely.  Loom also provides some additional data and utilities for extracting cluster metadata.

As an example, consider the following Loom service definition:
::
    {
        "description": "Bootstrap a chef-client to My Chef server",
        "name": "continuuity-chef-bootstrap",
        "provisioner": {
            "actions": {
                "install": {
                    "type": "shell",
                    "fields": {
                        "script": "chef_client_bootstrap.sh",
                        "args": "https://mychefserver:443 \"role[base]\""
                    }
                }
            }
        }
    }

This defines a service named "continuuity-chef-bootstrap" which has one defined action for "install".  When the 
install action is invoked for this service, the ``type`` field indicates to the provisioner to use the Shell Automator
plugin to manage this action.  The Shell Automator defines two custom fields: a required ``script`` and optional
``args``.  The ``script`` field can be a standard shell command, or a custom script.  Note that ``script`` can either
be a full path to a known executable, or a relative path to a script bundled with the Shell Automator plugin (more on
this below).  The ``args`` field is optional.  In this example, when the install action is invoked for this 
service, the Shell automator will first ensure this script is transferred to the remote host(s), then effectively 
invoke the following command on them:
::
	chef_client_bootstrap.sh https://mychefserver:443 "role[base]"

Note that this example is a real script included with the Shell Automator, which can be used to bootstrap a Loom host to an existing Chef server!

How it Works
============

A more in-depth look at Shell Automator:
	1. The source of truth for any custom scripts is the local plugin directory on the provisioner host.  For example using the default packages: ``/opt/loom/provisioner/daemon/plugins/automators/shell_automator/scripts``.
	2. During the "bootstrap" action:
		a. These scripts are bundled and scp'd to the remote host
		b. The scripts are extracted on the remote host, by default to ``/var/cache/loom/scripts``
	3. When a defined shell service action runs (ie, install, configure, start, etc):
		a. The Shell Automator first generates a copy of the current task's JSON data
		b. This task json data is scp'd to the remote host, where it can be referenced by the executed script
		c. The Shell Automator invokes a command on the remote host which
			i. Sets the current working directory to the scripts directory
			ii. Adds the scripts directory to the $PATH
			iii. Invokes a "hidden" wrapper script, which runs the defined script with the defined args.  The purpose of the wrapper script is to provide some useful utilities (see JSON lookup below)


Bootstrap
=========

Each Loom Automator plugin is responsible for implementing a bootstrap method in which it performs any actions it needs 
to be able to carry out further tasks. The Shell Automator plugin performs the following actions for a bootstrap task:
        1. Bundle its local copy of the scripts directory into a tarball, ``scripts.tar.gz``, unless the tarball exists already and was created in the last 10 minutes.
        2. Logs into the remote box and creates the Loom cache diretory ``/var/cache/loom``.
        3. SCP the local tarball to the remote Loom cache directory.
        4. Extracts the tarball on the remote host in the loom cache directory, creating ``/var/cache/loom/scripts``.

The most important thing to note is that upon adding any new script to the local directory, the tarball will be 
regenerated within 10 minutes and used by all running provisioners.


JSON Lookup
===========

The Shell Automator has limited support for looking up values in the Loom cluster metadata.  This should be considered
an advanced feature and used with caution!  The current implementation uses json.sh: https://github.com/rcrowley/json.sh.

For every invocation of a user's defined script, the Shell Automator actually invokes a wrapper script which then 
sources the user's script.  The wrapper script provides the ``loom_lookup_key`` function.  This function takes a 
'/'-delimited key as an argument, for example ``config/automators``.  It will look up the corresponding key in the 
task json, and return the value.  If the value is an array, it will return the values space-delimited.  Example usage:
::
  task_id=`loom_lookup_key taskId 2>&1`
  echo "json root key \"taskId\" has value: $task_id"
  automators=`loom_lookup_key config/automators 2>&1`
  echo "automators in use for this cluster:"
  for i in $automators ; do echo $i ; done 


The wrapper script and implementation of loom_lookup_key is in the ``scripts/.lib`` directory.  Those interested in
this functionality should familiarize themselves with the code and be aware of the potential pitfalls (duplicate 
key names, etc).


Adding your own Scripts
=======================

	1. add your script to the ``$LOOM_HOME/provisioner/daemon/plugins/automators/shell_automator/scripts`` directory.
	2. add/edit a service definition with an action of type "shell"
	3. specify the command to run, optional args, and during which stage it should run.


Best Practices
==============

	* Shell Automator can be very handy for edge cases where the standard automator plugins and configuration management falls short.  As illustration, with the included chef_client_bootstrap.sh utility, it is possible to provision a cluster using one set of cookbooks, and then hand it off to a separate chef-server.
	* Shell Automator has the capability to run user-defined commands as root.  Administrators should take precautions.
	* Understand the risks and pitfalls of the JSON lookup before depending on it.

