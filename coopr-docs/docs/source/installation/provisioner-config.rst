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

Example
^^^^^^^^
Shown below is an example configuration with settings that you are most likely to change.
::

  <configuration>
    <property>
      <name>provisioner.capacity</name>
      <value>10</value>
      <description>Max number of running workers for this provisioner</description>
    </property>
    <property>
      <name>provisioner.server.uri</name>
      <value>http://localhost:55054</value>
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
  </configuration>

Running Provisioner to use SSL
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

To configure the provisioner to support server with SSL, add or update this property in ``provisioner-site.xml``::

    <property>
      <name>provisioner.server.uri</name>
      <value>https://localhost:55054</value>
      <description>URI of server to connect to</description>
    </property>

To configure the provisioner to support server that uses mutual authentication with SSL,
setup these environment variables:

====================================     ==========================    =======================================
   Environment variable                     Default Value                     Description
====================================     ==========================    =======================================
TRUST_CERT_PATH                             None                        Trusted certificate file location.
TRUST_CERT_PASSWORD                         None                        Trusted certificate password.
====================================     ==========================    =======================================

Configuration
^^^^^^^^^^^^^

A full list of available configuration settings and their default values are given below:

.. list-table::
   :header-rows: 1

   * - Config setting
     - Default
     - Description
   * - provisioner.server.uri
     - http://localhost:55054 
     - URI of server to connect to.
   * - provisioner.bind.ip
     - 0.0.0.0
     - Local IP provisioner should listen on.
   * - provisioner.bind.port
     - 55056
     - Local Port provisioner should listen on.
   * - provisioner.register.ip
     - 
     - Routable IP for the server to call back to this provisioner's API. If left empty, the provisioner will attempt to auto-detect.
   * - provisioner.data.dir
     - /var/coopr/data/provisioner/data
     - Provisioner storage directory for plugin data resources
   * - provisioner.work.dir
     - /var/coopr/data/provisioner/work
     - Provisioner working directory
   * - provisioner.capacity
     - 10
     - Max number of running workers for this provisioner. Assume each worker takes 256mb of memory.
   * - provisioner.heartbeat.interval
     - 10
     - Interval in seconds to send heartbeat to server
