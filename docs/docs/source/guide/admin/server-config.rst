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

.. server-config-reference:

.. index::
   single: Server Configuration

====================
Server Configuration
====================

.. include:: /guide/admin/admin-links.rst

Configuring the server
----------------------

Loom server uses Zookeeper for task coordination and a database for persistent data. The server will work out of the box
without any configuration options with an in process Zookeeper and an embedded Derby DB; however, we 
strongly recommend that administrators supply their own Zookeeper quorum and database outside of Continuuity Loom for performance and
maintainability. Below we indicate how you can supply your own database (in this case MySQL server) for storage, 
and the associated JDBC connector in the server configuration file.

Zookeeper
^^^^^^^^^
The zookeeper quorum, a collection of nodes running instances of Zookeeper, is specified as a comma delimited list of ``<host>:<port>`` (e.g. ``server1:2181,server2:2181,server3:2181``).

Database
^^^^^^^^
Continuuity Loom uses JDBC for database access. To provide your own database, and for Continuuity Loom to access it, you must specify a driver, a connection string,
a user, and a password, as shown in the following example.  We also recommend specifying a validation query to be used with jdbc connection 
pooling.  The query will change based on which database you are using.  
::

  <?xml version="1.0"?>
  <?xml-stylesheet type="text/xsl" href="configuration.xsl"?>
  <configuration>
    <property>
      <name>loom.host</name>
      <value>127.0.0.1</value>
      <description>Specifies the hostname/IP address for the server to bind to</description>
    </property>
    <property>
      <name>zookeeper.quorum</name>
      <value>127.0.0.1:2181</value>
      <description>Specifies the zookeeper host:port</description>
    </property>
    <property>
      <name>loom.jdbc.driver</name>
      <value>com.mysql.jdbc.Driver</value>
      <description>Specifies DB driver</description>
    </property>
    <property>
      <name>loom.jdbc.connection.string</name>
      <value>jdbc:mysql://127.0.0.1:3306/loom?useLegacyDatetimeCode=false</value>
      <description>Specifies DB connection string</description>
    </property>
    <property>
      <name>loom.db.user</name>
      <value>loom</value>
      <description>DB user</description>
    </property>
    <property>
      <name>loom.db.password</name>
      <value>loomers</value>
      <description>DB user password</description>
    </property>
    <property>
      <name>loom.jdbc.validation.query</name>
      <value>SELECT 1</value>
      <description>query used with connection pools to validate a jdbc connection taken from a pool</description>
    </property>
  </configuration>

Callbacks
^^^^^^^^^
The Server can be configured to run callbacks before any cluster operation begins, after an
operation succeeds, and after an operation fails. By default, no callbacks are run. Out of the
box, the Server supports sending an HTTP POST request containing cluster and job information to
configurable endpoints. You can also write your own custom callback and plug it in.
See :doc:`Cluster Callbacks </guide/admin/callbacks>` for more information on how to write your own custom callbacks.

To enable HTTP POST callbacks you must specify a url to send the request to.  There is a configuration
setting for the url to use on start, success, and failure of a cluster operation. If a url is not given,
no request will be sent. By default, a request will be for every type of cluster operation, but the Server
can be configured to only send the request for certain types of cluster operations by providing a comma
separated list of operations in the configuration. An example of configuration settings is shown below.
::

  <?xml version="1.0"?>
  <?xml-stylesheet type="text/xsl" href="configuration.xsl"?>
  <configuration>
    <property>
      <name>loom.callback.http.start.url</name>
      <value>http://host:port/start/path</value>
      <description>URL to send POST request to at the start of a cluster operation</description>
    </property>
    <property>
      <name>loom.callback.http.start.triggers</name>
      <value>cluster_create,restart_services,stop_services,cluster_configure_with_restart</value>
      <description>comma separated list of cluster operations that will trigger the HTTP POST call</description>
    </property>
    <property>
      <name>loom.callback.http.failure.url</name>
      <value>http://host:port/failure/path</value>
      <description>URL to send POST request to if a cluster operation fails</description>
    </property>
    <property>
      <name>loom.callback.http.failure.triggers</name>
      <value>cluster_create</value>
      <description>comma separated list of cluster operations that will trigger the HTTP POST call</description>
    </property>
  </configuration>

With the configuration above, a HTTP Post request will be sent to http://host:port/start/path before the start of every 
CLUSTER_CREATE, RESTART_SERVICES, STOP_SERVICES, and CLUSTER_CONFIGURE_WITH_RESTART operation. If no triggers are given, 
the request is sent before the start of every cluster operation. Similarly, a HTTP POST request will be sent to 
http://host:port/failure/path if a CLUSTER_CREATE operation fails. Since no success url is given, no request is sent when
cluster operations complete successfully. The full list of cluster operations are: 
CLUSTER_CREATE, CLUSTER_DELETE, CLUSTER_CONFIGURE, CLUSTER_CONFIGURE_WITH_RESTART, STOP_SERVICES, START_SERVICES, 
RESTART_SERVICES, and ADD_SERVICES. 

Configuration
^^^^^^^^^^^^^

