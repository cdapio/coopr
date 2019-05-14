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
from the github project at https://github.com/cdapio/coopr/releases.

Release Highlights
------------------
  * Docker support, ability to deploy Docker services and manage Docker containers
  * Provider plugin for Digital Ocean added
  * New UI, rewritten to be more modular and extensible
  * CLI tool, allows interaction via simple commands
  * Splits the Server APIs into internal and external APIs, served on different ports
  * Node usage metrics, provides detailed metrics by tenant, user, or template
  * Significant bug and stability fixes to provisioner and server
  * Initial support for versioned templates
  * Initial SSL support for UI and server

  * New Services: Impala, Node.js modules, PHP modules, Docker, Nginx 
  * New Cluster Templates: Docker, MEAN, MongoDB
  * Coopr cookbook updates (coopr_*)
  * :ref:`Upgrade Guide <upgrade-to-0.9.9>` for upgrading Coopr from 0.9.8 to 0.9.9

Change List
-----------

New Features
^^^^^^^^^^^^

  * `COOPR-195 <https://issues.cask.co/browse/COOPR-195>`_ - Entity versioning
  * `COOPR-199 <https://issues.cask.co/browse/COOPR-199>`_ - Ability to suspend jobs
  * `COOPR-418 <https://issues.cask.co/browse/COOPR-418>`_ - Add more detailed usage statistics
  * `COOPR-493 <https://issues.cask.co/browse/COOPR-493>`_ - Add a command line interface
  * `COOPR-496 <https://issues.cask.co/browse/COOPR-496>`_ - Add some way to expose links for a cluster
  * `COOPR-497 <https://issues.cask.co/browse/COOPR-497>`_ - Split out sensitive config setting into a separate config file
  * `COOPR-513 <https://issues.cask.co/browse/COOPR-513>`_ - Add labels to admin entities
  * `COOPR-531 <https://issues.cask.co/browse/COOPR-531>`_ - Command Line Shell for Coopr similar to CDAP CLI
  * `COOPR-542 <https://issues.cask.co/browse/COOPR-542>`_ - Add ability to configure UI to use TLS
  * `COOPR-563 <https://issues.cask.co/browse/COOPR-563>`_ - Support for DigitalOcean provider
  * `COOPR-567 <https://issues.cask.co/browse/COOPR-567>`_ - Split server APIs to separate ports
  * `COOPR-594 <https://issues.cask.co/browse/COOPR-594>`_ - SSH Host-key validation
  * `COOPR-609 <https://issues.cask.co/browse/COOPR-609>`_ - TLS configuration for Coopr components
  * `COOPR-611 <https://issues.cask.co/browse/COOPR-611>`_ - Secure communications with SSL
  * `COOPR-615 <https://issues.cask.co/browse/COOPR-615>`_ - Secure communications with ZooKeeper using SASL
  * `COOPR-621 <https://issues.cask.co/browse/COOPR-621>`_ - Do not require Internet access for chef-solo automator
  * `COOPR-672 <https://issues.cask.co/browse/COOPR-672>`_ - Attribute-driven ability to install/upgrade/remove packages
  * `COOPR-674 <https://issues.cask.co/browse/COOPR-674>`_ - Support for installing Impala on clusters when CDH is installed.
  * `COOPR-678 <https://issues.cask.co/browse/COOPR-678>`_ - Docker Automator
  * `COOPR-718 <https://issues.cask.co/browse/COOPR-718>`_ - Attribute-driven coopr_hosts /etc/hosts ordering
  * `COOPR-737 <https://issues.cask.co/browse/COOPR-737>`_ - Promote angular-based UI to default

Improvements
^^^^^^^^^^^^

  * `COOPR-64 <https://issues.cask.co/browse/COOPR-64>`_ - Confirmations should mention item being acted upon
  * `COOPR-300 <https://issues.cask.co/browse/COOPR-300>`_ - Move lease times away from milliseconds
  * `COOPR-338 <https://issues.cask.co/browse/COOPR-338>`_ - Improve disk handling for AWS
  * `COOPR-367 <https://issues.cask.co/browse/COOPR-367>`_ - Standalone should copy loom-site.xml.example into server/conf
  * `COOPR-500 <https://issues.cask.co/browse/COOPR-500>`_ - Include organization in pom
  * `COOPR-537 <https://issues.cask.co/browse/COOPR-537>`_ - Add documentation to Cluster Pause feature
  * `COOPR-545 <https://issues.cask.co/browse/COOPR-545>`_ - Better handling of memory settings for Coopr Server
  * `COOPR-556 <https://issues.cask.co/browse/COOPR-556>`_ - remove CORS
  * `COOPR-600 <https://issues.cask.co/browse/COOPR-600>`_ - CLI: doesn't show current user or tenant
  * `COOPR-601 <https://issues.cask.co/browse/COOPR-601>`_ - CLI: alias ? to TAB
  * `COOPR-604 <https://issues.cask.co/browse/COOPR-604>`_ - CLI: help command output should be more readable
  * `COOPR-605 <https://issues.cask.co/browse/COOPR-605>`_ - CLI: help should be context-aware
  * `COOPR-624 <https://issues.cask.co/browse/COOPR-624>`_ - CLI: output is always in DEBUG
  * `COOPR-639 <https://issues.cask.co/browse/COOPR-639>`_ - coopr-cli jar is too large (63M)
  * `COOPR-641 <https://issues.cask.co/browse/COOPR-641>`_ - Update netty-http
  * `COOPR-647 <https://issues.cask.co/browse/COOPR-647>`_ - Template dropdown should use label or name instead of description
  * `COOPR-665 <https://issues.cask.co/browse/COOPR-665>`_ - API endpoints should only return a single version
  * `COOPR-670 <https://issues.cask.co/browse/COOPR-670>`_ - Provisioner info log on create should show image name, not value
  * `COOPR-675 <https://issues.cask.co/browse/COOPR-675>`_ - refactor provisioner worker
  * `COOPR-681 <https://issues.cask.co/browse/COOPR-681>`_ - Command Line Interface should provide usage when given invalid command
  * `COOPR-722 <https://issues.cask.co/browse/COOPR-722>`_ - GCE handle quota exceed error
  * `COOPR-736 <https://issues.cask.co/browse/COOPR-736>`_ - Update fog to 1.26.0

