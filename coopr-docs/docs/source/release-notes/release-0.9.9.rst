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

.. _release-notes-0.9.9:

.. index::
   single: Release Notes

===================
0.9.9 Release Notes
===================

Welcome to the Coopr 0.9.9 release. Release notes with links to issue details can be seen
from the github project at https://github.com/caskdata/coopr/releases.

Release Highlights
------------------
  * Docker support, ability to deploy Docker services and manage Docker containers
  * Provider plugin for Digital Ocean added
  * New UI, rewritten to be more modular and extensible
  * CLI tool, allows interaction via simple commands
  * Split Server APIs into internal and external, served on different ports
  * Node usage metrics, provides detailed metrics by tenant, user, or template
  * Significant bug and stability fixes to provisioner and server
  * Initial support for versioned templates
  * Initial SSL support for UI and server

  * New Services: Impala, Node.js modules, PHP modules, Docker, Nginx 
  * New Cluster Templates: Docker, MEAN, MongoDB
  * Coopr cookbook updates (coopr_*)

Change List
-----------

.. |JIRA| replace:: https://issues.cask.co/browse/

New Features
............
|JIRA|[COOPR-195] - Entity versioning
|JIRA|[COOPR-199] - Ability to suspend jobs
|JIRA|[COOPR-418] - Add more detailed usage statistics
|JIRA|[COOPR-493] - Add a command line interface
|JIRA|[COOPR-496] - Add some way to expose links for a cluster
|JIRA|[COOPR-497] - Split out sensitive config setting into a separate config file
|JIRA|[COOPR-513] - Add labels to admin entities
|JIRA|[COOPR-531] - Command Line Shell for Coopr similar to CDAP CLI
|JIRA|[COOPR-542] - Add ability to configure UI to use TLS
|JIRA|[COOPR-563] - Support for DigitalOcean provider
|JIRA|[COOPR-567] - Split server APIs to separate ports.
|JIRA|[COOPR-594] - SSH Host-key validation
|JIRA|[COOPR-609] - TLS configuration for Coopr components
|JIRA|[COOPR-611] - Secure communications with SSL
|JIRA|[COOPR-615] - Secure communications with ZooKeeper using SASL
|JIRA|[COOPR-621] - Do not require Internet access for chef-solo automator
|JIRA|[COOPR-672] - Attribute-driven ability to install/upgrade/remove packages
|JIRA|[COOPR-674] - Support for installing Impala on clusters when CDH is installed.
|JIRA|[COOPR-678] - Docker Automator
|JIRA|[COOPR-718] - Attribute-driven coopr_hosts /etc/hosts ordering
|JIRA|[COOPR-737] - Promote angular-based UI to default

Improvements
............
|JIRA|[COOPR-64] - Confirmations should mention item being acted upon
|JIRA|[COOPR-300] - Move lease times away from milliseconds
|JIRA|[COOPR-338] - Improve disk handling for AWS
|JIRA|[COOPR-367] - Standalone should copy loom-site.xml.example into server/conf
|JIRA|[COOPR-500] - Include organization in pom
|JIRA|[COOPR-537] - Add documentation to Cluster Pause feature
|JIRA|[COOPR-545] - Better handling of memory settings for Coopr Server
|JIRA|[COOPR-556] - remove CORS
|JIRA|[COOPR-600] - CLI: doesn't show current user or tenant
|JIRA|[COOPR-601] - CLI: alias ? to TAB
|JIRA|[COOPR-604] - CLI: help command output should be more readable
|JIRA|[COOPR-605] - CLI: help should be context-aware
|JIRA|[COOPR-624] - CLI: output is always in DEBUG
|JIRA|[COOPR-639] - coopr-cli jar is too large (63M)
|JIRA|[COOPR-641] - Update netty-http
|JIRA|[COOPR-647] - Template dropdown should use label or name instead of description
|JIRA|[COOPR-665] - API endpoints should only return a single version
|JIRA|[COOPR-670] - Provisioner info log on create should show image name, not value
|JIRA|[COOPR-675] - refactor provisioner worker
|JIRA|[COOPR-681] - Command Line Interface should provide usage when given invalid command
|JIRA|[COOPR-722] - GCE handle quota exceed error
|JIRA|[COOPR-736] - Update fog to 1.26.0

