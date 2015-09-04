..
   Copyright © 2012-2014 Cask Data, Inc.

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
   single: Coopr Installation

.. highlight:: console

==================
Coopr Installation
==================

.. highlight:: console

Overview
========

This document will guide you through the process of installing Coopr
on your own cluster with the official installation image.

System Requirements
===================

.. _minimum-hardware:

Hardware Requirements
---------------------
Systems hosting the Coopr components must meet these hardware specifications, in addition to having CPUs
with a minimum speed of 2 GHz:

.. list-table::
   :widths: 25 25 50
   :header-rows: 1

   * - Component
     - Hardware Component
     - Specifications
   * - Coopr UI
     - RAM
     - 512MB minimum, 1 GB recommended
   * - Coopr Server 
     - RAM
     - 1 GB minimum, 3 GB recommended
   * - Coopr Provisioners
     - RAM
     - 256 MB per provisioner worker; for example, with 10 workers, 2.5 GB minimum
   * - Database (ex: MySQL)
     - RAM
     - 1 GB minimum, 2 GB recommended
   * - 
     - Disk
     - Disk usage increases as Coopr usage increases; suggested minimum of 50GB

.. _system-requirements:

Supported Operating Systems
---------------------------

Coopr has been tested against these platforms:

 * CentOS 6.4
 * Ubuntu 12.04

Supported Databases
-------------------

Coopr supports any database with a jdbc driver that supports standard SQL queries. It has been tested with these databases:

 * (Default) Derby
 * MySQL version 5.1 or above

Supported Zookeeper Versions
----------------------------

Coopr has been tested with these versions of Zookeeper:

 * Apache Zookeeper version 3.4 or above
 * CDH4 or CDH5 Zookeeper
 * HDP1 or HDP2 Zookeeper

Supported OpenStack Versions
----------------------------
Coopr has been extensively tested on Havana, but it also supports Grizzly out of the box.

.. note:: Click here for more information on how :doc:`Openstack should be configured <openstack-config>` currently to support provisioning with Coopr. Several limitations that exist will be eliminated in future releases of Coopr.

Supported Internet Protocols
----------------------------
Coopr requires IPv4. IPv6 is currently not supported.

Supported Browsers
------------------
 * Mozilla Firefox version 26 or above
 * Google Chrome version 31 or above
 * Safari version 5.1 or above

Supported Node.js Versions
----------------------------
Coopr supports Node.js version 0.10.26 or above.

.. _prerequisites:

Software Prerequisites
======================

Coopr requires Java™. JDK or JRE version 6 or 7 must be installed in your environment. Coopr is certified with Oracle JDK 6.0_31, Oracle JDK 7.0_51 and OpenJDK 6b27-1.12.6.

Linux
-----
`Click here <http://www.java.com/en/download/manual.jsp>`_ to download the Java Runtime for Linux and Solaris. Following installation, please set the ``JAVA_HOME`` environment variable.

Mac OS
------
On Mac OS X, the JVM is bundled with the operating system. Following installation, please set the ``JAVA_HOME`` environment variable.

.. _installation-file:

Installing from File
====================

.. note:: Installation of Coopr packages creates a user with the username 'coopr'. If the user 'coopr' already exists on the system, then that user account will be used to run all Coopr services. The username can also be externally created using LDAP.

Yum
---
To install each of the Coopr components locally from a Yum package:

.. container:: highlight

  .. parsed-literal::
   
    |$| sudo yum localinstall coopr-server-|version|.el6.x86_64.rpm
    |$| sudo yum localinstall coopr-provisioner-|version|.el6.x86_64.rpm
    |$| sudo yum localinstall coopr-ui-|version|.el6.x86_64.rpm

Debian
------
To install each of the Coopr components locally from a Debian package:

.. container:: highlight

  .. parsed-literal::

    |$| sudo dpkg -i coopr-server\_\ |version|.ubuntu.12.04_amd64.deb
    |$| sudo dpkg -i coopr-provisioner\_\ |version|.ubuntu.12.04_amd64.deb
    |$| sudo dpkg -i coopr-ui\_\ |version|.ubuntu.12.04_amd64.deb

.. _installation-repository:

Installing from Repository
==========================

RPM using Yum
-------------
Download the Cask Yum repo definition file::

  $ sudo curl -o /etc/yum.repos.d/coopr.repo http://repository.cask.co/centos/6/x86_64/coopr/0.9/coopr.repo

This will create the file ``/etc/yum.repos.d/coopr.repo`` with::

  [coopr]
  name=Coopr Packages
  baseurl=http://repository.cask.co/centos/6/x86_64/coopr/0.9
  enabled=1
  gpgcheck=1


Add the Cask Public GPG Key to your repository::

  $ sudo rpm --import http://repository.cask.co/centos/6/x86_64/coopr/0.9/pubkey.gpg

