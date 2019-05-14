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

.. _release-notes-0.9.7:

.. index::
   single: Release Notes

===================
0.9.7 Release Notes
===================

Welcome to Coopr 0.9.7 release. Release notes with links to issue details can be seen from the github project at https://github.com/cdapio/coopr/releases.

Release Theme : Extensibility 
--------------------------------

Release Highlights
------------------
  * Plugin registration to support surfacing of plugin-defined fields for configuring providers and automators
  * Finer grained dependencies for service life-cycle hooks. This allows specifying install time, runtime, and optional dependencies. E.g. You may want some service X to be installed before service Y, or start service X after service Y but only if service Y is installed on the cluster
  * Life-cycle callbacks for clusters for integrating with enterprise assets (e.g. Metering, Monitoring, ...)
  * Ability to add additional configured services to an existing live cluster. (e.g. Create a Hadoop cluster with only HDFS and MapReduce and then later installing HBase on the cluster)
  * Support for starting, stopping and restarting services 
  * Personalizable UI skins
  * More out-of-box cookbooks

    * Apache Hive(tm) support for clusters
    * Enable a secure cluster with support for Kerberos

  * Lot of bug fixes
  * Lot of testing

Change List
-----------
  * Finer Grained Dependencies ( Issues: #1 #70 #87 #96 #149 )
  * Plugin Registry ( Issues: #5 #102 #111 #117 )
  * Updated UI with skins support ( Issues: #9 #10 #160 #188 )
  * Updated Rackspace support ( Issues: #38 #54 #88 #194 )
  * Updated Joyent support ( Issues: #39 #54 ) 
  * Cluster reconfiguration support ( Issues: #55 #64 ) 
  * Hive Support ( Issues: #63 #86 #91 #92 #103 #108 #134 #146 #157 #190 )
  * Add services to existing cluster ( Issues: #65 #66 #69 #78 #79 #83 )
  * Start/Stop/Restart services ( Issues: #71 #79 )
  * Prevent database column overflow ( Issues: #81 )
  * Lifecycle callback hooks ( Issues: #93 )
  * Kerberos client and server support ( Issues: #94 #99 )
  * Secure Hadoop cluster support ( Issues: #95 #97 #99 #110 #144 #150 )
  * Update /etc/hosts support for DNS Suffix ( Issues: #107 )
  * Heartbleed OpenSSL security fix for Rackspace/Joyent images ( Issues: #114 #118 )
  * Rename Chef Automator to Chef Solo Automator ( Issues: #124 )
  * Solver performance improvements ( Issues: #147 )
  * Remove redundant example directory ( Issues: #148 )
  * Add service list to node properties ( Issues: #172 #177 )
  * Upgrade script ( Issues: #195 )

