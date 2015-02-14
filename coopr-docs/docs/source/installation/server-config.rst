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

.. server-config-reference:

.. index::
   single: Server Configuration

====================
Server Configuration
====================

.. highlight:: xml

Coopr Server uses Zookeeper for task coordination and a database for persistent data. The
server will work out of the box without any configuration options using an in-process
Zookeeper and an embedded Derby DB; however, we strongly recommend that administrators
supply their own Zookeeper quorum and database outside of Coopr for performance and
maintainability. Below, we indicate how you can supply your own database (in this example,
a MySQL server) for storage with an associated JDBC connector in the server configuration
file.

The Coopr Server needs to be restarted after any configuration changes are made, as they
are not automatically reread.

Zookeeper
=========
The zookeeper quorum, a collection of nodes running instances of Zookeeper, is specified
as a comma-delimited list of ``<host>:<port>``::

  server1:2181,server2:2181,server3:2181

Database
========
Coopr uses JDBC for database access. To provide your own database, and for Coopr to access
it, you must specify a driver, a connection string, a user, and a password, as shown in
the following example.  We also recommend specifying a validation query to be used with
jdbc connection pooling.  The query will change based on which database you are using.  
::

  <?xml version="1.0"?>
  <?xml-stylesheet type="text/xsl" href="configuration.xsl"?>
  <configuration>
    <property>
      <name>server.host</name>
      <value>127.0.0.1</value>
      <description>Specifies the hostname/IP address for the server to bind to</description>
    </property>
    <property>
      <name>server.zookeeper.quorum</name>
      <value>127.0.0.1:2181</value>
      <description>Specifies the zookeeper host:port</description>
    </property>
    <property>
      <name>server.jdbc.driver</name>
      <value>com.mysql.jdbc.Driver</value>
      <description>Specifies DB driver</description>
    </property>
    <property>
      <name>server.jdbc.connection.string</name>
      <value>jdbc:mysql://127.0.0.1:3306/coopr?useLegacyDatetimeCode=false</value>
      <description>Specifies DB connection string</description>
    </property>
    <property>
      <name>server.db.user</name>
      <value>coopr</value>
      <description>DB user</description>
    </property>
    <property>
      <name>server.db.password</name>
      <value>cooprers</value>
      <description>DB user password</description>
    </property>
    <property>
      <name>server.jdbc.validation.query</name>
      <value>SELECT 1</value>
      <description>Query used with connection pools to validate a JDBC connection taken from a pool</description>
    </property>
  </configuration>

Plugin Resource Store
=====================
By default, the server stores plugin resources on the local file system. If you are running multiple servers for
HA, you must either configure the server to write the resources to some shared filesystem like NFS, or you must
supply your own distributed file store. To set the location that plugin resources are written to, add the following
setting to your config. 
::

    <property>
      <name>server.plugin.store.localfilestore.data.dir</name>
      <value>/shared/path</value>
      <description>base path where plugin resources will be written to</description>
    </property>

If you are using an alternate distributed file store, you must provide a class that implements the ``PluginStore`` interface.
See the :doc:`javadocs </javadocs/index>` for more information about the interface. Once you have implemented the interface,
you must build a jar and include it in the lib directory for the server, and edit the following config setting in your config.
::

    <property>
      <name>server.plugin.store.class</name>
      <value>fully qualified cla/shared/path</value>
      <description>fully qualified class name that implements your plugin store</description>
    </property>

For more details about plugin resources, see the :doc:`Plugin Resources Guide </guide/admin/plugin-resources>`.