Instructions for installing each of the Coopr components are as below::

  $ sudo yum install coopr-server
  $ sudo yum install coopr-provisioner
  $ sudo yum install coopr-ui

Debian using APT
----------------
Download the Cask Apt repo definition file::

  $ sudo curl -o /etc/apt/sources.list.d/coopr.list http://repository.cask.co/ubuntu/precise/amd64/coopr/0.9/coopr.list

This will create the file ``/etc/apt/sources.list.d/coopr.list`` with::

  deb [ arch=amd64 ] http://repository.cask.co/ubuntu/precise/amd64/coopr/0.9 precise coopr


Add the Cask Public GPG Key to your repository::

  $ curl -s http://repository.cask.co/ubuntu/precise/amd64/coopr/0.9/pubkey.gpg | sudo apt-key add -

Instructions for installing each of the Coopr components are as below::

  $ sudo apt-get update
  $ sudo apt-get install coopr-server
  $ sudo apt-get install coopr-provisioner
  $ sudo apt-get install coopr-ui

Update-Alternatives
-------------------
Coopr packages by default use the ``alternatives`` system to initialize a configuration
directory which will not be overwritten on subsequent package upgrades.  This directory is
``/etc/coopr/conf.coopr/`` and is pointed to by the symlink ``/etc/coopr/conf/``.  The
``/etc/coopr/conf.dist/`` directory is owned by the Coopr packages and should not be
customized.  To doublecheck that ``/etc/coopr/conf.coopr/`` is the active configuration,
simply run::

  $  update-alternatives --display coopr-conf

Database Configuration
----------------------
By default, Coopr uses an embedded Derby database. However, you can optionally choose to enable remote database for Coopr Server.
Additional steps are required to configure this setting.

Sample MySQL setup
^^^^^^^^^^^^^^^^^^
**Download and add the database connector JAR**

Execute the following command on the Coopr Server machine:

For RHEL/CentOS/Oracle Linux::

  $ sudo yum install mysql-connector-java*

For Ubuntu::

  $ sudo apt-get install libmysql-java*

After the install, the MySQL JAR is placed in ``/usr/share/java/``. Copy the installed JAR files to the
``/opt/coopr/server/lib/`` directory on your Coopr Server machine. Verify that the JAR file has appropriate permissions.

.. note::
  * After installing the MySQL connector, the Java version may change.  Make sure you are using Java 1.6 or 1.7 from Oracle.  You may need to run ``update-alternatives --config java`` to do this.
  * The minimum required version of MySQL connector is 5.1.6.
  * You can also download MySQL JDBC driver JAR (mysql-connector-java) from `MySQL website <http://dev.mysql.com/downloads/connector/j>`_.

**Setup database**

You will need to set up an account and a database in MySQL. An example schema file (for MySQL) for this can be found at
``/opt/coopr/server/sql``.

.. highlight:: console

If you are setting up a MySQL database from scratch you can run the following on your
mysql machine to complete the database setup::

  $ mysql -u root -p -e 'create database coopr;'
  $ mysql -u root -p -e 'grant all privileges on coopr.* to "coopr"@"<coopr-server>" identified by "<password>";'
  $ mysql -u coopr -p coopr < /opt/coopr/server/sql/create-tables-mysql.sql
  $ mysql -u coopr -p coopr -e 'show tables;'
 +--------------------+
 | Tables_in_coopr    |
 +--------------------+
 | automatorTypes     |
 | clusterTemplates   |
 | clusters           |
 | hardwareTypes      |
 | imageTypes         |
 | jobs               |
 | nodes              |
 | pluginMeta         |
 | providerTypes      |
 | providers          |
 | provisionerWorkers |
 | provisioners       |
 | services           |
 | tasks              |
 | tenants            |
 | users              |
 +--------------------+

where passwords are replaced and entered as needed.

Configuration
=============

Server
------

Site Config
^^^^^^^^^^^
Coopr Server settings can be changed under the ``/etc/coopr/conf/coopr-site.xml`` configuration file.
You will likely want to add configuration settings for zookeeper, your database, and server host. There is also
an example configuration file you can examine at ``/etc/coopr/conf/coopr-site.xml.example``.
For a list of available configurations, see the :doc:`Server Configuration </installation/server-config>` page. 

Environment
^^^^^^^^^^^
The Server environmental variables can be set at ``/etc/default/coopr-server``. The configurable variables are as below:

.. list-table::
   :header-rows: 1

   * - Variable
     - Default
     - Description
   * - ``COOPR_LOG_DIR``
     - /var/log/coopr
     - Path for the log directory
   * - ``COOPR_JMX_OPTS``
     -
     - JMX options for monitoring the Coopr Server
   * - ``COOPR_GC_OPTS``
     -
     - java garbage collection options to use when running the Coopr Server
   * - ``COOPR_JAVA_OPTS``
     - -XX:+UseConcMarkSweepGC -XX:+UseParNewGC
     - java options to use when running the Coopr Server

