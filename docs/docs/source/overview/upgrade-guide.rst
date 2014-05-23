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

.. _overview_upgrade-guide:

.. index::
   single: Upgrade Guide

=============
Upgrade Guide
=============
.. _upgrade-guide:

This guide describes how to upgrade Continuuity Loom from 0.9.6 to the |release| Release.

Checklist
=========

* 1 - Login to the Loom UI as admin and click "Export" to export a JSON file containing:

  * Hardware Types
  * Image Types
  * Providers
  * Services
  * Cluster Templates

* 2 - Stop Loom services

  .. parsed-literal::
   $ sudo /etc/init.d/loom-provisioner stop
   $ sudo /etc/init.d/loom-ui stop
   $ sudo /etc/init.d/loom-server stop

* 3 - Stop Database and take a backup

  * The embedded Derby database and ZooKeeper server are stopped when Loom Server is stopped
  * If you are using another database, such as MySQL, follow your standard procedures for backing up the Loom database
  * If you are using the embedded Derby database or ZooKeeper, perform the following
  * Replace ``/var/loom/data`` below with the location from ``loom.local.data.dir`` in ``/etc/loom/conf/loom-site.xml`` if you have modified it from default

  .. parsed-literal::
   $ sudo cp -R /var/loom/data /var/loom/data.backup

* 4 - Verify configuration path

  * Double check that ``/etc/loom/conf.loom`` is the active configuration

  .. parsed-literal::
   $ update-alternatives --display loom-conf

  * If not, run the following to update it

  .. parsed-literal::
   $ update-alternatives --install /etc/loom/conf loom-conf /etc/loom/conf.loom 20

* 5 - Upgrade Loom packages

  * If upgrading from file, follow the instructions at :doc:`Installing from File </guide/installation/index>`
  * If upgrading from repository, follow the instructions at :doc:`Installing from Repository </guide/installation/index>` and replace the ``install`` on each command with ``upgrade``

* 6 - Start Database and import schema (Optional)

  * If you are using MySQL, start the service and update the schema

  .. parsed-literal::
   $ mysql -u loom -p loom < /opt/loom/server/config/sql/loom-create-tables-mysql.sql

  * If you are using another external database, you will need to perform the table changes, manually

* 7 - Update JSON with new layout

  * Continuuity Loom |release| modified the JSON layout for Automator and Provider plugins, this fixes them

  .. parsed-literal::
   $ /opt/loom/server/bin/loom-0.9.7-upgrade.rb -f export.json -o upload.json

* 8 - Start Loom Server

  .. parsed-literal::
   $ sudo /etc/init.d/loom-server start

* 9 - Register Loom Provisioner plugins

  * The Loom Provisioner plugins registration informs the Loom Server how to interact with the plugin

  .. parsed-literal::
   $ sudo /etc/init.d/loom-provisioner register

* 10 - Start Loom UI

  .. parsed-literal::
   $ sudo /etc/init.d/loom-ui start

* 11 - Import new JSON

  * Login to the Loom UI as admin and click "Import" and select ``upload.json`` from your local machine

* 12 - Start Loom Provisioners

  .. parsed-literal::
   $ sudo /etc/init.d/loom-provisioner start
