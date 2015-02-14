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

.. index::
   single: Provisioner Configuration

=========================
Provisioner Configuration
=========================

The Provisioner is a running process that manages workers and plugin resources. Workers are separate
processes that perform individual tasks for a specific tenant. Plugin resources are files or archives
that may be used to execute tasks. Chef cookbooks are examples of plugin resources. Each provisioner
has a capacity, which is the maximum number of workers it can manage. When configuring capacity for
your provisioner, assume each worker takes 256mb of memory. CPU usage is negligible. The provisioner
periodically sends a heartbeat to the server to tell the system it is alive. Live provisioners may
get requests from the server to add or remove workers for different tenants.

.. highlight:: xml

Example
^^^^^^^^
Shown below is an example configuration (``provisioner-site.xml``) with some of the optional settings:: 

  <configuration>
    <property>
      <name>provisioner.capacity</name>
      <value>10</value>
      <description>Max number of running workers for this provisioner</description>
    </property>
    <property>
      <name>provisioner.server.uri</name>
      <value>http://localhost:55055</value>
      <description>URI of server to connect to</description>
    </property>
    <property>
      <name>provisioner.data.dir</name>
      <value>/var/coopr/data/provisioner/data</value>
      <description>Provisioner storage directory for plugin data resources</description>
    </property>
    <property>
      <name>provisioner.work.dir</name>
      <value>/var/coopr/data/provisioner/work</value>
      <description>Provisioner working directory</description>
    </property>
    <property>
      <name>provisioner.register.ip</name>
      <value>127.0.0.1</value>
      <description>Routable IP for the server to call back to this provisioner's API</description>
    </property>
  </configuration>
  
**Notes:** Changes to the ``provisioner-site.xml`` file require a restart of the provisioner.
The provisioner will run with no properties set (an empty ``provisioner-site.xml`` file)
as all properties have defaults set.


Running Provisioner with SSL
^^^^^^^^^^^^^^^^^^^^^^^^^^^^

To configure the provisioner to support server with SSL, add or update this property in ``provisioner-site.xml``::

    <property>
      <name>provisioner.server.uri</name>
      <value>https://localhost:55055</value>
      <description>URI of server to connect to</description>
    </property>


.. highlight:: console


Configuration
^^^^^^^^^^^^^

A full list of available configuration settings and their default values are given below:

.. list-table::
   :header-rows: 1

   * - Config setting
     - Default
     - Description
   * - ``provisioner.server.uri``
     - ``http://localhost:55055``
     - URI of server to connect to
   * - ``provisioner.bind.ip``
     - ``0.0.0.0``
     - Local IP provisioner should listen on
   * - ``provisioner.bind.port``
     - ``55056``
     - Local Port provisioner should listen on
   * - ``provisioner.register.ip``
     - 
     - Routable IP for the server to call back to this provisioner's API; if left empty,
       the provisioner will attempt to auto-detect
   * - ``provisioner.daemonize``
     - ``false``
     - Run provisioner as a daemon. Ensure you also specify a log directory
   * - ``provisioner.data.dir``
     - ``/var/coopr/data/provisioner/data``
     - Provisioner storage directory for plugin data resources
   * - ``provisioner.work.dir``
     - ``/var/coopr/data/provisioner/work``
     - Provisioner working directory
   * - ``provisioner.capacity``
     - ``10``
     - Max number of running workers for this provisioner; assume each worker takes 256mb of memory
   * - ``provisioner.heartbeat.interval``
     - ``10``
     - Interval in seconds to send heartbeat to server
   * - ``provisioner.log.dir``
     -
     - Provisioner log directory
   * - ``provisioner.log.rotation.shift.age``
     - ``7``
     - number of old log files to keep, or frequency of rotation (daily, weekly, or monthly)
   * - ``provisioner.log.rotation.shift.size``
     - ``10485760``
     - maximum logfile size. only applies when shift.age is a number
   * - ``provisioner.log.level``
     - ``info``
     - log level; one of: debug, info, warn, error, fatal