Bugs
^^^^

  * `COOPR-487 <https://issues.cask.co/browse/COOPR-487>`_ - Data disk mounting fails on Joyent/CentOS
  * `COOPR-488 <https://issues.cask.co/browse/COOPR-488>`_ - standalone script doesn't load defaults if restart is used
  * `COOPR-494 <https://issues.cask.co/browse/COOPR-494>`_ - Place safeguards in ClusterCallback
  * `COOPR-507 <https://issues.cask.co/browse/COOPR-507>`_ - Updated Coopr Docker images instructions
  * `COOPR-508 <https://issues.cask.co/browse/COOPR-508>`_ - Enhance Joyent delete to not fail on missing servers
  * `COOPR-511 <https://issues.cask.co/browse/COOPR-511>`_ - Google provisioner plugin disk names
  * `COOPR-519 <https://issues.cask.co/browse/COOPR-519>`_ - cdap singlenode logs fill up the root partition
  * `COOPR-538 <https://issues.cask.co/browse/COOPR-538>`_ - Default sudoers has requiretty on RHEL
  * `COOPR-544 <https://issues.cask.co/browse/COOPR-544>`_ - NGUI: Hide theming feature
  * `COOPR-553 <https://issues.cask.co/browse/COOPR-553>`_ - Cannot run tests
  * `COOPR-554 <https://issues.cask.co/browse/COOPR-554>`_ - Server queues broken in HA mode
  * `COOPR-557 <https://issues.cask.co/browse/COOPR-557>`_ - Flicker on welcome page when clicking on header buttons
  * `COOPR-585 <https://issues.cask.co/browse/COOPR-585>`_ - Using provider hostnames only when not configured
  * `COOPR-586 <https://issues.cask.co/browse/COOPR-586>`_ - cluster configs overrides service configs, need to do deep merge
  * `COOPR-588 <https://issues.cask.co/browse/COOPR-588>`_ - Standalone coopr.sh SSL code has broken startup
  * `COOPR-599 <https://issues.cask.co/browse/COOPR-599>`_ - coopr-cli doesn't build
  * `COOPR-603 <https://issues.cask.co/browse/COOPR-603>`_ - CLI: rename "sync plugins" to "sync resources"
  * `COOPR-608 <https://issues.cask.co/browse/COOPR-608>`_ - Can't run coopr-cli.jar
  * `COOPR-612 <https://issues.cask.co/browse/COOPR-612>`_ - maven-shade-plugin corrupt coopr-cli jar
  * `COOPR-613 <https://issues.cask.co/browse/COOPR-613>`_ - UI does not show log messages for failed actions
  * `COOPR-614 <https://issues.cask.co/browse/COOPR-614>`_ - CDAP singlenode template fails to start on AWS
  * `COOPR-619 <https://issues.cask.co/browse/COOPR-619>`_ - COOPR ngui shows cluster created message before creating clusters
  * `COOPR-620 <https://issues.cask.co/browse/COOPR-620>`_ - Auth server doesn't start with jdk 1.7 on secure hadoop/secure cdap cluster
  * `COOPR-622 <https://issues.cask.co/browse/COOPR-622>`_ - server /status endpoint not closing connection
  * `COOPR-623 <https://issues.cask.co/browse/COOPR-623>`_ - CLI: quit doesn't quit
  * `COOPR-625 <https://issues.cask.co/browse/COOPR-625>`_ - CLI: commands do not function when given on command line
  * `COOPR-626 <https://issues.cask.co/browse/COOPR-626>`_ - Server seems susceptible to hangs/failures when run in an HA setup
  * `COOPR-642 <https://issues.cask.co/browse/COOPR-642>`_ - CLI coopr-cli JAR is huge
  * `COOPR-644 <https://issues.cask.co/browse/COOPR-644>`_ - CLI move tests under coopr-cli directory
  * `COOPR-645 <https://issues.cask.co/browse/COOPR-645>`_ - Remove old UI integration tests
  * `COOPR-651 <https://issues.cask.co/browse/COOPR-651>`_ - Update netty-http to 0.8.0
  * `COOPR-652 <https://issues.cask.co/browse/COOPR-652>`_ - Calls to /status should set "Connection: close" header
  * `COOPR-660 <https://issues.cask.co/browse/COOPR-660>`_ - Coopr Server does not accept JSON input
  * `COOPR-661 <https://issues.cask.co/browse/COOPR-661>`_ - Coopr Server builds failing
  * `COOPR-666 <https://issues.cask.co/browse/COOPR-666>`_ - UI should not force base service on clusters
  * `COOPR-682 <https://issues.cask.co/browse/COOPR-682>`_ - Unused setting kafka.broker.quorum is added to cdap-site.xml
  * `COOPR-685 <https://issues.cask.co/browse/COOPR-685>`_ - server leaking zookeeper watches
  * `COOPR-686 <https://issues.cask.co/browse/COOPR-686>`_ - Coopr UI lease expiration slider broken/dangerous
  * `COOPR-689 <https://issues.cask.co/browse/COOPR-689>`_ - Dummy provisioner load-mock.sh uses wrong API port
  * `COOPR-691 <https://issues.cask.co/browse/COOPR-691>`_ - Can't start CLI if Coopr isn't running locally
  * `COOPR-692 <https://issues.cask.co/browse/COOPR-692>`_ - deleting of GCE hosts without a provider id dangerous
  * `COOPR-696 <https://issues.cask.co/browse/COOPR-696>`_ - google disk delete issues
  * `COOPR-700 <https://issues.cask.co/browse/COOPR-700>`_ - standalone data directory should be moved
  * `COOPR-702 <https://issues.cask.co/browse/COOPR-702>`_ - coopr_base::default fails for vanilla standalone due to users databag
  * `COOPR-703 <https://issues.cask.co/browse/COOPR-703>`_ - MySQL upgrade SQL script doesn't work
  * `COOPR-704 <https://issues.cask.co/browse/COOPR-704>`_ - ec2 key fields not populated with provider defaults
  * `COOPR-705 <https://issues.cask.co/browse/COOPR-705>`_ - coopr-base can interfere with sudo access in vanilla standalone
  * `COOPR-706 <https://issues.cask.co/browse/COOPR-706>`_ - Scheduling jobs fails after upgrade
  * `COOPR-709 <https://issues.cask.co/browse/COOPR-709>`_ - Registering provisioner capabilities causes an error
  * `COOPR-711 <https://issues.cask.co/browse/COOPR-711>`_ - After 0.9.8->0.9.9 upgrade, templates cannot be uploaded
  * `COOPR-713 <https://issues.cask.co/browse/COOPR-713>`_ - cdap-distributed template failing on hive-metastore db permissions
  * `COOPR-721 <https://issues.cask.co/browse/COOPR-721>`_ - Provider-specified hostnames cause issues with YARN/Hive
  * `COOPR-723 <https://issues.cask.co/browse/COOPR-723>`_ - coopr install fails for docker-base template with centos images (yum-epel not found)
  * `COOPR-724 <https://issues.cask.co/browse/COOPR-724>`_ - coopr install fails for docker-base template with ubuntu 12 images (cannot find apt)
  * `COOPR-728 <https://issues.cask.co/browse/COOPR-728>`_ - Upstream cacerts.pem dropped some certs
  * `COOPR-729 <https://issues.cask.co/browse/COOPR-729>`_ - joyent plugin provider fails on Ubuntu for cdap-dist: Failure formatting disk error
  * `COOPR-730 <https://issues.cask.co/browse/COOPR-730>`_ - joyent plugin provider fails on CentOS: confirm stage fails to unmount disk
  * `COOPR-731 <https://issues.cask.co/browse/COOPR-731>`_ - Standalone startup script broken


Known Issues
------------

  * Provisioner: non-fatal sudo errors may be seen prior to coopr_hosts (part of base service) being run
  * Node usage API: Coopr Node Usage Hours 'now-Nx' does not work                                         `COOPR-714 <https://issues.cask.co/browse/COOPR-714>`_
  * Node usage API: API allows arbitrary, random parameters to be used as keys                            `COOPR-716 <https://issues.cask.co/browse/COOPR-716>`_
  * UI: If base is removed from selected roles/services prior to install, it still gets installed         `COOPR-666 <https://issues.cask.co/browse/COOPR-666>`_
  * UI: Unable to edit providers with multiple required field sets                                        `COOPR-712 <https://issues.cask.co/browse/COOPR-714>`_
  * UI: Service Constraints fields do not display entered value during Template creation                  `COOPR-739 <https://issues.cask.co/browse/COOPR-739>`_