Callbacks
=========
The Server can be configured to run callbacks before any cluster operation begins, after an
operation succeeds, and after an operation fails. By default, no callbacks are run. Out of the
box, the Server supports sending an HTTP POST request containing cluster and job information to
configurable endpoints. You can also write your own custom callback and plug it in.
See :doc:`Cluster Callbacks </guide/superadmin/callbacks>` for information on how to write your own custom callbacks.

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
      <name>server.callback.http.start.url</name>
      <value>http://host:port/start/path</value>
      <description>URL to send POST request to at the start of a cluster operation</description>
    </property>
    <property>
      <name>server.callback.http.start.triggers</name>
      <value>cluster_create,restart_services,stop_services,cluster_configure_with_restart</value>
      <description>Comma separated list of cluster operations that will trigger the HTTP POST call</description>
    </property>
    <property>
      <name>server.callback.http.failure.url</name>
      <value>http://host:port/failure/path</value>
      <description>URL to send POST request to if a cluster operation fails</description>
    </property>
    <property>
      <name>server.callback.http.failure.triggers</name>
      <value>cluster_create</value>
      <description>Comma separated list of cluster operations that will trigger the HTTP POST call</description>
    </property>
  </configuration>

With the configuration above, a HTTP Post request will be sent to
``http://host:port/start/path`` before the start of every ``CLUSTER_CREATE``,
``RESTART_SERVICES``, ``STOP_SERVICES``, and ``CLUSTER_CONFIGURE_WITH_RESTART`` operation.
If no triggers are given, the request is sent before the start of every cluster operation.
Similarly, a HTTP POST request will be sent to http://host:port/failure/path if a
``CLUSTER_CREATE`` operation fails. Since no success url is given, no request is sent when
cluster operations complete successfully. The full list of cluster operations are::

  CLUSTER_CREATE
  CLUSTER_DELETE
  CLUSTER_CONFIGURE
  CLUSTER_CONFIGURE_WITH_RESTART
  STOP_SERVICES
  START_SERVICES
  RESTART_SERVICES
  ADD_SERVICES


Running a Server with SSL
=========================

SSL configuration requires these steps: (the first two steps are only necessary once per machine)

- Create a java keystore, keeping track of the keystore password and keystore key password
- Put the keystore in place (e.g. in /etc/pki/java/serverkeystore.jks as in the example configuration below)
- Correctly configure the UI (next step)


SSL Configuration
^^^^^^^^^^^^^^^^^

To enable running a server with SSL, add this property to ``coopr-site.xml`` (while the
configuration file path is set in ``/etc/alternatives/...``, the default directory is
``/etc/coopr/conf`` and the configuration files are typically located there)::

  <property>
    <name>server.ssl.enabled</name>
    <value>true</value>
    <description>Whether or not to run the server over SSL</description>
  </property>

Set the following properties in ``coopr-site.xml`` for the keystore paths::

  <!-- Keystore path for external API SSL (server.ssl.enabled) -->
  <property>
    <name>server.ssl.keystore.path</name>
    <value>/etc/pki/java/serverkeystore.jks</value>
    <description>Keystore file location</description>
  </property>

  <!-- Keystore path for internal API SSL (server.tasks.ssl.enabled) -->
  <property>
    <name>server.tasks.ssl.keystore.path</name>
    <value>/etc/pki/java/serverkeystore.jks</value>
    <description>Tasks Keystore file location</description>
  </property>

Modify the values shown above to reflect the paths of your relevant keystores.


Add these properties to ``coopr-security.xml``::

    <!-- keystore info for external API SSL (server.ssl.enabled) -->
    <property>
      <name>server.ssl.keystore.password</name>
      <value>Keystore password</value>
      <description>Keystore password</description>
    </property>
    <property>
      <name>server.ssl.cert.password</name>
      <value>Keystore key password</value>
      <description>Keystore key password</description>
    </property>

    <!-- keystore info for internal API SSL (server.tasks.ssl.enabled) -->
    <property>
      <name>server.tasks.ssl.keystore.password</name>
      <value>Keystore password</value>
      <description>Keystore password</description>
    </property>
    <property>
      <name>server.tasks.ssl.cert.password</name>
      <value>Keystore key password</value>
      <description>Keystore key password</description>
    </property>


.. rubric:: SSL Notes

- The keystore needs to be created before configuring SSL in Coopr. The keystore's passwords 
  and information should be gathered accordingly, in preparation for that.