A full list of available configuration settings and their default values are given below:

.. list-table::
   :header-rows: 1

   * - Config setting
     - Default
     - Description
   * - zookeeper.quorum
     - A local value determined by an in-memory Zookeeper.
     - Zookeeper quorum for the server.
   * - zookeeper.namespace
     - "/loom"
     - Namespace to use in Zookeeper
   * - zookeeper.session.timeout.millis
     - 40000
     - Zookeeper session timeout value in milliseconds.
   * - loom.port
     - 55054
     - Port for the server.
   * - loom.host
     - "localhost"
     - Hostname/IP address for the server to bind to.
   * - loom.jdbc.driver
     - org.apache.derby.jdbc.EmbeddedDriver
     - JDBC driver to use for database operations.
   * - loom.jdbc.connection.string
     - "jdbc:derby:/var/loom/data/db/loom;create=true"
     - JDBC connection string to user for database operations.
   * - loom.jdbc.validation.query
     - "VALUES 1" when using default for loom.jdbc.driver (Derby), null otherwise.
     - Validation query used by JDBC connection pool to validate new DB connections.  mysql, postgres, and microsoft sql server can use "select 1".  oracle can use "select 1 from dual".
   * - loom.jdbc.max.active.connections
     - 100
     - Maximum active JDBC connections.
   * - loom.db.user
     - "loom"
     - Database user.
   * - loom.db.password
     - null
     - Database password.
   * - loom.solver.num.threads
     - 20
     - Number of threads used for solving cluster layout.
   * - loom.local.data.dir
     - "/var/loom/data"
     - Local data directory that default in-memory Zookeeper and embedded Derby will use.
   * - loom.task.timeout.seconds
     - 1800
     - Number of seconds the server will wait before timing out a provisioner task and marking it as failed.
   * - loom.cluster.cleanup.seconds
     - 180
     - Interval, in seconds, between server housekeeping runs. Housekeeping includes timing out tasks, expiring clusters, etc.
   * - loom.netty.exec.num.threads
     - 50
     - Number of execution threads for the server.
   * - loom.netty.worker.num.threads
     - 20
     - Number of worker threads for the server.
   * - loom.node.max.log.length
     - 2048
     - Maximum log size in bytes for capturing stdout and stderr for actions performed on cluster nodes. Logs longer than set limit will be trimmed from the head of the file.
   * - loom.node.max.num.actions
     - 200
     - Maximum number of actions saved for a node. Oldest action will be removed when actions exceeding this limit are performed on a node.
   * - loom.max.cluster.size
     - 10000
     - Maximum number of nodes that a given cluster can be created with.
   * - loom.max.action.retries
     - 3
     - Maximum number of times a task gets retried when it fails.
   * - scheduler.run.interval.seconds
     - 1
     - Interval, in seconds, various runs are scheduled on the server.
   * - loom.ids.start.num
     - 1
     - Along with ``loom.ids.increment.by``, this setting is used to partition the ID space for :doc:`Multi-Datacenter High Availability </guide/bcp/multi-data-center-bcp>`. The ID generation in a datacenter will start from this number. Each datacenter will need to have a different start number so that the IDs do not overlap. All Loom Servers in a datacenter should share the same value of ``loom.ids.start.num``.
   * - loom.ids.increment.by
     - 1
     - Along with ``loom.ids.start.num``, this setting is used to partition the ID space for :doc:`Multi-Datacenter High Availability </guide/bcp/multi-data-center-bcp>`. The IDs will increment by this number in a datacenter. All datacenters have to share the same value of ``loom.ids.increment.by`` to prevent overlapping of IDs. This number has to be large enough to enable future datacenter expansion.
   * - loom.callback.class 
     - com.continuuity.loom.scheduler.callback.HttpPostClusterCallback
     - Class to use for executing cluster callbacks.
   * - loom.callback.http.start.url
     - none
     - If HttpPostClusterCallback is in use, url to send cluster and job information to before cluster operations start. Leave unset if no request should be sent.
   * - loom.callback.http.success.url 
     - none
     - If HttpPostClusterCallback is in use, url to send cluster and job information to after cluster operations complete successfully. Leave unset if no request should be sent.
   * - loom.callback.http.failure.url 
     - none
     - If HttpPostClusterCallback is in use, url to send cluster and job information to after cluster operations fail. Leave unset if no request should be sent.
   * - loom.callback.http.start.triggers
     - all operations
     - comma separated list of cluster operations that should trigger an HTTP POST request to be sent before start of the operation. 
   * - loom.callback.http.success.triggers
     - all operations
     - comma separated list of cluster operations that should trigger an HTTP POST request to be sent after the operation completes successfully. 
   * - loom.callback.http.failure.triggers
     - all operations
     - comma separated list of cluster operations that should trigger an HTTP POST request to be sent after the operation fails. 
   * - loom.callback.http.socket.timeout 
     - 10000
     - socket timeout in milliseconds for http callbacks. 
   * - loom.callback.http.max.connections
     - 100
     - max number of concurrent http connections for callbacks. If the max is reached, the next callback to try and send a request blocks until an open connection frees up.
