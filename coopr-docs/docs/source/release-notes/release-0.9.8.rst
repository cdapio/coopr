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

.. _release-notes-0.9.8:

.. index::
   single: Release Notes

===================
0.9.8 Release Notes
===================

Welcome to the Coopr 0.9.8 release. Release notes with links to issue details can be seen from the github project at https://github.com/cdapio/coopr/releases.

Release Theme : Multi-Tenancy
-----------------------------

Release Highlights
------------------
  * Project renamed to Coopr (Cluster Oriented Operations and Provisioning of Resources)
  * Multi-Tenancy added, with each tenant having its own admin, users, and data
  * Dynamic worker assignment, giving superadmins the ability to add or reduce provisioner workers on demand
  * Plugin resource system introduced, giving tenant admins the ablity to add and manage their own resources like their own Chef cookbooks 
  * Provider plugins for Amazon Web Services and Google Compute added
  * Additional cookbooks provided out of the box, including support for MongoDB, simple DNS, and Kerberos
  * Multiple bug fixes

Change List
-----------
  * AWS provider support ( Issues: #22 #322 )
  * MongoDB support for clusters ( Issues: #43 #130 )
  * Cluster size constraint ( Issues: #76 #418 )
  * Remove 'ALL RIGHTS RESERVED' footer ( Issues: #123 #340 )
  * Ruby testing with rspec/rubocop ( Issues: #128 #132 #133 )
  * Nginx support for clusters ( Issues: #131 #323 )
  * Sensu monitoring support ( Issues: #167 )
  * Update 'IP' header on cluster page ( Issues: #204 #341 )
  * Configuration settings renamed ( Issues: #213 )
  * Ability to sync an active cluster's template to its current version ( Issues: #214 )
  * Cluster owner macro ( Issues: #221 )
  * Firewall updates ( Issues: #226 #227 )
  * Memcached support for clusters ( Issues: #237 )
  * Multi Tenancy ( Issues: #239, #253, #277, #284, #292, #325, #326 )
  * Upgrade script ( Issues: #258 )
  * Fix Joyent server delete ( Issues: #272 )
  * Google Compute provider support ( Issues: #273 #474 )
  * Provisioner Multi Tenancy ( Issues: #290 #296 )
  * Updated provider plugin using fog ( Issues: #288 #310 )
  * Queue Metrics ( Issues: #301 )
  * Settable max for clusters and nodes in a tenant ( Issues: #302 )
  * Bootstrap a tenant ( Issues: #304 )
  * Larger HW: xlarge, xxlarge ( Issues: #312 #346 )
  * SSH hangs on some providers ( Issues: #313 #351 )
  * Allow non-root users for SSH ( Issues: #314 #320 )
  * PHP module support ( Issues: #317 )
  * Support for multiple IP addresses ( Issues: #343 #347 )
  * Standalone build updates for multitenancy ( Issues #349 )
  * UI: Add 'sshuser' field to imagetypes ( Issues: #366 )
  * Added 'icon' field to admin entities ( Issues: #371 #431 )
  * Added additional info in get clusters API ( Issues: #401 #430 )
  * UI: fix when a provider type has no admin fields ( Issues: #409 )
  * Provisioner setup script ( Issues: #521 )