Bugs
....
|JIRA|[COOPR-487] - Data disk mounting fails on Joyent/CentOS
|JIRA|[COOPR-488] - standalone script doesn't load defaults if restart is used
|JIRA|[COOPR-494] - Place safeguards in ClusterCallback
|JIRA|[COOPR-507] - Updated Coopr Docker images instructions
|JIRA|[COOPR-508] - Enhance Joyent delete to not fail on missing servers
|JIRA|[COOPR-511] - Google provisioner plugin disk names
|JIRA|[COOPR-519] - cdap singlenode logs fill up the root partition
|JIRA|[COOPR-538] - Default sudoers has requiretty on RHEL
|JIRA|[COOPR-544] - ngui - Hide theming feature
|JIRA|[COOPR-553] - Cannot run tests
|JIRA|[COOPR-554] - Server queues broken in HA mode
|JIRA|[COOPR-557] - Flicker on welcome page when clicking on header buttons
|JIRA|[COOPR-585] - Using provider hostnames only when not configured
|JIRA|[COOPR-586] - cluster configs overrides service configs - need to do deep merge
|JIRA|[COOPR-588] - Standalone coopr.sh SSL code has broken startup
|JIRA|[COOPR-599] - coopr-cli doesn't build
|JIRA|[COOPR-603] - CLI: rename "sync plugins" to "sync resources"
|JIRA|[COOPR-608] - Can't run coopr-cli.jar
|JIRA|[COOPR-612] - maven-shade-plugin corrupt coopr-cli jar
|JIRA|[COOPR-613] - UI does not show log messages for failed actions
|JIRA|[COOPR-614] - CDAP singlenode template fails to start on AWS
|JIRA|[COOPR-619] - COOPR ngui shows cluster created message before creating clusters
|JIRA|[COOPR-620] - Auth server doesn't start with jdk 1.7 on secure hadoop/secure cdap cluster
|JIRA|[COOPR-622] - server /status endpoint not closing connection
|JIRA|[COOPR-623] - CLI: quit doesn't quit
|JIRA|[COOPR-625] - CLI: commands do not function when given on command line
|JIRA|[COOPR-626] - Server seems susceptible to hangs/failures when run in an HA setup
|JIRA|[COOPR-642] - [CLI] coopr-cli JAR is huge
|JIRA|[COOPR-644] - [CLI] move tests under coopr-cli directory
|JIRA|[COOPR-645] - Remove old UI integration tests
|JIRA|[COOPR-651] - Update netty-http to 0.8.0
|JIRA|[COOPR-652] - Calls to /status should set "Connection: close" header
|JIRA|[COOPR-660] - Coopr Server does not accept JSON input
|JIRA|[COOPR-661] - Coopr Server builds failing
|JIRA|[COOPR-666] - UI should not force base service on clusters
|JIRA|[COOPR-682] - Unused setting kafka.broker.quorum is added to cdap-site.xml
|JIRA|[COOPR-685] - server leaking zookeeper watches
|JIRA|[COOPR-686] - Coopr UI lease expiration slider broken/dangerous
|JIRA|[COOPR-689] - Dummy provisioner load-mock.sh uses wrong API port
|JIRA|[COOPR-691] - Can't start CLI if Coopr isn't running locally
|JIRA|[COOPR-692] - deleting of GCE hosts without a provider id dangerous
|JIRA|[COOPR-696] - google disk delete issues
|JIRA|[COOPR-700] - standalone data directory should be moved
|JIRA|[COOPR-702] - coopr_base::default fails for vanilla standalone due to users databag
|JIRA|[COOPR-703] - MySQL upgrade SQL script doesn't work
|JIRA|[COOPR-704] - ec2 key fields not populated with provider defaults
|JIRA|[COOPR-705] - coopr-base can interfere with sudo access in vanilla standalone
|JIRA|[COOPR-706] - Scheduling jobs fails after upgrade
|JIRA|[COOPR-709] - Registering provisioner capabilities causes an error
|JIRA|[COOPR-711] - After 0.9.8->0.9.9 upgrade, templates cannot be uploaded
|JIRA|[COOPR-713] - cdap-distributed template failing on hive-metastore db permissions
|JIRA|[COOPR-721] - Provider-specified hostnames cause issues with YARN/Hive
|JIRA|[COOPR-723] - coopr install fails for docker-base template with centos images (yum-epel not found)
|JIRA|[COOPR-724] - coopr install fails for docker-base template with ubuntu 12 images (cannot find apt)
|JIRA|[COOPR-728] - Upstream cacerts.pem dropped some certs
|JIRA|[COOPR-729] - joyent plugin provider fails on Ubuntu for cdap-dist - Failure formatting disk error
|JIRA|[COOPR-730] - joyent plugin provider fails on CentOS - confirm stage fails to unmount disk
|JIRA|[COOPR-731] - Standalone startup script broken