Provisioner
-----------

Site Config
^^^^^^^^^^^
Coopr Provisioner settings can be changed under the ``/etc/coopr/conf/provisioner-site.xml`` configuration file.
You will likely want to add configuration settings for the server uri the provisioner should connect to.
For a list of available configurations, see the :doc:`Provisioner Configuration </installation/provisioner-config>` page.

Environment
^^^^^^^^^^^
The Provisioner environmental variables can be set at ``/etc/default/coopr-provisioner``. The configurable variables are as below:

.. list-table::
   :header-rows: 1

   * - Variable
     - Default
     - Description
   * - ``COOPR_LOG_DIR``
     - /var/log/coopr
     - Path for the log directory
   * - ``COOPR_LOG_LEVEL``
     - info
     - Logging level
   * - ``PROVISIONER_SITE_CONF``
     - /etc/coopr/conf/provisioner-site.xml
     - Location of site config

UI
--

Environment
^^^^^^^^^^^
The UI environment variables can be set at ``/etc/default/coopr-ui``. The configurable variables are as below:

.. list-table::
   :header-rows: 1

   * - Variable
     - Default
     - Description
   * - ``COOPR_LOG_DIR``
     - /var/log/coopr
     - Path for the log directory
   * - ``COOPR_SERVER_URI``
     - http://localhost:55054
     - The URI for Coopr Server
   * - ``COOPR_UI_PORT``
     - 8100
     - The port number that hosts the UI

.. _starting_stopping:

Starting and Stopping Coopr Services
====================================
By default, Coopr's installation RPMs and PKGs do not configure auto start of the services in the ``init.d``. We leave
that privilege to the administrator. For each Coopr component and its related service (such as the Server, Provisioner, and UI),
there is a launch script, which you may use to execute a desired operation. For example, to start, stop, or check status
for a Coopr Provisioner, you can use::

  $ sudo /etc/init.d/coopr-server start|stop
  $ sudo /etc/init.d/coopr-provisioner start|stop|status
  $ sudo /etc/init.d/coopr-ui start|stop

.. _loading_defaults:

Initial Setup
=============
The very first time you install Coopr, you will need to perform some data initialization. The first thing you must do is
register the provisioner plugins, and the plugin resources included with Coopr. If you have not configured the server
port, it defaults to 55054::

 $ sudo COOPR_SERVER_URI=http://<coopr-server>:<coopr-port> /opt/coopr/provisioner/bin/setup.sh

Coopr provides a set of useful default templates that covers most supported use cases. For new users and administrators of Coopr, we
recommend installing these defaults as a starting point for template definition. These defaults are required for running
the example in the :doc:`Quick Start Guide </guide/quickstart/index>`, and are included in the server package. To load these templates, run::

 $ sudo COOPR_SERVER_URI=http://<coopr-server>:<coopr-port> /opt/coopr/server/config/defaults/load-defaults.sh

.. _logs:

Log Management
==============

Location
--------
By default, Coopr logs are located at ``/var/log/coopr``.  This can be changed by editing the corresponding ``/etc/default/coopr-server``,
``/etc/default/coopr-ui``, or ``/etc/default/coopr-provisioner`` file.

Options
-------
Log options for the server, such as log level, can be changed by editing the ``/etc/coopr/conf/logback.xml`` file.  Log level for
the provisioner can be changed by editing the ``/etc/default/coopr-provisioner`` file.

Rotation
--------
Coopr depends on the external Linux utility logrotate to rotate its logs. Coopr
packages contain logrotate configurations in ``/etc/logrotate.d`` but it does not perform the rotations itself.
Please ensure logrotate is enabled on your Coopr hosts.


Upgrading Coopr
===============

To upgrade Coopr from 0.9.8 to 0.9.9, please refer to the :ref:`Upgrade Guide <upgrade-to-0.9.9>`.


.. _common-issues:

Common Installation Issues
==========================

* A common issue is installing Coopr on machines that have Open JDK installed rather than Oracle JDK.

* If you see JDBC exceptions in the Coopr Server log such as::

    Caused by: java.lang.AbstractMethodError: com.mysql.jdbc.PreparedStatement.setBlob(ILjava/io/InputStream;)

  it means your JDBC connector version is too old.  Upgrade to a newer version to solve the problem.

* If you are running your mysql server on the same machine as the Coopr Server and are
  seeing connection issues in the Coopr Server logs, you may need to explicitly grant access
  to ``"coopr"@"localhost"`` instead of relying on the wildcard. 
