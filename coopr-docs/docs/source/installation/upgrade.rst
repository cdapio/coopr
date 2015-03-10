..
   Copyright © 2015 Cask Data, Inc.

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at
 
       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.

.. index::
   single: Coopr Upgrade

.. _upgrade-to-0.9.9:

==================
Coopr Upgrade
==================

.. highlight:: console

This guide describes how to upgrade Cask Coopr from 0.9.8 to the |release| Release.

Overview
========

This upgrade involves three key steps:
 * A database upgrade
 * Changing the provisioner server URI port
 * Running the upgrade script


Compatibility Notes
-------------------

Provisioner plugins must now extend from Coopr::Plugin::Automator or Coopr::Plugin::Provider. 
Any existing custom plugins need to be updated accordingly.


Upgrade Steps
=============

On the Coopr servers:

1. Stop all processes

  .. code-block:: bash

   $ sudo /etc/init.d/coopr-provisioner stop
   $ sudo /etc/init.d/coopr-ui stop
   $ sudo /etc/init.d/coopr-server stop

2. Back up your database

  * It is recommended to backup your database. Consult your database vendor's documentation for instructions.

3. Upgrade Database Schema (if using an external database)

  * You have to update the schema for any external database. However, we only provide example SQL files for MySQL.
    The changes need to be made to any external database. To update MySQL:

  .. code-block:: bash

   $ mysql -p coopr < /opt/coopr/server/sql/upgrade-tables-pre0.9.9-to-0.9.9.sql

4. Change the provisioner.server.uri port

  * Edit the ``provisioner.server.uri`` property in ``/etc/coopr/conf/provisioner-site.xml`` and change the port from 55054 to 55055 unless you have changed the port with server.tasks.uri, in which case you should match the port below to that one. Here is an example (changing your server to the correct host/FQDN):

  .. code-block:: bash

   <property>
     <name>provisioner.server.uri</name>
     <value>http://myserver.mydomain.com:55055</value>
     <description>URI of server to connect to</description>
   </property> 

5. Run the upgrade script

  .. code-block:: bash

   $ sudo /etc/init.d/coopr-server upgrade

6. Start Coopr Server, UI and Provisioner services

  .. code-block:: bash

   $ sudo /etc/init.d/coopr-server start
   $ sudo /etc/init.d/coopr-ui start
   $ sudo /etc/init.d/coopr-provisioner start