- The keystore path (``server.ssl.keystore.path``) is not a directory, but rather the full
  path of the keystore file itself, such as ``/etc/pki/java/mykeystore``.

- For the keystore configuration, update the *Keystore path* and replace *Keystore password*
  and *Keystore key password* with the actual file path of the keystore, the keystore
  password and the key password respectively that were used in generating the keystore.

.. role:: raw-html(raw)
   :format: html
   
.. |br-space| replace:: :raw-html:`<br />&nbsp;&nbsp;&nbsp;&nbsp;`

Server Configuration
====================

An alphabetical list of the available configuration settings and their default values:

.. list-table::
   :widths: 30 30 40
   :header-rows: 1

   * - Config setting
     - Default
     - Description

   * - | ``server.callback.class``
     - | ``co.cask.coopr.``
       | ``scheduler.callback.``
       | ``HttpPostClusterCallback``
     - Class to use for executing cluster callbacks

   * - | ``server.callback.``
       | ``http.failure.triggers``
     - all operations
     - Comma-separated list of cluster operations that should trigger an HTTP POST request
       to be sent after the operation fails

   * - | ``server.callback.``
       | ``http.failure.url``
     - 
     - If ``HttpPostClusterCallback`` is in use, URL to send cluster and job information
       to after cluster operations fail; leave unset if no request should be sent

   * - | ``server.callback.``
       | ``http.max.connections``
     - ``100``
     - Maximum number of concurrent http connections for callbacks; if the max is reached, the
       next callback to try and send a request blocks until an open connection frees up

   * - | ``server.callback.``
       | ``http.socket.timeout``
     - ``10000``
     - Socket timeout in milliseconds for HTTP callbacks

   * - | ``server.callback.``
       | ``http.start.triggers``
     - all operations
     - Comma-separated list of cluster operations that should trigger an HTTP POST request
       to be sent before start of the operation

   * - | ``server.callback.``
       | ``http.start.url``
     - 
     - If ``HttpPostClusterCallback`` is in use, URL to send cluster and job information
       to before cluster operations start; leave unset if no request should be sent

   * - | ``server.callback.``
       | ``http.success.triggers``
     - all operations
     - Comma-separated list of cluster operations that should trigger an HTTP POST request
       to be sent after the operation completes successfully

   * - | ``server.callback.``
       | ``http.success.url``
     - 
     - If ``HttpPostClusterCallback`` is in use, URL to send cluster and job information
       to after cluster operations complete successfully; leave unset if no request should
       be sent

   * - | ``server.cluster.``
       | ``cleanup.seconds``
     - ``180``
     - Interval, in seconds, between server housekeeping runs; housekeeping such as timing
       out tasks and expiring clusters

   * - ``server.db.password``
     -  
     - Database password

   * - ``server.db.user``
     - ``coopr``
     - Database user

   * - ``server.host``
     - ``localhost``
     - Hostname/IP address for the server to bind to

   * - | ``server.ids.increment.by``
     - ``1``
     - Along with ``server.ids.start.num``, this setting is used to partition the ID space
       for :doc:`Multi-Datacenter High Availability </guide/bcp/multi-data-center-bcp>`. The
       IDs will increment by this number in a datacenter. All datacenters have to share the
       same value of ``server.ids.increment.by`` to prevent overlapping of IDs. This number
       has to be large enough to enable future datacenter expansion.

   * - | ``server.ids.start.num``
     - ``1``
     - Along with ``server.ids.increment.by``, this setting is used to partition the ID
       space for :doc:`Multi-Datacenter High Availability
       </guide/bcp/multi-data-center-bcp>`. The ID generation in a datacenter will start
       from this number. Each datacenter will need to have a different start number so that
       the IDs do not overlap. All Coopr Servers in a datacenter should share the same value
       of ``server.ids.start.num``.

   * - | ``server.jdbc.``
       | ``connection.string``
     - | ``jdbc:derby:``
       | ``/var/coopr/data/db/coopr;``
       | ``create=true``
     - JDBC connection string to user for database operations

   * - | ``server.jdbc.driver``
     - | ``org.apache.derby.``
       | ``jdbc.EmbeddedDriver``
     - JDBC driver to use for database operations

   * - | ``server.jdbc.``
       | ``max.active.connections``
     - ``100``
     - Maximum active JDBC connections

   * - | ``server.jdbc.``
       | ``validation.query``
     - ``"VALUES 1"`` when using default for ``server.jdbc.driver`` (Derby); ``"null"`` otherwise
     - Validation query used by JDBC connection pool to validate new DB connections;
       MySQL, PostgreSQL, and Microsoft SQL Server can use ``"select 1"``; Oracle can use
       ``"select 1 from dual"``

   * - | ``server.local.data.dir``
     - ``/var/coopr/data``
     - Local data directory that default in-memory Zookeeper and embedded Derby will use

   * - | ``server.metrics.``
       | ``queue.cache.seconds``
     - ``10``
     - Seconds to cache queue metrics in memory before recalculating; queue metrics
       require walking through the queue and are therefore expensive to compute

   * - | ``server.netty.``
       | ``exec.num.threads``
     - ``50``
     - Number of execution threads for the server

   * - | ``server.netty.``
       | ``worker.num.threads``
     - ``20``
     - Number of worker threads for the server

   * - | ``server.node.max.log.length``
     - ``2048``
     - Maximum log size in bytes for capturing stdout and stderr for actions performed on
       cluster nodes; logs longer than set limit will be trimmed from the head of the file

   * - | ``server.node.max.num.actions``
     - ``200``
     - Maximum number of actions saved for a node; oldest action will be removed when
       actions exceeding this limit are performed on a node

   * - | ``server.max.action.retries``
     - ``3``
     - Maximum number of times a task gets retried when it fails

   * - | ``server.max.cluster.size``
     - ``10000``
     - Maximum number of nodes that a given cluster can be created with

   * - | ``server.plugin.store.class``
     - | ``co.cask.coopr.``
       | ``store.provisioner.``
       | ``LocalFilePluginStore``
     - Class to use to store plugin resources

   * - | ``server.plugin.``
       | ``store.localfilestore.``
       | ``data.dir``
     - | ``/var/coopr/data/``
       | ``plugins/resources``
     - Data directory to store plugin resources when using the local file plugin store

   * - ``server.port``
     - ``55054``
     - Port for the server

   * - | ``server.provisioner.``
       | ``request.max.retries``
     - ``2``
     - Maximum number of times to retry a failed request to a provisioner before reassigning
       its workers and deleting it

   * - | ``server.provisioner.``
       | ``request.ms.between.retries``
     - ``500``
     - Milliseconds to wait before retrying a failed request to a provisioner

   * - | ``server.provisioner.``
       | ``request.socket.timeout.ms``
     - ``10000``
     - Socket timeout in milliseconds to use when making requests to provisioners

   * - | ``server.provisioner.``
       | ``timeout.check.interval.secs``
     - ``60``
     - Seconds between checks for timed out provisioners

   * - | ``server.provisioner.``
       | ``timeout.secs``
     - ``120``
     - Seconds to wait for a provisioner heartbeat before moving its workers and deleting it

   * - | ``server.scheduler.``
       | ``run.interval.seconds``
     - ``1``
     - Interval, in seconds, various runs are scheduled on the server

   * - | ``server.solver.num.threads``
     - ``20``
     - Number of threads used for solving cluster layout

   * - ``server.ssl.enabled``
     - ``false``
     - Enable running server with SSL

   * - | ``server.task.``
       | ``timeout.seconds``
     - ``1800``
     - Number of seconds the server will wait before timing out a provisioner task and marking it as failed

   * - | ``server.zookeeper.namespace``
     - ``/coopr``
     - Namespace to use in Zookeeper

   * - | ``server.zookeeper.``
       | ``session.timeout.millis``
     - ``40000``
     - Zookeeper session timeout value in milliseconds

   * - | ``server.zookeeper.quorum``
     - A local value determined by an in-memory Zookeeper
     - Zookeeper quorum for the server
